package logger;

import static logger.Logger.Level.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

public final class Logger {

	public static enum Level { NO_LOGGING, ERROR, INFO, DEBUG };
	
	private static final Prefix emptyPrefix = new Prefix() {
		@Override
		public String getPrefix() { return ""; }
	};
	private static Logger nullLogger = Logger.create(NO_LOGGING, new Writer() {
		@Override
		public void close() throws IOException { }
		@Override
		public void flush() throws IOException { }
		@Override
		public void write(char[] arg0, int arg1, int arg2) throws IOException {	}
	}, emptyPrefix);
	
	public static Logger logger = createStderrLogger(DEBUG, emptyPrefix);

	private Writer writer;
	private Prefix prefix;
	private Level level;
	
	private Logger(Level level, Writer writer, Prefix prefix) {
		this.level = level;
		this.writer = writer;
		this.prefix = prefix;
	}
	
	public static Logger create(Level level, Writer writer, Prefix prefix) {
		return new Logger(level, writer, prefix);
	}
	
	public static Logger create(Level level, File logFile, Prefix prefix) throws IOException {
		logFile.getParentFile().mkdirs();
		return new Logger(level, new FileWriter(logFile), prefix);
	}
	
	public static Logger create(Level level, File logFile) throws IOException {
		return create(level, logFile, new Prefix() {
			@Override
			public String getPrefix() { return ""; }
		});
	}
	
	public static Logger createStderrLogger(Level level, Prefix prefix) {
		return Logger.create(level, new OutputStreamWriter(System.err), prefix);
	}
	
	public static Logger nullLogger() {
		return nullLogger;
	}
	
	public void log(Level level, String format, Object... parameters) {
		if (level.ordinal() > this.level.ordinal())
			return;
		String message = prefix.getPrefix() + level.ordinal() + "| " + String.format(format, parameters) + '\n';
		try {
			writer.append(message);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static interface Prefix {
		public String getPrefix();
	}

}
