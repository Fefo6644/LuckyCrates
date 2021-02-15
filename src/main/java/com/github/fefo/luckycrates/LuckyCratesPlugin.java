package com.github.fefo.luckycrates;

import com.github.fefo.luckycrates.config.ConfigAdapter;
import com.github.fefo.luckycrates.config.ConfigKeys;
import com.github.fefo.luckycrates.config.adapter.JsonConfigAdapter;
import com.github.fefo.luckycrates.internal.CratesMap;
import com.github.fefo.luckycrates.internal.InteractantsHandler;
import com.github.fefo.luckycrates.internal.SpinningCrate;
import com.github.fefo.luckycrates.listeners.ChunkLoadListener;
import com.github.fefo.luckycrates.listeners.ChunkUnloadListener;
import com.github.fefo.luckycrates.listeners.CrateInteractListener;
import com.github.fefo.luckycrates.listeners.CrateRemoveListener;
import com.github.fefo.luckycrates.messages.SubjectFactory;
import com.github.fefo.luckycrates.util.TaskScheduler;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;

public final class LuckyCratesPlugin extends JavaPlugin {

  public static final Logger LOGGER = LoggerFactory.getLogger(LuckyCratesPlugin.class);
  private static final Pattern PATH_SEPARATOR = Pattern.compile("/");

  private final Path dataFolder = getDataFolder().toPath();
  private final ConfigAdapter configAdapter = new JsonConfigAdapter(this, this.dataFolder);
  private final TaskScheduler scheduler = new TaskScheduler(this);
  private final CratesMap cratesMap = new CratesMap(this);
  private final InteractantsHandler interactantsHandler = new InteractantsHandler();
  private SubjectFactory subjectFactory;

  public Path getDataFolderPath() {
    return this.dataFolder;
  }

  public ConfigAdapter getConfigAdapter() {
    return this.configAdapter;
  }

  public TaskScheduler getScheduler() {
    return this.scheduler;
  }

  public CratesMap getCratesMap() {
    return this.cratesMap;
  }

  public InteractantsHandler getInteractantsHandler() {
    return this.interactantsHandler;
  }

  public SubjectFactory getSubjectFactory() {
    return this.subjectFactory;
  }

  @Override
  public void onLoad() {
    try {
      this.configAdapter.load();
    } catch (final IOException exception) {
      LOGGER.error("Could not create/load config!");
      throw new RuntimeException(exception);
    }
  }

  @Override
  public void onEnable() {
    this.subjectFactory = new SubjectFactory(this);

    try {
      this.cratesMap.load();
    } catch (final Exception exception) {
      LOGGER.error("Could not create/load crates data!");
      throw new RuntimeException(exception);
    }

    new CommandHandler(this);
    Bukkit.getPluginManager().registerEvents(new CrateInteractListener(this), this);
    Bukkit.getPluginManager().registerEvents(new CrateRemoveListener(this), this);
    Bukkit.getPluginManager().registerEvents(new ChunkLoadListener(this), this);
    Bukkit.getPluginManager().registerEvents(new ChunkUnloadListener(this), this);

    this.scheduler.async(() -> {
      this.cratesMap.values().forEach(crate -> {
        crate.rotate(2.0 * Math.PI * this.configAdapter.get(ConfigKeys.RPM));
      });
    }, 25L, 25L);

    this.scheduler.async(() -> {
      final long now = System.currentTimeMillis();
      this.cratesMap.values().stream()
                    .filter(SpinningCrate::shouldDisappear)
                    .filter(chest -> chest.getHiddenUntil() != Long.MIN_VALUE)
                    .filter(chest -> chest.getHiddenUntil() <= now)
                    .forEach(chest -> chest.setHiddenUntil(Long.MIN_VALUE));
    }, 500L, 500L);
  }

  @Override
  public void onDisable() {
    this.cratesMap.clear();
    this.scheduler.shutdown();
  }

  public boolean reload() {
    try {
      this.configAdapter.reload();
      this.cratesMap.reload();
      return true;
    } catch (final IOException exception) {
      LOGGER.error("There was an error while reloading files", exception);
      return false;
    }
  }

  @Override
  public Logger getSLF4JLogger() {
    return LOGGER;
  }

  public void saveResource(final String resource, final Path folder, final boolean overwrite) throws IOException {
    final Path dest = Paths.get(folder.toString(), PATH_SEPARATOR.split(resource));
    if (Files.exists(dest) && !overwrite) {
      return;
    }
    if (Files.notExists(dest)) {
      Files.createDirectories(dest.getParent());
    }

    try (final InputStream stream = getClassLoader().getResourceAsStream(resource)) {
      if (stream == null) {
        throw new IOException("stream is null");
      }
      Files.copy(stream, dest);
    } catch (final IOException exception) {
      throw new IOException("Failed to save resource from jar " + resource, exception);
    }
  }
}
