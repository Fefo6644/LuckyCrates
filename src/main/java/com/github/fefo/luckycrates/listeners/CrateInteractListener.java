//
// LuckyCrates - Better your KitPvP world with some fancy lootcrates.
// Copyright (C) 2021  Fefo6644 <federico.lopez.1999@outlook.com>
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <https://www.gnu.org/licenses/>.
//

package com.github.fefo.luckycrates.listeners;

import com.github.fefo.luckycrates.LuckyCratesPlugin;
import com.github.fefo.luckycrates.config.ConfigAdapter;
import com.github.fefo.luckycrates.config.ConfigKeys;
import com.github.fefo.luckycrates.internal.CrateType;
import com.github.fefo.luckycrates.internal.Loot;
import com.github.fefo.luckycrates.internal.SpinningCrate;
import com.github.fefo.luckycrates.messages.Message;
import com.github.fefo.luckycrates.util.TaskScheduler;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

public final class CrateInteractListener implements Listener {

  private final LuckyCratesPlugin plugin;
  private final TaskScheduler scheduler;
  private final ConfigAdapter configAdapter;

  public CrateInteractListener(final LuckyCratesPlugin plugin) {
    this.plugin = plugin;
    this.scheduler = plugin.getScheduler();
    this.configAdapter = plugin.getConfigAdapter();
  }

  @EventHandler
  public void onCrateInteract(final PlayerInteractAtEntityEvent event) {
    if (this.plugin.getCratesMap().isEmpty()) {
      return;
    }

    final Player player = event.getPlayer();
    final Entity entity = event.getRightClicked();
    final UUID uuid = entity.getUniqueId();

    if (entity.getType() != EntityType.ARMOR_STAND || !this.plugin.getCratesMap().containsKey(uuid)) {
      return;
    }

    event.setCancelled(true);
    if (this.plugin.getInteractantsHandler().isRemoving(player.getUniqueId())) {
      return;
    }

    final SpinningCrate crate = this.plugin.getCratesMap().get(uuid);
    if (crate.getHiddenUntil() > System.currentTimeMillis()) {
      return;
    }

    final CrateType crateType = this.plugin.getCratesMap().getCrateType(crate.getType());
    final Optional<String> permission = crateType.getPermission();
    if (!(permission.map(player::hasPermission).orElse(true))) {
      Message.LEGACY.send(this.plugin.getSubjectFactory().player(player), this.configAdapter.get(ConfigKeys.NO_PERM_MESSAGE));
      return;
    }

    if (crate.shouldDisappear()) {
      final long hiddenUntil = System.currentTimeMillis() + crateType.getRandomTime() * 1000L;
      crate.setHiddenUntil(hiddenUntil);

      try {
        this.plugin.getCratesMap().save();
      } catch (final IOException exception) {
        this.plugin.getSLF4JLogger().error("Could not save data file!", exception);
      }
    }

    final Loot randomLoot = crateType.getRandomLoot();

    for (final String command : randomLoot.commands) {
      Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("%player%", player.getName()));
    }

    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.MASTER, 1.0f, 1.0f);

    if (player.hasPermission(this.configAdapter.get(ConfigKeys.INSTANT_GIVE_PERM))) {
      final Map<Integer, ItemStack> notStored = player.getInventory().addItem(randomLoot.items.toArray(new ItemStack[0]));
      final Location spawnLocation = player.getEyeLocation().subtract(0.0, 1.0, 0.0);
      final World world = player.getWorld();
      for (final ItemStack item : notStored.values()) {
        world.dropItem(spawnLocation, item);
      }
      return;
    }

    // amazing pre-1.13
    final AtomicReference<BukkitTask> task = new AtomicReference<>();
    task.set(this.scheduler.sync(new Runnable() {

      private int index = 0;

      @Override
      public void run() {
        while (true) {
          if (this.index < randomLoot.items.size()) {
            if (randomLoot.items.get(this.index) == null ||
                randomLoot.items.get(this.index).getType() == Material.AIR) {
              ++this.index;
              continue;
            }

            player.getWorld().dropItem(crate.getLocation().add(0.0, 1.0, 0.0), randomLoot.items.get(this.index++))
                  .setVelocity(new Vector()); // [0.0, 0.0, 0.0]
            player.playSound(player.getLocation(), Sound.BLOCK_TRIPWIRE_CLICK_OFF, SoundCategory.MASTER, 1.0f, 1.0f);

            return;
          }

          if (task.get() != null) {
            task.get().cancel();
          }

          return;
        }
      }
    }, 0L, 20L / 3L));
  }
}