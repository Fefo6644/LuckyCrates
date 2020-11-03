package me.fefo.luckycrates;

import me.fefo.facilites.VariousUtils;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.command.CommandSender;

import static net.md_5.bungee.api.ChatColor.GOLD;
import static net.md_5.bungee.api.ChatColor.GRAY;
import static net.md_5.bungee.api.ChatColor.RED;
import static net.md_5.bungee.api.ChatColor.YELLOW;
import static net.md_5.bungee.api.chat.ComponentBuilder.FormatRetention.NONE;

public enum Message {
  PREFIX(builder("[").color(GRAY)
                     .append("L").bold(true).color(YELLOW)
                     .append("C").color(GOLD)
                     .append("] ", NONE).color(GRAY)),
  VERSION(builder("Lucky").color(YELLOW)
                          .append("Crates").color(GOLD)
                          .append(" - ").color(GRAY)
                          .append("v{0}").color(YELLOW)),
  USAGE_TITLE(prefixed("Usages:").color(RED)),
  USAGE_COMMAND(builder("/{0}").color(RED)
                               .event(hover(builder("/{0}").color(RED)))
                               .event(suggest("/{0}"))),
  PLAYERS_ONLY(prefixed("Only players can run this command!").color(RED)),
  FILES_RELOADED(prefixed("Files reloaded successfully!").color(YELLOW)),
  ACTION_CANCELLED(prefixed("Action cancelled").color(YELLOW)),
  CRATE_REMOVED(prefixed("{0}").underlined(true).color(GOLD)
                               .append(" crate removed", NONE).color(YELLOW)),
  CRATE_REMOVED_AT(prefixed("{0}").underlined(true).color(GOLD)
                                  .append(" crate removed at ", NONE).color(YELLOW)
                                  .append("x:{1} y:{2} z:{3}").color(GRAY)
                                  .event(hover(builder("Click to teleport").color(GRAY)))
                                  .event(suggest("/teleport {-1} {1} {2} {3}"))),
  HIT_TO_REMOVE(prefixed("Hit a crate to remove it").color(YELLOW)),
  RUN_TO_CANCEL(prefixed("Run the command again to cancel").color(YELLOW)),
  NO_LOADED_CRATES(prefixed("There are no loaded crates to remove").color(RED)),
  COULDNT_FIND_NEAREST(prefixed("Couldn't find the nearest crate within loaded chunks in this world").color(RED)),
  ALREADY_OCCUPIED(prefixed("This place is already occupied by another crate!").color(RED)),
  SELECT_ANOTHER_LOCATION(prefixed("Please, select another location").color(RED)),
  CRATE_PLACED(prefixed("{0}").underlined(true).color(GOLD)
                              .append(" crate placed successfully!", NONE).color(YELLOW));

  private static ComponentBuilder prefixed(final String text) {
    return new ComponentBuilder(new TextComponent(PREFIX.components)).append(text, NONE);
  }

  private static ComponentBuilder builder(final String text) {
    return new ComponentBuilder(text);
  }

  private static HoverEvent hover(final ComponentBuilder builder) {
    return new HoverEvent(HoverEvent.Action.SHOW_TEXT, builder.create());
  }

  private static HoverEvent hover(final BaseComponent[] components) {
    return new HoverEvent(HoverEvent.Action.SHOW_TEXT, components);
  }

  private static ClickEvent suggest(final String text) {
    return new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, text);
  }

  private static ClickEvent run(final String text) {
    return new ClickEvent(ClickEvent.Action.RUN_COMMAND, text);
  }

  private static ClickEvent openUrl(final String text) {
    return new ClickEvent(ClickEvent.Action.OPEN_URL, text);
  }

  private final BaseComponent[] components;

  Message(final ComponentBuilder builder) {
    components = builder.create();
  }

  public void send(final CommandSender target, final String... replacements) {
    VariousUtils.sendMessage(target, replace(components, target.getName(), replacements));
  }

  private BaseComponent[] replace(final BaseComponent[] components, final String target, final String... replacements) {
    final BaseComponent[] copies = new BaseComponent[components.length];

    for (int i = 0, componentsLength = components.length; i < componentsLength; ++i) {
      final BaseComponent component = components[i];
      final BaseComponent copy = component.duplicate();
      copies[i] = copy;

      for (int j = -1; j < replacements.length; ++j) {
        final String replacement = j == -1 ? target : replacements[j];

        if (copy instanceof TextComponent) {
          ((TextComponent) copy).setText(((TextComponent) copy).getText().replace("{" + j + "}", replacement));
        }

        if (copy.getClickEvent() != null) {
          copy.setClickEvent(suggest(copy.getClickEvent().getValue().replace("{" + j + "}", replacement)));
        }
      }

      if (copy.getHoverEvent() != null) {
        copy.setHoverEvent(hover(replace(copy.getHoverEvent().getValue(), target, replacements)));
      }
    }

    return copies;
  }
}
