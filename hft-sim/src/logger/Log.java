package logger;

import static logger.Log.Level.NO_LOGGING;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;

/**
 * A simple logger that just allows writing structured messages to a stream with
 * some filtering. This is not meant to log long running processes.
 * 
 * @author erik
 * 
 */
public final class Log {

	public static enum Level { NO_LOGGING, ERROR, INFO, DEBUG };
	
	private static final Clock secondClock = new Clock() {
		@Override
		public long getTime() { return System.nanoTime() / 1000 % 1000000; }
		@Override
		public int getPadding() { return 6; }
	};
	
	public static Clock milliClock() {
		return secondClock;
	}
	
	private static Log nullLogger = Log.create(NO_LOGGING, new Writer() {
		@Override
		public void close() throws IOException { }
		@Override
		public void flush() throws IOException { }
		@Override
		public void write(char[] cbuf, int off, int len) throws IOException { }
	}, new Clock() {
		@Override
		public long getTime() { return 0; }
		@Override
		public int getPadding() { return 0; }
	});
	
	/**
	 * Use this as the global default logger to log any/all messages. Defaults to a nullLogger
	 */
	private static Log log = nullLogger;

	private PrintWriter printwriter;
	private Clock clock;
	private Level level;
	
	private Log(Level level, Writer out, Clock clock) {
		this.level = level;
		this.printwriter = new PrintWriter(out);
		this.clock = clock;
	}
	
	public static Log create(Level level, Writer writer, Clock clock) {
		return new Log(level, writer, clock);
	}
	
	public static Log create(Level level, File logFile, Clock clock) throws IOException {
		logFile.getParentFile().mkdirs();
		return new Log(level, new FileWriter(logFile), clock);
	}
	
	public static Log create(Level level, File logFile) throws IOException {
		logFile.getParentFile().mkdirs();
		return new Log(level, new FileWriter(logFile), secondClock);
	}
	
	public void closeLogger() throws IOException {
		printwriter.flush();
		printwriter.close();
	}
	
	/**
	 * Creates a logger that prints everything to System.err.
	 * 
	 * Potentially useful for tests
	 */
	public static Log createStderrLogger(Level level, Clock clock) {
		return Log.create(level, new PrintWriter(System.err), clock);
	}
	
	/**
	 * Creates a logger that ignores all logging
	 * @return
	 */
	public static Log nullLogger() {
		return nullLogger;
	}

	public static void setLogger(Log logger) {
		Log.log = logger;
	}
	
	public static void log(Level level, String format, Object... parameters) {
		Log.log.logf(level, format, parameters);
	}
	
	/**
	 * Method to log a message. This should be used in a very strict way, that
	 * is format should be a static string (not one that you build), and all of
	 * the paramaterised options should be stuck in parameters. An example call
	 * is
	 * 
	 * <code>log(INFO, "Object %s, Int %d, Float %.4f", new Object(), 5, 6.7)</code>
	 * 
	 * By calling log like this you can avoid expensive logging operations when
	 * you're not actually logging, and use the fact that the strings are
	 * written directly to the file without having to build a large string in
	 * memory first.
	 * 
	 * For more information on how to properly format strings you can look at
	 * the String.format method, or the PrintWriter.format method.
	 * 
	 * @param level
	 * @param format
	 * @param parameters
	 */
	public void logf(Level level, String format, Object... parameters) {
		if (level.ordinal() > this.level.ordinal())
			return;
		printwriter.format("%" + clock.getPadding() + "d", clock.getTime()).append("| ");
		printwriter.append(Integer.toString(level.ordinal())).append("| ");
		printwriter.format(format, parameters);
		printwriter.append('\n');
	}
	
	/**
	 * An interface to allow arbitrary nulltimes for the logged entries
	 */
	public static interface Clock {
		public long getTime();
		public int getPadding();
	}

}
