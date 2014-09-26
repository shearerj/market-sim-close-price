package systemmanager;

import static logger.Log.log;
import static logger.Log.Level.INFO;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import activity.AgentArrival;
import activity.Clear;
import activity.LiquidateAtFundamental;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;

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
 * This class represents a single simulation. Standard usage is:
 * <ol>
 * <li>Create Simulation Object</li>
 * <li>executeEvents</li>
 * <li>getObservations</li>
 * </ol>
 * 
 * @author erik
 * 
 */
public class Simulation {

	protected final Scheduler scheduler;
	protected final FundamentalValue fundamental;
	protected final SIP sip;
	protected final Collection<Market> markets;
	protected final Collection<Agent> agents;
	protected final Collection<Player> players;

	protected final SimulationSpec specification;
	protected final TimeStamp simulationLength;
	protected final Observations observations;
	
	/**
	 * Create the simulation with specified random seed and simulation spec
	 * file.
	 */
	public Simulation(SimulationSpec spec, Random rand) {
		this.specification = spec;

		EntityProperties simProps = spec.getSimulationProps();
		Collection<MarketProperties> marketProps = spec.getMarketProps();
		Collection<AgentProperties> agentProps = spec.getAgentProps();
		Map<String, Multiset<AgentProperties>> playerConfig = spec.getPlayerProps();

		this.simulationLength = TimeStamp.create(simProps.getAsLong(Keys.SIMULATION_LENGTH));
		
		this.fundamental = FundamentalValue.create(
				simProps.getAsDouble(Keys.FUNDAMENTAL_KAPPA),
				simProps.getAsInt(Keys.FUNDAMENTAL_MEAN),
				simProps.getAsDouble(Keys.FUNDAMENTAL_SHOCK_VAR),
				new Random(rand.nextLong()));
		this.scheduler = new Scheduler(new Random(rand.nextLong()));
		this.sip = new SIP(scheduler, TimeStamp.create(simProps.getAsInt(Keys.NBBO_LATENCY)));
		this.markets = setupMarkets(marketProps, rand);
		this.agents = setupAgents(agentProps, rand);
		this.players = setupPlayers(playerConfig, simProps, rand);
		this.observations = new Observations(specification, markets, agents,
				players, fundamental);

		for (Market market : markets)
			scheduler.executeActivity(new Clear(market));
		for (Agent agent : agents) {
			scheduler.scheduleActivity(agent.getArrivalTime(), new AgentArrival(agent));
		}
	}
	
	/**
	 * Sets up the markets
	 */
	protected Collection<Market> setupMarkets(Collection<MarketProperties> marketProps, Random rand) {
		Builder<Market> markets = ImmutableList.builder();
		Random ran = new Random(rand.nextLong());
		for (MarketProperties mktProps : marketProps) {
			MarketFactory factory = new MarketFactory(scheduler, sip, ran);
			for (int i = 0; i < mktProps.getAsInt(Keys.NUM, 1); i++)
				markets.add(factory.createMarket(mktProps));
		}
		return markets.build();
	}
	
	/**
	 * Sets up non player agents
	 */
	protected Collection<Agent> setupAgents(Collection<AgentProperties> agentProps, Random rand) {
		// Not immutable because players also adds agents
		Collection<Agent> agents = Lists.newArrayList();
		for (AgentProperties agProps : agentProps) {
			int number = agProps.getAsInt(Keys.NUM, 0);
			double arrivalRate = agProps.getAsDouble(Keys.ARRIVAL_RATE, 0.075);
			
			AgentFactory factory = new AgentFactory(scheduler, fundamental, sip, markets,
					arrivalRate, new Random(rand.nextLong()));

			for (int i = 0; i < number; i++)
				agents.add(factory.createAgent(agProps));
		}
		return agents;
	}
	
	/**
	 * Sets up player agents
	 */
	protected Collection<Player> setupPlayers(Map<String, Multiset<AgentProperties>> playerConfig,
			EntityProperties modelProps, Random rand) {
		Builder<Player> players = ImmutableList.builder();
		double arrivalRate = modelProps.getAsDouble(Keys.ARRIVAL_RATE, 0.075);

		// Generate Players
		for (Entry<String, Multiset<AgentProperties>> e : playerConfig.entrySet()) {
			String role = e.getKey();
			for (Multiset.Entry<AgentProperties> propCounts : e.getValue().entrySet()) {
				AgentProperties agProp = propCounts.getElement();
				AgentFactory factory = new AgentFactory(scheduler, fundamental, sip, markets,
						arrivalRate, new Random(rand.nextLong()));
				
				for (int i = 0; i < propCounts.getCount(); i++) {
					Agent agent = factory.createAgent(propCounts.getElement());
					agents.add(agent);
					Player player = new Player(role, agProp.getConfigString(), agent);
					players.add(player);
				}
			}
		}
		return players.build();
	}

	/**
	 * Method to execute all events in the Event Queue.
	 */
	public void executeEvents() {
		Observations.BUS.register(observations);
		scheduler.executeUntil(simulationLength.minus(TimeStamp.create(1)));
		for (Agent agent : agents) {
			scheduler.executeActivity(new LiquidateAtFundamental(agent));
		}
		log.log(INFO, "[[[ Simulation Over ]]]");
		Observations.BUS.unregister(observations);
	}
	
	public TimeStamp getCurrentTime() {
		return scheduler.currentTime;
	}
	
	/**
	 * Gets the observations, which are only useful at the end of the simulation.
	 */
	public Observations getObservations() {
		return observations;
	}

}
