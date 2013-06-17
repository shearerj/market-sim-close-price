/*
 * The contents of this file are subject to the Locomotive Public License
 * (LPL), a derivative of the Mozilla Public License, version 1.0. You
 * may not use this file except in compliance with the License; You may
 * obtain a copy of the License at http://www.locomotive.org/license/.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The initial developers of this code under the LPL is Leverage Information
 * Systems.  Portions created by Leverage are Copyright (C) 1998 Leverage
 * Information Systems. All Rights reserved.
 *
 *  $Id: Log.java,v 1.17 2004/09/27 18:11:55 chengsf Exp $
 */
package systemmanager;

import java.io.*;
import java.util.*;
import java.text.SimpleDateFormat;
import java.io.StringWriter;
import java.io.PrintWriter;

/**
 * A simple logging facility. Data can be logged to a file, along with a simple
 * datestamp (datestamp can be turned off if desired), similiar to the UNIX
 * syslog facility. <BR>
 * <BR>
 * 
 * At midnight, the current logfile will be closed, and a new one will be
 * opened. A percent symbol, representing the insertion point for a date string,
 * should be placed into the log_path to allow the server to correctly open the
 * new file. An example log_path is the string &quot;logs/loco1-%.log&quot;. The
 * standard log of the Loco is of this type, as is the Loco EventLog.
 */
public class Log {
	private String error_string;
	private RandomAccessFile raf;
	private boolean isopen = false;
	private int log_level = 0;
	private int day_opened = 0;
	private String original_input_path;
	private String root_path;
	// private SimpleDateFormat sdf;
	private Calendar cal;
	private StringBuffer sb;
	private boolean prepend_date = true; // Default to datestamping
	private boolean echo = false;
	private boolean overwrite = false;

	// If these are changed, change isValidLogLevel.
	public static final int NO_LOGGING = 0;
	public static final int ERROR = 1;
	public static final int INFO = 2;
	public static final int DEBUG = 3;

	/**
	 * Create a new Log.
	 * 
	 * @param lev
	 *            the logging level for this log
	 * @param sroot
	 *            the path to the distributions's root directory
	 * @param log_path
	 *            the log path, relative to the root. If this string contains a
	 *            '%' (percent), the current date will be substituted into the
	 *            filename at that location in the path.
	 * @param o
	 *            specifies if the log file is overwritten or appended. true =
	 *            overwrite, false append.
	 */
	public Log(int lev, String sroot, String log_path, boolean o)
			throws IOException {
		log_level = (lev < NO_LOGGING) ? NO_LOGGING : lev;

		root_path = sroot;
		original_input_path = log_path;
		overwrite = o;

		init();
	}

	/**
	 * called by the constructor
	 */
	private synchronized void init() throws IOException {
		// sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
		cal = Calendar.getInstance();
		sb = new StringBuffer();

		if (!createNewLog()) {
			throw new IOException(error_string);
		}
	}

	/**
	 * Set the echo flag to control if message gets written to stdout.
	 * 
	 * @param state
	 *            The new echo state (true echo, false do not)
	 */
	public synchronized void setEcho(boolean state) {
		echo = state;
	}

	/**
	 * Adjusts the logging level to the specified level. Calls to log () with a
	 * level higher than that level previously set with a setLevel() call or the
	 * constructor will be ignored. Example: if your current logging level in
	 * the Log object is ERROR, calls with logging level DEBUG will be ignored,
	 * but with level ERROR it will be written to the logfile. <BR>
	 * <BR>
	 * 
	 * @param newlev
	 *            the new logging level.
	 */
	public synchronized void setLevel(int newlev) {
		log_level = (newlev < NO_LOGGING) ? NO_LOGGING : newlev;
	}

	public synchronized int getLevel() {
		return log_level;
	}
	
	public synchronized boolean shouldLog(int lev) {
		return (lev <= log_level);
	}

	// called when new day
	public synchronized boolean createNewLog() {
		isopen = false;
		error_string = "";

		if (original_input_path == null) {
			return (true);
		}

		String logpath = getFullLogPath();
		try {
			raf = new RandomAccessFile(logpath, "rw");
			if (!overwrite)
				raf.seek(raf.length());
		} catch (IOException ioe) {
			error_string = ioe.toString();
			return (false);
		}
		isopen = true;
		return (true);
	}

	private synchronized String getFullLogPath() {

		String path = original_input_path;

		if (path.indexOf("/") != 0) {
			path = root_path + "/" + path;
		}

		Date dt = new java.util.Date();
		SimpleDateFormat localsdf = new SimpleDateFormat("yyyyMMdd");

		cal.setTime(dt);
		day_opened = cal.get(Calendar.DAY_OF_MONTH);

		StringBuffer sb = new StringBuffer();
		char c;

		for (int i = 0; i < path.length(); i++) {
			c = path.charAt(i);
			if (c != '%') {
				sb.append(c);
			} else {
				sb.append(localsdf.format(dt));
			}
		}
		return (sb.toString());
	}

