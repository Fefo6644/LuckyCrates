//
// This file is part of LuckyCrates, licensed under the MIT License.
//
// Copyright (c) 2021 Fefo6644 <federico.lopez.1999@outlook.com>
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

package com.github.fefo.luckycrates.internal;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import com.github.fefo.luckycrates.LuckyCratesPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.SplittableRandom;
import java.util.UUID;

public final class CrateType {

  private static final SplittableRandom RANDOM = new SplittableRandom();
  private static final Base64.Encoder ENCODER = Base64.getEncoder();

  static {
    try {
      // Workaround to not trigger 'Illegal reflective access' warning
      final Class<?> moduleClass = Class.forName("java.lang.Module");
      final Method getModuleMethod = Class.class.getMethod("getModule");
      final Method addOpensMethod = moduleClass.getMethod("addOpens", String.class, moduleClass);

      final Object splittableRandomModule = getModuleMethod.invoke(SplittableRandom.class);
      final Object crateDataModule = getModuleMethod.invoke(CrateType.class);

      addOpensMethod.invoke(splittableRandomModule, SplittableRandom.class.getPackage().getName(), crateDataModule);
    } catch (final Throwable exception) {
      // Means we are on Java 8 or below, nothing to worry about
    }

    try {
      final Field seedField = SplittableRandom.class.getDeclaredField("seed");
      seedField.setAccessible(true);
      LuckyCratesPlugin.LOGGER.info(String.format("CrateData random seed: %d", seedField.getLong(RANDOM)));
    } catch (final ReflectiveOperationException exception) {
      // ¯\_(ツ)_/¯
    }
  }

  private static ItemStack getCustomSkull(final String url) {
    final PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID());
    final byte[] encoded = ENCODER.encode(String.format("{textures:{SKIN:{url:\"%s\"}}}", url).getBytes());
    final ProfileProperty property = new ProfileProperty("textures", new String(encoded));
    profile.setProperty(property);

    final ItemStack itemStack = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
    final SkullMeta skullMeta = (SkullMeta) itemStack.getItemMeta();
    skullMeta.setPlayerProfile(profile);
    itemStack.setItemMeta(skullMeta);

    return itemStack;
  }

  private final String permission;
  private final ItemStack skull;
  private final int secondsHiddenMin;
  private final int secondsHiddenMax;
  private final List<Loot> rewards;

  public CrateType(final String permission, final String texture,
                   final int secondsHiddenMin, final int secondsHiddenMax, final Collection<Loot> rewards) {
    this.permission = permission == null || permission.equalsIgnoreCase("false") ? null : permission;
    this.skull = getCustomSkull("https://textures.minecraft.net/texture/" + texture);
    this.secondsHiddenMin = secondsHiddenMin;
    this.secondsHiddenMax = secondsHiddenMax;
    this.rewards = new ArrayList<>(rewards);
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
    return this.rewards.get(0);
  }
}
