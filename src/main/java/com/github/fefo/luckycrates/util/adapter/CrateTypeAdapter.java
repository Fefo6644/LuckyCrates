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

package com.github.fefo.luckycrates.util.adapter;

import com.github.fefo.luckycrates.internal.CrateType;
import com.github.fefo.luckycrates.internal.Loot;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import org.apache.commons.lang.Validate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class CrateTypeAdapter extends TypeAdapter<CrateType> {

  public static final CrateTypeAdapter ADAPTER = new CrateTypeAdapter();

  private CrateTypeAdapter() { }

  @Override
  public void write(final JsonWriter out, final CrateType crateType) throws IOException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  @Override
  public CrateType read(final JsonReader in) throws IOException {
    in.beginObject();

    String permission = null;
    String texture = null;
    Integer secondsHiddenMin = null;
    Integer secondsHiddenMax = null;
    List<Loot> rewards = null;

    while (in.hasNext()) {
      switch (in.nextName()) {
        case "requiresPermission": {
          if (in.peek() == JsonToken.STRING) {
            permission = in.nextString();
          } else {
            in.skipValue();
          }
          break;
        }

        case "texture": {
          texture = in.nextString();
          break;
        }

        case "secondsHidden": {
          in.beginObject();

          while (in.hasNext()) {
            switch (in.nextName()) {
              case "min": {
                secondsHiddenMin = in.nextInt();
                break;
              }

              case "max": {
                secondsHiddenMax = in.nextInt();
                break;
              }

              default: {
                in.skipValue();
                break;
              }
            }
          }

          in.endObject();
          break;
        }

        case "rewards": {
          in.beginArray();

          rewards = new ArrayList<>();

          while (in.hasNext()) {
            rewards.add(LootAdapter.ADAPTER.read(in));
          }

          in.endArray();
          break;
        }

        default: {
          in.skipValue();
          break;
        }
      }
    }

    Validate.notNull(texture, "texture");
    Validate.notNull(secondsHiddenMin, "secondsHidden.min");
    Validate.notNull(secondsHiddenMax, "secondsHidden.max");
    Validate.isTrue(secondsHiddenMin <= secondsHiddenMax, "secondsHidden.min must be less than or equal to secondsHidden.max");
    Validate.notNull(rewards, "rewards");

    in.endObject();
    return new CrateType(permission, texture, secondsHiddenMin, secondsHiddenMax, rewards);
  }
}