	/**
	 * logs an line to the logfile, containing the string s. If the lev
	 * parameter is greater than the current log level, the call will simply
	 * return, without writing.
	 * 
	 * @param lev
	 *            the level of this log request. If higher than the current
	 *            logging level, this request will be ignored
	 * @param s
	 *            the string to be written to the log
	 */
	public synchronized void log(int lev, String src, String s) {
		/*
		 * Control-M is the Carriage Return.
		 * 
		 * MacOS terminates lines with CR Unix terminates lines with LF
		 * 
		 * Java/C escaping for CR is \r or (char)13 Java/C escaping for LF is \n
		 * or (char)10
		 */
		// s = MySystem.cleanXMLStr(s);

		if (lev <= log_level) {
			Date dt = new Date();
			cal.setTime(dt);
			if (day_opened != cal.get(Calendar.DAY_OF_MONTH)) {
				closeLog();
				createNewLog();
			}

			if (isopen) {
				// Prepend the date only if we should..
				if (prepend_date) {
					sb.append(dt.getTime());
					sb.append("|");
					sb.append(src);
					sb.append("|");
				}
				sb.append(lev);
				sb.append("|");
				sb.append(s);
				sb.append("\n");

				try {
					raf.writeBytes(sb.toString());
					if (echo)
						System.out.print(sb.toString());
				} catch (InterruptedIOException ioe) {
					// do nothing is writing is interrupted
				} catch (IOException ioe) {
					closeLog();
					System.out
							.println("write to log failed: " + ioe.toString());
					ioe.printStackTrace();
				}

				sb.setLength(0);
			}
		}
	}

	/**
	 * logs an line to the logfile, containing the string s. If the lev
	 * parameter is greater than the current log level, the call will simply
	 * return, without writing.
	 * 
	 * @param lev
	 *            the level of this log request. If higher than the current
	 *            logging level, this request will be ignored
	 * @param s
	 *            the string to be written to the log
	 */
	public synchronized void log(int lev, String s) {
		s = s.replaceAll("\n", "");

		if (lev <= log_level) {
			Date dt = new Date();
			cal.setTime(dt);
			if (day_opened != cal.get(Calendar.DAY_OF_MONTH)) {
				closeLog();
				createNewLog();
			}

			if (isopen) {
				// Prepend the date only if we should..
				if (prepend_date) {
					sb.append(dt.getTime());
					sb.append("|");
					sb.append("X");
					sb.append("|");
				}
				sb.append(lev);
				sb.append("|");
				sb.append(s);
				sb.append("\n");

				try {
					raf.writeBytes(sb.toString());
					if (echo)
						System.out.print(sb.toString());
				} catch (InterruptedIOException ioe) {
					// do nothing is writing is interrupted
				} catch (IOException ioe) {
					closeLog();
					System.out
							.println("write to log failed: " + ioe.toString());
					ioe.printStackTrace();
				}

				sb.setLength(0);
			}
		}
	}

	/**
	 * Closes this Log. Any subsequent writes to this Log are simply ignored.
	 */
	public synchronized void closeLog() {
		isopen = false;

		try {
			raf.close();
		} catch (IOException ioe) {
		}
	}

	/**
	 * Get the root path.
	 * 
	 * @return Returns the root path.
	 */
	public synchronized String getRootPath() {
		return root_path;
	}

	/**
	 * Get relative path to the log file.
	 * 
	 * @return Returns the relative path to the log file.
	 */
	public synchronized String getLogPath() {
		return original_input_path;
	}

	/**
	 * Call this method with a prepend value of false to turn off the automatic
	 * prepending of the date on each log output line. <BR>
	 * <BR>
	 * 
	 * @param prepend
	 *            make this true if you want the Log object to prepend the date
	 *            to the beginning of your log output lines, or false to turn
	 *            off the datestamping.
	 */
	public synchronized void setPrependDate(boolean prepend) {
		prepend_date = prepend;
	}

	// return a string representation of the stack trace for exception e
	public synchronized static String stackTraceToString(Exception e) {
		StringWriter buffer = new StringWriter();
		PrintWriter out = new PrintWriter(buffer);
		e.printStackTrace(out);
		return buffer.toString();
	}

	/**
	 * Move the current log file to gameID_[log-name] and create a new log file.
	 * 
	 * @param gameID
	 *            the game ID
	 * @return the name of the new log file, no path information; on an error,
	 *         returns an empty string.
	 */
	public synchronized String saveLog(int gameID) {
		closeLog();

		isopen = false;
		error_string = "";

		if (original_input_path == null) {
			return "";
		}

		String path = original_input_path;

		if (path.indexOf("/") != 0) {
			path = root_path + "/" + path;
		}

		// The name of the new log file.
		String newLog = gameID + "_" + getLogPath();
		// The name, with abs path, of the new log file.
		String newAbsLog = getRootPath() + "/" + gameID + "_" + getLogPath();

		File s1 = new File(getRootPath() + "/" + getLogPath());
		File t1 = new File(newAbsLog);
		if (!s1.renameTo(t1))
			return "";

		createNewLog();
		return newLog;
	}

	protected synchronized void finalize() throws Throwable {
		closeLog();
	}
}
