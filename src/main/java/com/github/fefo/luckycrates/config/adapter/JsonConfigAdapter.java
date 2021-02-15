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

package com.github.fefo.luckycrates.config.adapter;

import com.github.fefo.luckycrates.LuckyCratesPlugin;
import com.github.fefo.luckycrates.config.ConfigAdapter;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;

public final class JsonConfigAdapter extends ConfigAdapter {

  private static final Type CONFIG_TYPE = TypeToken.getParameterized(Map.class, String.class, Object.class).getType();
  private static final Gson GSON = new Gson();

  public JsonConfigAdapter(final LuckyCratesPlugin plugin, final Path dataFolder) {
    super(plugin, dataFolder, "config.json");
  }

  @Override
  protected void reload(final boolean force) throws IOException {
    Map<String, Object> map;

    try (final Reader reader = Files.newBufferedReader(this.configPath)) {
      try {
        map = GSON.fromJson(reader, CONFIG_TYPE);
      } catch (final JsonParseException exception) {
        map = ImmutableMap.of();

        final String message = String.format("There was an error reading %s, making backup and generating an empty JSON file. "
                                             + "Please send the faulty file to the plugin author!",
                                             this.configPath.toString());
        this.plugin.getSLF4JLogger().warn(message, exception);

        final String backup = String.format("config.%s.err.json", DATE_TIME_FORMATTER.format(Instant.now()));
        Files.move(this.configPath, this.configPath.resolveSibling(backup));
        createConfigTemplate();
      }
    }

    this.rootRaw.putAll(map != null ? map : ImmutableMap.of());
  }
}
