package logger;

import static logger.Log.Level.NO_LOGGING;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;

public final class Log {

	public static enum Level { NO_LOGGING, ERROR, INFO, DEBUG };
	
	private static final Prefix emptyPrefix = new Prefix() {
		@Override
		public String getPrefix() { return ""; }
	};
	private static Log nullLogger = Log.create(NO_LOGGING, new PrintWriter(new Writer() {
		@Override
		public void close() throws IOException { }
		@Override
		public void flush() throws IOException { }
		@Override
		public void write(char[] cbuf, int off, int len) throws IOException { }
	}), emptyPrefix);
	
	public static Log log = nullLogger;

	private PrintWriter printwriter;
	private Prefix prefix;
	private Level level;
	
	private Log(Level level, PrintWriter printwriter, Prefix prefix) {
		this.level = level;
		this.printwriter = printwriter;
		this.prefix = prefix;
	}
	
	public static Log create(Level level, PrintWriter printwriter, Prefix prefix) {
		return new Log(level, printwriter, prefix);
	}
	
	public static Log create(Level level, File logFile, Prefix prefix) throws IOException {
		logFile.getParentFile().mkdirs();
		return new Log(level, new PrintWriter(logFile), prefix);
	}
	
	public static Log create(Level level, File logFile) throws IOException {
		return create(level, logFile, emptyPrefix);
	}
	
	public static Log createStderrLogger(Level level, Prefix prefix) {
		return Log.create(level, new PrintWriter(System.err), prefix);
	}
	
	public static Log nullLogger() {
		return nullLogger;
	}

	public void log(Level level, String format, Object... parameters) {
		if (level.ordinal() > this.level.ordinal())
			return;
		printwriter.append(prefix.getPrefix());
		printwriter.append(Integer.toString(level.ordinal())).append("| ");
		printwriter.format(format, parameters);
		printwriter.append('\n');
	}
	
	public static interface Prefix {
		public String getPrefix();
	}

}
