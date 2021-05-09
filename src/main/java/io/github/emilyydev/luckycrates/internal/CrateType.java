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

import com.google.common.collect.ImmutableList;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.util.Base64;
import java.util.Collection;
import java.util.Optional;
import java.util.SplittableRandom;
import java.util.UUID;

public final class CrateType {

  private static final SplittableRandom RANDOM = new SplittableRandom();
  private static final Base64.Encoder ENCODER = Base64.getEncoder();
  private static final MethodHandle PROFILE_FIELD_SETTER;

  static {
    MethodHandle profileFieldSetter;
    final MethodHandles.Lookup lookup = MethodHandles.lookup();

    try {
      final ItemMeta itemMeta = Bukkit.getItemFactory().getItemMeta(Material.SKULL_ITEM);
      final Class<?> craftMetaSkullClass = itemMeta.getClass();
      final Field reflectedProfileField = craftMetaSkullClass.getDeclaredField("profile");
      reflectedProfileField.setAccessible(true);
      profileFieldSetter = lookup.unreflectSetter(reflectedProfileField);
    } catch (final Throwable throwable) {
      final MethodType dummyMethodType = MethodType.methodType(void.class, ItemMeta.class, GameProfile.class);
      try {
        profileFieldSetter = lookup.findStatic(CrateType.class, "dummy", dummyMethodType);
      } catch (final ReflectiveOperationException exception) {
        // won't be reached
        throw new RuntimeException(exception);
      }
    }

    PROFILE_FIELD_SETTER = profileFieldSetter;
  }

  private static void dummy(final ItemMeta meta, final GameProfile profile) { }

  private static ItemStack getCustomSkull(final String url) {
    final GameProfile profile = new GameProfile(UUID.randomUUID(), null);
    final byte[] encoded = ENCODER.encode(String.format("{textures:{SKIN:{url:\"%s\"}}}", url).getBytes());
    final Property property = new Property("textures", new String(encoded));
    profile.getProperties().put("textures", property);
    // many textures much wow

    final ItemStack itemStack = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
    final SkullMeta skullMeta = (SkullMeta) itemStack.getItemMeta();
    try {
      PROFILE_FIELD_SETTER.invoke(skullMeta, profile);
      itemStack.setItemMeta(skullMeta);
    } catch (final Throwable throwable) {
      throwable.printStackTrace();
    }

    return itemStack;
  }

  private final String permission;
  private final ItemStack skull;
  private final int secondsHiddenMin;
  private final int secondsHiddenMax;
  private final Collection<Loot> rewards;

  public CrateType(final String permission, final String texture,
                   final int secondsHiddenMin, final int secondsHiddenMax,
                   final Collection<Loot> rewards) {
    this.permission = permission == null || permission.equalsIgnoreCase("false") ? null : permission;
    this.skull = getCustomSkull("https://textures.minecraft.net/texture/" + texture);
    this.secondsHiddenMin = secondsHiddenMin;
    this.secondsHiddenMax = secondsHiddenMax;
    this.rewards = ImmutableList.copyOf(rewards);
  }

  public @NotNull ItemStack getSkull() {
    return this.skull;
  }

  public int getRandomTime() {
    return RANDOM.nextInt(this.secondsHiddenMin, this.secondsHiddenMax);
  }

  public Optional<String> getPermission() {
    return Optional.ofNullable(this.permission);
  }

  public Loot getRandomLoot() {
    int totalWeight = 0;
    for (final Loot loot : this.rewards) {
      totalWeight += loot.weight;
    }

    int randomPick = RANDOM.nextInt(totalWeight);
    for (final Loot loot : this.rewards) {
      if (randomPick < loot.weight) {
        return loot;
      } else {
        randomPick -= loot.weight;
      }
    }

    // This will never be reached but compilers are stupid.
    return this.rewards.iterator().next();
  }
}
