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

package com.github.fefo.luckycrates.config;

import com.github.fefo.luckycrates.config.types.DoubleConfigKey;
import com.github.fefo.luckycrates.config.types.StringConfigKey;

public final class ConfigKeys {

  public static final DoubleConfigKey RPM = new DoubleConfigKey("rpm", 45.0, true);

  public static final StringConfigKey INSTANT_GIVE_PERM =
      new StringConfigKey("instantGivePerm", "group.vip", true);

  public static final StringConfigKey NO_PERM_MESSAGE =
      new StringConfigKey("noPermMessage", "&c&oYou need VIP rank or higher to open this crate", true);

  private ConfigKeys() {
    throw new UnsupportedOperationException();
  }
}
