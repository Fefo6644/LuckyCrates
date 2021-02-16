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
import com.github.fefo.luckycrates.internal.CratesMap;
import com.github.fefo.luckycrates.internal.SpinningCrate;
import com.github.fefo.luckycrates.messages.Message;
import com.github.fefo.luckycrates.messages.MessagingSubject;
import com.github.fefo.luckycrates.messages.PlayerMessagingSubject;
import com.github.fefo.luckycrates.messages.SubjectFactory;
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
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class LuckyCratesCommand extends Command implements Listener {

  private static final Joiner OR_JOINER = Joiner.on('|');

  private final LuckyCratesPlugin plugin;
  private final CratesMap cratesMap;
  private final SubjectFactory subjectFactory;
  private final Predicate<? super String> commandPredicate;
  private final CommandDispatcher<PlayerMessagingSubject> dispatcher = new CommandDispatcher<>();
  private final RootCommandNode<PlayerMessagingSubject> root = this.dispatcher.getRoot();

  public LuckyCratesCommand(final LuckyCratesPlugin plugin) {
    super("luckycrates", "Command used to place, locate and remove spinning crates", "/luckycrates help", ImmutableList.of("lc"));
    this.plugin = plugin;
    this.cratesMap = plugin.getCratesMap();
    this.subjectFactory = plugin.getSubjectFactory();

    this.commandPredicate = Pattern.compile(
        "^/?(?:" + plugin.getName() + ":)?(?:" + getName() + '|' + OR_JOINER.join(getAliases()) + ") ",
        Pattern.CASE_INSENSITIVE).asPredicate();

    setPermission("luckycrates.use");
    setPermissionMessage(Message.NO_PERMISSION.legacy("use this command"));
    Bukkit.getCommandMap().register(plugin.getName(), this);

    final LiteralArgumentBuilder<PlayerMessagingSubject> builder = literal(getName());

    builder
        .requires(subject -> subject.getPlayer().hasPermission(getPermission()))
        .then(literal("help")
                  .executes(this::printInfo))
        .then(literal("nearest")
                  .executes(this::teleportNearest))
        .then(literal("reload")
                  .executes(this::reload))
        .then(literal("remove")
                  .executes(this::remove)
                  .then(literal("nearest")
                            .executes(this::removeNearest)))
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
    this.cratesMap.getCategorizedCrateTypes()
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

  private void printUsage(final PlayerMessagingSubject source) {
    Message.USAGE_TITLE.send(source);
    for (final String usage : this.dispatcher.getAllUsage(this.root, source, true)) {
      Message.USAGE_COMMAND.send(source, usage);
    }
  }

  private int teleportNearest(final CommandContext<PlayerMessagingSubject> context) {
    final PlayerMessagingSubject source = context.getSource();
    final UUID nearestCrate = getNearestCrate(source.getPlayer());

    if (nearestCrate != null) {
      source.getPlayer().teleport(this.cratesMap.get(nearestCrate).getLocation());
    } else {
      Message.COULDNT_FIND_NEAREST.send(source);
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
    final PlayerMessagingSubject source = context.getSource();

    if (!this.cratesMap.isEmpty()) {
      if (this.plugin.getInteractantsHandler().stopRemoving(source.getPlayer().getUniqueId())) {
        Message.ACTION_CANCELLED.send(source);
      } else {
        this.plugin.getInteractantsHandler().startRemoving(source.getPlayer().getUniqueId());
        Message.HIT_TO_REMOVE.send(source);
        Message.RUN_TO_CANCEL.send(source);
      }
    } else {
      Message.NO_LOADED_CRATES.send(source);
      return 0;
    }

    return 1;
  }

  private int removeNearest(final CommandContext<PlayerMessagingSubject> context) {
    final PlayerMessagingSubject source = context.getSource();
    final UUID nearestCrate = getNearestCrate(source.getPlayer());

    if (nearestCrate != null) {
      final SpinningCrate crate = this.cratesMap.remove(nearestCrate);
      final String type = crate.getType();
      final Location crateLocation = crate.getLocation();

      try {
        this.cratesMap.save();
      } catch (final IOException exception) {
        this.plugin.getSLF4JLogger().error("Could not save data file!", exception);
      }

      Message.CRATE_REMOVED_AT.send(source, source.getPlayer().getName(), type, crateLocation);
    } else {
      Message.COULDNT_FIND_NEAREST.send(source);
      return 0;
    }

    return 1;
  }

  private int set(final CommandContext<PlayerMessagingSubject> context) {
    final PlayerMessagingSubject source = context.getSource();
    final String crateType = context.getArgument("type", String.class);
    createCrate(source, true, crateType);
    return 1;
  }

  private int setPersistent(final CommandContext<PlayerMessagingSubject> context) {
    final PlayerMessagingSubject source = context.getSource();
    final String crateType = context.getArgument("type", String.class);
    createCrate(source, false, crateType);
    return 1;
  }

  private void createCrate(final PlayerMessagingSubject subject, final boolean shouldDisappear, final String type) {
    final Location location = subject.getPlayer().getLocation().clone();

    if (this.cratesMap.isPlaceOccupied(location)) {
      Message.ALREADY_OCCUPIED.send(subject);
      Message.SELECT_ANOTHER_LOCATION.send(subject);

    } else {
      final SpinningCrate crate = new SpinningCrate(location, type, shouldDisappear, this.cratesMap.getCrateType(type).getSkull());
      this.cratesMap.put(crate.getUuid(), crate);
      Message.CRATE_PLACED.send(subject, type);

      try {
        this.cratesMap.save();
      } catch (final IOException exception) {
        this.plugin.getSLF4JLogger().error("Could not save data file!", exception);
      }
    }
  }

  private UUID getNearestCrate(final Player player) {
    return player.getWorld().getEntitiesByClass(ArmorStand.class).stream()
                 .map(Entity::getUniqueId)
                 .filter(this.cratesMap::containsKey)
                 .map(this.cratesMap::get)
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

  @EventHandler(ignoreCancelled = true)
  private void onAsyncTabComplete(final AsyncTabCompleteEvent event) {
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
