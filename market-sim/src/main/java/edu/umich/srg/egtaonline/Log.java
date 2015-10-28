package edu.umich.srg.egtaonline;

import java.io.Closeable;
import java.io.File;
import java.io.FileWriter;
import java.io.Flushable;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.function.Function;

/**
 * A simple logger that just allows writing structured messages to a stream with some filtering.
 * This is not meant to log long running processes.
 * 
 * @author erik
 * 
 */
public class Log implements Closeable, Flushable {

  public static enum Level {
    NO_LOGGING, ERROR, INFO, DEBUG
  };

  private static Log nullLogger = new Log(Level.NO_LOGGING, new Writer() {
    @Override
    public void close() throws IOException {}

    @Override
    public void flush() throws IOException {}

    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {}
  }, (l) -> "");

  private final PrintWriter printwriter;
  private Function<Level, String> prefix;
  private Level level;

  protected Log(Level level, Writer out, Function<Level, String> prefix) {
    this.level = level;
    this.printwriter = new PrintWriter(out);
    this.prefix = prefix;
  }

  public static Log create(Level level, Writer writer, Function<Level, String> prefix) {
    if (level == Level.NO_LOGGING)
      return nullLogger;

    return new Log(level, writer, prefix);
  }

  public static Log create(Level level, File logFile, Function<Level, String> prefix)
      throws IOException {
    if (level == Level.NO_LOGGING)
      return nullLogger;
    logFile.getParentFile().mkdirs();
    return create(level, new FileWriter(logFile), prefix);
  }

  public static Log create(Level level, File logFile) throws IOException {
    logFile.getParentFile().mkdirs();
    return create(level, logFile, (l) -> "");
  }

  @Override
  public void close() throws IOException {
    printwriter.close();
  }

  @Override
  public void flush() {
    printwriter.flush();
  }

  /**
   * Creates a logger that prints everything to System.err.
   * 
   * Potentially useful for tests
   */
  public static Log createStderrLogger(Level level, Function<Level, String> prefix) {
    return Log.create(level, new PrintWriter(System.err), prefix);
  }

  /**
   * Creates a logger that ignores all logging
   * 
   * @return
   */
  public static Log nullLogger() {
    return nullLogger;
  }

  /** Sets the prefix */
  public void setPrefix(Function<Level, String> prefix) {
    this.prefix = prefix;
  }

  /**
   * Method to log a message. This should be used in a very strict way, that is format should be a
   * static string (not one that you build), and all of the paramaterised options should be stuck in
   * parameters. An example call is
   * 
   * <code>log(INFO, "Object %s, Int %d, Float %.4f", new Object(), 5, 6.7)</code>
   * 
   * By calling log like this you can avoid expensive logging operations when you're not actually
   * logging, and use the fact that the strings are written directly to the file without having to
   * build a large string in memory first.
   * 
   * For more information on how to properly format strings you can look at the String.format
   * method, or the PrintWriter.format method.
   * 
   * @param level
   * @param format
   * @param parameters
   */
  public void log(Level level, String format, Object... parameters) {
    if (level.ordinal() > this.level.ordinal())
      return;
    printwriter.append(prefix.apply(level));
    printwriter.format(format, parameters);
    printwriter.append('\n');
  }

  public void error(String format, Object... parameters) {
    log(Level.ERROR, format, parameters);
  }

  public void info(String format, Object... parameters) {
    log(Level.INFO, format, parameters);
  }

  public void debug(String format, Object... parameters) {
    log(Level.DEBUG, format, parameters);
  }

  @Override
  public String toString() {
    return level == Level.NO_LOGGING ? "Null Logger" : level + " Logger";
  }

}
