package me.fefo.luckycrates.internal;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.util.EulerAngle;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class SpinnyCrate {

  private final CratesManager manager;
  private final String crateName;
  private final UUID uuid;
  private final @Nullable ArmorStand as;
  private final boolean shouldDisappear;
  private long hiddenUntil = 0L;

  public SpinnyCrate(final CratesManager manager, final Location loc, final String crateName, final boolean shouldDisappear) {
    this.manager = manager;

    loc.setX(loc.getBlockX() + .5);
    loc.setY(loc.getBlockY() - 1.0);
    loc.setZ(loc.getBlockZ() + .5);
    loc.setYaw(.0f);
    loc.setPitch(.0f);
    as = (ArmorStand) loc.getWorld().spawnEntity(loc, EntityType.ARMOR_STAND);
    as.setHeadPose(EulerAngle.ZERO);
    as.setBodyPose(EulerAngle.ZERO);
    as.setLeftArmPose(EulerAngle.ZERO);
    as.setRightArmPose(EulerAngle.ZERO);
    as.setLeftLegPose(EulerAngle.ZERO);
    as.setRightLegPose(EulerAngle.ZERO);
    as.setGravity(false);
    as.setVisible(false);
    as.setBasePlate(false);
    as.setSmall(false);
    as.getEquipment().setHelmet(manager.categorizedCrates.get(crateName).getSkull());
    uuid = as.getUniqueId();

    this.crateName = crateName;
    this.shouldDisappear = shouldDisappear;
  }

  public SpinnyCrate(final CratesManager manager, final String crateName, final UUID uuid, final long hiddenUntil, final boolean shouldDisappear) {
    this.manager = manager;
    as = (ArmorStand) Bukkit.getEntity(uuid);
    this.crateName = crateName;
    this.uuid = uuid;
    this.hiddenUntil = hiddenUntil;
    this.shouldDisappear = shouldDisappear;
  }

  public @Nullable Location getLocation() {
    if (as != null) {
      return as.getEyeLocation();
    }
    return null;
  }

  public long getHiddenUntil() { return hiddenUntil; }
  public void setHiddenUntil(final long hiddenUntil) {
    this.hiddenUntil = hiddenUntil;
    if (hiddenUntil == 0L) {
      if (as == null) {
        return;
      }
      as.getEquipment().setHelmet(manager.categorizedCrates.get(crateName).getSkull());
    } else {
      if (as == null) {
        return;
      }
      as.getEquipment().setHelmet(null);
    }
  }

  public String getCrateName() {
    return crateName;
  }

  public UUID getUUID() {
    return uuid;
  }

  public void kill() {
    if (as != null) {
      as.remove();
    }
  }

  public boolean rotate(final double rad) {
    if (as == null) {
      return false;
    }
    as.setHeadPose(as.getHeadPose()
                     .add(.0,
                          rad / 1200.0,
                          .0));
    return true;
  }

  public boolean shouldDisappear() {
    return shouldDisappear;
  }

  @Override
  public boolean equals(@Nullable final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    return uuid.equals(((SpinnyCrate) o).uuid);
  }

  @Override
  public int hashCode() {
    return uuid.hashCode();
  }
}
