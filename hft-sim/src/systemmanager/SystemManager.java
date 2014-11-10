package systemmanager;

import static com.google.common.base.Preconditions.checkArgument;
import static logger.Log.Level.INFO;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.Random;

import logger.Log;
import systemmanager.Keys.NumSims;
import systemmanager.Keys.RandomSeed;
import systemmanager.SimulationSpec.SimSpecDeserializer;
import utils.Rand;
import utils.SummStats;

import com.google.common.base.Objects;
import com.google.common.collect.Multimap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import data.Observations;
import data.Observations.PlayerObservationSerializer;
import data.Observations.SummStatsSerializer;
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
	
	protected static final DateFormat LOG_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
	protected static final Gson gson = new GsonBuilder()
	.registerTypeAdapter(SummStats.class, new SummStatsSerializer())
//	.registerTypeAdapter(Map.class, new StddevFeaturesSerializer())
	.registerTypeAdapter(Multimap.class, new PlayerObservationSerializer())
	.serializeSpecialFloatingPointValues() // XXX This will allow serializing NaNs for testing, but will cause errors for EGTA which doesn't like NaNs...
	.registerTypeAdapter(SimulationSpec.class, new SimSpecDeserializer())
	.create();

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
		SimulationSpec specification = gson.fromJson(simSpecIn, SimulationSpec.class);
		Properties props = new Properties();
		props.load(propIn);

		Props simProps = specification.getSimulationProps();
		int totalSimulations = simProps.get(NumSims.class);
		long baseRandomSeed = simProps.get(RandomSeed.class);
		
		int logLevel = Integer.parseInt(props.getProperty("logLevel", "0"));
		
		Observations observations = Observations.create(specification.getPlayerProps(), specification.getSimulationProps());
		Random rand = Rand.create();
		
		for (int i = 0; i < totalSimulations; i++) {
			// This formula means that you'll get the same simulations regardless of the number of observations or simulations
			rand.setSeed(Objects.hashCode(baseRandomSeed, observationNumber * totalSimulations + i));
			Simulation sim = Simulation.create(specification, rand, logOut, Log.Level.values()[logLevel]);
			
			sim.log(INFO, "Random Seed: %d", baseRandomSeed);
			sim.log(INFO, "Configuration: %s", specification);
			
			sim.executeEvents();
			observations.add(sim.getStatistics(), sim.getPlayers());
		}
		
		// Add specification if flag is set
		JsonObject obs = gson.toJsonTree(observations).getAsJsonObject();
		if (Boolean.parseBoolean(props.getProperty("outputConfig", "false")))
			obs.get("features").getAsJsonObject().add("config", specification.getRawSpec());
		
		gson.toJson(obs, obsOut);
		obsOut.flush();
	}
	
}
