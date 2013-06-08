package systemmanager;

import data.*;
import event.*;

import java.util.*;
import java.io.*;
import java.text.DateFormat;

/**
 * This class serves the purpose of the Client in the Command pattern, 
 * in that it instantiates the Activity objects and provides the methods
 * to execute them later.
 * 
 * Usage: java -jar hft.jar [simulation folder name] [sample #]
 * 
 * @author ewah
 */
public class SystemManager {

	private static EventManager eventManager;
	private static SystemData data;
	private static Observations obs;
	private static Properties envProps;
	private static Log log;

	/**
	 * Constructor
	 */
	public SystemManager() {
		data = new SystemData();
		envProps = new Properties();
		obs = new Observations(data);
	}
	
	/**
	 * Only one argument, which is the sample number, is processed
	 * 
	 * Two input arguments: first is simulation folder, second is sample number
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		
		SystemManager manager = new SystemManager();

		if (args.length == 2) {
			data.simDir = args[0] + "/";
			if (data.simDir.charAt(0) == '/') 
				data.simDir = data.simDir.substring(1);
			data.num = Integer.parseInt(args[1]);
		} else {
			data.simDir = "";
			data.num = 1;
		}
		
		manager.setup();
		manager.executeEvents();
		manager.aggregateResults();
		manager.close();
	}

	/**
	 * Initialize parameters based on configuration file.
	 */
	public void setup() {

		try {
			// =================================================
			// Load parameters

			// Read environment parameters & set up environment
			loadConfig(envProps, Consts.configDir + Consts.configFile);
			
			// Create log file
			int logLevel = Integer.parseInt(envProps.getProperty("logLevel"));
			Date now = new Date();
			String logFilename = data.simDir.substring(0, 
					data.simDir.length()-1).replace("/", "-") + "_" + data.num;
			logFilename += "_" + DateFormat.getDateInstance(DateFormat.MEDIUM, 
					Locale.UK).format(now);
			logFilename += "_" + DateFormat.getTimeInstance(DateFormat.MEDIUM, 
					Locale.UK).format(now);
			logFilename = logFilename.replace(":",".");
			
			try {
				// Check first if directory exists
				File f = new File(data.simDir);
				if (!f.exists()) {
					// Simulations directory not found
					System.err.println(this.getClass().getSimpleName() + 
							"::setup(String): simulation folder not found");
					System.exit(1);
				}
				// Check for logs directory
				String logPath = data.simDir + Consts.logDir;
				f = new File(logPath);
				if (!f.exists()) {
					// Create directory
					new File(logPath).mkdir();
				}
				// Create log file
				log = new Log(logLevel, ".", logPath + logFilename + ".txt", true);
			} catch (Exception e) {
				System.err.println(this.getClass().getSimpleName() + 
						"::setup(String): error creating log file");
				e.printStackTrace();
			}

			// Log properties
			log.log(Log.DEBUG, envProps.toString());
			
			// Read simulation specification file
			try {
				// Check first if simulation spec file exists
				File f = new File(data.simDir + Consts.simSpecFile);
				if (!f.exists()) {
					// Spec file is not found
					System.err.println(this.getClass().getSimpleName() + 
							"::setup(String): simulation_spec.json file not found");
					System.exit(1);
				}
			} catch (Exception e) {
				System.err.println(this.getClass().getSimpleName() + 
						"::setup(String): error accessing spec file");
				e.printStackTrace();
			}
			// Read simulation_spec.json file
			SimulationSpec specs = new SimulationSpec(data.simDir + 
					Consts.simSpecFile,	log, data);

			// Create event manager
			eventManager = new EventManager(data.simLength, log);

			// Set up / create entities
			SystemSetup s = new SystemSetup(specs, eventManager, data, log);
			s.setupAll();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Method to execute all events in the Event Queue.
	 */
	public void executeEvents() {
		try {
			while (!eventManager.isEventQueueEmpty()) {
				eventManager.executeCurrentEvent();
			}
			String s = "STATUS: Event queue is now empty.";
			log.log(Log.INFO, s);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Shuts down simulation. Removes empty log file if log level is 0.
	 */
	public void close() {
		File f = new File(data.simDir + Consts.logDir);
		if (f.exists() && log.getLevel() == Log.NO_LOGGING) {
			// remove the empty log file
			f.delete();
		}
	}
	
	/**
	 * Load a configuration file InputStream into a Properties object.
	 * @param p
	 * @param config
	 */
	public void loadInputStream(Properties p, InputStream config) {
		try {
			if (p == null)
				return;
			p.load(config);
		} catch (IOException e) {
			String s = this.getClass().getSimpleName() + "::loadConfig(InputStream): " + 
					"error opening/processing config file: " + config + "/" + e;
			log.log(Log.ERROR, s);
			System.err.println(s);
			System.exit(0);
		}
	}

	
	/**
	 * Load a configuration file into a Properties object.
	 * @param p
	 * @param config	name of configuration file
	 */
	public void loadConfig(Properties p, String config) {
		try {
			loadInputStream(p, new FileInputStream(config));
		} catch (FileNotFoundException e) {
			String s = this.getClass().getSimpleName() + "::loadConfig(String): " + 
					"error opening/processing config file: " + config + "/" + e;
			log.log(Log.ERROR, s);
			System.err.println(s);
			System.exit(0);
		}
	}
	
	
	/**
	 * Generate results report (payoff data, feature data logging).
	 */
	public void aggregateResults() {
		try {			
			File file = new File(data.simDir + Consts.obsFile + data.num + ".json");
			FileWriter txt = new FileWriter(file);
			txt.write(obs.generateObservationFile());
			txt.close();
		} catch (Exception e) {
			String s = this.getClass().getSimpleName() + "::aggregateResults(): " + 
						"error creating observation file";
			System.err.println(s);
			e.printStackTrace();
		}
	}
}
