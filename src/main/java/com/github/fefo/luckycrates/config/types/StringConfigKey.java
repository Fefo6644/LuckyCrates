package com.github.fefo.luckycrates.config.types;

import com.github.fefo.luckycrates.config.ConfigAdapter;
import com.github.fefo.luckycrates.config.ConfigKey;
import org.jetbrains.annotations.NotNull;

public class StringConfigKey extends ConfigKey<String> {

  public StringConfigKey(final @NotNull String key, final @NotNull String fallback, final boolean reloadable) {
    super(key, fallback, reloadable);
  }

  @Override
  public @NotNull String get(final @NotNull ConfigAdapter configAdapter) {
    final String string = configAdapter.getString(this.key);
    return string != null ? string : this.fallback;
  }
}
