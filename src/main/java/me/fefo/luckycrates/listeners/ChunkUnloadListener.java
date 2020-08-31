package me.fefo.luckycrates.listeners;

import me.fefo.facilites.SelfRegisteringListener;
import me.fefo.luckycrates.LuckyCrates;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.world.ChunkUnloadEvent;

public final class ChunkUnloadListener extends SelfRegisteringListener {
  private final LuckyCrates plugin;

  public ChunkUnloadListener(final LuckyCrates plugin) {
    super(plugin);
    this.plugin = plugin;
  }

  @EventHandler
  public void onChunkUnload(final ChunkUnloadEvent event) {
    if (plugin.spinnyCrates.size() == 0) {
      return;
    }

    for (final Entity entity : event.getChunk().getEntities()) {
      if (entity.getType() != EntityType.ARMOR_STAND) {
        continue;
      }

      plugin.spinnyCrates.remove(entity.getUniqueId());
    }
  }
}
