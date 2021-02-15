//
// This file is part of LuckyCrates, licensed under the MIT License.
//
// Copyright (c) 2021 Fefo6644 <federico.lopez.1999@outlook.com>
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

package com.github.fefo.luckycrates.internal;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.EulerAngle;

import java.util.UUID;

public final class SpinningCrate {

  private final Location location;
  private final String type;
  private final UUID uuid;
  private final boolean shouldDisappear;
  private long hiddenUntil = Long.MIN_VALUE;
  private transient ItemStack skull;
  private transient ArmorStand stand;

  public SpinningCrate(Location location, final String category, final boolean shouldDisappear, final ItemStack skull) {
    this.skull = skull;
    location = location.toBlockLocation();

    location.setX(location.getBlockX() + 0.5);
    location.setY(location.getBlockY() - 1.0);
    location.setZ(location.getBlockZ() + 0.5);
    location.setYaw(0.0f);
    location.setPitch(0.0f);

    this.stand = location.getWorld().spawn(location, ArmorStand.class, armorStand -> {
      armorStand.setHeadPose(EulerAngle.ZERO);
      armorStand.setBodyPose(EulerAngle.ZERO);
      armorStand.setLeftArmPose(EulerAngle.ZERO);
      armorStand.setRightArmPose(EulerAngle.ZERO);
      armorStand.setLeftLegPose(EulerAngle.ZERO);
      armorStand.setRightLegPose(EulerAngle.ZERO);
      armorStand.setGravity(false);
      armorStand.setVisible(false);
      armorStand.setBasePlate(false);
      armorStand.setSmall(false);
      armorStand.getEquipment().setHelmet(skull);
    });
    this.uuid = this.stand.getUniqueId();
    this.location = this.stand.getEyeLocation();

    this.type = category;
    this.shouldDisappear = shouldDisappear;
  }

  public SpinningCrate(final UUID uuid, final Location location, final String type,
                       final long hiddenUntil, final boolean shouldDisappear) {
    this.uuid = uuid;
    this.location = location;
    this.type = type;
    this.hiddenUntil = hiddenUntil;
    this.shouldDisappear = shouldDisappear;
  }

  public void setSkull(final ItemStack skull) {
    this.skull = skull;
    if (this.stand != null) {
      this.stand.getEquipment().setHelmet(skull);
    }
  }

  void kill() {
    this.stand.remove();
    unload();
  }

  void unload() {
    this.stand = null;
  }

  public void summon() {
    this.stand = (ArmorStand) Bukkit.getEntity(this.uuid);
  }

  public Location getLocation() {
    return this.location.clone();
  }

  public long getHiddenUntil() {
    return this.hiddenUntil;
  }

  public void setHiddenUntil(final long hiddenUntil) {
    final long now = System.currentTimeMillis();

    this.hiddenUntil = hiddenUntil;
    if (hiddenUntil <= now) {
      if (this.stand != null && this.stand.getEquipment().getHelmet() == null) {
        this.stand.getEquipment().setHelmet(this.skull);
      }
    } else {
      if (this.stand != null) {
        this.stand.getEquipment().setHelmet(null);
      }
    }
  }

  public String getType() {
    return this.type;
  }

  public UUID getUuid() {
    return this.uuid;
  }

  public void rotate(final double rad) {
    if (this.stand != null) {
      this.stand.setHeadPose(this.stand.getHeadPose().add(0.0, rad / 2400.0, 0.0));
    }
  }

  public boolean shouldDisappear() {
    return this.shouldDisappear;
  }

  @Override
  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof SpinningCrate)) {
      return false;
    }

    final SpinningCrate that = (SpinningCrate) other;
    return this.uuid.equals(that.uuid);
  }

  @Override
  public int hashCode() {
    return this.uuid.hashCode();
  }
}
