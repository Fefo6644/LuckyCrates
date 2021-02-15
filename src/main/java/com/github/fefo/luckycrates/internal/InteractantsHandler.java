package com.github.fefo.luckycrates.internal;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class InteractantsHandler {

  private final Set<UUID> playersRemovingCrates = new HashSet<>();

  public boolean startRemoving(final UUID uuid) {
    return this.playersRemovingCrates.add(uuid);
  }

  public boolean isRemoving(final UUID uuid) {
    return this.playersRemovingCrates.contains(uuid);
  }

  public boolean isAnyoneRemoving() {
    return !this.playersRemovingCrates.isEmpty();
  }

  public boolean stopRemoving(final UUID uuid) {
    return this.playersRemovingCrates.remove(uuid);
  }
}
