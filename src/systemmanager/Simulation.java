package systemmanager;

import static logger.Logger.log;
import static logger.Logger.Level.INFO;

import java.util.Collection;
import java.util.Map.Entry;
import java.util.Random;

import utils.Pair;
import activity.AgentArrival;
import activity.Clear;
import activity.LiquidateAtFundamental;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;
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
import entity.agent.HFTAgent;
import entity.agent.MarketMaker;
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
		JsonObject playerConfig = spec.getPlayerProps();

		this.simulationLength = new TimeStamp(simProps.getAsLong(Keys.SIMULATION_LENGTH));
		TimeStamp lastTime = new TimeStamp(simulationLength.getInTicks() - 1); // Last time to schedule something.

		this.fundamental = new FundamentalValue(
				simProps.getAsDouble(Keys.FUNDAMENTAL_KAPPA),
				simProps.getAsInt(Keys.FUNDAMENTAL_MEAN),
				simProps.getAsDouble(Keys.FUNDAMENTAL_SHOCK_VAR),
				new Random(rand.nextLong()));
		this.scheduler = new Scheduler(new Random(rand.nextLong()));
		this.sip = new SIP(scheduler, new TimeStamp(simProps.getAsInt(Keys.NBBO_LATENCY)));
		this.markets = setupMarkets(marketProps, rand);
		this.agents = setupAgents(agentProps, rand);
		this.players = setupPlayers(simProps, playerConfig, rand);
		this.observations = new Observations(specification, markets, agents,
				players, fundamental);

		// XXX Log markets and their configuration?
		for (Market market : markets)
			scheduler.executeActivity(new Clear(market));
		for (Agent agent : agents) {
			scheduler.scheduleActivity(agent.getArrivalTime(), new AgentArrival(agent));
			if ((agent instanceof MarketMaker) || (agent instanceof HFTAgent))
				scheduler.scheduleActivity(lastTime, new LiquidateAtFundamental(agent));
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
	protected Collection<Player> setupPlayers(EntityProperties modelProps,
			JsonObject playerConfig, Random rand) {
		Builder<Player> players = ImmutableList.builder();
		/*
		 * First group by role and agentType for legacy reasons / arrival rate
		 * reasons XXX Re-think how to schedule player arrival rates. (Maybe be
		 * less important if agent's reenter)
		 */
		double arrivalRate = modelProps.getAsDouble(Keys.ARRIVAL_RATE, 0.075);
		
		Multiset<RoleStrat> counts = HashMultiset.create();
		for (Entry<String, JsonElement> role : playerConfig.entrySet())
			for (JsonElement strat : role.getValue().getAsJsonArray())
				counts.add(new RoleStrat(role.getKey(), strat.getAsString()));

		// Generate Players
		for (Multiset.Entry<RoleStrat> roleEnt : counts.entrySet()) {
			String role = roleEnt.getElement().role();
			String strat = roleEnt.getElement().strat();

			AgentFactory factory = new AgentFactory(scheduler, fundamental, sip, markets,
					arrivalRate, new Random(rand.nextLong()));
			for (int i = 0; i < roleEnt.getCount(); i++) {
				Agent agent = factory.createAgent(new AgentProperties(strat));
				agents.add(agent);
				Player player = new Player(role, strat, agent);
				players.add(player);
			}
		}
		return players.build();
	}

	/**
	 * Method to execute all events in the Event Queue.
	 */
	public void executeEvents() {
		Observations.BUS.register(observations);
		scheduler.executeUntil(simulationLength);
		log(INFO, "[[[ Simulation Over ]]]");
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
	
	protected static class RoleStrat extends Pair<String, String> {
		protected RoleStrat(String role, String strat) {
			super(role, strat);
		}
	
		protected String role() { return left; }
		protected String strat() { return right; }
	}

}
