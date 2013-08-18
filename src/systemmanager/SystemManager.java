package systemmanager;

import static com.google.common.base.Preconditions.checkArgument;
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
import java.util.Collection;
import java.util.Date;
import java.util.Map.Entry;
import java.util.Properties;

import logger.Logger;
import logger.Logger.Prefix;
import utils.Pair;
import utils.Rands;
import activity.AgentArrival;
import activity.Clear;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;
import com.google.common.primitives.Longs;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

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

	protected final Rands rand;
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
		checkArgument(simFolder.exists(),
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
		long seed = Longs.hashCode(simProps.getAsLong(Keys.RAND_SEED, System.currentTimeMillis()) + obsNum);
		rand = new Rands(seed);
		modelName = simProps.getAsString(Keys.MODEL_NAME); // TODO Move name generation here?

		simulationLength = new TimeStamp(simProps.getAsLong(Keys.SIMULATION_LENGTH));
		eventManager = new EventManager(new Rands(rand.nextLong()));
		
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
				new Rands(rand.nextLong()));
		sip = new SIP(new TimeStamp(simProps.getAsInt(Keys.NBBO_LATENCY)));
		markets = setupMarkets(marketProps);
		agents = setupAgents(agentProps);
		players = setupPlayers(simProps, playerConfig);

		for (Market market : markets)
			log(INFO, "Created Market: " + market);
	}
	
	protected Collection<Market> setupMarkets(Collection<MarketProperties> marketProps) {
		Builder<Market> markets = ImmutableList.builder();
		MarketFactory factory = new MarketFactory(sip);
		for (MarketProperties mktProps : marketProps)
			for (int i = 0; i < mktProps.getAsInt(Keys.NUM, 1); i++)
				markets.add(factory.createMarket(mktProps));
		return markets.build();
	}
	
	protected Collection<Agent> setupAgents(Collection<AgentProperties> agentProps) {
		// Not immutable because players also adds agents
		Collection<Agent> agents = Lists.newArrayList();
		for (AgentProperties agProps : agentProps) {
			int number = agProps.getAsInt(Keys.NUM, 0);
			double arrivalRate = agProps.getAsDouble(Keys.ARRIVAL_RATE, 0.075);
			
			// XXX In general the arrival process and market generation can be
			// generic, but for now we'll stick with the original implementation which is round
			// robin markets and poisson arrival
			AgentFactory factory = new AgentFactory(fundamental, sip, markets,
					arrivalRate, new Rands(rand.nextLong()));

			for (int i = 0; i < number; i++)
				agents.add(factory.createAgent(agProps));
		}
		return agents;
	}
	
	protected Collection<Player> setupPlayers(EntityProperties modelProps,
			JsonObject playerConfig) {
		Builder<Player> players = ImmutableList.builder();
		// First group by role and agentType for legacy reasons / arrival rate reasons
		// TODO Re-think how to schedule player arrival rates. (Maybe be less important if agent's
		// reenter)
		double arrivalRate = modelProps.getAsDouble(Keys.ARRIVAL_RATE, 0.075);
		
		Multiset<RoleStrat> counts = HashMultiset.create();
		for (Entry<String, JsonElement> role : playerConfig.entrySet())
			for (JsonElement strat : role.getValue().getAsJsonArray())
				counts.add(new RoleStrat(role.getKey(), strat.getAsString()));

		// Generate Players
		for (Multiset.Entry<RoleStrat> roleEnt : counts.entrySet()) {
			String role = roleEnt.getElement().role();
			String strat = roleEnt.getElement().strat();

			AgentFactory factory = new AgentFactory(fundamental, sip, markets,
					arrivalRate, new Rands(rand.nextLong()));
			for (int i = 0; i < roleEnt.getCount(); i++) {
				Agent agent = factory.createAgent(new AgentProperties(strat));
				agents.add(agent);
				Player player = new Player(role, strat, agent);
				players.add(player);
			}
		}
		return players.build();
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
	
	protected static class RoleStrat extends Pair<String, String> {
		protected RoleStrat(String role, String strat) {
			super(role, strat);
		}
	
		protected String role() { return left; }
		protected String strat() { return right; }
	}

}
