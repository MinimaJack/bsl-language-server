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

import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class StringUtils {
  public static String getTextRange(String[] contentList, Range range) {
    Position start = range.getStart();
    Position end = range.getEnd();

    if (start.getLine() > contentList.length
      || end.getLine() > contentList.length) {
      throw new ArrayIndexOutOfBoundsException("Range goes beyond the boundaries of the parsed document");
    }
    if (start.getLine() == end.getLine()) {
      return contentList[start.getLine()].substring(start.getCharacter(), end.getCharacter());
    }

    return IntStream
      .rangeClosed(start.getLine(), end.getLine())
      .mapToObj(i -> {
        if (i == start.getLine()) {
          return contentList[i].substring(start.getCharacter());
        } else if (i == end.getLine()) {
          return contentList[i].substring(0, end.getCharacter());
        } else {
          return contentList[i];
        }
      })
      .collect(Collectors.joining("\n"));
  }
}
