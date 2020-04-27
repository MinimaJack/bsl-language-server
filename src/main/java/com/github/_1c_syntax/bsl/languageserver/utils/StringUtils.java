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
