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
