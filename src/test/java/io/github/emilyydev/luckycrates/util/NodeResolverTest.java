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

package io.github.emilyydev.luckycrates.util;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class NodeResolverTest {

  private static Executable split(final String node) {
    return () -> NodeResolver.splitByUnescapedPeriods(node);
  }

  private static Executable determine(final String node) {
    return () -> NodeResolver.determineNodeType(node);
  }

  @Test
  public void split_malformedNode_whitespace() {
    final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, split("some.malformed-node.with a\\.whitespace"));
    assertTrue(exception.getMessage().contains("Node cannot contain whitespaces"));
  }

  @Test
  public void split_malformedNode_escapedWhitespace() {
    final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, split("some.malformed-node.with\\ a\\.whitespace"));
    assertTrue(exception.getMessage().contains("Node cannot contain whitespaces"));
  }

  @Test
  public void split_wellFormedNode() {
    final List<String> expected = ImmutableList.of("one", "well", "formed", "node");
    final List<String> actual = NodeResolver.splitByUnescapedPeriods("one.well.formed.node");
    assertIterableEquals(expected, actual);
  }

  @Test
  public void split_wellFormedNode_escapedPeriods() {
    final List<String> expected = ImmutableList.of("one", "well.formed", "node");
    final List<String> actual = NodeResolver.splitByUnescapedPeriods("one.well\\.formed.node");
    assertIterableEquals(expected, actual);
  }

  @Test
  public void split_wellFormedNode_consecutivePeriods() {
    final List<String> expected = ImmutableList.of("one", "well", "formed", "node");
    final List<String> actual = NodeResolver.splitByUnescapedPeriods("one.well..formed.node");
    assertIterableEquals(expected, actual);
  }

  @Test
  public void split_wellFormedNode_consecutivePeriodsSomeEscaped() {
    final List<String> expected = ImmutableList.of("one", "well", ".", "formed", "node");
    final List<String> actual = NodeResolver.splitByUnescapedPeriods("one.well.\\..formed.node");
    assertIterableEquals(expected, actual);
  }

  @Test
  public void determine_malformedNode_totallyEmpty() {
    final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, determine(""));
    assertTrue(exception.getMessage().contains("Node is effectively empty"));
  }

  @Test
  public void determine_malformedNode_virtuallyEmpty() {
    final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, determine("..."));
    assertTrue(exception.getMessage().contains("Node is effectively empty"));
  }

  @Test
  public void determine_malformedGroupNode_noGroup() {
    final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, determine("group"));
    assertTrue(exception.getMessage().contains("Malformed group node"));
  }

  @Test
  public void determine_malformedGroupNode_tooManySections() {
    final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, determine("group.malformed.node"));
    assertTrue(exception.getMessage().contains("Malformed group node"));
  }

  @Test
  public void determine_malformedMetaNode_noKeyNorValue() {
    final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, determine("meta"));
    assertTrue(exception.getMessage().contains("Malformed meta variable node"));
  }

  @Test
  public void determine_malformedMetaNode_noValue() {
    final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, determine("meta.key"));
    assertTrue(exception.getMessage().contains("Malformed meta variable node"));
  }

  @Test
  public void determine_malformedMetaNode_tooManySections() {
    final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, determine("meta.key.value.malformed.node"));
    assertTrue(exception.getMessage().contains("Malformed meta variable node"));
  }

  @Test
  public void determine_wellFormedGroupNode() {
    final List<String> expected = ImmutableList.of("moderator");
    final NodeResolver.NodeType.Info nodeInfo = NodeResolver.determineNodeType("group.moderator");
    assertEquals(NodeResolver.NodeType.PARENT_GROUP, nodeInfo.getNodeType());
    assertIterableEquals(expected, nodeInfo.getParts());
  }

  @Test
  public void determine_wellFormedMetaNode() {
    final List<String> expected = ImmutableList.of("claims", "50000");
    final NodeResolver.NodeType.Info nodeInfo = NodeResolver.determineNodeType("meta.claims.50000");
    assertEquals(NodeResolver.NodeType.META_VARIABLE, nodeInfo.getNodeType());
    assertIterableEquals(expected, nodeInfo.getParts());
  }

  @Test
  public void determine_regularPermissionNode() {
    final List<String> expected = ImmutableList.of("some.random.uncategorized.node");
    final NodeResolver.NodeType.Info nodeInfo = NodeResolver.determineNodeType("some.random.uncategorized.node");
    assertEquals(NodeResolver.NodeType.PERMISSION, nodeInfo.getNodeType());
    assertIterableEquals(expected, nodeInfo.getParts());
  }
}
