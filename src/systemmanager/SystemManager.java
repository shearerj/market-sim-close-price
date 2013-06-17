package systemmanager;

import data.*;
import event.*;

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

	protected static DateFormat DATE_FORMAT = DateFormat.getDateInstance(
			DateFormat.MEDIUM, Locale.UK);
	protected static DateFormat TIME_FORMAT = DateFormat.getTimeInstance(
			DateFormat.MEDIUM, Locale.UK);

	protected final EventManager eventManager;
	protected final SystemData data;
	protected final Observations obs;
	protected final Properties envProps;
	protected final SimulationSpec spec;
	protected final Log log;

	protected final int num; // sample number used for labeling output files
	protected final File simFolder; // simulation folder name

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

		try {
			SystemManager manager = new SystemManager(simFolder, simNumber);
			manager.executeEvents();
			manager.aggregateResults();
		} catch (IOException | IllegalArgumentException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	public SystemManager(File simFolder, int simNumber) throws IOException {
		if (!simFolder.exists())
			throw new IllegalArgumentException(
					"Simulation Folder must already exist and contain a simulation spec file. "
							+ simFolder.getAbsolutePath() + " does not exist.");

		this.simFolder = simFolder;
		this.num = simNumber;

		envProps = getProperties();
		log = getLog(); // Must be called after "envProps"
		data = new SystemData();
		spec = getSimulationSpec(); // Must be after "log" and "data"
		obs = new Observations(data);
		eventManager = new EventManager(data.simLength, log);

		new SystemSetup(spec, eventManager, data, log).setupAll();
	}

	protected Properties getProperties() throws IOException {
		Properties props = new Properties();
		props.load(new FileInputStream(new File(new File(simFolder, Consts.configDir), Consts.configFile)));
		return props;
	}

	/**
	 * Must be done after "envProps" exists
	 */
	protected Log getLog() throws IOException {
		// Create log file
		int logLevel = Integer.parseInt(envProps.getProperty("logLevel"));

		Date now = new Date();
		StringBuilder logFileName = new StringBuilder(simFolder.getPath()
				.replace('/', '_'));
		logFileName.append('_').append(num).append('_');
		logFileName.append(DATE_FORMAT.format(now)).append('_');
		// TODO This replace should could be done by modifying the date
		// format
		logFileName.append(TIME_FORMAT.format(now).replace(':', '.'));
		logFileName.append(".txt");

		File logDir = new File(simFolder, Consts.logDir);
		logDir.mkdirs();

		File logFile = new File(logDir, logFileName.toString());
		if (logLevel == Log.NO_LOGGING)
			logFile.deleteOnExit();

		// Create log file
		// TODO Look into constructor, I think the 2nd and 3rd arguments
		// have to do with base directory and then logFileName. In this case
		// "getPath" will probably do the right thing given the current
		// setup, but isn't guaranteed to.
		Log log = new Log(logLevel, ".", logFile.getPath(), true);

		// Log properties
		log.log(Log.DEBUG, envProps.toString());

		return log;
	}

	/**
	 * Must be done after "log" and "data" exist
	 * 
	 * @return
	 */
	protected SimulationSpec getSimulationSpec() {
		// Read simulation specification file
		File simulationSpecFile = new File(simFolder, Consts.simSpecFile);
		if (!simulationSpecFile.exists())
			throw new IllegalArgumentException("Simulation Spec file ("
					+ simulationSpecFile.getAbsolutePath() + ") doesn't exist");

		// Read simulation_spec.json file
		return new SimulationSpec(simulationSpecFile.getAbsolutePath(), log,
				data);
	}

	/**
	 * Method to execute all events in the Event Queue.
	 */
	public void executeEvents() {
		while (!eventManager.isEmpty()) {
			eventManager.executeNext();
		}
		log.log(Log.INFO, "STATUS: Simulation has ended.");
	}

	/**
	 * Generate results report (payoff data, feature data logging).
	 * 
	 * @throws IOException
	 */
	public void aggregateResults() throws IOException {
		File results = new File(simFolder, Consts.obsFile + data.num + ".json");
		FileWriter writer = new FileWriter(results);
		writer.write(obs.generateObservationFile());
		writer.close();
	}

}
