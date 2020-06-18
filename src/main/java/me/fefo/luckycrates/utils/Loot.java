package me.fefo.luckycrates.utils;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public final class Loot {
  public final int rarity;
  public final String[] commands;
  public final ItemStack[] items;

  public Loot(final int rarity,
              final @NotNull String[] commands,
              final @NotNull ItemStack[] items) {
    this.rarity = rarity;
    this.commands = commands;
    this.items = items.clone();
  }
}
