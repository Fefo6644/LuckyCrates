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

package io.github.emilyydev.luckycrates.util.adapter;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import io.github.emilyydev.luckycrates.internal.CrateMap;
import io.github.emilyydev.luckycrates.internal.CrateType;
import io.github.emilyydev.luckycrates.internal.Loot;
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
            rewards.add(CrateMap.GSON.fromJson(in, Loot.class));
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
    Validate.notEmpty(rewards, "rewards cannot be an empty list");

    in.endObject();
    return new CrateType(permission, texture, secondsHiddenMin, secondsHiddenMax, rewards);
  }
}
