package model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import activity.AgentArrival;
import activity.Clear;

import entity.SIP;
import market.Bid;
import market.Price;
import market.Transaction;
import systemmanager.Consts;
import systemmanager.Consts.SMAgentType;
import systemmanager.Consts.MarketType;
import systemmanager.SimulationSpec;
import utils.RandPlus;
import data.AgentProperties;
import data.AgentPropsPair;
import data.FundamentalValue;
import data.EntityProperties;
import data.Player;
import data.SystemData;
import data.TimeSeries;
import entity.Agent;
import entity.LAAgent;
import entity.LAInformationProcessor;
import entity.Market;
import entity.SMAgentFactory;
import event.EventManager;
import event.TimeStamp;
import generators.Generator;
import generators.IDGenerator;

/**
 * MARKETMODEL
 * 
 * Base class for specifying a market model (e.g. two-market model, centralized
 * call market, etc.).
 * 
 * Multiple types of market models can be included in a single simulation trial.
 * Agents present in the simulation will reside in a primary market model (for
 * payoff output purposes in the observation files), but agents behave
 * independently within each model.
 * 
 * If the market model has only one market, then it is assumed to be a
 * "centralized" model. Note that not only can market properties be set here,
 * but also the assignments of agents to markets. The number of agents of each
 * type is fixed globally, but all or a subset of these agents can be assigned
 * to be in any of the available markets (single-market agents). If not
 * specified, all background agents are assumed to trade in all available
 * markets in the model.
 * 
 * Note:
 * 
 * Configuration: - Each model may have various configurations, e.g. specifying
 * the players allowed in that instance of the model. - Each configuration is a
 * string, and they are separated by commas.
 * 
 * For example, in the spec file:
 * 
 * "MARKETMODEL": "A,B"
 * 
 * would indicate that for the given model, there is one instance of
 * configuration A and one instance of configuration B. The system, in this
 * case, would also determine that it needs to create two instances of this
 * model.
 * 
 * @author ewah
 */
public abstract class MarketModel {

	// -- begin reorg --

	protected final int modelID;
	protected final FundamentalValue fundamental;
	protected final Collection<Market> markets;
	protected final Collection<Transaction> trans;
	protected final Collection<Agent> agents;
	protected final Collection<Player> players;

	protected Map<Double, Double> modelSurplus; // hashed by rho value
	protected TimeSeries NBBOSpreads; // NBBO bid/ask spread values

	protected final RandPlus rand;
	//protected int nextAgentID; not sure if we need this
	
	public SIP sip;
	protected final Generator<Integer> agentIDgen;
	protected final Generator<Integer> ipIDgen;

	// -- end reorg --

	protected String config; // TODO Does this need to be saved? or just used at
								// construction?
	protected SystemData data;
	protected ArrayList<Integer> agentIDs; // IDs of associated agents
	protected ArrayList<Integer> ipIDs; // IDs of associated ips
	protected EntityProperties modelProperties;
	protected ArrayList<AgentPropsPair> agentConfig;
	protected ArrayList<MarketObjectPair> modelMarketConfig;
	protected Collection<TimeStamp> latencies;

	// Store information on market IDs for each market specified in
	// modelProperties
	// protected ArrayList<Integer> marketIDs;

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
		this.modelSurplus = new HashMap<Double, Double>();
		this.agentIDgen = new IDGenerator();
		this.ipIDgen = new IDGenerator();
		this.NBBOSpreads = new TimeSeries();

		// FIXME actually initialize SIP -- why is this tick size? should be latency!!!
		this.sip = new SIP(getipIDgen(), modelID, new TimeStamp(100) /* tick size */);

