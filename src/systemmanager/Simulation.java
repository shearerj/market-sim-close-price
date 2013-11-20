package systemmanager;

import static logger.Logger.log;
import static logger.Logger.Level.INFO;

import java.util.Collection;
import java.util.Map.Entry;
import java.util.Random;

import utils.Pair;
import activity.AgentArrival;
import activity.Clear;

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
public class Simulation {

	protected final EventManager eventManager;
	protected final FundamentalValue fundamental;
	protected final SIP sip;
	protected final Collection<Market> markets;
	protected final Collection<Agent> agents;
	protected final Collection<Player> players;

	protected final SimulationSpec specification;
	protected final TimeStamp simulationLength;
	
	public Simulation(SimulationSpec spec, Random rand) {
		this.specification = spec;

		EntityProperties simProps = spec.getSimulationProps();
		Collection<MarketProperties> marketProps = spec.getMarketProps();
		Collection<AgentProperties> agentProps = spec.getAgentProps();
		JsonObject playerConfig = spec.getPlayerProps();

		simulationLength = new TimeStamp(simProps.getAsLong(Keys.SIMULATION_LENGTH));

		fundamental = new FundamentalValue(
				simProps.getAsDouble(Keys.FUNDAMENTAL_KAPPA),
				simProps.getAsInt(Keys.FUNDAMENTAL_MEAN),
				simProps.getAsDouble(Keys.FUNDAMENTAL_SHOCK_VAR),
				new Random(rand.nextLong()));
		sip = new SIP(new TimeStamp(simProps.getAsInt(Keys.NBBO_LATENCY)));
		markets = setupMarkets(marketProps, rand);
		agents = setupAgents(agentProps, rand);
		players = setupPlayers(simProps, playerConfig, rand);

		// XXX Log markets and their configuration?
		eventManager = new EventManager(new Random(rand.nextLong()));
		for (Market market : markets)
			eventManager.addActivity(new Clear(market, TimeStamp.IMMEDIATE));
		for (Agent agent : agents)
			eventManager.addActivity(new AgentArrival(agent, agent.getArrivalTime()));
	}
	
	protected Collection<Market> setupMarkets(Collection<MarketProperties> marketProps, Random rand) {
		Builder<Market> markets = ImmutableList.builder();
		Random ran = new Random(rand.nextLong());
		for (MarketProperties mktProps : marketProps) {
			MarketFactory factory = new MarketFactory(sip, ran);
			for (int i = 0; i < mktProps.getAsInt(Keys.NUM, 1); i++)
				markets.add(factory.createMarket(mktProps));
		}
		return markets.build();
	}
	
	protected Collection<Agent> setupAgents(Collection<AgentProperties> agentProps, Random rand) {
		// Not immutable because players also adds agents
		Collection<Agent> agents = Lists.newArrayList();
		for (AgentProperties agProps : agentProps) {
			int number = agProps.getAsInt(Keys.NUM, 0);
			double arrivalRate = agProps.getAsDouble(Keys.ARRIVAL_RATE, 0.075);
			
			// XXX In general the arrival process and market generation can be
			// generic, but for now we'll stick with the original implementation which is round
			// robin markets and poisson arrival
			AgentFactory factory = new AgentFactory(fundamental, sip, markets,
					arrivalRate, new Random(rand.nextLong()));

			for (int i = 0; i < number; i++)
				agents.add(factory.createAgent(agProps));
		}
		return agents;
	}
	
	protected Collection<Player> setupPlayers(EntityProperties modelProps,
			JsonObject playerConfig, Random rand) {
		Builder<Player> players = ImmutableList.builder();
		// First group by role and agentType for legacy reasons / arrival rate reasons
		// XXX Re-think how to schedule player arrival rates. (Maybe be less important if agent's
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
		eventManager.executeUntil(simulationLength);
		log(INFO, "[[[ Simulation Over ]]]");
	}
	
	public TimeStamp getCurrentTime() {
		return eventManager.currentTime;
	}
	
	public Observations getObservations() {
		return new Observations(specification, markets, agents, players,
				fundamental);
	}
	
	protected static class RoleStrat extends Pair<String, String> {
		protected RoleStrat(String role, String strat) {
			super(role, strat);
		}
	
		protected String role() { return left; }
		protected String strat() { return right; }
	}

}
