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

package com.github.fefo.luckycrates.internal;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class InteractantsHandler {

  private final Set<UUID> playersRemovingCrates = new HashSet<>();

  public boolean startRemoving(final UUID uuid) {
    return this.playersRemovingCrates.add(uuid);
  }

  public boolean isRemoving(final UUID uuid) {
    return this.playersRemovingCrates.contains(uuid);
  }

  public boolean isAnyoneRemoving() {
    return !this.playersRemovingCrates.isEmpty();
  }

  public boolean stopRemoving(final UUID uuid) {
    return this.playersRemovingCrates.remove(uuid);
  }
}
