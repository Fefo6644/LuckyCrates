package me.fefo.luckycrates;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class CommanderKeen implements CommandExecutor, TabCompleter {
  private final LuckyCrates plugin;

  public CommanderKeen(@NotNull LuckyCrates plugin) { this.plugin = plugin; }

  @Override
  public boolean onCommand(CommandSender sender,
                           Command cmd,
                           String alias,
                           String[] args) {
    if (!(sender instanceof Player)) {
      sender.sendMessage(ChatColor.RED + "Only players can use this command");
      return true;
    }

    switch (args.length) {
      case 0: {
        sender.sendMessage(ChatColor.DARK_AQUA + "LuckyCrates " +
                           ChatColor.GRAY + "- " +
                           ChatColor.AQUA + "v" + plugin.getDescription().getVersion());
        return true;
      }

      case 1: {
        if (args[0].equalsIgnoreCase("reload")) {
          plugin.reload();
          sender.sendMessage(ChatColor.AQUA + "Files reloaded successfully!");
          return true;
        }

        if (!args[0].equalsIgnoreCase("remove")) {
          return false;
        }

        if (plugin.spinnyCrates.size() > 0) {
          if (plugin.playersRemovingCrate.remove(((Player) sender).getUniqueId())) {
            sender.sendMessage(ChatColor.AQUA + "Action cancelled");
          } else {
            plugin.playersRemovingCrate.add(((Player) sender).getUniqueId());
            final String[] messages = new String[] {
                ChatColor.AQUA + "Hit a crate to remove it",
                ChatColor.AQUA + "Run the command again to cancel"
            };
            sender.sendMessage(messages);
          }
        } else {
          sender.sendMessage(ChatColor.RED + "There are no crates to remove");
        }

        return true;
      }

      case 2: {
        final boolean shouldDisappear;
        if (args[0].equalsIgnoreCase("setpersistent")) {
          shouldDisappear = false;
        } else if (args[0].equalsIgnoreCase("set")) {
          shouldDisappear = true;
        } else {
          return false;
        }

        final Location loc = ((Player) sender).getLocation().clone();

        if (SpinnyCrate.isPlaceOccupied(loc)) {
          final String[] messages = new String[] {
              ChatColor.RED + "This place is already occupied by another crate!",
              ChatColor.RED + "Please, select another location"
          };
          sender.sendMessage(messages);
        } else {
          final SpinnyCrate sc = new SpinnyCrate(loc, args[1], shouldDisappear);
          plugin.spinnyCrates.put(sc.getUUID(), sc);
          sender.sendMessage(ChatColor.AQUA + "Crate placed successfully!");

          final ConfigurationSection cs = plugin.cratesDataYaml.createSection(sc.getUUID().toString());
          cs.set(LuckyCrates.YAML_HIDDEN_UNTIL, 0L);
          cs.set(LuckyCrates.YAML_SHOULD_DISAPPEAR, shouldDisappear);
          cs.set(LuckyCrates.YAML_CRATE_TYPE, args[1]);
          try {
            plugin.cratesDataYaml.save(plugin.cratesDataFile);
          } catch (IOException e) {
            plugin.getLogger().severe("Could not save data file!");
            e.printStackTrace();
          }
        }

        return true;
      }

      default:
        return false;
    }
  }

  @Override
  public List<String> onTabComplete(CommandSender sender,
                                    Command cmd,
                                    String alias,
                                    String[] args) {
    final ArrayList<String> ret = new ArrayList<>();

    if (args.length == 1) {
      if ("setpersistent".startsWith(args[0])) {
        ret.add("setpersistent");
      }

      if ("set".startsWith(args[0])) {
        ret.add("set");
      }

      if ("remove".startsWith(args[0])) {
        ret.add("remove");
      }

      if ("reload".startsWith(args[0])) {
        ret.add("reload");
      }
    } else if (args.length == 2 &&
               args[0].startsWith("set")) {
      for (String crate : SpinnyCrate.categorisedCrates.keySet()) {
        if (crate.startsWith(args[1])) {
          ret.add(crate);
        }
      }
    }

    return ret;
  }
}
