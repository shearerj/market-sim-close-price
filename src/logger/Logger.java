package logger;

import java.io.File;
import java.io.IOException;

public class Logger {

	public static enum Level { NO_LOGGING, ERROR, INFO, DEBUG };

	protected static Log logger;
	protected static boolean outError = true;
	protected static Prefix prefix = new EmptyPrefix();
	
	public static void setup(int lev, File logFile, boolean outError, Prefix prefix) {
		try {
			logFile.getParentFile().mkdirs();
			logger = new Log(lev, ".", logFile.getPath(), true);
			logger.setPrependDate(false);
			Logger.outError = outError;
			Logger.prefix = prefix;
		} catch (IOException e) {
			System.err.println("Couldn't create log file");
			e.printStackTrace();
		}
	}
	
	public static void setup(int lev, File logFile, boolean outError) {
		setup(lev, logFile, outError, prefix);
	}
	
	public static void setup(int lev, File logFile) {
		setup(lev, logFile, outError, prefix);
	}

	public static void log(Level level, String message) {
		if (getLevel() == Level.NO_LOGGING)
			return;
		message = prefix.getPrefix() + message;
		logger.log(level.ordinal(), message);
		if (level == Level.ERROR)
			System.err.println(message);
	}
	
	public static void log(Level level, Object message) {
		log(level, message.toString());
	}

	public static Level getLevel() {
		return logger == null ? Level.NO_LOGGING
				: Level.values()[logger.getLevel()];
	}
	
	public static interface Prefix {
		public String getPrefix();
	}
	
	private static class EmptyPrefix implements Prefix {
		@Override
		public String getPrefix() { return ""; }
	}

}
