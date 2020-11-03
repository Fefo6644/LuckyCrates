package me.fefo.luckycrates.internal;

import me.fefo.luckycrates.LuckyCrates;
import me.fefo.luckycrates.util.CrateData;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;

public class CratesManager {

  private final LuckyCrates plugin;
  public final HashMap<String, CrateData> categorizedCrates = new HashMap<>();

  public CratesManager(final LuckyCrates plugin) {
    this.plugin = plugin;
  }

  public void reloadCrates() {
    categorizedCrates.clear();
    for (File crateFile : plugin.cratesListFolder.listFiles()) {
      if (!crateFile.getName().endsWith(".yml")) {
        continue;
      }

      if (crateFile.getName().equalsIgnoreCase("example.yml")) {
        continue;
      }

      try {
        final CrateData crateData = new CrateData(crateFile.getName().replace(".yml", "").replace(' ', '_'),
                                                  YamlConfiguration.loadConfiguration(crateFile));
        categorizedCrates.put(crateData.getName(), crateData);
      } catch (AssertionError ex) {
        plugin.getLogger().warning("Crate \"" + crateFile.getName() + "\" not loaded!");
        plugin.getLogger().warning(ex.getMessage());
      }
    }
  }

  public boolean isPlaceOccupied(@NotNull final Location location) {
    final Location loc = location.clone();
    loc.setX(loc.getBlockX() + .5);
    loc.setY(loc.getBlockY() - 1.0);
    loc.setZ(loc.getBlockZ() + .5);
    final Collection<Entity> nearbyEntities = loc.getWorld().getNearbyEntities(loc, .0625, .0625, .0625);

    for (final Entity e : nearbyEntities) {
      if (e instanceof ArmorStand && plugin.spinnyCrates.containsKey(e.getUniqueId())) {
        return true;
      }
    }
    return false;
  }

}
