package com.github.fefo.luckycrates.listeners;

import com.github.fefo.luckycrates.LuckyCratesPlugin;
import com.github.fefo.luckycrates.internal.CratesMap;
import com.github.fefo.luckycrates.internal.SpinningCrate;
import com.github.fefo.luckycrates.messages.Message;
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

  private final LuckyCratesPlugin plugin;
  private final CratesMap cratesMap;

  public CrateRemoveListener(final LuckyCratesPlugin plugin) {
    this.plugin = plugin;
    this.cratesMap = plugin.getCratesMap();
  }

  @EventHandler
  public void crateRemove(@NotNull final EntityDamageByEntityEvent event) {
    final Entity damaged = event.getEntity();
    final UUID uuid = damaged.getUniqueId();
    final SpinningCrate crate = this.cratesMap.get(uuid);

    if (!this.plugin.getInteractantsHandler().isAnyoneRemoving() || crate == null) {
      return;
    }

    event.setCancelled(true);

    if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) {
      return;
    }

    final Entity damager = event.getDamager();

    if (damager instanceof Player && this.plugin.getInteractantsHandler().stopRemoving(damager.getUniqueId())) {
      this.cratesMap.remove(uuid);
      final String type = crate.getType();

      try {
        this.cratesMap.save();
      } catch (final IOException exception) {
        this.plugin.getSLF4JLogger().error("Could not save data file!", exception);
      }

      Message.CRATE_REMOVED.send(this.plugin.getSubjectFactory().sender(damager), type);
    }
  }
}
