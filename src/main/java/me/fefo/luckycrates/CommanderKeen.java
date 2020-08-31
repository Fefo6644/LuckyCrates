package me.fefo.luckycrates;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.tree.RootCommandNode;
import me.fefo.facilites.ColorFormat;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class CommanderKeen implements TabExecutor {
  private final RootCommandNode<Player> rootNode = new RootCommandNode<>();
  private final CommandDispatcher<Player> dispatcher = new CommandDispatcher<>(rootNode);
  private final RootCommandNode<ConsoleCommandSender> consoleRootNode = new RootCommandNode<>();
  private final CommandDispatcher<ConsoleCommandSender> consoleDispatcher = new CommandDispatcher<>(consoleRootNode);
  private final LuckyCrates plugin;

  public CommanderKeen(@NotNull final LuckyCrates plugin) {
    this.plugin = plugin;

    final LiteralArgumentBuilder<Player> builder = LiteralArgumentBuilder.literal("lc");

    builder.executes(this::printVersion)
           .then(LiteralArgumentBuilder.<Player>literal("nearest")
                     .executes(this::teleportNearest))

           .then(LiteralArgumentBuilder.<Player>literal("reload")
                     .executes(this::reload))

           .then(LiteralArgumentBuilder.<Player>literal("remove")
                     .executes(this::remove)

                     .then(LiteralArgumentBuilder.<Player>literal("nearest")
                               .executes(this::removeNearest)))

           .then(LiteralArgumentBuilder.<Player>literal("set")
                     .then(RequiredArgumentBuilder.<Player, String>argument("type", StringArgumentType.word())

                               .suggests((context, suggestionsBuilder) -> {
                                 argsFilterer(suggestionsBuilder.getRemaining(),
                                              SpinnyCrate.categorizedCrates.keySet(),
                                              String::toString).forEach(suggestionsBuilder::suggest);

                                 return suggestionsBuilder.buildFuture();
                               })
                               .executes(this::set)))

           .then(LiteralArgumentBuilder.<Player>literal("setpersistent")
                     .then(RequiredArgumentBuilder.<Player, String>argument("type", StringArgumentType.word())

                               .suggests((context, suggestionsBuilder) -> {
                                 argsFilterer(suggestionsBuilder.getRemaining(),
                                              SpinnyCrate.categorizedCrates.keySet(),
                                              String::toString).forEach(suggestionsBuilder::suggest);

                                 return suggestionsBuilder.buildFuture();
                               })
                               .executes(this::setPersistent)));

    rootNode.addChild(builder.build());


    final LiteralArgumentBuilder<ConsoleCommandSender> consoleBuilder = LiteralArgumentBuilder.literal("lc");

    consoleBuilder.executes(this::printVersion)
                  .then(LiteralArgumentBuilder.<ConsoleCommandSender>literal("reload")
                            .executes(this::reload));
    consoleRootNode.addChild(consoleBuilder.build());
  }

  @Override
  public boolean onCommand(@NotNull final CommandSender sender,
                           @NotNull final Command command,
                           @NotNull final String label,
                           @NotNull final String[] args) {
    final String cmd = ("lc " + String.join(" ", args)).trim();

    if (!(sender instanceof Player)) {
      final ParseResults<ConsoleCommandSender> result = consoleDispatcher.parse(cmd, ((ConsoleCommandSender) sender));

      try {
        consoleDispatcher.execute(result);
      } catch (CommandSyntaxException exception) {
        sender.sendMessage(ColorFormat.format("&cConsole usages:"));
        sender.sendMessage(ColorFormat.format("  &c/luckycrates [reload]"));
      }

      return true;
    }

    final ParseResults<Player> result = dispatcher.parse(cmd, ((Player) sender));

    try {
      dispatcher.execute(result);
    } catch (CommandSyntaxException exception) {
      return false;
    }

    return true;
  }

  @Override
  public List<String> onTabComplete(@NotNull final CommandSender sender,
                                    @NotNull final Command command,
                                    @NotNull final String alias,
                                    @NotNull final String[] args) {
    final String cmd = "lc " + String.join(" ", args);

    if (sender instanceof Player) {
      final ParseResults<Player> result = dispatcher.parse(cmd, ((Player) sender));

      return dispatcher.getCompletionSuggestions(result)
                       .join()
                       .getList()
                       .stream()
                       .map(Suggestion::getText)
                       .collect(Collectors.toList());
    } else {
      final ParseResults<ConsoleCommandSender> result = consoleDispatcher.parse(cmd, ((ConsoleCommandSender) sender));

      return consoleDispatcher.getCompletionSuggestions(result)
                              .join()
                              .getList()
                              .stream()
                              .map(Suggestion::getText)
                              .collect(Collectors.toList());
    }
  }

  private <T> List<String> argsFilterer(final String cursor, final Collection<T> validValues, final Function<T, String> toString) {
    return validValues.stream()
                      .map(toString)
                      .filter(s -> s.startsWith(cursor))
                      .collect(Collectors.toList());
  }

  private int printVersion(final CommandContext<? extends CommandSender> context) {
    context.getSource().sendMessage(ColorFormat.format("&3LuckyCrates &7- &bv"
                                                       + plugin.getDescription().getVersion()));
    return 0;
  }

  private int teleportNearest(final CommandContext<Player> context) {
    final Player player = context.getSource();

    return 0;
  }

  private int reload(final CommandContext<? extends CommandSender> context) {
    plugin.reloadConfig();
    context.getSource().sendMessage(ColorFormat.format("&bFiles reloaded successfully!"));
    return 0;
  }

  private int remove(final CommandContext<Player> context) {
    final Player player = context.getSource();

    if (plugin.spinnyCrates.size() > 0) {
      if (plugin.playersRemovingCrate.remove(player.getUniqueId())) {
        player.sendMessage(ColorFormat.format("&bAction cancelled"));
      } else {
        plugin.playersRemovingCrate.add(player.getUniqueId());
        player.sendMessage(ColorFormat.format("&bHit a crate to remove it"));
        player.sendMessage(ColorFormat.format("&bRun the command again to cancel"));
      }
    } else {
      player.sendMessage(ColorFormat.format("&cThere are no crates to remove"));
    }
    return 0;
  }

  private int removeNearest(final CommandContext<Player> context) {
    final Player player = context.getSource();

    return 0;
  }

  private int set(final CommandContext<Player> context) {
    final Player player = context.getSource();
    final String crateType = context.getArgument("type", String.class);
    createCrate(player, true, crateType);

    return 0;
  }

  private int setPersistent(final CommandContext<Player> context) {
    final Player player = context.getSource();
    final String crateType = context.getArgument("type", String.class);
    createCrate(player, false, crateType);

    return 0;
  }

  private void createCrate(final Player player, final boolean shouldDisappear, final String type) {
    final Location loc = player.getLocation().clone();

    if (SpinnyCrate.isPlaceOccupied(loc)) {
      player.sendMessage(ColorFormat.format("&cThis place is already occupied by another crate!"));
      player.sendMessage(ColorFormat.format("&cPlease, select another location"));

    } else {
      final SpinnyCrate sc = new SpinnyCrate(loc, type, shouldDisappear);
      plugin.spinnyCrates.put(sc.getUUID(), sc);
      player.sendMessage(ColorFormat.format("&bCrate placed successfully!"));

      final ConfigurationSection cs = plugin.cratesDataYaml.createSection(sc.getUUID().toString());
      cs.set(LuckyCrates.YAML_HIDDEN_UNTIL, 0L);
      cs.set(LuckyCrates.YAML_SHOULD_DISAPPEAR, shouldDisappear);
      cs.set(LuckyCrates.YAML_CRATE_TYPE, type);

      try {
        plugin.cratesDataYaml.save(plugin.cratesDataFile);
      } catch (IOException e) {
        plugin.getLogger().severe("Could not save data file!");
        e.printStackTrace();
      }
    }
  }
}
