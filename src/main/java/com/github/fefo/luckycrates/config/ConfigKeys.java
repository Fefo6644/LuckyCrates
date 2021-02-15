package com.github.fefo.luckycrates.config;

import com.github.fefo.luckycrates.config.types.DoubleConfigKey;
import com.github.fefo.luckycrates.config.types.StringConfigKey;

public final class ConfigKeys {

  public static final DoubleConfigKey RPM = new DoubleConfigKey("rpm", 45.0, true);

  public static final StringConfigKey INSTANT_GIVE_PERM =
      new StringConfigKey("instantGivePerm", "group.vip", true);

  public static final StringConfigKey NO_PERM_MESSAGE =
      new StringConfigKey("noPermMessage", "&c&oYou need VIP rank or higher to open this crate", true);

  private ConfigKeys() {
    throw new UnsupportedOperationException();
  }
}
