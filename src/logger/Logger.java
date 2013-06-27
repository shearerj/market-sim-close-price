package logger;

import java.io.IOException;

public class Logger {

	public static final int NO_LOGGING = 0;
	public static final int ERROR = 1;
	public static final int INFO = 2;
	public static final int DEBUG = 3;
	
	protected static Log logger;
	
	public static void setup(int lev, String sroot, String log_path, boolean o) {
		try {
			logger = new Log(lev, sroot, log_path, o);
		} catch (IOException e) {
			System.err.println("Couldn't create log file");
			e.printStackTrace();
		}
	}
	
	public static void log(int level, String message) {
		if (logger == null)
			System.err.println("Logger Not Initialized! Can't write message\"" + message + "\"");
		else
			logger.log(level, message);
	}
	
	public static boolean shouldLog(int level) {
		return logger != null && logger.shouldLog(level);
	}
	
}
