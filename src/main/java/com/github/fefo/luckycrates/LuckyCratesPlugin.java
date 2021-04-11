//
// This file is part of LuckyCrates, licensed under the MIT License.
//
// Copyright (c) 2021 Fefo6644 <federico.lopez.1999@outlook.com>
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in all
// copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// SOFTWARE.
//

package com.github.fefo.luckycrates;

import com.github.fefo.luckycrates.config.ConfigAdapter;
import com.github.fefo.luckycrates.config.ConfigKeys;
import com.github.fefo.luckycrates.config.adapter.JsonConfigAdapter;
import com.github.fefo.luckycrates.internal.CrateMap;
import com.github.fefo.luckycrates.internal.InteractantsHandler;
import com.github.fefo.luckycrates.internal.SpinningCrate;
import com.github.fefo.luckycrates.listeners.ChunkLoadListener;
import com.github.fefo.luckycrates.listeners.ChunkUnloadListener;
import com.github.fefo.luckycrates.listeners.CrateInteractListener;
import com.github.fefo.luckycrates.listeners.CrateRemoveListener;
import com.github.fefo.luckycrates.messages.SubjectFactory;
import com.github.fefo.luckycrates.util.TaskScheduler;
import net.milkbowl.vault.chat.Chat;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public final class LuckyCratesPlugin extends JavaPlugin implements Listener {

  private static final Pattern PATH_SEPARATOR = Pattern.compile("/");

  private final Path dataFolder = getDataFolder().toPath();
  private final ConfigAdapter configAdapter = new JsonConfigAdapter(this, this.dataFolder);
  private final TaskScheduler scheduler = new TaskScheduler(this);
  private final CrateMap crateMap = new CrateMap(this);
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

  public CrateMap getCratesMap() {
    return this.crateMap;
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
      getLogger().severe("Could not create/load config!");
      throw new RuntimeException(exception);
    }
  }

  @Override
  public void onEnable() {
    this.subjectFactory = new SubjectFactory(this);

    try {
      this.crateMap.load();
    } catch (final Exception exception) {
      getLogger().severe("Could not create/load crates data!");
      throw new RuntimeException(exception);
    }

    final Chat vaultChat = Bukkit.getServicesManager().load(Chat.class);
    new LuckyCratesCommand(this);
    Bukkit.getPluginManager().registerEvents(new CrateInteractListener(this, vaultChat), this);
    Bukkit.getPluginManager().registerEvents(new CrateRemoveListener(this), this);
    Bukkit.getPluginManager().registerEvents(new ChunkLoadListener(this), this);
    Bukkit.getPluginManager().registerEvents(new ChunkUnloadListener(this), this);

    this.scheduler.async(() -> {
      this.crateMap.values().forEach(crate -> {
        crate.rotate(2.0 * Math.PI * this.configAdapter.get(ConfigKeys.RPM));
      });
    }, 25L, 25L);

    this.scheduler.async(() -> {
      final long now = System.currentTimeMillis();
      this.crateMap.values().stream()
                   .filter(SpinningCrate::shouldDisappear)
                   .filter(chest -> chest.getHiddenUntil() != Long.MIN_VALUE)
                   .filter(chest -> chest.getHiddenUntil() <= now)
                   .forEach(chest -> chest.setHiddenUntil(Long.MIN_VALUE));
    }, 500L, 500L);
  }

  @Override
  public void onDisable() {
    this.crateMap.clear();
    this.scheduler.shutdown();
  }

  public boolean reload() {
    try {
      this.configAdapter.reload();
      this.crateMap.reload();
      return true;
    } catch (final IOException exception) {
      getLogger().severe("There was an error while reloading files");
      exception.printStackTrace();
      return false;
    }
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

  public <T extends Event> void registerListener(final Class<T> eventType, final Consumer<T> handler) {
    registerListener(eventType, handler, EventPriority.NORMAL, true);
  }

  public <T extends Event> void registerListener(final Class<T> eventType, final Consumer<T> handler, final EventPriority priority) {
    registerListener(eventType, handler, priority, true);
  }

  public <T extends Event> void registerListener(final Class<T> eventType, final Consumer<T> handler, final boolean callIfCancelled) {
    registerListener(eventType, handler, EventPriority.NORMAL, callIfCancelled);
  }

  public <T extends Event> void registerListener(final Class<T> eventType, final Consumer<T> handler, final EventPriority priority, final boolean callIfCancelled) {
    Bukkit.getPluginManager().registerEvent(eventType, this, priority,
                                            ((listener, event) -> {
                                              if (eventType.isInstance(event)) {
                                                handler.accept(eventType.cast(event));
                                              }
                                            }), this, !callIfCancelled);
  }
}
