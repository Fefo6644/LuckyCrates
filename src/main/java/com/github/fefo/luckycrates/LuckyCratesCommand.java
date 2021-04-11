//
// This file is part of LuckyCrates, licensed under the MIT License.
//
// Copyright (c) 2021 Fefo6644 <federico.lopez.1999@outlook.com>
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

package com.github.fefo.luckycrates;

import com.destroystokyo.paper.event.server.AsyncTabCompleteEvent;
import com.github.fefo.luckycrates.internal.CrateMap;
import com.github.fefo.luckycrates.internal.SpinningCrate;
import com.github.fefo.luckycrates.messages.Message;
import com.github.fefo.luckycrates.messages.MessagingSubject;
import com.github.fefo.luckycrates.messages.PlayerMessagingSubject;
import com.github.fefo.luckycrates.messages.SubjectFactory;
import com.github.fefo.luckycrates.util.CommandMapHelper;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.RootCommandNode;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class LuckyCratesCommand extends Command implements Listener {

  private static final Joiner OR_JOINER = Joiner.on('|');

  private final LuckyCratesPlugin plugin;
  private final CrateMap crateMap;
  private final SubjectFactory subjectFactory;
  private final Predicate<? super String> commandPredicate;
  private final CommandDispatcher<PlayerMessagingSubject> dispatcher = new CommandDispatcher<>();
  private final RootCommandNode<PlayerMessagingSubject> root = this.dispatcher.getRoot();

  public LuckyCratesCommand(final LuckyCratesPlugin plugin) {
    super("luckycrates", "Command used to place, locate and remove spinning crates", "/luckycrates help", ImmutableList.of("lc"));
    this.plugin = plugin;
    this.crateMap = plugin.getCratesMap();
    this.subjectFactory = plugin.getSubjectFactory();

    this.commandPredicate = Pattern.compile(
        "^/?(?:" + plugin.getName() + ":)?(?:" + getName() + '|' + OR_JOINER.join(getAliases()) + ") ",
        Pattern.CASE_INSENSITIVE).asPredicate();

    setPermission("luckycrates.use");
    setPermissionMessage(Message.NO_PERMISSION.legacy("use this command"));
    CommandMapHelper.getCommandMap().register(plugin.getName(), this);

    try {
      Class.forName("com.destroystokyo.paper.event.server.AsyncTabCompleteEvent");
      Bukkit.getPluginManager().registerEvents(this, plugin);
    } catch (final ClassNotFoundException exception) {
      // ignore, we just won't compute suggestion completions asynchronously
    }

    final LiteralArgumentBuilder<PlayerMessagingSubject> builder = literal(getName());
    builder
        .requires(subject -> subject.getPlayer().hasPermission(getPermission()))
        .then(literal("help").executes(this::printInfo))
        .then(literal("nearest").executes(this::teleportNearest))
        .then(literal("reload").executes(this::reload))
        .then(literal("remove")
                  .executes(this::remove)
                  .then(literal("nearest").executes(this::removeNearest)))
        .then(literal("set")
                  .then(argument("type", StringArgumentType.word())
                            .suggests(this::suggestCrateType)
                            .executes(this::set)))
        .then(literal("setpersistent")
                  .then(argument("type", StringArgumentType.word())
                            .suggests(this::suggestCrateType)
                            .executes(this::setPersistent)));

    this.root.addChild(builder.build());
  }

  private CompletableFuture<Suggestions> suggestCrateType(final CommandContext<PlayerMessagingSubject> context, final SuggestionsBuilder builder) {
    final String lowercase = builder.getRemaining().toLowerCase(Locale.ROOT);
    this.crateMap.getCategorizedCrateTypes()
                 .keySet().stream()
                 .filter(s -> s.toLowerCase(Locale.ROOT).startsWith(lowercase))
                 .forEach(builder::suggest);
    return builder.buildFuture();
  }

  private int printInfo(final CommandContext<PlayerMessagingSubject> context) {
    Message.PLUGIN_INFO.send(context.getSource(), this.plugin);
    printUsage(context.getSource());
    return 1;
  }

  private void printUsage(final PlayerMessagingSubject subject) {
    Message.USAGE_TITLE.send(subject);
    for (final String usage : this.dispatcher.getAllUsage(this.root, subject, true)) {
      Message.USAGE_COMMAND.send(subject, usage);
    }
  }

  private int teleportNearest(final CommandContext<PlayerMessagingSubject> context) {
    final PlayerMessagingSubject subject = context.getSource();
    final Player player = subject.getPlayer();
    if (player == null) {
      return 0;
    }
    final UUID nearestCrate = getNearestCrate(player);

    if (nearestCrate != null) {
      player.teleport(this.crateMap.get(nearestCrate).getLocation());
    } else {
      Message.COULDNT_FIND_NEAREST.send(subject);
      return 0;
    }

    return 1;
  }

  private int reload(final CommandContext<PlayerMessagingSubject> context) {
    if (this.plugin.reload()) {
      Message.FILES_RELOADING_SUCCESS.send(context.getSource());
    } else {
      Message.FILES_RELOADING_FAILED.send(context.getSource());
    }
    return 1;
  }

  private int remove(final CommandContext<PlayerMessagingSubject> context) {
    final PlayerMessagingSubject subject = context.getSource();
    final Player player = subject.getPlayer();
    if (player == null) {
      return 0;
    }

    if (!this.crateMap.isEmpty()) {
      if (this.plugin.getInteractantsHandler().stopRemoving(player.getUniqueId())) {
        Message.ACTION_CANCELLED.send(subject);
      } else {
        this.plugin.getInteractantsHandler().startRemoving(player.getUniqueId());
        Message.HIT_TO_REMOVE.send(subject);
        Message.RUN_TO_CANCEL.send(subject);
      }
    } else {
      Message.NO_LOADED_CRATES.send(subject);
      return 0;
    }

    return 1;
  }

  private int removeNearest(final CommandContext<PlayerMessagingSubject> context) {
    final PlayerMessagingSubject subject = context.getSource();
    final Player player = subject.getPlayer();
    if (player == null) {
      return 0;
    }
    final UUID nearestCrate = getNearestCrate(player);

    if (nearestCrate != null) {
      final SpinningCrate crate = this.crateMap.remove(nearestCrate);
      final String type = crate.getType();
      final Location crateLocation = crate.getLocation();

      try {
        this.crateMap.save();
      } catch (final IOException exception) {
        this.plugin.getLogger().severe("Could not save data file!");
        exception.printStackTrace();
      }

      Message.CRATE_REMOVED_AT.send(subject, player.getName(), type, crateLocation);
    } else {
      Message.COULDNT_FIND_NEAREST.send(subject);
      return 0;
    }

    return 1;
  }

  private int set(final CommandContext<PlayerMessagingSubject> context) {
    final PlayerMessagingSubject subject = context.getSource();
    final String crateType = context.getArgument("type", String.class);
    createCrate(subject, true, crateType);
    return 1;
  }

  private int setPersistent(final CommandContext<PlayerMessagingSubject> context) {
    final PlayerMessagingSubject subject = context.getSource();
    final String crateType = context.getArgument("type", String.class);
    createCrate(subject, false, crateType);
    return 1;
  }

  private void createCrate(final PlayerMessagingSubject subject, final boolean shouldDisappear, final String type) {
    final Player player = subject.getPlayer();
    if (player == null) {
      return;
    }
    final Location location = player.getLocation().clone();

    if (this.crateMap.isPlaceOccupied(location)) {
      Message.ALREADY_OCCUPIED.send(subject);
      Message.SELECT_ANOTHER_LOCATION.send(subject);

    } else {
      final SpinningCrate crate = new SpinningCrate(location, type, shouldDisappear, this.crateMap.getCrateType(type).getSkull());
      this.crateMap.put(crate.getUuid(), crate);
      Message.CRATE_PLACED.send(subject, type);

      try {
        this.crateMap.save();
      } catch (final IOException exception) {
        this.plugin.getLogger().severe("Could not save data file!");
        exception.printStackTrace();
      }
    }
  }

  private UUID getNearestCrate(final Player player) {
    return player.getWorld().getEntitiesByClass(ArmorStand.class).stream()
                 .map(Entity::getUniqueId)
                 .map(this.crateMap::get)
                 .filter(Objects::nonNull)
                 .min(new CratesSorter(player))
                 .map(SpinningCrate::getUuid)
                 .orElse(null);
  }

  @Override
  public boolean execute(final @NotNull CommandSender sender,
                         final @NotNull String label,
                         final @NotNull String @NotNull [] args) {
    final MessagingSubject subject = this.subjectFactory.sender(sender);

    if (!subject.isPlayer()) {
      Message.PLAYERS_ONLY.send(subject);
      return true;
    }

    final String cmd = getName() + ' ' + String.join(" ", args);
    final ParseResults<PlayerMessagingSubject> result = this.dispatcher.parse(cmd.trim(), subject.asPlayerSubject());

    try {
      this.dispatcher.execute(result);
    } catch (final CommandSyntaxException exception) {
      printUsage(subject.asPlayerSubject());
    }

    return true;
  }

  private List<String> tabComplete(final MessagingSubject subject, final String input) {
    if (!subject.isPlayer()) {
      return ImmutableList.of();
    }

    final ParseResults<PlayerMessagingSubject> result = this.dispatcher.parse(input, subject.asPlayerSubject());
    return this.dispatcher.getCompletionSuggestions(result).join()
                          .getList().stream()
                          .map(Suggestion::getText)
                          .collect(Collectors.toList());
  }

  @Override
  public @NotNull List<String> tabComplete(final @NotNull CommandSender sender,
                                           final @NotNull String alias,
                                           final @NotNull String @NotNull [] args) {
    final MessagingSubject subject = this.subjectFactory.sender(sender);
    final String input = getName() + ' ' + String.join(" ", args);
    return tabComplete(subject, input);
  }

  private void asyncTabComplete(final AsyncTabCompleteEvent event) {
    if (event.isHandled() || !event.isCommand()) {
      return;
    }

    final String buffer = event.getBuffer();
    if (!this.commandPredicate.test(buffer)) {
      return;
    }

    final MessagingSubject subject = this.subjectFactory.sender(event.getSender());
    final String input = getName() + buffer.substring(buffer.indexOf(' '));
    event.setCompletions(tabComplete(subject, input));
    event.setHandled(true);
  }

  private LiteralArgumentBuilder<PlayerMessagingSubject> literal(final String name) {
    return LiteralArgumentBuilder.literal(name);
  }

  private <T> RequiredArgumentBuilder<PlayerMessagingSubject, T> argument(final String name, final ArgumentType<T> type) {
    return RequiredArgumentBuilder.argument(name, type);
  }

  private static final class CratesSorter implements Comparator<SpinningCrate> {

    private final Location origin;

    public CratesSorter(final Entity source) {
      this.origin = source.getLocation().clone();
    }

    @Override
    public int compare(final SpinningCrate first, final SpinningCrate second) {
      return Double.compare(this.origin.distanceSquared(first.getLocation()),
                            this.origin.distanceSquared(second.getLocation()));
    }
  }
}
