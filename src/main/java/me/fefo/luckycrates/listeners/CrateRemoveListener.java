package me.fefo.luckycrates.listeners;

import me.fefo.facilites.SelfRegisteringListener;
import me.fefo.luckycrates.LuckyCrates;
import me.fefo.luckycrates.Message;
import me.fefo.luckycrates.internal.SpinnyCrate;
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

    if (plugin.playersRemovingCrate.isEmpty() || plugin.spinnyCrates.isEmpty() || !plugin.spinnyCrates.containsKey(uuid)) {
      return;
    }

    e.setCancelled(true);

    if (e.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) {
      return;
    }

    final Entity damager = e.getDamager();

    if (damager instanceof Player && plugin.playersRemovingCrate.remove(damager.getUniqueId())) {
      final SpinnyCrate crate = plugin.spinnyCrates.remove(uuid);
      final String type = crate.getCrateName();
      crate.kill();
      plugin.cratesDataYaml.set(uuid.toString(), null);
      try {
        plugin.cratesDataYaml.save(plugin.cratesDataFile);
      } catch (IOException ex) {
        plugin.getLogger().severe("Could not save data file!");
        ex.printStackTrace();
      }

      Message.CRATE_REMOVED.send(damager, type);
    }
  }
}
