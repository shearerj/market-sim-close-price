package systemmanager;

import event.*;
import entity.*;
import activity.*;
import model.*;

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

	private EventManager eventManager;
	private SystemData data;
	private Observations obs;
	
	private static int num;						// sample number used for labeling output files
	private static String simFolder;			// simulation folder name
	
	private Properties envProps;
	private Log log;
	private int logLevel;
	private String logFilename;

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
			simFolder = args[0] + "/";
			num = Integer.parseInt(args[1]);
		} else {
			simFolder = "";
			num = 1;
		}
		
		manager.setup();
		manager.executeEvents();
		manager.aggregateResults();
		manager.close();
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
			System.err.print(e);
		}
	}

	/**
	 * Shuts down simulation. Removes empty log file if log level is 0.
	 */
	public void close() {
		File f = new File(simFolder + Consts.logDir);
		if (f.exists() && logLevel == 0) {
			// remove the empty log file
			f.delete();
		}
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
			data.obsNum = num;

			// Create log file
			logLevel = Integer.parseInt(envProps.getProperty("logLevel"));
			Date now = new Date();
			logFilename = simFolder + num;
			logFilename += "_" + DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(now);
			logFilename = logFilename.replace("/", "-");
			logFilename = logFilename.replace(" ", "_");
			logFilename = logFilename.replace(":","");
			
			try {
				// Check first if directory exists
				File f = new File(simFolder + Consts.logDir);
				if (!f.exists()) {
					// Create directory
					new File(simFolder + Consts.logDir).mkdir();
				}
				log = new Log(logLevel, ".", simFolder + Consts.logDir + logFilename + ".txt", true);
			} catch (Exception e) {
				e.printStackTrace();
				System.err.print("setup(String): error creating log file");
			}

			// Log properties
			log.log(Log.DEBUG, envProps.toString());
			
			// Read simulation specification file
			SimulationSpec specs = new SimulationSpec(simFolder + Consts.simSpecFile, log, data);

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
			String s = "loadConfig(InputStream): error opening/processing config file: " + config + "/" + e;
			log.log(Log.ERROR, s);
			System.err.print(s);
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
			String s = "loadConfig(String): error opening/processing config file: " + config + "/" + e;
			log.log(Log.ERROR, s);
			System.err.print(s);
			System.exit(0);
		}
	}
	
	
	/**
	 * Generate results report (payoff data, feature data logging). Only adds observations
	 * from agents in the primary model (the primary game).
	 */
	public void aggregateResults() {
		try {
			for (Iterator<Integer> it = data.getAgentIDs().iterator(); it.hasNext(); ) {
				int id = it.next();
				if (data.getPrimaryAgentIDs().contains(id)) {
					// only add observations for players in primary model
					obs.addObservation(id);
				} else {
					// add as a feature observations for players in other market models
					obs.addObservationAsFeature(id);
				}
			}
			
			obs.addFeature("", obs.getConfiguration());
			obs.addFeature("interval", obs.getTimeStampFeatures(data.getIntervals()));
			obs.addFeature("pv", obs.getPriceFeatures(data.getPrivateValues()));
			obs.addFeature("expire", obs.getTimeStampFeatures(data.getExpirations()));
			obs.addTransactionComparison();
			getModelResults();
			
			File file = new File(simFolder + Consts.obsFilename + num + ".json");
			FileWriter txt = new FileWriter(file);
			txt.write(obs.generateObservationFile());
			txt.close();
			
		} catch (Exception e) {
			String s = "aggregateResults(): error creating observation file";
			e.printStackTrace();
			System.err.println(s);
		}
	}
	
	/**
	 * Gets market results by model.
	 */
	private void getModelResults() {
		for (Map.Entry<Integer, MarketModel> entry : data.getModels().entrySet()) {
			MarketModel model = entry.getValue();
			
			String prefix = model.getLogName() + "_";

			obs.addFeature(prefix + "spreads", obs.getSpreadInfo(model));
			obs.addFeature(prefix + "surplus", obs.getSurplusFeatures(model));
			obs.addFeature(prefix + "transactions", obs.getTransactionInfo(model));
			obs.addFeature(prefix + "exec_speed", obs.getExecutionSpeed(model));
			// obs.addFeature(prefix + "depths", obs.getDepthInfo(ids));
		}
	}
}
