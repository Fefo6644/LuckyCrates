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

package com.github.fefo.luckycrates.util;

import com.google.common.collect.ImmutableList;
import net.milkbowl.vault.chat.Chat;
import org.bukkit.entity.Player;

import java.util.List;

public final class NodeResolver {

  /**
   * Tests a player for a parent group ({@code "group.<group>"}),
   * a meta variable ({@code "meta.<key>.<value>"}) or a regular permission node (anything else).
   *
   * @param player    the player to test the node on.
   * @param node      the node to test.
   * @param vaultChat Vault's ""chat"" interface to check for parent groups and meta variables.
   * @return the result of the check.
   * @throws IllegalArgumentException if:
   *                                  <ul>
   *                                    <li>the node contains effectively no "sections" separated by periods '.'</li>
   *                                    <li>the node contains whitespace characters ({@link Character#isWhitespace(char)})</li>
   *                                    <li>the node is a group node ({@code "group.<group>"}) and is malformed
   *                                    (too many periods '.' unescaped)</li>
   *                                    <li>the node is a meta variable node ({@code "meta.<key>.<value>"}) and is malformed
   *                                    (too many periods '.' unescaped)</li>
   *                                  </ul>
   */
  public static boolean determineNode(final Player player, final String node, final Chat vaultChat) throws IllegalArgumentException {
    final List<String> split = splitByUnescapedPeriods(node.trim());
    if (split.isEmpty()) {
      throw new IllegalArgumentException(String.format("Node is effectively empty: %s", node));
    }

    final String discriminator = split.get(0);
    if (discriminator.equalsIgnoreCase("group")) {
      if (split.size() > 2) {
        throw new IllegalArgumentException(String.format("Malformed group node (escape any periods in the group name): %s", node));
      }
      return vaultChat.playerInGroup(player, split.get(1));
    }

    if (discriminator.equalsIgnoreCase("meta")) {
      if (split.size() > 3) {
        throw new IllegalArgumentException(String.format("Malformed meta variable node (escape any periods in the key/value): %s", node));
      }
      final String key = split.get(1);
      final String expectedValue = split.get(2);
      final String actualValue = vaultChat.getPlayerInfoString(player, key, null);
      return expectedValue.equalsIgnoreCase(actualValue);
    }

    return player.hasPermission(node.trim());
  }

  /**
   * Splits an input node by periods ({@code '.'}) that are <i>not</i> escaped
   * with a backslash ({@code '\\'}).
   *
   * @param node the input node to split.
   * @return the list containing the parts split from the node.
   * @throws IllegalArgumentException if {@link Character#isWhitespace(char)} is true
   *                                  for any {@code char} in the input node.
   */
  static List<String> splitByUnescapedPeriods(final String node) throws IllegalArgumentException {
    // package-private for unit testing brrrr
    final ImmutableList.Builder<String> builder = ImmutableList.builder();

    boolean escaping = false;
    StringBuilder stringBuilder = new StringBuilder();
    for (final char c : node.toCharArray()) {
      if (Character.isWhitespace(c)) {
        throw new IllegalArgumentException(String.format("Node cannot contain whitespaces: %s", node));
      }

      if (!escaping) {
        switch (c) {
          case '.': {
            // don't add empty sections (e.g.: "part1..part2" will return { "part1", "part2" })
            if (stringBuilder.length() > 0) {
              builder.add(stringBuilder.toString());
              stringBuilder = new StringBuilder();
            }
            escaping = false;
            continue;
          }

          case '\\': {
            escaping = true;
            continue;
          }
        }
      }

      escaping = false;
      stringBuilder.append(c);
    }

    // add last word to the list if present
    if (stringBuilder.length() > 0) {
      builder.add(stringBuilder.toString());
    }

    return builder.build();
  }

  private NodeResolver() {
    throw new UnsupportedOperationException();
  }
}
