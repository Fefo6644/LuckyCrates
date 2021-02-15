package com.github.fefo.luckycrates.config.types;

import com.github.fefo.luckycrates.config.ConfigAdapter;
import com.github.fefo.luckycrates.config.ConfigKey;
import org.jetbrains.annotations.NotNull;

public class DoubleConfigKey extends ConfigKey<Double> {

  public DoubleConfigKey(final @NotNull String key,
                         final double fallback,
                         final boolean reloadable) {
    super(key, fallback, reloadable);
  }

  @Override
  public @NotNull Double get(final @NotNull ConfigAdapter configAdapter) {
    final Double value = configAdapter.getDouble(this.key);
    return value != null ? value : this.fallback;
  }
}
