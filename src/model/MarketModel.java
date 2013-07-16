package model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import market.Bid;
import market.Price;
import market.Transaction;
import systemmanager.Consts;
import systemmanager.SimulationSpec;
import utils.RandPlus;
import activity.AgentArrival;
import activity.Clear;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import data.AgentProperties;
import data.EntityProperties;
import data.FundamentalValue;
import data.Player;
import data.TimeSeries;
import entity.Agent;
import entity.Market;
import entity.SIP;
import entity.SMAgentFactory;
import event.EventManager;
import event.TimeStamp;
import generators.Generator;
import generators.IDGenerator;

/**
 * MARKETMODEL
 * 
 * Base class for specifying a market model (e.g. two-market model, centralized call market, etc.).
 * 
 * Multiple types of market models can be included in a single simulation trial. Agents present in
 * the simulation will reside in a primary market model (for payoff output purposes in the
 * observation files), but agents behave independently within each model.
 * 
 * If the market model has only one market, then it is assumed to be a "centralized" model. Note
 * that not only can market properties be set here, but also the assignments of agents to markets.
 * The number of agents of each type is fixed globally, but all or a subset of these agents can be
 * assigned to be in any of the available markets (single-market agents). If not specified, all
 * background agents are assumed to trade in all available markets in the model.
 * 
 * Note:
 * 
 * Configuration: - Each model may have various configurations, e.g. specifying the players allowed
 * in that instance of the model. - Each configuration is a string, and they are separated by
 * commas.
 * 
 * For example, in the spec file:
 * 
 * "MARKETMODEL": "A,B"
 * 
 * would indicate that for the given model, there is one instance of configuration A and one
 * instance of configuration B. The system, in this case, would also determine that it needs to
 * create two instances of this model.
 * 
 * @author ewah
 */
public abstract class MarketModel {

	protected final int modelID;
	protected final RandPlus rand;
	protected final SIP sip;
	protected final FundamentalValue fundamental;
	protected final Collection<Market> markets;
	protected final Collection<Transaction> trans;
	protected final Collection<Agent> agents;
	protected final Collection<Player> players;
	protected final TimeSeries NBBOSpreads; // NBBO bid/ask spread values TODO Move to SIP
	protected final Generator<Integer> agentIDgen;
	protected final Generator<Integer> ipIDgen;

	public MarketModel(int modelID, FundamentalValue fundamental,
			Map<AgentProperties, Integer> agentProps,
			EntityProperties modelProps, JsonObject playerConfig, RandPlus rand) {

		this.modelID = modelID;
		this.rand = rand;
		this.fundamental = fundamental;
		// XXX Perhaps HashSets instead of ArrayLists?
		this.markets = new ArrayList<Market>();
		this.agents = new ArrayList<Agent>();
		this.players = new ArrayList<Player>();
		this.trans = new ArrayList<Transaction>();
		this.agentIDgen = new IDGenerator();
		this.ipIDgen = new IDGenerator();
		this.NBBOSpreads = new TimeSeries();
		this.sip = new SIP(nextIPID(), modelID, new TimeStamp(100));

		// Setup
		setupMarkets(modelProps);
		setupAgents(modelProps, agentProps);
		setupPlayers(modelProps, playerConfig);
	}

	protected abstract void setupMarkets(EntityProperties modelProps);

	protected void setupAgents(EntityProperties modelProps,
			Map<AgentProperties, Integer> agentProps) {
		for (Entry<AgentProperties, Integer> type : agentProps.entrySet()) {
			AgentProperties agProps = type.getKey();
			int number = type.getValue();

			// FIXME Replace 0 with default arrival rate
			//
			// In general the arrival process and market generation can be
			// generic or even specified, but for now we'll stick with the
			// original implementation
			SMAgentFactory factory = new SMAgentFactory(this, agentIDgen,
					agProps.getAsLong(SimulationSpec.ARRIVAL_RATE, 100),
					new RandPlus(rand.nextLong()));

			for (int i = 0; i < number; i++)
				agents.add(factory.createAgent(agProps));
		}
	}

	private void setupPlayers(EntityProperties modelProps,
			JsonObject playerConfig) {
		// First group by role and agentType for legacy reasons
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
				int count = stratEnt.getValue();

				// FIXME Arrival rate (100)?
				SMAgentFactory factory = new SMAgentFactory(this, agentIDgen,
						100, new RandPlus(rand.nextLong()));

				for (int i = 0; i < count; i++) {
					Agent agent = factory.createAgent(new AgentProperties(strat));
					agents.add(agent);
					Player player = new Player(role, strat, agent);
					players.add(player);
				}
			}
		}
	}

	public SIP getSIP() {
		return this.sip;
	}

	public void scheduleActivities(EventManager manager) {
		// XXX There is probably a better way to initialize the first market clear. This has a
		// problem if an agent strategy happens before the clear but at time 0. That shouldn't be
		// allowed.
		for (Market market : markets)
			manager.addActivity(new Clear(market, Consts.START_TIME));
		for (Agent agent : agents)
			manager.addActivity(new AgentArrival(agent, agent.getArrivalTime()));
	}

	public int nextIPID() {
		return ipIDgen.next();
	}

	public Price getFundamentalAt(TimeStamp ts) {
		if (fundamental == null)
		// return new Price(0);
			throw new IllegalStateException("No Fundamental Value...");
		return fundamental.getValueAt(ts);
	}

	public final String getName() {
		return getClass().getSimpleName();
	}
	
	public int getNumMarkets() {
		return markets.size();
	}

	public int getID() {
		return modelID;
	}

	public Collection<Transaction> getTrans() {
		return trans;
	}

	public void addTrans(Transaction tr) {
		trans.add(tr);
	}

	public ArrayList<Bid> getAllBids() {
		ArrayList<Bid> modelBids = new ArrayList<Bid>();
		for (Market mkt : markets) {
			modelBids.addAll(mkt.getAllBids());
		}
		return modelBids;
	}

	public Collection<Market> getMarkets() {
		return Collections.unmodifiableCollection(markets);
	}

	public Collection<Agent> getAgents() {
		return Collections.unmodifiableCollection(agents);
	}

	public Collection<Player> getPlayers() {
		return Collections.unmodifiableCollection(players);
	}

	public void addNBBOSpread(TimeStamp ts, int spread) {
		this.NBBOSpreads.add(ts, (double) spread);
	}

	public TimeSeries getNBBOSpreads() {
		return this.NBBOSpreads;
	}

	@Override
	public int hashCode() {
		return modelID;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof MarketModel)) return false;
		MarketModel mm = (MarketModel) obj;
		return modelID == mm.modelID;
	}

	@Override
	public String toString() {
		return new String("{" + modelID + "}");
	}
}
