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

package io.github.emilyydev.luckycrates.messages;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;

import static net.kyori.adventure.text.Component.join;
import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.event.ClickEvent.openUrl;
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
                  text(plugin.getDescription().getVersion()))
          .hoverEvent(text("github.com/emilyy-dev/LuckyCrates", GRAY))
          .clickEvent(openUrl("https://github.com/emilyy-dev/LuckyCrates"));

  Args1<String> LEGACY = legacy -> legacyAmpersand().deserialize(legacy);

  Args1<String> NO_PERMISSION = action -> prefixed(
      text("You are not allowed to", RED),
      space(),
      text(action, RED));

  Args0 USAGE_TITLE = () -> prefixed(text("Usages:", WHITE));

  Args1<String> USAGE_COMMAND = command -> prefixed(
      text()
          .color(GRAY)
          .content('/' + command)
          .hoverEvent(showText(text()
                                   .append(text("Click to run:", WHITE),
                                           space(),
                                           text('/', GRAY),
                                           text(command, GRAY))))
          .clickEvent(suggestCommand('/' + command)));

  Args0 PLAYERS_ONLY = () -> prefixed(text("Only players can run this command", RED));

  Args0 FILES_RELOADING_FAILED = () -> prefixed(text("There was an error while reloading files", RED));

  Args0 FILES_RELOADING_SUCCESS = () -> prefixed(text("Files reloaded successfully", YELLOW));

  Args0 ACTION_CANCELLED = () -> prefixed(text("Action cancelled", YELLOW));

  Args1<String> CRATE_REMOVED = type -> prefixed(
      text(type, GOLD, UNDERLINED),
      space(),
      text("crate removed", YELLOW));

  Args3<String, String, Location> CRATE_REMOVED_AT = (name, type, location) -> prefixed(
      text()
          .append(
              text(type, GOLD, UNDERLINED),
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
                        .hoverEvent(showText(text("Click to teleport", WHITE)));
                  }))
          .apply(builder -> {
            final String x = String.format("%.2f", location.getX());
            final String y = String.format("%.2f", location.getY());
            final String z = String.format("%.2f", location.getZ());
            final String command = String.format("/teleport %s %s %s %s", name, x, y, z);

            builder.clickEvent(suggestCommand(command));
          }));

  Args0 HIT_TO_REMOVE = () -> prefixed(text("Hit a crate to remove it", YELLOW));

  Args0 RUN_TO_CANCEL = () -> prefixed(text("Run the command again to cancel", YELLOW));

  Args0 NO_LOADED_CRATES = () -> prefixed(text("There are no loaded crates to remove", RED));

  Args0 COULDNT_FIND_NEAREST = () -> prefixed(text("Couldn't find the nearest crate within loaded chunks in this world", RED));

  Args0 ALREADY_OCCUPIED = () -> prefixed(text("This place is already occupied by another crate", RED));

  Args0 SELECT_ANOTHER_LOCATION = () -> prefixed(text("Please, select another location", RED));

  Args1<String> CRATE_PLACED = type -> prefixed(
      text(type, GOLD, UNDERLINED),
      space(),
      text("crate placed successfully", YELLOW));

  Args0 CHECK_CONSOLE_FOR_ERRORS = () -> prefixed(text("Please check the console for any errors", RED));

  static TextComponent prefixed(final ComponentLike first, final ComponentLike... rest) {
    if (rest.length == 0) {
      return text()
          .append(PREFIX)
          .append(space())
          .append(first)
          .build();
    } else {
      return text()
          .append(PREFIX)
          .append(space())
          .append(first)
          .append(space())
          .append(rest)
          .build();
    }
  }

  @FunctionalInterface
  interface Args0 {

    ComponentLike build();

    default void send(final Audience audience) {
      audience.sendMessage(build());
    }
  }

  @FunctionalInterface
  interface Args1<T> {

    ComponentLike build(T t);

    default void send(final Audience audience, final T t) {
      audience.sendMessage(build(t));
    }
  }

  @FunctionalInterface
  interface Args3<T, S, R> {

    ComponentLike build(T t, S s, R r);

    default void send(final Audience audience, final T t, final S s, final R r) {
      audience.sendMessage(build(t, s, r));
    }
  }
}
