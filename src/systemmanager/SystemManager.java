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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;

import activity.AgentArrival;
import activity.Clear;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import logger.Logger;
import logger.Logger.Prefix;
import utils.RandPlus;
import data.AgentProperties;
import data.EntityProperties;
import data.FundamentalValue;
import data.MarketProperties;
import data.Observations;
import data.Player;
import entity.agent.Agent;
import entity.agent.AgentFactory;
import entity.infoproc.SIP;
import entity.market.Market;
import entity.market.MarketFactory;
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

	protected final RandPlus rand;
	protected final EventManager eventManager;
	protected final FundamentalValue fundamental;
	protected final SIP sip;
	protected final Collection<Market> markets;
	protected final Collection<Agent> agents;
	protected final Collection<Player> players;

	protected final int obsNum; // sample number used for labeling output files
	protected final File simFolder; // simulation folder
	protected final SimulationSpec spec;
	protected final TimeStamp simulationLength;
	protected final String modelName;

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
		EntityProperties simProps = spec.getSimulationProps();
		Collection<MarketProperties> marketProps = spec.getMarketProps();
		Collection<AgentProperties> agentProps = spec.getAgentProps();
		JsonObject playerConfig = spec.getPlayerProps();

		// XXX Adding obsNum is a hack so that it's easy to run a set of simulations with the same
		// random seed. This is fine if the random number generator is good, and a seed only one off
		// produces sufficiently independent draws, but if this isn't the case this is poisoning
		// results.
		long seed = simProps.getAsLong(Keys.RAND_SEED, System.currentTimeMillis()) + obsNum;
		rand = new RandPlus(seed);
		modelName = simProps.getAsString(Keys.MODEL_NAME); // TODO Move name generation here?

		simulationLength = new TimeStamp(simProps.getAsLong(Keys.SIMULATION_LENGTH));
		eventManager = new EventManager(new RandPlus(rand.nextLong()));
		
		initializeLogger(getLogLevel(), simFolder, simNumber, eventManager,
				simProps.getAsInt(Keys.SIMULATION_LENGTH, 10000),
				simProps.getAsInt(Keys.MODEL_NUM));
		log(INFO, modelName);
		log(INFO, "Random Seed: " + seed);
		log(INFO, "Configuration: " + spec);

		fundamental = new FundamentalValue(
				simProps.getAsDouble(Keys.FUNDAMENTAL_KAPPA),
				simProps.getAsInt(Keys.FUNDAMENTAL_MEAN),
				simProps.getAsDouble(Keys.FUNDAMENTAL_SHOCK_VAR),
				new RandPlus(rand.nextLong()));
		sip = new SIP(new TimeStamp(simProps.getAsInt(Keys.NBBO_LATENCY)));
		markets = new ArrayList<Market>();
		agents = new ArrayList<Agent>();
		players = new ArrayList<Player>();
		setupMarkets(marketProps);
		setupAgents(agentProps);
		setupPlayers(simProps, playerConfig);

		for (Market market : markets)
			log(INFO, "Created Market: " + market);
	}
	
	protected void setupMarkets(Collection<MarketProperties> marketProps) {
		MarketFactory factory = new MarketFactory(sip);
		for (MarketProperties mktProps : marketProps)
			for (int i = 0; i < mktProps.getAsInt(Keys.NUM, 1); i++)
				markets.add(factory.createMarket(mktProps));
	}
	
	protected void setupAgents(Collection<AgentProperties> agentProps) {
		for (AgentProperties agProps : agentProps) {
			int number = agProps.getAsInt(Keys.NUM, 0);
			double arrivalRate = agProps.getAsDouble(Keys.ARRIVAL_RATE, 0.075);
			
			// XXX In general the arrival process and market generation can be
			// generic, but for now we'll stick with the original implementation which is round
			// robin markets and poisson arrival
			AgentFactory factory = new AgentFactory(fundamental, sip, markets,
					arrivalRate, new RandPlus(rand.nextLong()));

			for (int i = 0; i < number; i++)
				agents.add(factory.createAgent(agProps));
		}
	}
	
	protected void setupPlayers(EntityProperties modelProps,
			JsonObject playerConfig) {
		// First group by role and agentType for legacy reasons / arrival rate reasons
		// TODO Re-think how to schedule player arrival rates. (Maybe be less important if agent's
		// reenter)
		Map<String, Map<String, Integer>> roleStratCounts = new HashMap<String, Map<String, Integer>>();
		for (Entry<String, JsonElement> roleEnt : playerConfig.entrySet()) {
			String role = roleEnt.getKey();
			JsonArray strats = roleEnt.getValue().getAsJsonArray();
			Map<String, Integer> stratCounts = roleStratCounts.get(role);
			if (stratCounts == null) {
				stratCounts = new HashMap<String, Integer>();
				roleStratCounts.put(role, stratCounts);
			}

			for (JsonElement jStrat : strats) {
				String strat = jStrat.getAsString();
				Integer count = stratCounts.get(strat);
				stratCounts.put(strat, count == null ? 1 : count + 1);
			}
		}

		// Generate Players
		for (Entry<String, Map<String, Integer>> roleEnt : roleStratCounts.entrySet()) {
			String role = roleEnt.getKey();
			for (Entry<String, Integer> stratEnt : roleEnt.getValue().entrySet()) {
				String strat = stratEnt.getKey();
				int number = stratEnt.getValue();
				double arrivalRate = modelProps.getAsDouble(Keys.ARRIVAL_RATE, 0.075);

				AgentFactory factory = new AgentFactory(fundamental, sip,
						markets, arrivalRate, new RandPlus(rand.nextLong()));

				for (int i = 0; i < number; i++) {
					Agent agent = factory.createAgent(new AgentProperties(strat));
					agents.add(agent);
					Player player = new Player(role, strat, agent);
					players.add(player);
				}
			}
		}
	}

	protected int getLogLevel() throws IOException {
		Properties props = new Properties();
		props.load(new FileInputStream(new File(Consts.CONFIG_DIR,
				Consts.CONFIG_FILE)));
		return Integer.parseInt(props.getProperty("logLevel"));
	}

	/**
	 * Must be done after "envProps" exists
	 */
	protected static void initializeLogger(int logLevel, File simFolder,
			int num, final EventManager eventManager, int simLength, final int modelNumber)
			throws IOException {
		StringBuilder logFileName = new StringBuilder(
				simFolder.getPath().replace('/', '_'));
		logFileName.append('_').append(num).append('_');
		logFileName.append(DATE_FORMAT.format(new Date())).append(".txt");

		File logDir = new File(simFolder, Consts.LOG_DIR);
		logDir.mkdirs();

		File logFile = new File(logDir, logFileName.toString());
		final int digits = Integer.toString(simLength).length();

		// Create log file
		Logger.setup(logLevel, logFile, true, new Prefix() {
			@Override
			// TODO If/when only one market model, change this to prefix model id
			public String getPrefix() {
				return String.format("%" + digits + "d|%d| ",
						eventManager.getCurrentTime().getInTicks(), modelNumber);
			}
		});
		
		if (Logger.getLevel() == NO_LOGGING)
			logFile.deleteOnExit();
	}

	/**
	 * Method to execute all events in the Event Queue.
	 */
	public void executeEvents() {
		for (Market market : markets)
			eventManager.addActivity(new Clear(market, TimeStamp.IMMEDIATE));
		for (Agent agent : agents)
			eventManager.addActivity(new AgentArrival(agent, agent.getArrivalTime()));
		eventManager.executeUntil(simulationLength);
		log(INFO, "[[[ Simulation Over ]]]");
	}

	public void aggregateResults() throws IOException {
		if (Logger.getLevel() == DEBUG) { // Write out objects for further analysis
			File objects = new File(simFolder, Consts.OBJS_FILE_PREFIX + obsNum + ".bit");
			ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(objects));
			out.writeObject(spec);
			out.writeObject(fundamental);
			out.writeObject(sip);
			out.writeObject(markets);
			out.writeObject(agents);
			out.writeObject(players);
			out.close();
		}
		File results = new File(simFolder, Consts.OBS_FILE_PREFIX + obsNum + ".json");
		Observations obs = new Observations(spec, markets, agents, players,
				fundamental, sip, modelName, obsNum);
		obs.writeToFile(results);
	}

}
