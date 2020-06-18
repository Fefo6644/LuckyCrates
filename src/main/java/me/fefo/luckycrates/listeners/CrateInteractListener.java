package me.fefo.luckycrates.listeners;

import me.fefo.luckycrates.Main;
import me.fefo.luckycrates.SpinnyCrate;
import me.fefo.luckycrates.utils.CrateData;
import me.fefo.luckycrates.utils.Loot;
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
  private final Main main;

  public CrateInteractListener(Main main) { this.main = main; }

  @EventHandler
  public void onCrateInteract(@NotNull PlayerInteractAtEntityEvent e) {
    if (main.spinnyCrates.size() > 0) {
      final Entity entity = e.getRightClicked();
      final UUID uuid = entity.getUniqueId();

      if (entity instanceof ArmorStand) {
        if (main.spinnyCrates.containsKey(uuid)) {
          e.setCancelled(true);
          if (main.playersRemovingCrate.contains(e.getPlayer().getUniqueId())) {
            return;
          }

          final SpinnyCrate sc = main.spinnyCrates.get(uuid);
          if (sc.getHiddenUntil() == 0L) {
            final CrateData cratePicked = SpinnyCrate.categorisedCrates.get(sc.getCrateName());
            if (cratePicked.requiresPerm() &&
                !e.getPlayer().hasPermission(cratePicked.getPermRequired())) {
              e.getPlayer().sendMessage(main.getConfig()
                                            .getString("noPermMessage")
                                            .replace('&', '§'));
              return;
            }

            if (sc.shouldDisappear()) {
              final long hiddenUntil = Instant.now().toEpochMilli() + cratePicked.getRandomTime();
              sc.setHiddenUntil(hiddenUntil);
              final ConfigurationSection cs = main.cratesDataYaml.getConfigurationSection(sc.getUUID().toString());
              cs.set(Main.YAML_HIDDEN_UNTIL, hiddenUntil);
              try {
                main.cratesDataYaml.save(main.cratesDataFile);
              } catch (IOException ex) {
                main.getLogger().severe("Could not save data file!");
                ex.printStackTrace();
              }
            }

            final Player player = e.getPlayer();
            final Loot randomLoot = cratePicked.getRandomLoot();

            for (String command : randomLoot.commands) {
              main.getServer().dispatchCommand(main.getServer().getConsoleSender(),
                                               command.replace("%player%", player.getName()));
            }

            player.playSound(player.getLocation(),
                             Sound.ENTITY_EXPERIENCE_ORB_PICKUP,
                             SoundCategory.MASTER,
                             1.0f, 1.0f);

            if (player.hasPermission(main.getConfig().getString("instantGivePerm"))) {
              final HashMap<Integer, ItemStack> notStoredItems = player.getInventory().addItem(randomLoot.items);
              for (ItemStack item : notStoredItems.values()) {
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
                                      .add(0, 5, 0),
                                    randomLoot.items[index++])
                          .setVelocity(new Vector());
                    player.playSound(player.getLocation(),
                                     Sound.BLOCK_TRIPWIRE_CLICK_OFF,
                                     SoundCategory.MASTER,
                                     1.0f, 1.0f);
                  }
                }
              }.runTaskTimer(main, 0, 20 / 3);
            }
          }
        }
      }
    }
  }
}
