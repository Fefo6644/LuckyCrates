package me.fefo.luckycrates;

import me.fefo.facilites.TaskUtil;
import me.fefo.luckycrates.internal.CratesManager;
import me.fefo.luckycrates.internal.SpinnyCrate;
import me.fefo.luckycrates.listeners.ChunkLoadListener;
import me.fefo.luckycrates.listeners.ChunkUnloadListener;
import me.fefo.luckycrates.listeners.CrateInteractListener;
import me.fefo.luckycrates.listeners.CrateRemoveListener;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
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

  private final TaskUtil scheduler = new TaskUtil(this);
  private final CratesManager manager = new CratesManager(this);

  public TaskUtil getScheduler() {
    return scheduler;
  }

  public CratesManager getCratesManager() {
    return manager;
  }

  @Override
  public void onEnable() {
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
      reloadConfig();
    } catch (IOException e) {
      getLogger().severe("Could not create data file!");
      throw new RuntimeException();
    }

    final CommanderKeen<Player> ck = new CommanderKeen<>(this, Player.class::cast);
    getCommand("luckycrates").setExecutor(ck);
    getCommand("luckycrates").setTabCompleter(ck);
    new CrateInteractListener(this);
    new CrateRemoveListener(this);
    new ChunkLoadListener(this);
    new ChunkUnloadListener(this);

    scheduler.sync(() -> {
      for (final SpinnyCrate sc : spinnyCrates.values()) {
        if (!sc.rotate(2 * Math.PI * rpm)) {
          spinnyCrates.put(sc.getUUID(), new SpinnyCrate(manager, sc.getCrateName(), sc.getUUID(), sc.getHiddenUntil(), sc.shouldDisappear()));
        }
      }
    }, 0L, 1L);

    scheduler.sync(() -> {
      final int yamlHash = cratesDataYaml.getValues(true).hashCode();
      final long now = Instant.now().toEpochMilli();

      for (SpinnyCrate sc : spinnyCrates.values()) {
        if (sc.getHiddenUntil() != 0L && sc.getHiddenUntil() <= now) {
          sc.setHiddenUntil(0L);
          cratesDataYaml.set(sc.getUUID() + "." + YAML_HIDDEN_UNTIL, 0L);
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
    }, 0L, 20L);
  }

  @Override
  public void reloadConfig() {
    super.reloadConfig();
    rpm = getConfig().getDouble("rpm", 45.0);

    manager.reloadCrates();
    spinnyCrates.clear();
    cratesDataYaml = YamlConfiguration.loadConfiguration(cratesDataFile);
    for (String k : cratesDataYaml.getKeys(false)) {
      final ConfigurationSection cs = cratesDataYaml.getConfigurationSection(k);
      if (!manager.categorizedCrates.containsKey(cs.getString(YAML_CRATE_TYPE))) {
        continue;
      }
      spinnyCrates.put(UUID.fromString(k), new SpinnyCrate(manager, cs.getString(YAML_CRATE_TYPE), UUID.fromString(k),
                                                           cs.getLong(YAML_HIDDEN_UNTIL, 0L), cs.getBoolean(YAML_SHOULD_DISAPPEAR, true)));
    }
  }
}
