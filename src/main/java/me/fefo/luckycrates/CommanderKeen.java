package me.fefo.luckycrates;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import me.fefo.luckycrates.internal.CratesManager;
import me.fefo.luckycrates.internal.SpinnyCrate;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.mojang.brigadier.arguments.StringArgumentType.word;
import static me.fefo.facilites.VariousUtils.filterArg;

public final class CommanderKeen<S extends Player> implements TabExecutor {

  private LiteralArgumentBuilder<S> literal(final String name) {
    return LiteralArgumentBuilder.literal(name);
  }

  private <T> RequiredArgumentBuilder<S, T> argument(final String name, final ArgumentType<T> type) {
    return RequiredArgumentBuilder.argument(name, type);
  }

  private final LuckyCrates plugin;
  private final CratesManager manager;
  private final Function<CommandSender, S> transformer;
  private final CommandDispatcher<S> dispatcher = new CommandDispatcher<>();

  public CommanderKeen(final LuckyCrates plugin, final Function<CommandSender, S> transformer) {
    this.plugin = plugin;
    this.manager = plugin.getCratesManager();
    this.transformer = transformer;

    dispatcher.register(literal("lc").executes(this::printVersion)
                                     .then(literal("help").executes(this::printUsage))
                                     .then(literal("nearest").executes(this::teleportNearest))
                                     .then(literal("reload").executes(this::reload))
                                     .then(literal("remove").executes(this::remove)
                                                            .then(literal("nearest").executes(this::removeNearest)))
                                     .then(literal("set").then(argument("type", word()).suggests(this::suggestCrateType).executes(this::set)))
                                     .then(literal("setpersistent").then(argument("type", word()).suggests(this::suggestCrateType).executes(this::setPersistent))));
  }

  @Override
  public boolean onCommand(@NotNull final CommandSender sender, @NotNull final Command command, @NotNull final String label, @NotNull final String[] args) {
    if (!(sender instanceof Player)) {
      Message.PLAYERS_ONLY.send(sender);
      return true;
    }

    final String cmd = "lc " + String.join(" ", args);
    final ParseResults<S> result = dispatcher.parse(cmd.trim(), transformer.apply(sender));

    try {
      dispatcher.execute(result);
    } catch (CommandSyntaxException exception) {
      printUsage(result.getContext().build(cmd));
    }

    return true;
  }

  @Override
  public List<String> onTabComplete(@NotNull final CommandSender sender, @NotNull final Command command, @NotNull final String alias, @NotNull final String[] args) {
    if (!(sender instanceof Player)) {
      return Collections.emptyList();
    }

    final String cmd = "lc " + String.join(" ", args);
    final ParseResults<S> result = dispatcher.parse(cmd, transformer.apply(sender));

    return dispatcher.getCompletionSuggestions(result).join()
                     .getList().stream()
                     .map(Suggestion::getText)
                     .collect(Collectors.toList());
  }

  private CompletableFuture<Suggestions> suggestCrateType(final CommandContext<S> context, final SuggestionsBuilder builder) {
    filterArg(builder.getRemaining(), manager.categorizedCrates.keySet(), String::toString).forEach(builder::suggest);
    return builder.buildFuture();
  }

  private int printVersion(final CommandContext<S> context) {
    Message.VERSION.send(context.getSource(), plugin.getDescription().getVersion());
    return 0;
  }

  private int printUsage(final CommandContext<S> context) {
    printVersion(context);
    final S source = context.getSource();

    Message.USAGE_TITLE.send(source);
    for (final String usage : dispatcher.getAllUsage(dispatcher.getRoot(), source, true)) {
      Message.USAGE_COMMAND.send(source, usage);
    }

    return 0;
  }

  private int teleportNearest(final CommandContext<S> context) {
    final S source = context.getSource();
    final UUID nearestCrate = getNearestCrate(source);

    if (nearestCrate != null) {
      source.teleport(plugin.spinnyCrates.get(nearestCrate).getLocation());
    } else {
      Message.COULDNT_FIND_NEAREST.send(source);
    }

    return 0;
  }

  private int reload(final CommandContext<S> context) {
    plugin.reloadConfig();
    Message.FILES_RELOADED.send(context.getSource());
    return 0;
  }

  private int remove(final CommandContext<S> context) {
    final S source = context.getSource();

    if (!plugin.spinnyCrates.isEmpty()) {
      if (plugin.playersRemovingCrate.remove(source.getUniqueId())) {
        Message.ACTION_CANCELLED.send(source);
      } else {
        plugin.playersRemovingCrate.add(source.getUniqueId());
        Message.HIT_TO_REMOVE.send(source);
        Message.RUN_TO_CANCEL.send(source);
      }
    } else {
      Message.NO_LOADED_CRATES.send(source);
    }
    return 0;
  }

  private int removeNearest(final CommandContext<S> context) {
    final S source = context.getSource();
    final UUID nearestCrate = getNearestCrate(source);

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

      Message.CRATE_REMOVED_AT.send(source, type, String.valueOf(x), String.valueOf(y), String.valueOf(z));
    } else {
      Message.COULDNT_FIND_NEAREST.send(source);
    }

    return 0;
  }

  private int set(final CommandContext<S> context) {
    final S source = context.getSource();
    final String crateType = context.getArgument("type", String.class);
    createCrate(source, true, crateType);

    return 0;
  }

  private int setPersistent(final CommandContext<S> context) {
    final S source = context.getSource();
    final String crateType = context.getArgument("type", String.class);
    createCrate(source, false, crateType);

    return 0;
  }

  private void createCrate(final Player player, final boolean shouldDisappear, final String type) {
    final Location loc = player.getLocation().clone();

    if (manager.isPlaceOccupied(loc)) {
      Message.ALREADY_OCCUPIED.send(player);
      Message.SELECT_ANOTHER_LOCATION.send(player);

    } else {
      final SpinnyCrate sc = new SpinnyCrate(manager, loc, type, shouldDisappear);
      plugin.spinnyCrates.put(sc.getUUID(), sc);
      Message.CRATE_PLACED.send(player, type);

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

  private UUID getNearestCrate(final Player player) {
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
