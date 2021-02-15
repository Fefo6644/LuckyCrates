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

package com.github.fefo.luckycrates.messages;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;

import static net.kyori.adventure.text.Component.join;
import static net.kyori.adventure.text.Component.newline;
import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.event.ClickEvent.suggestCommand;
import static net.kyori.adventure.text.event.HoverEvent.showText;
import static net.kyori.adventure.text.format.NamedTextColor.GOLD;
import static net.kyori.adventure.text.format.NamedTextColor.GRAY;
import static net.kyori.adventure.text.format.NamedTextColor.RED;
import static net.kyori.adventure.text.format.NamedTextColor.WHITE;
import static net.kyori.adventure.text.format.NamedTextColor.YELLOW;
import static net.kyori.adventure.text.format.TextDecoration.BOLD;
import static net.kyori.adventure.text.format.TextDecoration.UNDERLINED;
import static net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacyAmpersand;
import static net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection;

public interface Message {

  Component PREFIX =
      text()
          .color(GRAY)
          .append(text('['),
                  text('L', YELLOW, BOLD),
                  text('C', GOLD, BOLD),
                  text(']'))
          .build();

  Args1<Plugin> PLUGIN_INFO = plugin ->
      text()
          .color(YELLOW)
          .append(text("LuckyCrates"),
                  text(" - ", GRAY),
                  text('v'),
                  text(plugin.getDescription().getVersion()));

  Args1<String> LEGACY = legacy ->
      legacyAmpersand().deserialize(legacy);

  Args1<String> NO_PERMISSION = action ->
      prefixed()
          .color(RED)
          .append(text("You are not allowed to"),
                  space(),
                  text(action));

  Args0 USAGE_TITLE = () ->
      prefixed()
          .append(text("Usages:", WHITE));

  Args1<String> USAGE_COMMAND = command ->
      prefixed()
          .color(GRAY)
          .append(text('/'),
                  text(command))
          .hoverEvent(showText(text()
                                   .append(text("Click to run:", WHITE),
                                           space(),
                                           text('/', GRAY),
                                           text(command, GRAY))))
          .clickEvent(suggestCommand('/' + command));

  Args0 PLAYERS_ONLY = () ->
      prefixed()
          .append(text("Only players can run this command", RED));

  Args0 FILES_RELOADING_FAILED = () ->
      text()
          .color(RED)
          .append(join(text().append(newline(), prefixed()),
                       text("There was an error while reloading files"),
                       text("Please check the console for any errors")));

  Args0 FILES_RELOADING_SUCCESS = () ->
      prefixed()
          .append(text("Files reloaded successfully", YELLOW));

  Args0 ACTION_CANCELLED = () ->
      prefixed()
          .append(text("Action cancelled", YELLOW));

  Args1<String> CRATE_REMOVED = type ->
      prefixed()
          .append(text(type, GOLD, UNDERLINED),
                  space(),
                  text("crate removed", YELLOW));

  Args3<String, String, Location> CRATE_REMOVED_AT = (name, type, location) ->
      prefixed()
          .append(text(type, GOLD, UNDERLINED),
                  space(),
                  text("crate removed at", YELLOW),
                  space(),
                  text()
                      .color(GRAY)
                      .apply(builder -> {
                        final String blockX = "x:" + location.getBlockX();
                        final String blockY = "y:" + location.getBlockY();
                        final String blockZ = "z:" + location.getBlockZ();

                        builder
                            .append(join(space(),
                                         text(blockX),
                                         text(blockY),
                                         text(blockZ)))
                            .hoverEvent(showText(text()
                                                     .append(text("Click to teleport", WHITE))));
                      }))
          .apply(builder -> {
            final String x = String.format("%.2f", location.getX());
            final String y = String.format("%.2f", location.getY());
            final String z = String.format("%.2f", location.getZ());

            builder
                .clickEvent(suggestCommand("/teleport " + name + ' ' + x + ' ' + y + ' ' + z));
          });

  Args0 HIT_TO_REMOVE = () ->
      prefixed()
          .append(text("Hit a crate to remove it", YELLOW));

  Args0 RUN_TO_CANCEL = () ->
      prefixed()
          .append(text("Run the command again to cancel", YELLOW));

  Args0 NO_LOADED_CRATES = () ->
      prefixed()
          .append(text("There are no loaded crates to remove", RED));

  Args0 COULDNT_FIND_NEAREST = () ->
      prefixed()
          .append(text("Couldn't find the nearest crate within loaded chunks in this world", RED));

  Args0 ALREADY_OCCUPIED = () ->
      prefixed()
          .append(text("This place is already occupied by another crate", RED));

  Args0 SELECT_ANOTHER_LOCATION = () ->
      prefixed()
          .append(text("Please, select another location", RED));

  Args1<String> CRATE_PLACED = type ->
      prefixed()
          .append(text(type, GOLD, UNDERLINED),
                  space(),
                  text("crate placed successfully", YELLOW));

  static TextComponent.Builder prefixed() {
    return TextComponent.ofChildren(PREFIX, space()).toBuilder().resetStyle();
  }

  @FunctionalInterface
  interface Args0 {

    ComponentLike build();

    default void send(final Audience audience) {
      audience.sendMessage(build());
    }

    default String legacy() {
      return legacySection().serialize(build().asComponent());
    }
  }

  @FunctionalInterface
  interface Args1<T> {

    ComponentLike build(T t);

    default void send(final Audience audience, final T t) {
      audience.sendMessage(build(t));
    }

    default String legacy(final T t) {
      return legacySection().serialize(build(t).asComponent());
    }
  }

  @FunctionalInterface
  interface Args2<T, S> {

    ComponentLike build(T t, S s);

    default void send(final Audience audience, final T t, final S s) {
      audience.sendMessage(build(t, s));
    }

    default String legacy(final T t, final S s) {
      return legacySection().serialize(build(t, s).asComponent());
    }
  }

  @FunctionalInterface
  interface Args3<T, S, R> {

    ComponentLike build(T t, S s, R r);

    default void send(final Audience audience, final T t, final S s, final R r) {
      audience.sendMessage(build(t, s, r));
    }

    default String legacy(final T t, final S s, final R r) {
      return legacySection().serialize(build(t, s, r).asComponent());
    }
  }

  @FunctionalInterface
  interface Args4<T, S, R, Q> {

    ComponentLike build(T t, S s, R r, Q q);

    default void send(final Audience audience, final T t, final S s, final R r, final Q q) {
      audience.sendMessage(build(t, s, r, q));
    }

    default String legacy(final T t, final S s, final R r, final Q q) {
      return legacySection().serialize(build(t, s, r, q).asComponent());
    }
  }

  @FunctionalInterface
  interface Args5<T, S, R, Q, P> {

    ComponentLike build(T t, S s, R r, Q q, P p);

    default void send(final Audience audience, final T t, final S s, final R r, final Q q, final P p) {
      audience.sendMessage(build(t, s, r, q, p));
    }

    default String legacy(final T t, final S s, final R r, final Q q, final P p) {
      return legacySection().serialize(build(t, s, r, q, p).asComponent());
    }
  }
}
