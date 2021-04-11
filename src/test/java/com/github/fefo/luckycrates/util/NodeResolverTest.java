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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class NodeResolverTest {

  private static Executable split(final String node) {
    return () -> NodeResolver.splitByUnescapedPeriods(node);
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
}
