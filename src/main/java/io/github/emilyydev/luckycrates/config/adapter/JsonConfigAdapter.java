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

package io.github.emilyydev.luckycrates.config.adapter;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import io.github.emilyydev.luckycrates.LuckyCratesPlugin;
import io.github.emilyydev.luckycrates.config.ConfigAdapter;

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
                                             this.configPath);
        this.plugin.getLogger().warning(message);
        exception.printStackTrace();

        final String backup = String.format("config.%s.err.json", DATE_TIME_FORMATTER.format(Instant.now()));
        Files.move(this.configPath, this.configPath.resolveSibling(backup));
        createConfigTemplate();
      }
    }

    this.rootRaw.putAll(map != null ? map : ImmutableMap.of());
  }
}
