package me.fefo.luckycrates;

import me.fefo.luckycrates.listeners.ChunkLoadListener;
import me.fefo.luckycrates.listeners.ChunkUnloadListener;
import me.fefo.luckycrates.listeners.CrateInteractListener;
import me.fefo.luckycrates.listeners.CrateRemoveListener;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;
import java.util.UUID;

public final class LuckyCrates extends JavaPlugin {
  public static final String YAML_HIDDEN_UNTIL = "hiddenUntil";
  public static final String YAML_SHOULD_DISAPPEAR = "shouldDisappear";
  public static final String YAML_CRATE_TYPE = "crateType";

  public final Hashtable<UUID, SpinnyCrate> spinnyCrates = new Hashtable<>();
  public final Set<UUID> playersRemovingCrate = Collections.synchronizedSet(new HashSet<>());
  public File cratesListFolder;
  public File cratesDataFile;
  public YamlConfiguration cratesDataYaml;
  private double rpm = 45.0;

  @Override
  public void onEnable() {
    SpinnyCrate.setPlugin(this);

    try {
      saveDefaultConfig();
      cratesListFolder = new File(getDataFolder(), "crates" + File.separator);
      if (cratesListFolder.mkdirs()) {
        saveResource("crates/example.yml", false);
        saveResource("crates/common.yml", false);
        saveResource("crates/uncommon.yml", false);
        saveResource("crates/rare.yml", false);
        saveResource("crates/donor.yml", false);
      }
      cratesDataFile = new File(getDataFolder(), "cratesData.yml");
      cratesDataFile.createNewFile();
      reload();
    } catch (IOException e) {
      getLogger().severe("Could not create data file!");
      e.printStackTrace();
      getServer().getPluginManager().disablePlugin(this);
      return;
    }

    final CommanderKeen ck = new CommanderKeen(this);
    getCommand("luckycrates").setExecutor(ck);
    getCommand("luckycrates").setTabCompleter(ck);
    getServer().getPluginManager().registerEvents(new CrateInteractListener(this), this);
    getServer().getPluginManager().registerEvents(new CrateRemoveListener(this), this);
    getServer().getPluginManager().registerEvents(new ChunkLoadListener(this), this);
    getServer().getPluginManager().registerEvents(new ChunkUnloadListener(this), this);

    getServer().getScheduler().runTaskTimer(this, () -> {
      for (SpinnyCrate sc : spinnyCrates.values()) {
        if (!sc.rotate(2 * Math.PI * rpm)) {
          spinnyCrates.put(sc.getUUID(),
                           new SpinnyCrate(sc.getCrateName(),
                                           sc.getUUID(),
                                           sc.getHiddenUntil(),
                                           sc.shouldDisappear()));
        }
      }
    }, 0, 1);

    getServer().getScheduler().runTaskTimer(this, () -> {
      final int yamlHash = cratesDataYaml.getValues(true).hashCode();
      final long now = Instant.now().toEpochMilli();

      for (SpinnyCrate sc : spinnyCrates.values()) {
        if (sc.getHiddenUntil() != 0L &&
            sc.getHiddenUntil() <= now) {
          sc.setHiddenUntil(0L);
          cratesDataYaml.set(sc.getUUID() +
                             String.valueOf(cratesDataYaml.options().pathSeparator()) +
                             YAML_HIDDEN_UNTIL, 0L);
        }
      }

      if (yamlHash != cratesDataYaml.getValues(true).hashCode()) {
        try {
          cratesDataYaml.save(cratesDataFile);
        } catch (IOException e) {
          getLogger().severe("Could not save data file!");
          e.printStackTrace();
        }
      }
    }, 0, 20);
  }

  void reload() {
    reloadConfig();
    rpm = getConfig().getDouble("rpm", 45.0);

    SpinnyCrate.reloadCrates();
    spinnyCrates.clear();
    cratesDataYaml = YamlConfiguration.loadConfiguration(cratesDataFile);
    for (String k : cratesDataYaml.getKeys(false)) {
      final ConfigurationSection cs = cratesDataYaml.getConfigurationSection(k);
      if (!SpinnyCrate.categorisedCrates.containsKey(cs.getString(YAML_CRATE_TYPE))) {
        continue;
      }
      spinnyCrates.put(UUID.fromString(k),
                       new SpinnyCrate(cs.getString(YAML_CRATE_TYPE),
                                       UUID.fromString(k),
                                       cs.getLong(YAML_HIDDEN_UNTIL, 0L),
                                       cs.getBoolean(YAML_SHOULD_DISAPPEAR, true)));
    }
  }
}
