//
// LuckyCrates - Better your KitPvP world with some fancy lootcrates.
// Copyright (C) 2021  Fefo6644 <federico.lopez.1999@outlook.com>
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <https://www.gnu.org/licenses/>.
//

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
