package edu.umich.srg.util;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.base.Optional;
import com.google.common.base.Splitter;

import java.io.IOException;
import java.io.Writer;
import java.nio.CharBuffer;

/** Writer that indents and wraps a string. Starts with a newline to guarantee indent. */
public class IndentWriter extends Writer {

  private static final Splitter words = Splitter.onPattern("\\s+");
  private final Writer writer;
  private final int width;
  private final int indent;
  private int current;

  private IndentWriter(Writer backing, int width, int indent) {
    checkArgument(indent > 0);
    checkArgument(width > indent);
    this.writer = backing;
    this.width = width;
    this.indent = indent - 1;
    this.current = width;
  }

  public static IndentWriter withWidth(Writer backing, int width, int indent) {
    return new IndentWriter(backing, width, indent);
  }

  public static IndentWriter withDefaultWidth(Writer backing, int defaultWidth, int indent) {
    return withWidth(backing, Optional.fromNullable(System.getenv().get("COLUMNS"))
        .transform(Integer::parseInt).or(defaultWidth), indent);
  }

  public static IndentWriter withIndent(Writer backing, int indent) {
    return withDefaultWidth(backing, 80, indent);
  }

  @Override
  public void close() throws IOException {
    this.writer.close();
  }

  @Override
  public void flush() throws IOException {
    this.writer.flush();
  }

  @Override
  public void write(char[] buffer, int start, int end) throws IOException {
    for (String word : words.split(CharBuffer.wrap(buffer, start, end))) {
      if (word.length() + current >= width) {
        writer.append('\n');
        for (int i = 0; i < indent; ++i) {
          writer.append(' ');
        }
        current = indent;
      }
      writer.append(' ');
      writer.append(word);
      current += word.length() + 1;
    }
  }

}
