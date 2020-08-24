package me.fefo.luckycrates.listeners;

import me.fefo.luckycrates.LuckyCrates;
import me.fefo.luckycrates.SpinnyCrate;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;

public final class ChunkLoadListener implements Listener {
  private final LuckyCrates plugin;

  public ChunkLoadListener(LuckyCrates plugin) { this.plugin = plugin; }

  @EventHandler
  public void onChunkLoad(ChunkLoadEvent event) {
    if (plugin.cratesDataYaml.getKeys(false).size() == 0) {
      return;
    }

    for (Entity entity : event.getChunk().getEntities()) {
      if (!(entity instanceof ArmorStand)) {
        continue;
      }

      if (plugin.cratesDataYaml.getKeys(false).contains(entity.getUniqueId().toString())) {
        final ConfigurationSection cs = plugin.cratesDataYaml.getConfigurationSection(entity.getUniqueId()
                                                                                            .toString());
        plugin.spinnyCrates.put(entity.getUniqueId(),
                                new SpinnyCrate(cs.getString(LuckyCrates.YAML_CRATE_TYPE),
                                              entity.getUniqueId(),
                                              cs.getLong(LuckyCrates.YAML_HIDDEN_UNTIL),
                                              cs.getBoolean(LuckyCrates.YAML_SHOULD_DISAPPEAR)));
      }
    }
  }
}
