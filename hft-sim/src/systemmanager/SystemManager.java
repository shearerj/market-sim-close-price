package systemmanager;

import static com.google.common.base.Preconditions.checkArgument;
import static logger.Log.Level.INFO;
import static logger.Log.log;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.Random;

import logger.Log;
import logger.Log.Clock;

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
public abstract class SystemManager {

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
			execute(simulationFolder, observationNumber);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	protected static DateFormat LOG_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");

	public static void execute(File simFolder, int obsNum) throws IOException {
		// Check Directory
		checkArgument(simFolder.exists(), "Simulation folder must exist");
		checkArgument(simFolder.isDirectory(), "Simulation folder must be a directory");
		checkArgument(simFolder.canWrite(), "Simulation folder must be writable");
		// Check Spec File
		File simSpecFile = new File(simFolder, "simulation_spec.json");
		checkArgument(simSpecFile.exists(), "SimulationSpec file must exist in root directory");
		checkArgument(simSpecFile.canRead(), "SimulationSpec file must be readable");
		Reader specReader = new FileReader(simSpecFile);
		// Check Properties File
		File propsFile = new File("config", "env.properties");
		checkArgument(simSpecFile.exists(), "Properties file must exist in config/env.properties");
		checkArgument(simSpecFile.canRead(), "Properties file must be readable");
		Reader propsReader = new FileReader(propsFile);
		// Observation File
		File obsFile = new File(simFolder, "observation" + obsNum + ".json");
		checkArgument(simSpecFile.canWrite(), "Observations file must be writable");
		Writer obsWriter = new FileWriter(obsFile);
		// Log File
		StringBuilder logFileName = new StringBuilder(
				new File(".").toURI().relativize(simFolder.toURI()).getPath().replace('/', '_'));
		logFileName.append(obsNum).append('_');
		logFileName.append(LOG_DATE_FORMAT.format(new Date())).append(".txt");
		File logDir = new File(simFolder, "logs");
		logDir.mkdirs();
		Writer logWriter = new FileWriter(new File(logDir, logFileName.toString()));
		// Execute
		execute(specReader, propsReader, obsWriter, logWriter, obsNum);
		// Close and flush
		specReader.close();
		propsReader.close();
		obsWriter.close();
		logWriter.close();
	}
	
	public static void execute(Reader simSpecIn, Reader propIn, Writer obsOut, Writer logOut, int observationNumber) throws IOException {
		SimulationSpec specification = SimulationSpec.read(simSpecIn);
		Properties props = new Properties();
		props.load(propIn);

		EntityProperties simProps = specification.getSimulationProps();
		int totalSimulations = simProps.getAsInt(Keys.NUM_SIMULATIONS);
		long baseRandomSeed = simProps.getAsLong(Keys.RAND_SEED);
		
		int logLevel = Integer.parseInt(props.getProperty("logLevel", "0"));
		boolean outputConfig = Boolean.parseBoolean(props.getProperty("outputConfig", "false"));
		
		MultiSimulationObservations observations = new MultiSimulationObservations(outputConfig, totalSimulations);
		
		Random rand = new Random();
		for (int i = 0; i < totalSimulations; i++) {
			Market.nextID = Agent.nextID = ProcessorIDs.nextID = 1; // Reset ids
			rand.setSeed(Objects.hashCode(baseRandomSeed, observationNumber * totalSimulations + i));
			Simulation sim = new Simulation(specification, rand);
			
			initializeLogger(logLevel, logOut, sim);
			log(INFO, "Random Seed: %d", baseRandomSeed);
			log(INFO, "Configuration: %s", specification);
			
			sim.executeEvents();
			observations.addObservation(sim.getObservations());
		}
		observations.write(new PrintWriter(System.err));
		observations.write(obsOut);
	}

	protected static void initializeLogger(int logLevel, Writer logWriter, final Simulation simulation) {
		if (logLevel == 0) { // No logging
			Log.setLogger(Log.nullLogger());
		} else {
			Log.setLogger(Log.create(Log.Level.values()[logLevel], logWriter, new Clock() {
				@Override
				public long getTime() { return simulation.getCurrentTime().getInTicks(); }
				@Override
				public int getPadding() { return 6; }
			}));
		}
	}
	
}