		// Setup
		setupMarkets(modelProps);
		setupAgents(modelProps, agentProps);
		setupPlayers(modelProps, playerConfig);
	}
	
	public int getipIDgen() {
		return this.ipIDgen.next();
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
	
	/**
	 * @return SIP
	 */
	public SIP getSip() {
		return this.sip;
	}

	public void scheduleActivities(EventManager manager) {
		// TODO schedule sendToSIP
		for (Market market : markets)
			manager.addActivity(new Clear(market, Consts.START_TIME));
		for (Agent agent : agents)
			manager.addActivity(new AgentArrival(agent, agent.getArrivalTime()));
	}

	// TODO remove
	//public void addAgent(Agent agent) {
		//agents.add(agent);
	//}
	
	/**
	 * @return configuration string for this model.
	 */
	public abstract String getConfig();

	/**
	 * Format "MODELTYPE-CONFIG" unless config string is empty, then "MODELTYPE"
	 * If configuration string has a colon, i.e. CONFIG:PARAMS, then only
	 * include the CONFIG portion.
	 * 
	 * @return model name
	 */
	public String getFullName() {
		String configStr = this.getConfig();
		if (!this.getConfig().equals(""))
			configStr = "-" + configStr;
		String[] configs = configStr.split("[:]+");
		return this.getClass().getSimpleName().toUpperCase() + configs[0];
	}

	/**
	 * @return model name for observation file (format "modeltypeconfig")
	 */
	public String getLogName() {
		// return this.getClass().getSimpleName().toLower Case() +
		// this.getConfig().toLowerCase();
		return getFullName().toLowerCase().replace("-", "");
	}

	/**
	 * Adds an agent to the list of agents for the model.
	 * 
	 * @param id
	 */
	public void linkAgent(int id) {
		if (!agentIDs.contains(id))
			agentIDs.add(id);
	}

	/**
	 * Add an agent-property pair to the MarketModel.
	 * 
	 * @param agType
	 * @param agProperties
	 */
	public void addAgentPropertyPair(SMAgentType agType,
			EntityProperties agProperties) {
		AgentPropsPair app = new AgentPropsPair(agType, agProperties);
		agentConfig.add(app);
	}

	/**
	 * @return agentConfig
	 */
	public ArrayList<AgentPropsPair> getAgentConfig() {
		return agentConfig;
	}

	/**
	 * @return number of (additional, non-environment) agents specified by
	 *         config
	 */
	public int getNumModelAgents() {
		return agentConfig.size();
	}

	/**
	 * Add a market-property pair to the MarketModel.
	 * 
	 * @param mktType
	 * @param mktProperties
	 */
	public void addMarketPropertyPair(MarketType mktType,
			EntityProperties mktProperties) {
		MarketObjectPair mpp = new MarketObjectPair(mktType.toString(),
				mktProperties);
		modelMarketConfig.add(mpp);
	}

	/**
	 * Edits the market-property pair for the market at the given index. Retains
	 * the market type.
	 * 
	 * @param idx
	 * @param mktProperties
	 */
	public void editMarketPropertyPair(int idx, EntityProperties mktProperties) {
		MarketObjectPair mpp = modelMarketConfig.get(idx);
		modelMarketConfig.set(idx, new MarketObjectPair(mpp.getMarketType(),
				mktProperties));
	}

	public Price getFundamentalAt(TimeStamp ts) {
		if (fundamental == null)
			// return new Price(0);
			throw new IllegalStateException("No Fundamental Value...");
		return fundamental.getValueAt(ts);
	}

	/**
	 * @return modelMarketConfig
	 */
	public ArrayList<MarketObjectPair> getMarketConfig() {
		return modelMarketConfig;
	}

	/**
	 * @return number of markets in the model
	 */
	public int getNumMarkets() {
		return markets.size();
	}

	/**
	 * @return modelID
	 */
	public int getID() {
		return modelID;
	}

	public Collection<Transaction> getTrans() {
		return trans;
	}

	public void addTrans(Transaction tr) {
		this.trans.add(tr);
		this.addSurplus(tr);
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

	/**
	 * 
	 * @param rho
	 * @return Surplus for this model for the given value of rho
	 */
	public double getModelSurplus(double rho) {
		return this.modelSurplus.get(rho);
	}

	/**
	 * Update surplus for this and the agents involved in the transactions
	 * 
	 * @param tr
	 */
	public void addSurplus(Transaction tr) {
		int fund = this.getFundamentalAt(tr.getExecTime()).getPrice();
		for (double rho : Consts.rhos) {
			if (!this.modelSurplus.containsKey(rho))
				this.modelSurplus.put(rho, 0.0);
			// Updating buyer surplus
			Agent buyer = tr.getBuyer();
			if (buyer.getPrivateValue() != null) {
				double surplus = buyer.addSurplus(rho, fund, tr, true);
				this.modelSurplus.put(rho, this.modelSurplus.get(rho) + surplus);
			} else {
				double surplus = buyer.addSurplus(rho, fund, tr, true);
				this.modelSurplus.put(rho, this.modelSurplus.get(rho) + surplus);
			}
			// Updating seller surplus
			Agent seller = tr.getSeller();
			if (seller.getPrivateValue() != null) {
				double surplus = seller.addSurplus(rho, fund, tr, false);
				this.modelSurplus.put(rho, this.modelSurplus.get(rho) + surplus);
			} else {
				double surplus = seller.addSurplus(rho, fund, tr, false);
				this.modelSurplus.put(rho, this.modelSurplus.get(rho) + surplus);
			}
		}
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
		if (obj == null || !(obj instanceof MarketModel))
			return false;
		MarketModel mm = (MarketModel) obj;
		return modelID == mm.modelID;
	}

	@Override
	public String toString() {
		return new String("{" + modelID + "}");
	}
}
