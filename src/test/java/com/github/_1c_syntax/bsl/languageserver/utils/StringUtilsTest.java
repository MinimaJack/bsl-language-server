/*
 * This file is a part of BSL Language Server.
 *
 * Copyright © 2018-2020
 * Alexey Sosnoviy <labotamy@gmail.com>, Nikita Gryzlov <nixel2007@gmail.com> and contributors
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 *
 * BSL Language Server is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 *
 * BSL Language Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with BSL Language Server.
 */
package com.github._1c_syntax.bsl.languageserver.utils;

import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class StringUtilsTest {

  @Test
  void testStringUtils() {
    var range = new Range(new Position(0, 0), new Position(0, 4));
    var content = new String[]{"DemoString with some text"};
    assertEquals("Demo", StringUtils.getTextRange(content, range));
    range = new Range(new Position(0, 5), new Position(1, 6));
    content = new String[]{"line first", "second line"};
    assertEquals("first\nsecond", StringUtils.getTextRange(content, range));
  }

  @Test
  void testAssertionsWithStringUtils() {
    assertThrows(IndexOutOfBoundsException.class, () -> {
      var range = new Range(new Position(1, 0), new Position(1, 2));
      var content = new String[]{"DemoString with some text"};
      StringUtils.getTextRange(content, range);
    });
  }

}
