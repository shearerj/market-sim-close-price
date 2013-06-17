package systemmanager;

import event.*;
import entity.*;
import model.*;

import java.util.*;
import java.io.*;
import java.text.DateFormat;

/**
 * This class serves the purpose of the Client in the Command pattern, in that
 * it instantiates the Activity objects and provides the methods to execute them
 * later.
 * 
 * Usage: java -jar hft.jar [simulation folder name] [sample #]
 * 
 * @author ewah
 */
public class SystemManager {

	protected EventManager eventManager;
	protected final SystemData data;
	protected final Observations obs;

	protected final int num; // sample number used for labeling output files
	protected final File simFolder; // simulation folder name

	protected final Properties envProps;
	protected Log log;
	protected int logLevel;
	protected String logFilename;

	/**
	 * Constructor
	 */
	public SystemManager(File simFolder, int simNumber) {
		this.simFolder = simFolder;
		this.num = simNumber;
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
	public static void main(String... args) {

		File simFolder = new File(".");
		int simNumber = 1;
		switch (args.length) {
		default:
			simNumber = Integer.parseInt(args[1]);
		case 1:
			simFolder = new File(args[0]);
		case 0:
		}

		SystemManager manager = new SystemManager(simFolder, simNumber);
		manager.setup();
		manager.executeEvents();
		manager.aggregateResults();
		manager.close();
	}

	/**
	 * Method to execute all events in the Event Queue.
	 */
	public void executeEvents() {
		while (!eventManager.isEmpty()) {
			eventManager.executeCurrentEvent();
		}
		log.log(Log.INFO, "STATUS: Event queue is now empty.");
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
			logFilename = simFolder.getPath()
					.replace("/", "-") + "_" + num;
			logFilename += "_"
					+ DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.UK)
							.format(now);
			logFilename += "_"
					+ DateFormat.getTimeInstance(DateFormat.MEDIUM, Locale.UK)
							.format(now);
			logFilename = logFilename.replace(":", ".");

			try {
				// Check first if directory exists
				if (!simFolder.exists()) {
					// Simulations directory not found
					System.err.println(this.getClass().getSimpleName()
							+ "::setup(String): simulation folder not found");
					System.exit(1);
				}
				// Check for logs directory
				File f = new File(simFolder, Consts.logDir);
				if (!f.exists()) {
					// Create directory
					f.mkdir();
				}
				log = new Log(logLevel, ".", simFolder + Consts.logDir
						+ logFilename + ".txt", true);
			} catch (Exception e) {
				e.printStackTrace();
				System.err.println(this.getClass().getSimpleName()
						+ "::setup(String): error creating log file");
			}

			// Log properties
			log.log(Log.DEBUG, envProps.toString());

			// Read simulation specification file
			try {
				// Check first if simulation spec file exists
				File f = new File(simFolder + Consts.simSpecFile);
				if (!f.exists()) {
					// Spec file is not found
					System.err
							.println(this.getClass().getSimpleName()
									+ "::setup(String): simulation_spec.json file not found");
					System.exit(1);
				}
				log = new Log(logLevel, ".", simFolder + Consts.logDir
						+ logFilename + ".txt", true);
			} catch (Exception e) {
				e.printStackTrace();
				System.err.println(this.getClass().getSimpleName()
						+ "::setup(String): error accessing spec file");
			}
			SimulationSpec specs = new SimulationSpec(simFolder
					+ Consts.simSpecFile, log, data);

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
	 * 
	 * @param p
	 * @param config
	 */
	public void loadInputStream(Properties p, InputStream config) {
		try {
			if (p == null)
				return;
			p.load(config);
		} catch (IOException e) {
			String s = this.getClass().getSimpleName()
					+ "::loadConfig(InputStream): error opening/processing config file: "
					+ config + "/" + e;
			log.log(Log.ERROR, s);
			System.err.print(s);
			System.exit(0);
		}
	}

	/**
	 * Load a configuration file into a Properties object.
	 * 
	 * @param p
	 * @param config
	 *            name of configuration file
	 */
	public void loadConfig(Properties p, String config) {
		try {
			loadInputStream(p, new FileInputStream(config));
		} catch (FileNotFoundException e) {
			String s = this.getClass().getSimpleName()
					+ "::loadConfig(String): error opening/processing config file: "
					+ config + "/" + e;
			log.log(Log.ERROR, s);
			System.err.print(s);
			System.exit(0);
		}
	}

	/**
	 * Generate results report (payoff data, feature data logging).
	 */
	public void aggregateResults() {
		try {
			// log observations for players
			for (Iterator<Integer> it = data.getPlayerIDs().iterator(); it
					.hasNext();) {
				obs.addObservation(it.next());
			}
			// obs.addFeature("interval",
			// obs.getTimeStampFeatures(data.getIntervals()));
			obs.addFeature("", obs.getConfiguration());
			// obs.addFeature("pv",
			// obs.getPriceFeatures(data.getPrivateValues()));
			// obs.addFeature("pv",
			// obs.getPriceFeatures(data.getPrivateValues()));
			// obs.addTransactionComparison();
			getModelResults();

			File file = new File(simFolder + Consts.obsFilename + num + ".json");
			FileWriter txt = new FileWriter(file);
			txt.write(obs.generateObservationFile());
			txt.close();

		} catch (Exception e) {
			String s = this.getClass().getSimpleName()
					+ "::aggregateResults(): "
					+ "error creating observation file";
			e.printStackTrace();
			System.err.println(s);
		}
	}

	/**
	 * Gets market results by model.
	 */
	private void getModelResults() {
		for (Map.Entry<Integer, MarketModel> entry : data.getModels()
				.entrySet()) {
			MarketModel model = entry.getValue();

			String prefix = model.getLogName() + "_";

			// Spread info
			long maxTime = Math
					.round(data.getNumEnvAgents() / data.arrivalRate);
			long begTime = Market.quantize((int) maxTime, 500) - 1000;
			for (long i = Math.max(begTime, 500); i <= maxTime + 1000; i += 500) {
				// long i = 3000;
				obs.addFeature(prefix + "spreads_" + i,
						obs.getSpreadInfo(model, i));
				obs.addFeature(prefix + "price_vol_" + i,
						obs.getVolatilityInfo(model, i));
			}

			// Surplus features
			obs.addFeature(prefix + "surplus", obs.getSurplusFeatures(model));
			obs.addFeature(prefix + "surplus_disc",
					obs.getDiscountedSurplusFeatures(model));

			// Other features
			// if (model.getNumAgentType("MARKETMAKER") >= 1) {
			obs.addFeature(prefix + "marketmaker",
					obs.getMarketMakerInfo(model));
			// }
			obs.addFeature(prefix + "transactions",
					obs.getTransactionInfo(model));
			obs.addFeature(prefix + "exec_time", obs.getTimeToExecution(model));
			obs.addFeature(prefix + "routing", obs.getRegNMSRoutingInfo(model));
			// obs.addFeature(prefix + "depths", obs.getDepthInfo(ids));
		}
	}
}
