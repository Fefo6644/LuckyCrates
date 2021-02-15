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

import com.google.common.collect.ImmutableList;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

public final class Loot implements Comparable<Loot> {

  public final int weight;
  public final List<String> commands;
  public final List<ItemStack> items;

  public Loot(final int weight, final Collection<String> commands, final Collection<ItemStack> items) {
    this.weight = weight;
    this.commands = ImmutableList.copyOf(commands);
    this.items = ImmutableList.copyOf(items);
  }

  @Override
  public int compareTo(final @NotNull Loot that) {
    // higher weighted rewards come first
    return that.weight - this.weight;
  }
}
