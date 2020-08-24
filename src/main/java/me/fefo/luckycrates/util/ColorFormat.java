package me.fefo.luckycrates.util;

import net.md_5.bungee.api.ChatColor;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class ColorFormat {

  public static String format(@NotNull final String rawMessage) {
    Objects.requireNonNull(rawMessage);

    return ChatColor.translateAlternateColorCodes('&', rawMessage);
  }
}
