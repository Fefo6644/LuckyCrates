package me.fefo.luckycrates.listeners;

import me.fefo.luckycrates.LuckyCrates;
import me.fefo.luckycrates.SpinnyCrate;
import me.fefo.luckycrates.util.ColorFormat;
import me.fefo.luckycrates.util.CrateData;
import me.fefo.luckycrates.util.Loot;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.UUID;

public final class CrateInteractListener implements Listener {
  private final LuckyCrates plugin;

  public CrateInteractListener(LuckyCrates plugin) { this.plugin = plugin; }

  @EventHandler
  public void onCrateInteract(@NotNull PlayerInteractAtEntityEvent e) {
    if (plugin.spinnyCrates.size() > 0) {
      final Entity entity = e.getRightClicked();
      final UUID uuid = entity.getUniqueId();

      if (entity instanceof ArmorStand) {
        if (plugin.spinnyCrates.containsKey(uuid)) {
          e.setCancelled(true);
          if (plugin.playersRemovingCrate.contains(e.getPlayer().getUniqueId())) {
            return;
          }

          final SpinnyCrate sc = plugin.spinnyCrates.get(uuid);
          if (sc.getHiddenUntil() == 0L) {
            final CrateData cratePicked = SpinnyCrate.categorisedCrates.get(sc.getCrateName());
            if (cratePicked.requiresPerm() &&
                !e.getPlayer().hasPermission(cratePicked.getPermRequired())) {
              e.getPlayer().sendMessage(ColorFormat.format(plugin.getConfig()
                                                                 .getString("noPermMessage")));
              return;
            }

            if (sc.shouldDisappear()) {
              final long hiddenUntil = Instant.now().toEpochMilli() + cratePicked.getRandomTime();
              sc.setHiddenUntil(hiddenUntil);
              final ConfigurationSection cs = plugin.cratesDataYaml.getConfigurationSection(sc.getUUID().toString());
              cs.set(LuckyCrates.YAML_HIDDEN_UNTIL, hiddenUntil);
              try {
                plugin.cratesDataYaml.save(plugin.cratesDataFile);
              } catch (IOException ex) {
                plugin.getLogger().severe("Could not save data file!");
                ex.printStackTrace();
              }
            }

            final Player player = e.getPlayer();
            final Loot randomLoot = cratePicked.getRandomLoot();

            for (final String command : randomLoot.commands) {
              plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(),
                                                 command.replace("%player%", player.getName()));
            }

            player.playSound(player.getLocation(),
                             Sound.ENTITY_EXPERIENCE_ORB_PICKUP,
                             SoundCategory.MASTER,
                             1.0f, 1.0f);

            if (player.hasPermission(plugin.getConfig().getString("instantGivePerm"))) {
              final HashMap<Integer, ItemStack> notStoredItems = player.getInventory().addItem(randomLoot.items);
              for (final ItemStack item : notStoredItems.values()) {
                player.getWorld()
                      .dropItem(player.getEyeLocation()
                                      .subtract(0.0, 1.0, 0.0),
                                item);
              }
            } else {
              new BukkitRunnable() {
                int index = 0;

                @Override
                public void run() {
                  if (index < randomLoot.items.length) {
                    if (randomLoot.items[index] == null ||
                        randomLoot.items[index].getType().equals(Material.AIR)) {
                      ++index;
                      return;
                    }

                    player.getWorld()
                          .dropItem(sc.getLocation()
                                      .add(0, 1, 0),
                                    randomLoot.items[index++])
                          .setVelocity(new Vector());
                    player.playSound(player.getLocation(),
                                     Sound.BLOCK_TRIPWIRE_CLICK_OFF,
                                     SoundCategory.MASTER,
                                     1.0f, 1.0f);
                  } else {
                    cancel();
                  }
                }
              }.runTaskTimer(plugin, 0, 20 / 3);
            }
          }
        }
      }
    }
  }
}
