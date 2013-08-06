package systemmanager;

import static logger.Logger.log;
import static logger.Logger.Level.DEBUG;
import static logger.Logger.Level.INFO;
import static logger.Logger.Level.NO_LOGGING;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;

import logger.Logger;
import logger.Logger.Prefix;
import model.MarketModel;
import model.MarketModelFactory;
import utils.RandPlus;
import data.EntityProperties;
import data.FundamentalValue;
import data.ModelProperties;
import data.Observations;
import event.TimeStamp;

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

	protected final int obsNum; // sample number used for labeling output files
	protected final File simFolder; // simulation folder name
	protected final SimulationSpec spec;
	protected final TimeStamp simulationLength;

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
		this.obsNum = simNumber;

		// TODO move props to SimSpec?

		spec = new SimulationSpec(new File(simFolder, Consts.SIM_SPEC_FILE));
		EntityProperties simProps = spec.getSimulationProperties();
		long seed = simProps.getAsLong(SimulationSpec.RAND_SEED,
				System.currentTimeMillis());
		RandPlus rand = new RandPlus(seed);

		simulationLength = new TimeStamp(simProps.getAsLong(SimulationSpec.SIMULATION_LENGTH));
		eventManager = new EventManager(new RandPlus(rand.nextLong()));
		
		initializeLogger(getProperties(), simFolder, simNumber, eventManager);
		log(INFO, "Random Seed: " + seed);
		// TODO Log configuration

		FundamentalValue fundamental = new FundamentalValue(
				simProps.getAsDouble(SimulationSpec.FUNDAMENTAL_KAPPA),
				simProps.getAsInt(SimulationSpec.FUNDAMENTAL_MEAN),
				simProps.getAsDouble(SimulationSpec.FUNDAMENTAL_SHOCK_VAR),
				new RandPlus(rand.nextLong()));

		MarketModelFactory modelFactory = new MarketModelFactory(
				spec.getBackgroundAgents(), spec.getPlayerConfig(),
				fundamental, new RandPlus(rand.nextLong()));

		// TODO Only create one market model?
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
			int num, final EventManager eventManager) throws IOException {
		// Create log file
		int logLevel = Integer.parseInt(envProps.getProperty("logLevel"));

		Date now = new Date();
		StringBuilder logFileName = new StringBuilder(
				simFolder.getPath().replace('/', '_'));
		logFileName.append('_').append(num).append('_');
		logFileName.append(DATE_FORMAT.format(now)).append('_');
		// TODO This replace should/could be done by modifying the date
		// format
		logFileName.append(TIME_FORMAT.format(now).replace(':', '.'));
		logFileName.append(".txt");

		File logDir = new File(simFolder, Consts.LOG_DIR);
		logDir.mkdirs();

		File logFile = new File(logDir, logFileName.toString());

		// Create log file
		Logger.setup(logLevel, logFile, true, new Prefix() {
			@Override
			// TODO If/when only one market model, change this to prefix model id
			public String getPrefix() {
				return String.format("% 4d| ", eventManager.getCurrentTime().longValue());
			}
		});
		
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
		eventManager.executeUntil(simulationLength);
		log(INFO, "STATUS: Simulation has ended.");
	}

	public void aggregateResults() throws IOException {
		// TODO Also serialize all entity objects and models for later data analysis
		File results = new File(simFolder, Consts.OBS_FILE_PREFIX + obsNum + ".json");
		Observations obs = new Observations(spec, models, obsNum);
		obs.writeToFile(results);
	}

}
