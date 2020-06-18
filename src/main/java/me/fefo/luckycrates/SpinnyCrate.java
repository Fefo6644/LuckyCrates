package me.fefo.luckycrates;

import me.fefo.luckycrates.utils.CrateData;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.util.EulerAngle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.UUID;

public final class SpinnyCrate {
  private static Main main;
  public static final HashMap<String, CrateData> categorisedCrates = new HashMap<>();

  private final String crateName;
  private final @NotNull UUID uuid;
  private final @Nullable ArmorStand as;
  private final boolean shouldDisappear;
  private long hiddenUntil = 0L;

  public static void setMain(Main main) { SpinnyCrate.main = main; }

  public static void reloadCrates() {
    categorisedCrates.clear();
    for (File crateFile : main.cratesListFolder.listFiles()) {
      if (!crateFile.getName().endsWith(".yml")) {
        continue;
      }
      if (crateFile.getName().equalsIgnoreCase("example.yml")) {
        continue;
      }

      try {
        final CrateData crateData = new CrateData(crateFile.getName().replace(".yml", ""),
                                                  YamlConfiguration.loadConfiguration(crateFile));
        categorisedCrates.put(crateData.getName(), crateData);
      } catch (AssertionError ex) {
        main.getLogger().warning("Crate \"" + crateFile.getName() + "\" not loaded!");
        main.getLogger().warning(ex.getMessage());
      }
    }
  }

  public static boolean isPlaceOccupied(@NotNull final Location location) {
    final Location loc = location.clone();
    loc.setX(loc.getBlockX() + .5);
    loc.setY(loc.getBlockY() - 1.0);
    loc.setZ(loc.getBlockZ() + .5);
    final Collection<Entity> nearbyEntities = loc.getWorld().getNearbyEntities(loc, .0625, .0625, .0625);

    for (Entity e : nearbyEntities) {
      if (e instanceof ArmorStand &&
          main.spinnyCrates.containsKey(e.getUniqueId())) {
        return true;
      }
    }
    return false;
  }

  public SpinnyCrate(@NotNull final Location loc,
                     @NotNull final String crateName,
                     final boolean shouldDisappear) {
    loc.setX(loc.getBlockX() + .5);
    loc.setY(loc.getBlockY() - 1.0);
    loc.setZ(loc.getBlockZ() + .5);
    loc.setYaw(.0f);
    loc.setPitch(.0f);
    as = ((ArmorStand)loc.getWorld().spawnEntity(loc, EntityType.ARMOR_STAND));
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
    as.getEquipment().setHelmet(categorisedCrates.get(crateName).getSkull());
    uuid = as.getUniqueId();

    this.crateName = crateName;
    this.shouldDisappear = shouldDisappear;
  }

  public SpinnyCrate(@NotNull final String crateName,
                     @NotNull final UUID uuid,
                     final long hiddenUntil,
                     final boolean shouldDisappear) {
    as = ((ArmorStand)main.getServer().getEntity(uuid));
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
  public void setHiddenUntil(long hiddenUntil) {
    this.hiddenUntil = hiddenUntil;
    if (hiddenUntil == 0L) {
      if (as == null) {
        return;
      }
      as.getEquipment().setHelmet(categorisedCrates.get(crateName).getSkull());
    } else {
      if (as == null) {
        return;
      }
      as.getEquipment().setHelmet(null);
    }
  }

  public String getCrateName() { return crateName; }

  public @NotNull UUID getUUID() { return uuid; }
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

  public boolean shouldDisappear() { return shouldDisappear; }

  @Override
  public boolean equals(@Nullable Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SpinnyCrate that = (SpinnyCrate)o;
    return uuid.equals(that.uuid);
  }

  @Override
  public int hashCode() { return uuid.hashCode(); }
}