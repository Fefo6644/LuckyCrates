package me.fefo.luckycrates;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.tree.RootCommandNode;
import me.fefo.facilites.ColorFormat;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static me.fefo.facilites.BrigadierHelper.literal;
import static me.fefo.facilites.BrigadierHelper.pLiteral;
import static me.fefo.facilites.BrigadierHelper.pRequired;

public final class CommanderKeen implements TabExecutor {
  private static final RootCommandNode<Player> ROOT_NODE = new RootCommandNode<>();
  private static final CommandDispatcher<Player> DISPATCHER = new CommandDispatcher<>(ROOT_NODE);
  private static final RootCommandNode<CommandSender> CONSOLE_ROOT_NODE = new RootCommandNode<>();
  private static final CommandDispatcher<CommandSender> CONSOLE_DISPATCHER = new CommandDispatcher<>(CONSOLE_ROOT_NODE);

  private static final Map<String, ParseResults<Player>> RESULTS_CACHE = new HashMap<>();
  private static final Map<String, ParseResults<CommandSender>> CONSOLE_RESULTS_CACHE = new HashMap<>();

  private static LuckyCrates plugin;

  static {
    ROOT_NODE.addChild(pLiteral("lc").executes(CommanderKeen::printVersion)
                                     .then(pLiteral("help").executes(CommanderKeen::printUsage))
                                     .then(pLiteral("nearest").executes(CommanderKeen::teleportNearest))
                                     .then(pLiteral("reload").executes(CommanderKeen::reload))
                                     .then(pLiteral("remove").executes(CommanderKeen::remove)
                                                             .then(pLiteral("nearest").executes(CommanderKeen::removeNearest)))

                                     .then(pLiteral("set").then(pRequired("type", StringArgumentType.word())
                                                                    .suggests((context, suggestionsBuilder) -> {
                                                                      argsFilterer(suggestionsBuilder.getRemaining(),
                                                                                   SpinnyCrate.categorizedCrates.keySet(),
                                                                                   String::toString).forEach(suggestionsBuilder::suggest);

                                                                      return suggestionsBuilder.buildFuture();
                                                                    })
                                                                    .executes(CommanderKeen::set)))

                                     .then(pLiteral("setpersistent").then(pRequired("type", StringArgumentType.word())
                                                                              .suggests((context, suggestionsBuilder) -> {
                                                                                argsFilterer(suggestionsBuilder.getRemaining(),
                                                                                             SpinnyCrate.categorizedCrates.keySet(),
                                                                                             String::toString).forEach(suggestionsBuilder::suggest);

                                                                                return suggestionsBuilder.buildFuture();
                                                                              })
                                                                              .executes(CommanderKeen::setPersistent)))
                                     .build());

    CONSOLE_ROOT_NODE.addChild(literal("lc").executes(CommanderKeen::printVersion)
                                            .then(literal("help").executes(CommanderKeen::printUsage))
                                            .then(literal("reload").executes(CommanderKeen::reload))
                                            .build());
  }

  public CommanderKeen(@NotNull final LuckyCrates plugin) {
    CommanderKeen.plugin = plugin;
  }

  @Override
  public boolean onCommand(@NotNull final CommandSender sender,
                           @NotNull final Command command,
                           @NotNull final String label,
                           @NotNull final String[] args) {
    final String cmd = ("lc " + String.join(" ", args)).trim();

    if (!(sender instanceof Player)) {
      final ParseResults<CommandSender> result = CONSOLE_RESULTS_CACHE.getOrDefault(cmd, CONSOLE_DISPATCHER.parse(cmd, sender));

      try {
        CONSOLE_DISPATCHER.execute(result);
        CONSOLE_RESULTS_CACHE.putIfAbsent(cmd, result);
      } catch (CommandSyntaxException exception) {
        printUsage(result.getContext().build(cmd));
      }

      return true;
    }

    final ParseResults<Player> result = RESULTS_CACHE.getOrDefault(cmd, DISPATCHER.parse(cmd, ((Player) sender)));

    try {
      DISPATCHER.execute(result);
      RESULTS_CACHE.putIfAbsent(cmd, result);
    } catch (CommandSyntaxException exception) {
      printUsage(result.getContext().build(cmd));
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
      final ParseResults<Player> result = RESULTS_CACHE.getOrDefault(cmd, DISPATCHER.parse(cmd, ((Player) sender)));

      return DISPATCHER.getCompletionSuggestions(result)
                       .join()
                       .getList()
                       .stream()
                       .map(Suggestion::getText)
                       .collect(Collectors.toList());
    } else {
      final ParseResults<CommandSender> result = CONSOLE_RESULTS_CACHE.getOrDefault(cmd, CONSOLE_DISPATCHER.parse(cmd, sender));

      return CONSOLE_DISPATCHER.getCompletionSuggestions(result)
                               .join()
                               .getList()
                               .stream()
                               .map(Suggestion::getText)
                               .collect(Collectors.toList());
    }
  }

