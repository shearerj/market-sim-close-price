package logger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

public class Logger {

	public static enum Level { NO_LOGGING, ERROR, INFO, DEBUG };
	public static Logger logger;

	private Writer writer;
	private Prefix prefix;
	private Level level;
	
	public Logger(Level level, Writer writer, Prefix prefix) {
		this.level = level;
		this.writer = writer;
		this.prefix = prefix;
	}
	
	public Logger(Level level, File logFile, Prefix prefix) throws IOException {
		logFile.getParentFile().mkdirs();
		this.level = level;
		this.prefix = prefix;
		this.writer = new FileWriter(logFile);
	}
	
	public void log(Level level, String format, Object... parameters) {
		if (level.ordinal() > this.level.ordinal())
			return;
		String message = prefix.getPrefix() + level.ordinal() + '|' + String.format(format, parameters) + '\n';
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
