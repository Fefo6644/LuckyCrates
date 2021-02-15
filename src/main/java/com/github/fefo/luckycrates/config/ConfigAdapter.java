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

package com.github.fefo.luckycrates.config;

import com.github.fefo.luckycrates.LuckyCratesPlugin;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.regex.Pattern;

public abstract class ConfigAdapter {

  protected static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;
  protected static final Set<ConfigKey<?>> CONFIG_KEYS =
      ImmutableSet.of(ConfigKeys.RPM,
                      ConfigKeys.INSTANT_GIVE_PERM,
                      ConfigKeys.NO_PERM_MESSAGE);

  protected final LuckyCratesPlugin plugin;
  protected final Path configPath;
  protected final Path dataFolder;
  protected final String separator = ".";
  protected final Pattern separatorPattern = Pattern.compile(this.separator, Pattern.LITERAL);
  protected final Map<String, Object> rootRaw = new LinkedHashMap<>();
  protected final Map<String, Object> unwind = new LinkedHashMap<>();

  private final String configName;
  private boolean initialized = false;

  protected ConfigAdapter(final LuckyCratesPlugin plugin, final Path dataFolder, final String name) {
    this.plugin = plugin;
    this.dataFolder = dataFolder;
    this.configName = name;
    this.configPath = this.dataFolder.resolve(name);
  }

  public LuckyCratesPlugin getPlugin() {
    return this.plugin;
  }

  public void load() throws IOException {
    if (Files.notExists(this.dataFolder)) {
      Files.createDirectories(this.dataFolder);
    }

    createConfigTemplate();
    reload(true);
    this.initialized = true;
    CONFIG_KEYS.forEach(configKey -> this.unwind.put(configKey.getKey(), configKey.get(this)));
  }

  public void reload() throws IOException {
    this.rootRaw.clear();
    reload(false);
    CONFIG_KEYS.forEach(configKey -> {
      if (configKey.isReloadable()) {
        this.unwind.put(configKey.getKey(), configKey.get(this));
      }
    });
  }

  protected abstract void reload(final boolean force) throws IOException;

  protected void createConfigTemplate() throws IOException {
    if (Files.notExists(this.configPath)) {
      try (final InputStream inputStream = this.plugin.getResource(this.configName)) {
        if (inputStream != null) {
          Files.copy(inputStream, this.configPath);
        } else {
          throw new IOException("inputStream is null");
        }
      }
    }
  }

  public @Nullable Boolean getBoolean(final @NotNull String key) {
    Validate.notNull(key);
    return validate(key, get(key), Boolean.class);
  }

  public @Nullable Integer getInt(final @NotNull String key) {
    Validate.notNull(key);
    return validate(key, get(key), Integer.class);
  }

  public @Nullable Double getDouble(final @NotNull String key) {
    Validate.notNull(key);
    return validate(key, get(key), Double.class);
  }

  public @Nullable String getString(final @NotNull String key) {
    Validate.notNull(key);
    return validate(key, get(key), String.class);
  }

  @SuppressWarnings("unchecked")
  public <T> @Nullable List<T> getList(final @NotNull String key) {
    Validate.notNull(key);
    return validate(key, get(key), List.class);
  }

  @SuppressWarnings("unchecked")
  public @Nullable Map<String, Object> getSection(final @NotNull String key) {
    Validate.notNull(key);
    return validate(key, get(key), Map.class);
  }

  @SuppressWarnings("unchecked")
  public <T> @NotNull T get(final @NotNull ConfigKey<T> configKey) {
    Preconditions.checkState(this.initialized, "Cannot query config values at this stage; config not initialized yet");
    Validate.notNull(configKey);
    T value = (T) this.unwind.get(configKey.getKey());
    if (value == null) {
      value = configKey.get(this);
      this.unwind.put(configKey.getKey(), value);
    }
    return value;
  }

  private Object get(final String path) {
    return get(path, this.rootRaw);
  }

  private Object get(final String path, final Map<String, ?> map) {
    Preconditions.checkState(this.initialized, "Cannot query config values at this stage; config not initialized yet");
    final String[] pathComponents = this.separatorPattern.split(path);

    if (pathComponents.length == 1) {
      return map.get(path);
    }

    final StringJoiner joiner = new StringJoiner(this.separator);
    for (int i = 1; i < pathComponents.length; ++i) {
      joiner.add(pathComponents[i]);
    }

    final String nested = pathComponents[0];
    return get(joiner.toString(), validate(nested, map.get(nested), Map.class));
  }

  private <T> T validate(final String key, final Object value, final Class<T> type) {
    if (value == null) {
      this.plugin.getSLF4JLogger().warn(
          String.format("No value for config key \"%s\" (expected to be of type %s)",
                        key, type.getSimpleName()));
      return null;
    }

    if (!type.isInstance(value)) {
      this.plugin.getSLF4JLogger().warn(
          String.format("Config key \"%s\" expected to be of type %s but got %s instead",
                        key, type, value.getClass()));
      return null;
    }

    return type.cast(value);
  }
}