  private static <T> List<String> argsFilterer(final String cursor, final Collection<T> validValues, final Function<T, String> toString) {
    return validValues.stream()
                      .map(toString)
                      .filter(s -> s.startsWith(cursor))
                      .collect(Collectors.toList());
  }

  private static <S extends CommandSender> int printVersion(final CommandContext<S> context) {
    context.getSource().sendMessage(ColorFormat.format("&3LuckyCrates &7- &bv"
                                                       + plugin.getDescription().getVersion()));
    return 0;
  }

  private static <S extends CommandSender> int printUsage(final CommandContext<S> context) {
    printVersion(context);

    final S source = context.getSource();

    if (source instanceof Player) {
      source.sendMessage(ColorFormat.format("&cUsages:"));
      source.sendMessage(ColorFormat.format("  &c/luckycrates [help|nearest|reload]"));
      source.sendMessage(ColorFormat.format("  &c/luckycrates remove [nearest]"));
      source.sendMessage(ColorFormat.format("  &c/luckycrates (set|setpersistent) <crate type>"));
    } else {
      source.sendMessage(ColorFormat.format("&cUsages:"));
      source.sendMessage(ColorFormat.format("  &c/luckycrates [help|reload]"));
    }

    return 0;
  }

  private static int teleportNearest(final CommandContext<Player> context) {
    final Player player = context.getSource();
    final UUID nearestCrate = getNearestCrate(player);

    if (nearestCrate != null) {
      player.teleport(plugin.spinnyCrates.get(nearestCrate).getLocation());
    } else {
      player.sendMessage(ColorFormat.format("&cCouldn't find the nearest crate within loaded chunks in this world"));
    }

    return 0;
  }

  private static <S extends CommandSender> int reload(final CommandContext<S> context) {
    plugin.reloadConfig();
    context.getSource().sendMessage(ColorFormat.format("&bFiles reloaded successfully!"));
    return 0;
  }

  private static int remove(final CommandContext<Player> context) {
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

  private static int removeNearest(final CommandContext<Player> context) {
    final Player player = context.getSource();
    final UUID nearestCrate = getNearestCrate(player);

    if (nearestCrate != null) {
      final String type = plugin.spinnyCrates.get(nearestCrate).getCrateName();
      final Location crateLocation = plugin.spinnyCrates.get(nearestCrate).getLocation();
      assert crateLocation != null;
      final int x = crateLocation.getBlockX();
      final int y = crateLocation.getBlockY();
      final int z = crateLocation.getBlockZ();

      plugin.spinnyCrates.remove(nearestCrate).kill();
      plugin.cratesDataYaml.set(nearestCrate.toString(), null);

      try {
        plugin.cratesDataYaml.save(plugin.cratesDataFile);
      } catch (IOException ex) {
        plugin.getLogger().severe("Could not save data file!");
        ex.printStackTrace();
      }

      player.sendMessage(ColorFormat.format("&9" + type + " &bcrate removed at " +
                                            "&7x:" + x + " y:" + y + " z:" + z));

    } else {
      player.sendMessage(ColorFormat.format("&cCouldn't find the nearest crate within loaded chunks in this world"));
    }

    return 0;
  }

  private static int set(final CommandContext<Player> context) {
    final Player player = context.getSource();
    final String crateType = context.getArgument("type", String.class);
    createCrate(player, true, crateType);

    return 0;
  }

  private static int setPersistent(final CommandContext<Player> context) {
    final Player player = context.getSource();
    final String crateType = context.getArgument("type", String.class);
    createCrate(player, false, crateType);

    return 0;
  }

  private static void createCrate(final Player player, final boolean shouldDisappear, final String type) {
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

  private static UUID getNearestCrate(final Player player) {
    return player.getWorld()
                 .getEntitiesByClass(ArmorStand.class)
                 .stream()
                 .map(Entity::getUniqueId)
                 .filter(plugin.spinnyCrates::containsKey)
                 .map(plugin.spinnyCrates::get)
                 .min(new CratesSorter(player))
                 .map(SpinnyCrate::getUUID)
                 .orElse(null);
  }

  public static void clearCaches() {
    RESULTS_CACHE.clear();
    CONSOLE_RESULTS_CACHE.clear();
  }

  private static final class CratesSorter implements Comparator<SpinnyCrate> {

    private final Entity source;

    public CratesSorter(final Entity source) {
      this.source = source;
    }

    @Override
    public int compare(final SpinnyCrate first, final SpinnyCrate second) {
      return Double.compare(source.getLocation().distanceSquared(first.getLocation()),
                            source.getLocation().distanceSquared(second.getLocation()));
    }
  }
}
