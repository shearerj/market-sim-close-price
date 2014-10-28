package systemmanager;

import static logger.Log.Level.INFO;

import java.io.Writer;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Random;

import logger.Log;
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
import utils.Iterators2;

import com.google.common.collect.ImmutableCollection.Builder;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;

import data.FundamentalValue;
import data.FundamentalValue.FundamentalValueView;
import data.Player;
import data.Props;
import data.Stats;
import entity.agent.Agent;
import entity.agent.AgentFactory;
import entity.infoproc.SIP;
import entity.market.Market;
import entity.market.MarketFactory;
import entity.market.Price;
import event.Activity;
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
	protected final EventQueue eventQueue;
	protected final int simLength;
	protected final TimeStamp finalTime;
	
	// Simulation objects
	protected final FundamentalValue fundamental;
	protected final SIP sip;
	protected final Collection<Market> markets;
	protected final Collection<Agent> agents;
	protected final Collection<Player> players;

	// Bookkeeping
	protected final SimulationSpec specification;
	protected final Stats statistics;
	protected final Log log;
	
	// IDs
	public final Iterator<Integer> agentIds, marketIds;
	
	/**
	 * Create the simulation with specified random seed and simulation spec
	 * file.
	 */
	protected Simulation(SimulationSpec spec, Random rand, Writer logWriter, Level logLevel) {
		this.specification = spec;
		this.eventQueue = EventQueue.create(this, new Random(rand.nextLong()));
		this.log = Log.create(logLevel, logWriter, eventQueue);
		this.statistics = Stats.create();
		this.agentIds = Iterators2.counter();
		this.marketIds = Iterators2.counter();

		// FIXME Change to be static, so it's clear what needs to be initialized...
		Props simProps = spec.getSimulationProps();
		
		this.simLength = simProps.get(SimLength.class);
		this.finalTime = TimeStamp.of(simLength - 1); // because 0 is a valid timestamp
		
		this.fundamental = FundamentalValue.create(
				this,
				simProps.get(FundamentalKappa.class),
				simProps.get(FundamentalMean.class),
				simProps.get(FundamentalShockVar.class),
				new Random(rand.nextLong()));
		
		Builder<Agent> agentBuilder = ImmutableList.builder();
		this.markets = createMarkets(this, spec.getMarketProps(), rand);
		this.players = createPlayers(this, markets, agentBuilder, spec.getPlayerProps(), rand);
		this.agents = createAgents(this, markets, agentBuilder, spec.getAgentProps(), rand);
		
		this.sip = SIP.create(this, simProps.get(NbboLatency.class), markets);
	}
	
	public static Simulation create(SimulationSpec spec, Random rand, Writer logWriter, Level logLevel) {
		return new Simulation(spec, rand, logWriter, logLevel);
	}
	
	private static Collection<Market> createMarkets(Simulation sim, Multimap<MarketType, Props> marketProps, Random rand) {
		Builder<Market> markets = ImmutableList.builder();
		for (Entry<MarketType, Props> e : marketProps.entries()) {
			MarketFactory factory = MarketFactory.create(sim, new Random(rand.nextLong()));
			for (int i = 0; i < e.getValue().get(NumMarkets.class, Num.class); i++)
				markets.add(factory.createMarket(e.getKey(), e.getValue()));
		}
		return markets.build();
	}
	
	// Requires that any other agents already created in the player stage
	private static Collection<Agent> createAgents(Simulation sim, Collection<Market> markets, Builder<Agent> agents, Multimap<AgentType, Props> agentProps, Random rand) {
		for (Entry<AgentType, Props> e : agentProps.entries()) {
			int number = e.getValue().get(NumAgents.class, Num.class);
			AgentFactory factory = AgentFactory.create(sim, markets, new Random(rand.nextLong()));
			for (int i = 0; i < number; i++)
				agents.add(factory.createAgent(e.getKey(), e.getValue()));
		}
		return agents.build();
	}
	
	private static Collection<Player> createPlayers(Simulation sim, Collection<Market> markets, Builder<Agent> agentBuilder, Multiset<PlayerSpec> playerConfig, Random rand) {
		Builder<Player> players = ImmutableList.builder();

		for (Multiset.Entry<PlayerSpec> e : playerConfig.entrySet()) {
			Props agentProperties = e.getElement().agentProps;
			AgentFactory factory = AgentFactory.create(sim, markets, new Random(rand.nextLong()));
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
		for (Agent agent : agents)
			agent.liquidateAtPrice(finalFundamental);
		postStat(Stats.FUNDAMENTAL_END_PRICE, finalFundamental.doubleValue());
		log(INFO, "[[[ Simulation Over ]]]");
	}
	
	public void scheduleActivityIn(TimeStamp delay, Activity act) {
		eventQueue.scheduleActivity(getCurrentTime().plus(delay), act);
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
	
	public void postStat(String name, double value) {
		statistics.post(name, value);
	}
	
	public void postStat(String name, double value, long times) {
		statistics.post(name, value, times);
	}
	
	public void postTimedStat(String name, double value) {
		statistics.postTimed(eventQueue.getCurrentTime(), name, value);
	}
	
	public void postTimedStat(TimeStamp time, String name, double value) {
		statistics.postTimed(time, name, value);
	}
	
	public FundamentalValueView getFundamentalView(TimeStamp latency) {
		return fundamental.getView(latency);
	}
	
	public SIP getSIP() {
		return sip;
	}

	public int nextMarketId() {
		return marketIds.next();
	}

	public int nextAgentId() {
		return agentIds.next();
	}

}
