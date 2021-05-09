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

package io.github.emilyydev.luckycrates.config;

import org.apache.commons.lang.Validate;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;
import java.util.regex.Pattern;

public abstract class ConfigKey<T> {

  public static final Predicate<? super String> VALID_KEY = Pattern.compile("(?i)^[a-z-]{1,32}$")
                                                                   .asPredicate();

  protected final String key;
  protected final T fallback;
  protected final boolean reloadable;
  protected final int bakedHashCode;

  protected ConfigKey(final @NotNull String key,
                      final @NotNull T fallback,
                      final boolean reloadable) {
    Validate.notNull(key);
    Validate.isTrue(VALID_KEY.test(key));
    Validate.notNull(fallback);

    this.key = key;
    this.fallback = fallback;
    this.reloadable = reloadable;
    this.bakedHashCode = hashCodeBakery();
  }

  public abstract @NotNull T get(final @NotNull ConfigAdapter configAdapter);

  public boolean isReloadable() {
    return this.reloadable;
  }

  public @NotNull String getKey() {
    return this.key;
  }

  public @NotNull T getFallback() {
    return this.fallback;
  }

  @Override
  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ConfigKey)) {
      return false;
    }

    final ConfigKey<?> that = (ConfigKey<?>) other;
    if (this.reloadable != that.reloadable) {
      return false;
    }
    return this.key.equals(that.key);
  }

  @Override
  public int hashCode() {
    return this.bakedHashCode;
  }

  private int hashCodeBakery() {
    int result = this.key.hashCode();
    result = 31 * result + (this.reloadable ? 1 : 0);
    return result;
  }
}
