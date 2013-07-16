package logger;

import java.io.File;
import java.io.IOException;

public class Logger {
	
	public static enum Level { NO_LOGGING, ERROR, INFO, DEBUG };

	protected static Log logger;
	
	public static void setup(int lev, File logFile) {
		try {
			logFile.getParentFile().mkdirs();
			logger = new Log(lev, ".", logFile.getPath(), true);
		} catch (IOException e) {
			System.err.println("Couldn't create log file");
			e.printStackTrace();
		}
	}

	public static void log(Level level, String message) {
		if (logger == null)
			System.err.println("LNI: "
					+ message);
		else
			logger.log(level.ordinal(), message);
	}

	public static boolean shouldLog(int level) {
		return logger != null && logger.shouldLog(level);
	}

	public static Level getLevel() {
		return logger == null ? Level.NO_LOGGING : Level.values()[logger.getLevel()];
	}

}
