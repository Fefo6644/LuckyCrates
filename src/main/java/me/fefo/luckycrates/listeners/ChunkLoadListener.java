package me.fefo.luckycrates.listeners;

import me.fefo.facilites.SelfRegisteringListener;
import me.fefo.luckycrates.LuckyCrates;
import me.fefo.luckycrates.internal.SpinnyCrate;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.world.ChunkLoadEvent;

public final class ChunkLoadListener extends SelfRegisteringListener {

  private final LuckyCrates plugin;

  public ChunkLoadListener(final LuckyCrates plugin) {
    super(plugin);
    this.plugin = plugin;
  }

  @EventHandler
  public void onChunkLoad(final ChunkLoadEvent event) {
    if (plugin.cratesDataYaml.getKeys(false).isEmpty()) {
      return;
    }

    for (final Entity entity : event.getChunk().getEntities()) {
      if (entity.getType() != EntityType.ARMOR_STAND) {
        continue;
      }

      if (plugin.cratesDataYaml.getKeys(false).contains(entity.getUniqueId().toString())) {
        final ConfigurationSection cs = plugin.cratesDataYaml.getConfigurationSection(entity.getUniqueId().toString());
        plugin.spinnyCrates.put(entity.getUniqueId(),
                                new SpinnyCrate(plugin.getCratesManager(), cs.getString(LuckyCrates.YAML_CRATE_TYPE), entity.getUniqueId(),
                                                cs.getLong(LuckyCrates.YAML_HIDDEN_UNTIL), cs.getBoolean(LuckyCrates.YAML_SHOULD_DISAPPEAR)));
      }
    }
  }
}
