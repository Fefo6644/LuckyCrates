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


import com.github.fefo.luckycrates.internal.SpinningCrate;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.apache.commons.lang.Validate;
import org.bukkit.Location;

import java.io.IOException;
import java.util.UUID;

public final class SpinningCrateAdapter extends TypeAdapter<SpinningCrate> {

  public static final SpinningCrateAdapter ADAPTER = new SpinningCrateAdapter();

  private SpinningCrateAdapter() { }

  @Override
  public void write(final JsonWriter out, final SpinningCrate crate) throws IOException {
    out.beginObject();

    out.name("uuid").value(String.valueOf(crate.getUuid()));
    out.name("location");
    LocationAdapter.ADAPTER.write(out, crate.getLocation());
    out.name("type").value(crate.getType());
    out.name("hiddenUntil").value(crate.getHiddenUntil());
    out.name("shouldDisappear").value(crate.shouldDisappear());

    out.endObject();
  }

  @Override
  public SpinningCrate read(final JsonReader in) throws IOException {
    in.beginObject();

    UUID uuid = null;
    Location location = null;
    String type = null;
    Long hiddenUntil = null;
    Boolean shouldDisappear = null;

    while (in.hasNext()) {
      final String field = in.nextName();

      switch (field) {
        case "uuid":
          uuid = UUID.fromString(in.nextString());
          break;

        case "location":
          location = LocationAdapter.ADAPTER.read(in);
          break;

        case "type":
          type = in.nextString();
          break;

        case "hiddenUntil":
          hiddenUntil = in.nextLong();
          break;

        case "shouldDisappear":
          shouldDisappear = in.nextBoolean();
          break;

        default:
          in.skipValue();
          break;
      }
    }

    Validate.notNull(uuid, "uuid");
    Validate.notNull(location, "location");
    Validate.notNull(location.getWorld(), "location.getWorld()");
    Validate.notNull(type, "type");
    Validate.notNull(hiddenUntil, "hiddenUntil");
    Validate.notNull(shouldDisappear, "shouldDisappear");

    in.endObject();
    return new SpinningCrate(uuid, location, type, hiddenUntil, shouldDisappear);
  }
}
