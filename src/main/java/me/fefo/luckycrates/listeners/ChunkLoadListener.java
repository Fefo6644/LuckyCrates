package me.fefo.luckycrates.listeners;

import me.fefo.luckycrates.Main;
import me.fefo.luckycrates.SpinnyCrate;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;

public final class ChunkLoadListener implements Listener {
  private final Main main;

  public ChunkLoadListener(Main main) { this.main = main; }

  @EventHandler
  public void onChunkLoad(ChunkLoadEvent event) {
    if (main.cratesDataYaml.getKeys(false).size() == 0) {
      return;
    }

    for (Entity entity : event.getChunk().getEntities()) {
      if (!(entity instanceof ArmorStand)) {
        continue;
      }

      if (main.cratesDataYaml.getKeys(false).contains(entity.getUniqueId().toString())) {
        final ConfigurationSection cs = main.cratesDataYaml.getConfigurationSection(entity.getUniqueId()
                                                                                          .toString());
        main.spinnyCrates.put(entity.getUniqueId(),
                              new SpinnyCrate(cs.getString(Main.YAML_CRATE_TYPE),
                                              entity.getUniqueId(),
                                              cs.getLong(Main.YAML_HIDDEN_UNTIL),
                                              cs.getBoolean(Main.YAML_SHOULD_DISAPPEAR)));
      }
    }
  }
}
