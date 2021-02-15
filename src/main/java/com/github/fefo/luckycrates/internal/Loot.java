package com.github.fefo.luckycrates.internal;

import com.google.common.collect.ImmutableList;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

public final class Loot implements Comparable<Loot> {

  public final int weight;
  public final List<String> commands;
  public final List<ItemStack> items;

  public Loot(final int weight, final Collection<String> commands, final Collection<ItemStack> items) {
    this.weight = weight;
    this.commands = ImmutableList.copyOf(commands);
    this.items = ImmutableList.copyOf(items);
  }

  @Override
  public int compareTo(final @NotNull Loot that) {
    // higher weighted rewards come first
    return that.weight - this.weight;
  }
}
