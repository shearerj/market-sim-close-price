package systemmanager;

import static com.google.common.base.Preconditions.checkArgument;
import static logger.Log.Level.INFO;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Properties;
import java.util.Random;
import java.util.Set;

import java.nio.file.attribute.PosixFilePermission;

import logger.Log;
import systemmanager.Keys.NumSims;
import systemmanager.Keys.RandomSeed;
import utils.LazyFileWriter;
import utils.Rand;

import com.google.common.base.Objects;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.primitives.Ints;

import data.Observations;
import data.Observations.OutputType;
import data.Props;

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
	
	private static final Splitter propListSplitter = Splitter.on(';').omitEmptyStrings();

	/**
	 * Two input arguments: first is simulation folder, second is sample number
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

	public static void execute(final File simFolder, int obsNum) throws IOException {
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
		checkArgument(propsFile.exists(), "Properties file must exist in config/env.properties");
		checkArgument(propsFile.canRead(), "Properties file must be readable");
		Reader propsReader = new FileReader(propsFile);
		// Observation File
		File obsFile = new File(simFolder, "observation" + obsNum + ".json");
		obsFile.createNewFile();
		checkArgument(obsFile.canWrite(), "Observations file must be writable");
		Writer obsWriter = LazyFileWriter.create(obsFile);
		// Log File
		StringBuilder logFileName = new StringBuilder(
				new File(".").toURI().relativize(simFolder.toURI()).getPath().replace('/', '_'));
		logFileName.append(obsNum).append('_');
		
		// date format is not thread-safe, so should be created locally for use in public static method
		final DateFormat logDateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
		logFileName.append(logDateFormat.format(new Date())).append(".txt");
		Writer logWriter = LazyFileWriter.create(new File(new File(simFolder, "logs"), logFileName.toString()));
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
		
		// Load and parse properties
		Properties props = new Properties();
		props.load(propIn);
		
		Log.Level logLevel = Log.Level.values()[Integer.parseInt(props.getProperty("logLevel", "0"))];
		boolean egta = Boolean.parseBoolean(props.getProperty("egta", "true"));
		Iterable<String> whitelist = propListSplitter.splitToList(props.getProperty("whitelist", ""));
		Iterable<Integer> periods = ImmutableList.copyOf(Iterables.transform(
				propListSplitter.split(props.getProperty("periods", "")), Ints.stringConverter()));
		
		execute(specification, obsOut, logOut, observationNumber, logLevel, egta, whitelist, periods);
	}
	
	public static void execute(SimulationSpec specification, Writer obsOut, Writer logOut, int observationNumber,
			Log.Level logLevel, boolean egta, Iterable<String> whitelist, Iterable<Integer> periods) throws IOException {
		Props simProps = specification.getSimulationProps();
		int totalSimulations = simProps.get(NumSims.class);
		long baseRandomSeed = simProps.get(RandomSeed.class);
		
		Observations observations = Observations.create(specification, whitelist, periods);
		Random rand = Rand.create();
		
		for (int i = 0; i < totalSimulations; i++) {
			// This formula means that you'll get the same simulations regardless of the number of observations or simulations
			long seed = Objects.hashCode(baseRandomSeed, observationNumber * totalSimulations + i);
			rand.setSeed(seed);
			Simulation sim = Simulation.create(specification, rand, logOut, logLevel);
			
			sim.log(INFO, "Simulation Random Seed: %d", seed);
			sim.log(INFO, "Configuration: %s", specification);
			
			sim.executeEvents();
			observations.add(sim.getStatistics(), sim.getPlayers());
		}
		
		// Add specification if flag is set
		observations.write(obsOut, egta ? OutputType.EGTA : OutputType.DEFAULT);
		obsOut.flush();
	}
	
}
