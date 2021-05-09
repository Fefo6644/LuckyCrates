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

package io.github.emilyydev.luckycrates.internal;

import io.github.emilyydev.luckycrates.LuckyCratesPlugin;
import io.github.emilyydev.luckycrates.util.adapter.CrateTypeAdapter;
import io.github.emilyydev.luckycrates.util.adapter.LocationAdapter;
import io.github.emilyydev.luckycrates.util.adapter.LootAdapter;
import io.github.emilyydev.luckycrates.util.adapter.SpinningCrateAdapter;
import io.github.emilyydev.luckycrates.util.adapter.WorldAdapter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class CrateMap implements Map<UUID, SpinningCrate> {

  private static final Type CRATE_SET_TYPE = TypeToken.getParameterized(Set.class, SpinningCrate.class).getType();
  public static final Gson GSON =
      new GsonBuilder()
          .registerTypeAdapter(World.class, WorldAdapter.ADAPTER)
          .registerTypeAdapter(Location.class, LocationAdapter.ADAPTER)
          .registerTypeAdapter(SpinningCrate.class, SpinningCrateAdapter.ADAPTER)
          .registerTypeAdapter(Loot.class, LootAdapter.ADAPTER)
          .registerTypeAdapter(CrateType.class, CrateTypeAdapter.ADAPTER)
          .setPrettyPrinting()
          .create();

  private static final PathMatcher JSON_MATCHER = FileSystems.getDefault().getPathMatcher("regex:\\w+\\.json");
  private static final Predicate<Path> IS_JSON_FILE = path -> {
    return Files.isRegularFile(path) && JSON_MATCHER.matches(path.getFileName());
  };
  private static final Predicate<Path> IS_EXAMPLE_CRATE = path -> path.endsWith("example.json");

  private final LuckyCratesPlugin plugin;
  private final Path cratesFolder;
  private final Path cratesLocationsFile;
  private final Map<String, CrateType> categorizedCrateTypes = new HashMap<>();
  private final Map<UUID, SpinningCrate> crates = new HashMap<>();

  public CrateMap(final LuckyCratesPlugin plugin) {
    this.plugin = plugin;
    this.cratesFolder = plugin.getDataFolderPath().resolve("crates");
    this.cratesLocationsFile = plugin.getDataFolderPath().resolve("crateslocations.json");
  }

  public void load() throws IOException {
    if (Files.notExists(this.cratesFolder)) {
      Files.createDirectories(this.cratesFolder);
      this.plugin.saveResource("crates/example.json", this.plugin.getDataFolderPath(), false);
      this.plugin.saveResource("crates/common.json", this.plugin.getDataFolderPath(), false);
      this.plugin.saveResource("crates/uncommon.json", this.plugin.getDataFolderPath(), false);
      this.plugin.saveResource("crates/rare.json", this.plugin.getDataFolderPath(), false);
      this.plugin.saveResource("crates/donor.json", this.plugin.getDataFolderPath(), false);
    }

    createCratesLocationsFile();
    reload();
  }

  public void reload() throws IOException {
    this.categorizedCrateTypes.clear();
    this.crates.clear();

    try (final Stream<Path> crates = Files.walk(this.cratesFolder)) {
      crates
          .filter(IS_JSON_FILE.and(IS_EXAMPLE_CRATE.negate()))
          .forEach(path -> {
            final String typeName = path.getFileName().toString().replace(".json", "");
            try (final BufferedReader reader = Files.newBufferedReader(path)) {
              final CrateType type = GSON.fromJson(reader, CrateType.class);
              if (type != null) {
                this.categorizedCrateTypes.put(typeName, type);
              } else {
                this.plugin.getLogger().warning(path + " is invalid");
              }
            } catch (final IOException exception) {
              throw new RuntimeException(exception);
            } catch (final JsonSyntaxException exception) {
              this.plugin.getLogger().warning(path + " is invalid");
              exception.printStackTrace();
            }
          });
    } catch (final RuntimeException exception) {
      if (exception.getCause() instanceof IOException) {
        throw (IOException) exception.getCause();
      }

      throw exception;
    }

    try (final BufferedReader reader = Files.newBufferedReader(this.cratesLocationsFile)) {
      final Set<SpinningCrate> crates = GSON.fromJson(reader, CRATE_SET_TYPE);
      if (crates != null) {
        this.crates.putAll(crates.stream()
                                 .peek(crate -> crate.setSkull(this.getCrateType(crate.getType()).getSkull()))
                                 .peek(SpinningCrate::summon)
                                 .collect(Collectors.toMap(SpinningCrate::getUuid, Function.identity())));
      } else {
        this.plugin.getLogger().warning("There was an error while reading " + this.cratesLocationsFile);
      }
    } catch (final JsonSyntaxException exception) {
      this.plugin.getLogger().warning("There was an error while reading " + this.cratesLocationsFile);
      exception.printStackTrace();
    }
  }

  public void save() throws IOException {
    try (final BufferedWriter writer = Files.newBufferedWriter(this.cratesLocationsFile)) {
      GSON.toJson(this.crates.values(), CRATE_SET_TYPE, writer);
    }
  }

  public Map<String, CrateType> getCategorizedCrateTypes() {
    return this.categorizedCrateTypes;
  }

  public CrateType getCrateType(final String category) {
    return this.categorizedCrateTypes.get(category);
  }

  public boolean isPlaceOccupied(@NotNull Location location) {
    location = location.getBlock().getLocation().clone();
    location.setX(location.getBlockX() + 0.5);
    location.setY(location.getBlockY() - 1.0);
    location.setZ(location.getBlockZ() + 0.5);

    return location.getWorld()
                   .getNearbyEntities(location, 0.0625, 0.0625, 0.0625)
                   .stream()
                   .filter(ArmorStand.class::isInstance)
                   .map(Entity::getUniqueId)
                   .anyMatch(this.crates::containsKey);
  }

  public void summon(final UUID uuid) {
    this.crates.get(uuid).summon();
  }

  @Override
  public void clear() {
    this.crates.values().forEach(SpinningCrate::unload);
    this.crates.clear();
  }

  @Override
  public int size() {
    return this.crates.size();
  }

  @Override
  public boolean isEmpty() {
    return this.crates.isEmpty();
  }

  @Override
  public boolean containsKey(final Object key) {
    return this.crates.containsKey(key);
  }

  @Override
  public boolean containsValue(final Object value) {
    return this.crates.containsValue(value);
  }

  @Override
  public SpinningCrate get(final Object key) {
    return this.crates.get(key);
  }

  @Override
  public @Nullable SpinningCrate put(final UUID key, final SpinningCrate value) {
    value.setSkull(this.categorizedCrateTypes.get(value.getType()).getSkull());
    return this.crates.put(key, value);
  }

  @Override
  public SpinningCrate remove(final Object key) {
    final SpinningCrate crate = this.crates.remove(key);
    crate.kill();
    return crate;
  }

  public SpinningCrate unload(final UUID key) {
    final SpinningCrate crate = this.crates.get(key);
    crate.unload();
    return crate;
  }

  @Override
  public void putAll(final @NotNull Map<? extends UUID, ? extends SpinningCrate> map) {
    map.values().forEach(crate -> crate.setSkull(this.categorizedCrateTypes.get(crate.getType()).getSkull()));
    this.crates.putAll(map);
  }

  @Override
  public @NotNull Set<UUID> keySet() {
    return this.crates.keySet();
  }

  @Override
  public @NotNull Collection<SpinningCrate> values() {
    return this.crates.values();
  }

  @Override
  public @NotNull Set<Entry<UUID, SpinningCrate>> entrySet() {
    return this.crates.entrySet();
  }

  private void createCratesLocationsFile() throws IOException {
    if (Files.notExists(this.cratesLocationsFile)) {
      try (final BufferedWriter writer = Files.newBufferedWriter(this.cratesLocationsFile)) {
        writer.write("[]");
        writer.newLine();
      }
    }
  }
}
