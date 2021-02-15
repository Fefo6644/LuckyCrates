package com.github.fefo.luckycrates.listeners;

import com.github.fefo.luckycrates.LuckyCratesPlugin;
import com.github.fefo.luckycrates.internal.CratesMap;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkUnloadEvent;

public final class ChunkUnloadListener implements Listener {

  private final CratesMap cratesMap;

  public ChunkUnloadListener(final LuckyCratesPlugin plugin) {
    this.cratesMap = plugin.getCratesMap();
  }

  @EventHandler
  public void onChunkUnload(final ChunkUnloadEvent event) {
    if (this.cratesMap.isEmpty()) {
      return;
    }

    for (final Entity entity : event.getChunk().getEntities()) {
      if (entity.getType() == EntityType.ARMOR_STAND) {
        this.cratesMap.unload(entity.getUniqueId());
      }
    }
  }
}
