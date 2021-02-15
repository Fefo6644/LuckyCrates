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
