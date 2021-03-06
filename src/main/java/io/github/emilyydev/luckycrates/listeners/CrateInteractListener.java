//
// This file is part of LuckyCrates, licensed under the MIT License.
//
// Copyright (c) 2021 emilyy-dev
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in all
// copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// SOFTWARE.
//

package io.github.emilyydev.luckycrates.listeners;

import io.github.emilyydev.luckycrates.LuckyCratesPlugin;
import io.github.emilyydev.luckycrates.config.ConfigAdapter;
import io.github.emilyydev.luckycrates.config.ConfigKeys;
import io.github.emilyydev.luckycrates.internal.CrateType;
import io.github.emilyydev.luckycrates.internal.Loot;
import io.github.emilyydev.luckycrates.internal.SpinningCrate;
import io.github.emilyydev.luckycrates.messages.Message;
import io.github.emilyydev.luckycrates.util.NodeResolver;
import net.milkbowl.vault.chat.Chat;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class CrateInteractListener {

  private final LuckyCratesPlugin plugin;
  private final ConfigAdapter configAdapter;
  private final Chat vaultChat;

  public CrateInteractListener(final LuckyCratesPlugin plugin, final Chat vaultChat) {
    this.plugin = plugin;
    this.configAdapter = plugin.getConfigAdapter();
    this.vaultChat = vaultChat;
    plugin.registerListener(PlayerInteractAtEntityEvent.class, this::onCrateInteract);
  }

  private void onCrateInteract(final PlayerInteractAtEntityEvent event) {
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
    if (permission.isPresent()) {
      if (NodeResolver.checkNode(player, permission.get(), this.vaultChat)) {
        Message.LEGACY.send(this.plugin.getSubjectFactory().player(player), this.configAdapter.get(ConfigKeys.NO_PERM_MESSAGE));
        return;
      }
    }

    if (crate.shouldDisappear()) {
      final long hiddenUntil = System.currentTimeMillis() + crateType.getRandomTime() * 1000L;
      crate.setHiddenUntil(hiddenUntil);

      try {
        this.plugin.getCratesMap().save();
      } catch (final IOException exception) {
        this.plugin.getLogger().severe("Could not save data file!");
        exception.printStackTrace();
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

    new BukkitRunnable() {

      private int index = 0;

      @Override
      public void run() {
        while (true) {
          if (this.index < randomLoot.items.size()) {
            if (randomLoot.items.get(this.index) == null || randomLoot.items.get(this.index).getType() == Material.AIR) {
              ++this.index;
              continue;
            }

            player.getWorld().dropItem(crate.getLocation().add(0.0, 1.0, 0.0), randomLoot.items.get(this.index++))
                  .setVelocity(new Vector()); // [0.0, 0.0, 0.0]
            player.playSound(player.getLocation(), Sound.BLOCK_TRIPWIRE_CLICK_OFF, SoundCategory.MASTER, 1.0f, 1.0f);

            return;
          }

          cancel();
          return;
        }
      }
    }.runTaskTimer(this.plugin, 0L, 20L / 3L);
  }
}
