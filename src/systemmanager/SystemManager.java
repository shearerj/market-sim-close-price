package systemmanager;

import static logger.Logger.log;
import static logger.Logger.Level.DEBUG;
import static logger.Logger.Level.INFO;
import static logger.Logger.Level.NO_LOGGING;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import java.util.Collections;
import java.util.Date;
import java.util.Properties;

import logger.Logger;
import logger.Logger.Prefix;
import model.MarketModel;
import utils.RandPlus;
import data.EntityProperties;
import data.FundamentalValue;
import data.Observations;
import entity.market.Market;
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

	protected static DateFormat DATE_FORMAT = new SimpleDateFormat("dd-MMM-yyyy_HH.mm.ss");

	protected final EventManager eventManager;
	protected final MarketModel model;

	protected final int obsNum; // sample number used for labeling output files
	protected final File simFolder; // simulation folder
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

		spec = new SimulationSpec(new File(simFolder, Consts.SIM_SPEC_FILE));
		// XXX Move props to simspec?
		EntityProperties simProps = spec.getSimulationConfig();
		long seed = simProps.getAsLong(Keys.RAND_SEED,
				System.currentTimeMillis());
		RandPlus rand = new RandPlus(seed);

		simulationLength = new TimeStamp(simProps.getAsLong(Keys.SIMULATION_LENGTH));
		eventManager = new EventManager(new RandPlus(rand.nextLong()));
		
		initializeLogger(getProperties(), simFolder, simNumber, eventManager,
				spec.getSimulationConfig().getAsInt(Keys.SIMULATION_LENGTH,
						10000));
		log(INFO, "Random Seed: " + seed);
		log(INFO, "Configuration: " + spec);

		FundamentalValue fundamental = new FundamentalValue(
				simProps.getAsDouble(Keys.FUNDAMENTAL_KAPPA),
				simProps.getAsInt(Keys.FUNDAMENTAL_MEAN),
				simProps.getAsDouble(Keys.FUNDAMENTAL_SHOCK_VAR),
				new RandPlus(rand.nextLong()));

		// TODO Only create one market model?
		log(INFO, "------------------------------------------------");
		log(INFO, "            Creating MARKET MODELS");
		
		model = new MarketModel(fundamental, spec.getSimulationConfig(),
				spec.getMarketConfigs(), spec.getAgentConfigs(),
				spec.getPlayerConfig(), rand);

		log(INFO, null + ": " + model);
		for (Market market : model.getMarkets())
			log(INFO, market.getName() + " " + market + " in " + model);
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
			int num, final EventManager eventManager, final int simLength)
			throws IOException {
		// Create log file
		int logLevel = Integer.parseInt(envProps.getProperty("logLevel"));

		StringBuilder logFileName = new StringBuilder(
				simFolder.getPath().replace('/', '_'));
		logFileName.append('_').append(num).append('_');
		logFileName.append(DATE_FORMAT.format(new Date())).append(".txt");

		File logDir = new File(simFolder, Consts.LOG_DIR);
		logDir.mkdirs();

		File logFile = new File(logDir, logFileName.toString());

		// Create log file
		Logger.setup(logLevel, logFile, true, new Prefix() {
			@Override
			// TODO If/when only one market model, change this to prefix model id
			public String getPrefix() {
				return String.format("%" + Integer.toString(simLength).length()
						+ "d| ", eventManager.getCurrentTime().getInTicks());
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
		model.scheduleActivities(eventManager);
		eventManager.executeUntil(simulationLength);
		log(INFO, "STATUS: Simulation has ended.");
	}

	public void aggregateResults() throws IOException {
		if (Logger.getLevel() == DEBUG) { // Write out objects for further analysis
			File objects = new File(simFolder, Consts.OBJS_FILE_PREFIX + obsNum + ".bit");
			ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(objects));
			out.writeObject(model);
			out.close();
		}
		// TODO Serialize all entity objects and models for later data analysis
		File results = new File(simFolder, Consts.OBS_FILE_PREFIX + obsNum + ".json");
		Observations obs = new Observations(spec, Collections.singletonList(model), obsNum);
		obs.writeToFile(results);
	}

}
