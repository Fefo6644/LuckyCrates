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

package com.github.fefo.luckycrates.listeners;

import com.github.fefo.luckycrates.LuckyCratesPlugin;
import com.github.fefo.luckycrates.internal.CratesMap;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkUnloadEvent;

public final class ChunkUnloadListener implements Listener {

  private final CratesMap cratesMap;

  public ChunkUnloadListener(final LuckyCratesPlugin plugin) {
    this.cratesMap = plugin.getCratesMap();
  }

  @EventHandler
  public void onChunkUnload(final ChunkUnloadEvent event) {
    if (this.cratesMap.isEmpty()) {
      return;
    }

    for (final Entity entity : event.getChunk().getEntities()) {
      if (entity.getType() == EntityType.ARMOR_STAND) {
        this.cratesMap.unload(entity.getUniqueId());
      }
    }
  }
}
