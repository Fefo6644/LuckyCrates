package me.fefo.luckycrates.listeners;

import me.fefo.luckycrates.Main;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.UUID;

public final class CrateRemoveListener implements Listener {
  private final Main main;

  public CrateRemoveListener(Main main) { this.main = main; }

  @EventHandler
  public void crateRemove(@NotNull EntityDamageByEntityEvent e) {
    final Entity damaged = e.getEntity();
    final UUID uuid = damaged.getUniqueId();

    if (main.playersRemovingCrate.size() == 0 ||
        main.spinnyCrates.size() == 0 ||
        !main.spinnyCrates.containsKey(uuid)) {
      return;
    }

    e.setCancelled(true);

    if (e.getCause().equals(EntityDamageEvent.DamageCause.ENTITY_ATTACK)) {
      final Entity damager = e.getDamager();

      if (damager instanceof Player &&
          main.playersRemovingCrate.remove(damager.getUniqueId())) {
        main.spinnyCrates.get(uuid).kill();
        main.spinnyCrates.remove(uuid);
        main.cratesDataYaml.set(uuid.toString(), null);
        try {
          main.cratesDataYaml.save(main.cratesDataFile);
        } catch (IOException ex) {
          main.getLogger().severe("Could not save data file!");
          ex.printStackTrace();
        }
        damager.sendMessage(ChatColor.AQUA + "Crate removed");
      }
    }
  }
}
