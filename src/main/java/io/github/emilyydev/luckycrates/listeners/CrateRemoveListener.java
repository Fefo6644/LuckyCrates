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
import io.github.emilyydev.luckycrates.internal.SpinningCrate;
import io.github.emilyydev.luckycrates.messages.Message;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

import java.io.IOException;
import java.util.UUID;

public final class CrateRemoveListener {

  private final LuckyCratesPlugin plugin;
  private final CrateMap crateMap;

  public CrateRemoveListener(final LuckyCratesPlugin plugin) {
    this.plugin = plugin;
    this.crateMap = plugin.getCratesMap();
    plugin.registerListener(EntityDamageByEntityEvent.class, this::crateRemove);
  }

  private void crateRemove(final EntityDamageByEntityEvent event) {
    final Entity damaged = event.getEntity();
    final UUID uuid = damaged.getUniqueId();
    final SpinningCrate crate = this.crateMap.get(uuid);

    if (!this.plugin.getInteractantsHandler().isAnyoneRemoving() || crate == null) {
      return;
    }

    event.setCancelled(true);

    if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) {
      return;
    }

    final Entity damager = event.getDamager();

    if (damager instanceof Player && this.plugin.getInteractantsHandler().stopRemoving(damager.getUniqueId())) {
      this.crateMap.remove(uuid);
      final String type = crate.getType();

      try {
        this.crateMap.save();
      } catch (final IOException exception) {
        this.plugin.getLogger().severe("Could not save data file!");
        exception.printStackTrace();
      }

      Message.CRATE_REMOVED.send(this.plugin.getSubjectFactory().sender(damager), type);
    }
  }
}
