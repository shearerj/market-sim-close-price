package systemmanager;

import static logger.Log.Level.INFO;

import java.io.Writer;
import java.util.Collection;
import java.util.Map.Entry;
import java.util.Random;

import logger.Log;
import logger.Log.Clock;
import logger.Log.Level;
import systemmanager.Consts.AgentType;
import systemmanager.Consts.MarketType;
import systemmanager.Keys.FundamentalKappa;
import systemmanager.Keys.FundamentalMean;
import systemmanager.Keys.FundamentalShockVar;
import systemmanager.Keys.NbboLatency;
import systemmanager.Keys.Num;
import systemmanager.Keys.NumAgents;
import systemmanager.Keys.NumMarkets;
import systemmanager.Keys.SimLength;
import systemmanager.SimulationSpec.PlayerSpec;

import com.google.common.collect.ImmutableCollection.Builder;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;

import data.FundamentalValue;
import data.Player;
import data.Props;
import data.Stats;
import entity.agent.Agent;
import entity.agent.AgentFactory;
import entity.market.Market;
import entity.market.MarketFactory;
import entity.market.Price;
import entity.sip.SIP;
import event.EventQueue;
import event.TimeStamp;

/**
 * This class represents a single simulation. Standard usage is:
 * <ol>
 * <li>Create Simulation Object</li>
 * <li>executeEvents</li>
 * <li>getObservations</li>
 * </ol>
 * 
 * XXX partial Singleton pattern
 * 
 * @author erik
 * 
 */
public class Simulation {

	// Timing information
	private final EventQueue eventQueue;
	private final TimeStamp finalTime;
	
	// Simulation objects
	private final FundamentalValue fundamental;
	private final Collection<Player> players;
	private final Collection<Agent> agents;

	// Bookkeeping
	private final Stats stats;
	protected final Log log;
	
	/**
	 * Create the simulation with specified random seed and simulation spec
	 * file.
	 */
	protected Simulation(SimulationSpec spec, Random rand, Writer logWriter, Level logLevel) {
		this.log = Log.create(logLevel, logWriter, new Clock() {
			@Override public int getTimePadding() { return 6; }
			@Override public long getTime() { return eventQueue.getCurrentTime().getInTicks(); }
		});
		this.eventQueue = EventQueue.create(log, new Random(rand.nextLong()));
		
		this.stats = Stats.create();

		Props simProps = spec.getSimulationProps();
		this.finalTime = TimeStamp.of(simProps.get(SimLength.class) - 1);
		
		this.fundamental = FundamentalValue.create(stats, eventQueue,
				simProps.get(FundamentalKappa.class),
				simProps.get(FundamentalMean.class),
				simProps.get(FundamentalShockVar.class),
				new Random(rand.nextLong()));
		
		SIP sip = SIP.create(stats, eventQueue, log, new Random(rand.nextLong()), simProps.get(NbboLatency.class));
		
		MarketFactory marketFactory = MarketFactory.create(stats, eventQueue, log, new Random(rand.nextLong()), sip);
		Collection<Market> markets = createMarkets(marketFactory, spec.getMarketProps(), rand);
		
		Builder<Agent> agentBuilder = ImmutableList.builder();
		AgentFactory agentFactory = AgentFactory.create(stats, eventQueue, log, new Random(rand.nextLong()), sip, fundamental, markets);
		this.players = createPlayers(agentFactory, agentBuilder, spec.getPlayerProps(), rand);
		this.agents = createAgents(agentFactory, agentBuilder, spec.getAgentProps(), rand);
	}
	
	public static Simulation create(SimulationSpec spec, Random rand, Writer logWriter, Level logLevel) {
		return new Simulation(spec, rand, logWriter, logLevel);
	}
	
	private static Collection<Market> createMarkets(MarketFactory factory, Multimap<MarketType, Props> marketProps, Random rand) {
		Builder<Market> markets = ImmutableList.builder();
		for (Entry<MarketType, Props> e : marketProps.entries()) {
			for (int i = 0; i < e.getValue().get(NumMarkets.class, Num.class); i++)
				markets.add(factory.createMarket(e.getKey(), e.getValue()));
		}
		return markets.build();
	}
	
	// Requires that any other agents already created in the player stage
	private static Collection<Agent> createAgents(AgentFactory factory, Builder<Agent> agents, Multimap<AgentType, Props> agentProps, Random rand) {
		for (Entry<AgentType, Props> e : agentProps.entries()) {
			int number = e.getValue().get(NumAgents.class, Num.class);
			for (int i = 0; i < number; i++)
				agents.add(factory.createAgent(e.getKey(), e.getValue()));
		}
		return agents.build();
	}
	
	private static Collection<Player> createPlayers(AgentFactory factory, Builder<Agent> agentBuilder, Multiset<PlayerSpec> playerConfig, Random rand) {
		Builder<Player> players = ImmutableList.builder();

		for (Multiset.Entry<PlayerSpec> e : playerConfig.entrySet()) {
			Props agentProperties = e.getElement().agentProps;
			for (int i = 0; i < e.getCount(); i++) {
				Agent agent = factory.createAgent(e.getElement().type, agentProperties);
				agentBuilder.add(agent);
				Player player = new Player(e.getElement().descriptor, agent);
				players.add(player);
			}
		}
		return players.build();
	}
	
	/**
	 * Method to execute all events in the Event Queue.
	 */
	protected void executeEvents() {
		eventQueue.executeUntil(finalTime);
		Price finalFundamental = fundamental.getValueAt(getCurrentTime());
		eventQueue.propogateInformation();
		for (Agent agent : agents)
			agent.liquidateAtPrice(finalFundamental);
		stats.post(Stats.FUNDAMENTAL_END_PRICE, finalFundamental.doubleValue());
		log.log(INFO, "[[[ Simulation Over ]]]");
		log.flush();
	}
	
	public TimeStamp getCurrentTime() {
		return eventQueue.getCurrentTime();
	}
	
	public void log(Level level, String format, Object... parameters) {
		log.log(level, format, parameters);
	}
	
	public void flushLog() {
		log.flush();
	}
	
	Stats getStatistics() {
		return stats;
	}
	
	Collection<Player> getPlayers() {
		return players;
	}
	
}
