package me.fefo.luckycrates.util;

import org.bukkit.inventory.ItemStack;

public final class Loot {

  public final int rarity;
  public final String[] commands;
  public final ItemStack[] items;

  public Loot(final int rarity, final String[] commands, final ItemStack... items) {
    this.rarity = rarity;
    this.commands = commands;
    this.items = items.clone();
  }
}
