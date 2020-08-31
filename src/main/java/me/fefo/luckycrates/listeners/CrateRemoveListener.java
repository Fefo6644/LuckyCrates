package me.fefo.luckycrates.listeners;

import me.fefo.facilites.ColorFormat;
import me.fefo.facilites.SelfRegisteringListener;
import me.fefo.luckycrates.LuckyCrates;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.UUID;

public final class CrateRemoveListener extends SelfRegisteringListener {
  private final LuckyCrates plugin;

  public CrateRemoveListener(final LuckyCrates plugin) {
    super(plugin);
    this.plugin = plugin;
  }

  @EventHandler
  public void crateRemove(@NotNull final EntityDamageByEntityEvent e) {
    final Entity damaged = e.getEntity();
    final UUID uuid = damaged.getUniqueId();

    if (plugin.playersRemovingCrate.size() == 0 ||
        plugin.spinnyCrates.size() == 0 ||
        !plugin.spinnyCrates.containsKey(uuid)) {
      return;
    }

    e.setCancelled(true);

    if (e.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK) {
      final Entity damager = e.getDamager();

      if (damager instanceof Player &&
          plugin.playersRemovingCrate.remove(damager.getUniqueId())) {
        plugin.spinnyCrates.get(uuid).kill();
        plugin.spinnyCrates.remove(uuid);
        plugin.cratesDataYaml.set(uuid.toString(), null);
        try {
          plugin.cratesDataYaml.save(plugin.cratesDataFile);
        } catch (IOException ex) {
          plugin.getLogger().severe("Could not save data file!");
          ex.printStackTrace();
        }
        damager.sendMessage(ColorFormat.format("&bCrate removed"));
      }
    }
  }
}
