package com.github.fefo.luckycrates.listeners;

import com.github.fefo.luckycrates.LuckyCratesPlugin;
import com.github.fefo.luckycrates.internal.CratesMap;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;

import java.util.UUID;

public final class ChunkLoadListener implements Listener {

  private final CratesMap cratesMap;

  public ChunkLoadListener(final LuckyCratesPlugin plugin) {
    this.cratesMap = plugin.getCratesMap();
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onChunkLoad(final ChunkLoadEvent event) {
    if (this.cratesMap.isEmpty()) {
      return;
    }

    for (final Entity entity : event.getChunk().getEntities()) {
      if (entity.getType() == EntityType.ARMOR_STAND) {
        final UUID uuid = entity.getUniqueId();
        if (this.cratesMap.containsKey(uuid)) {
          this.cratesMap.summon(uuid);
        }
      }
    }
  }
}
