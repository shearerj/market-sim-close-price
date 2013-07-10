package systemmanager;

import static logger.Logger.log;
import static logger.Logger.Level.*;

import data.*;
import event.*;

import java.util.*;
import java.io.*;
import java.text.DateFormat;

import utils.RandPlus;

import logger.Logger;
import model.MarketModel;
import model.MarketModelFactory;

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
	protected final Collection<MarketModel> models;

	protected final int num; // sample number used for labeling output files
	protected final File simFolder; // simulation folder name
	protected final SimulationSpec spec;

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
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		} catch (IllegalArgumentException e) {
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

		// TODO move props to SimSpec?
		initializeLogger(getProperties(), simFolder, simNumber);

		spec = new SimulationSpec(new File(simFolder, Consts.SIM_SPEC_FILE));
		EntityProperties simProps = spec.getSimulationProperties();
		long seed = simProps.getAsLong(SimulationSpec.RAND_SEED,
				System.currentTimeMillis());
		log(INFO, "RandomSeed: " + seed);
		RandPlus rand = new RandPlus(seed);

		TimeStamp simLength = new TimeStamp(
				simProps.getAsLong(SimulationSpec.SIMULATION_LENGTH));
		eventManager = new EventManager(simLength,
				new RandPlus(rand.nextLong()));

		FundamentalValue fundamental = new FundamentalValue(
				simProps.getAsDouble(SimulationSpec.FUNDAMENTAL_KAPPA),
				simProps.getAsInt(SimulationSpec.FUNDAMENTAL_MEAN),
				simProps.getAsDouble(SimulationSpec.FUNDAMENTAL_SHOCK_VAR),
				new RandPlus(rand.nextLong()));

		MarketModelFactory modelFactory = new MarketModelFactory(
				spec.getBackgroundAgents(), spec.getPlayerConfig(),
				fundamental, new RandPlus(rand.nextLong()));

		log(INFO, "------------------------------------------------");
		log(INFO, "            Creating MARKET MODELS");
		models = new ArrayList<MarketModel>();
		for (ModelProperties props : spec.getModels()) {
			MarketModel model = modelFactory.createModel(props);
			models.add(model);
			// TODO Log markets?
			log(INFO, props.getModelType() + ": " + model);
		}
		log(INFO, "------------------------------------------------");
	}

	protected Properties getProperties() throws IOException {
		Properties props = new Properties();
		props.load(new FileInputStream(new File(Consts.CONFIG_DIR,
				Consts.CONFIG_FILE)));
		return props;
	}

	/**
	 * Must be done after "envProps" exists
	 */
	protected static void initializeLogger(Properties envProps, File simFolder,
			int num) throws IOException {
		// Create log file
		int logLevel = Integer.parseInt(envProps.getProperty("logLevel"));

		Date now = new Date();
		StringBuilder logFileName = new StringBuilder(
				simFolder.getPath().replace('/', '_'));
		logFileName.append('_').append(num).append('_');
		logFileName.append(DATE_FORMAT.format(now)).append('_');
		// TODO This replace should could be done by modifying the date
		// format
		logFileName.append(TIME_FORMAT.format(now).replace(':', '.'));
		logFileName.append(".txt");

		File logDir = new File(simFolder, Consts.LOG_DIR);
		logDir.mkdirs();

		File logFile = new File(logDir, logFileName.toString());

		// Create log file
		// TODO Look into constructor, I think the 2nd and 3rd arguments
		// have to do with base directory and then logFileName. In this case
		// "getPath" will probably do the right thing given the current
		// setup, but isn't guaranteed to.
		Logger.setup(logLevel, ".", logFile.getPath(), true);
		
		if (Logger.getLevel() == NO_LOGGING)
			logFile.deleteOnExit();

		// Log properties
		log(DEBUG, envProps.toString());
	}

	/**
	 * Method to execute all events in the Event Queue.
	 */
	public void executeEvents() {
		for (MarketModel model : models)
			model.scheduleActivities(eventManager);
		// TODO Get rid of event manager and move to system manager.
		eventManager.execute();
		log(INFO, "STATUS: Simulation has ended.");
	}

	public void aggregateResults() throws IOException {
		File results = new File(simFolder, Consts.OBS_FILE_PREFIX + num + ".json");
		Observations obs = new Observations(spec, models);
		obs.writeToFile(results);
	}

}
