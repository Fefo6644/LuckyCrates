package me.fefo.luckycrates.listeners;

import me.fefo.luckycrates.LuckyCrates;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkUnloadEvent;

public final class ChunkUnloadListener implements Listener {
  private final LuckyCrates plugin;

  public ChunkUnloadListener(LuckyCrates plugin) { this.plugin = plugin; }

  @EventHandler
  public void onChunkUnload(ChunkUnloadEvent event) {
    if (plugin.spinnyCrates.size() == 0) {
      return;
    }

    for (Entity entity : event.getChunk().getEntities()) {
      if (!(entity instanceof ArmorStand)) {
        continue;
      }

      plugin.spinnyCrates.remove(entity.getUniqueId());
    }
  }
}
