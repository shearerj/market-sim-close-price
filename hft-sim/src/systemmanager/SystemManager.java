package systemmanager;

import static logger.Log.log;
import static logger.Log.Level.INFO;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.Random;

import logger.Log;
import logger.Log.Prefix;

import com.google.common.base.Objects;

import data.EntityProperties;
import data.MultiSimulationObservations;
import entity.agent.Agent;
import entity.infoproc.ProcessorIDs;
import entity.market.Market;

/**
 * This class serves the purpose of the Client in the Command pattern, in that
 * it instantiates the Activity objects and provides the methods to execute them
 * later.
 * 
 * Usage: java -cp "$(ls lib/*.jar | tr '\n' :)dist/hft.jar" systemmanager.SystemManager [simulation folder name] [sample #]
 * 
 * @author ewah
 */
public class SystemManager {

	/**
	 * Two input arguments: first is simulation folder, second is sample number
	 * 
	 * @param args
	 */
	public static void main(String... args) {

		File simulationFolder = new File(".");
		int observationNumber = 1;
		switch (args.length) {
		default:
			observationNumber = Integer.parseInt(args[1]);
		case 1:
			simulationFolder = new File(args[0]);
		case 0:
		}

		try {
			SystemManager manager = new SystemManager(simulationFolder, observationNumber);
			manager.executeSimulations();
			manager.writeResults();
			log.closeLogger();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	protected static DateFormat LOG_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
	
	private final File simulationFolder;
	private final int observationNumber, totalSimulations, simulationLength, logLevel;
	private final boolean outputConfig;
	private final long baseRandomSeed;
	private final SimulationSpec specification;
	private final MultiSimulationObservations observations;

	/**
	 * Constructor reads everything in and sets appropriate variables
	 */
	public SystemManager(File simFolder, int obsNum) throws IOException {
		this.simulationFolder = simFolder;
		this.observationNumber = obsNum;
		this.specification = new SimulationSpec(new File(simFolder, Consts.SIM_SPEC_FILE));
		
		EntityProperties simProps = specification.getSimulationProps();
		this.totalSimulations = simProps.getAsInt(Keys.NUM_SIMULATIONS);
		this.baseRandomSeed = simProps.getAsLong(Keys.RAND_SEED);
		this.simulationLength = simProps.getAsInt(Keys.SIMULATION_LENGTH);
		
		Properties props = new Properties();
		props.load(new FileInputStream(new File(Consts.CONFIG_DIR, Consts.CONFIG_FILE)));
		this.logLevel = Integer.parseInt(props.getProperty("logLevel", "0"));
		this.outputConfig = Boolean.parseBoolean(props.getProperty("outputConfig", "false"));
		
		this.observations = new MultiSimulationObservations(outputConfig, totalSimulations);
	}
	
	/**
	 * Runs all of the simulations
	 */
	public void executeSimulations() throws IOException {
		Random rand = new Random();
		for (int i = 0; i < totalSimulations; i++) {
			Market.nextID = Agent.nextID = ProcessorIDs.nextID = 1; // Reset ids
			rand.setSeed(Objects.hashCode(baseRandomSeed, observationNumber * totalSimulations + i));
			Simulation sim = new Simulation(specification, rand);
			
			initializeLogger(logLevel, simulationFolder, observationNumber, i, sim, simulationLength);
			log.log(INFO, "Random Seed: %d", baseRandomSeed);
			log.log(INFO, "Configuration: %s", specification);
			
			sim.executeEvents();
			observations.addObservation(sim.getObservations());
		}
	}
	
	/**
	 * Must be done after "envProps" exists
	 */
	protected static void initializeLogger(int logLevel, File simulationFolder,
			int observationNumber, int simulationNumber, final Simulation simulation,
			int simulationLength) throws IOException {
		if (logLevel == 0) { // No logging
			log = Log.nullLogger();
			return;
		}
		
		StringBuilder logFileName = new StringBuilder(
				new File(".").toURI().relativize(simulationFolder.toURI()).getPath().replace('/', '_'));
		logFileName.append(observationNumber).append('_');
		logFileName.append(simulationNumber).append('_');
		logFileName.append(LOG_DATE_FORMAT.format(new Date())).append(".txt");

		File logDir = new File(simulationFolder, Consts.LOG_DIR);
		logDir.mkdirs();

		File logFile = new File(logDir, logFileName.toString());
		final int digits = Integer.toString(simulationLength).length();

		// Create log file
		log = Log.create(Log.Level.values()[logLevel], logFile, new Prefix() {
			@Override
			public String getPrefix() {
				return String.format("%" + digits + "d| ", simulation.getCurrentTime().getInTicks());
			}
		});
	}
	
	public void writeResults() throws IOException {
		File results = new File(simulationFolder, Consts.OBS_FILE_PREFIX + observationNumber + ".json");
		observations.writeToFile(results);
	}
	
}
