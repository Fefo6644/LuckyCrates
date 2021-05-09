//
// This file is part of LuckyCrates, licensed under the MIT License.
//
// Copyright (c) 2021 emilyy-dev
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

package io.github.emilyydev.luckycrates.listeners;

import io.github.emilyydev.luckycrates.LuckyCratesPlugin;
import io.github.emilyydev.luckycrates.internal.CrateMap;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventPriority;
import org.bukkit.event.world.ChunkLoadEvent;

import java.util.UUID;

public final class ChunkLoadListener {

  private final CrateMap crateMap;

  public ChunkLoadListener(final LuckyCratesPlugin plugin) {
    this.crateMap = plugin.getCratesMap();
    plugin.registerListener(ChunkLoadEvent.class, this::chunkLoad, EventPriority.MONITOR);
  }

  private void chunkLoad(final ChunkLoadEvent event) {
    if (this.crateMap.isEmpty()) {
      return;
    }

    for (final Entity entity : event.getChunk().getEntities()) {
      if (entity.getType() == EntityType.ARMOR_STAND) {
        final UUID uuid = entity.getUniqueId();
        if (this.crateMap.containsKey(uuid)) {
          this.crateMap.summon(uuid);
        }
      }
    }
  }
}
