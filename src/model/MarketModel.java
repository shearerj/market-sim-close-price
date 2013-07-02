package model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import market.Bid;
import market.Price;
import market.Transaction;
import systemmanager.Consts;
import systemmanager.Consts.AgentType;
import systemmanager.Consts.MarketType;
import utils.CollectionUtils;
import utils.RandPlus;
import data.AgentProperties;
import data.AgentPropsPair;
import data.FundamentalValue;
import data.ObjectProperties;
import data.SystemData;
import entity.Agent;
import entity.Market;
import entity.SMAgent;
import entity.SMAgentFactory;
import event.TimeStamp;
import generators.Generator;
import generators.PoissonArrivalGenerator;
import generators.RoundRobinGenerator;

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
	protected final FundamentalValue fundamentalGenerator;
	protected final Collection<Market> markets;
	protected final Collection<Transaction> trans;

	private final Collection<Agent> agents;

	protected HashMap<Double, Double> modelSurplus; // hashed by rho value

	protected final RandPlus rand;
	protected int nextAgentID;

	// -- end reorg --

	protected String config; // TODO Does this need to be saved? or just used at
								// construction?
	protected SystemData data;
	protected ArrayList<Integer> agentIDs; // IDs of associated agents
	protected ObjectProperties modelProperties;
	protected ArrayList<AgentPropsPair> agentConfig;
	protected ArrayList<MarketObjectPair> modelMarketConfig;

	// Store information on market IDs for each market specified in
	// modelProperties
	protected ArrayList<Integer> marketIDs;

	public MarketModel(int modelID, FundamentalValue fundamental,
			Map<AgentProperties, Integer> agentProps,
			ObjectProperties modelProps, RandPlus rand) {

		this.modelID = modelID;
		this.rand = rand;
		this.fundamentalGenerator = fundamental;
		// XXX Perhaps HashSets instead of ArrayLists?
		this.markets = new ArrayList<Market>();
		this.agents = new ArrayList<Agent>();
		this.trans = new ArrayList<Transaction>();
		this.nextAgentID = 1;

		// Setup
		setupMarkets(modelProps);
		setupBackgroundAgents(modelProps, agentProps);
		setupModelAgents(modelProps);
	}

	protected abstract void setupMarkets(ObjectProperties modelProps);

	protected abstract void setupModelAgents(ObjectProperties modelProps);

	protected void setupBackgroundAgents(ObjectProperties modelProps,
			Map<AgentProperties, Integer> agentProps) {
		for (Entry<AgentProperties, Integer> type : agentProps.entrySet()) {
			// FIXME Replace 0 with default arrival rate
			AgentProperties agProps = type.getKey();
			int number = type.getValue();

			// In general the arrival process and market generation can be
			// generic or even specified, but for now we'll stick with the
			// original implementation
			Generator<TimeStamp> arrivals = new PoissonArrivalGenerator(
					TimeStamp.startTime,
					modelProps.getAsLong("arrival-rate", 0), new RandPlus(
							rand.nextLong()));
			Generator<Market> marketRate = new RoundRobinGenerator<Market>(
					markets);

			SMAgentFactory factory = new SMAgentFactory(this, agProps,
					nextAgentID, number, arrivals, marketRate, new RandPlus(
							rand.nextLong()));

			for (SMAgent agent : CollectionUtils.toIterable(factory)) {
				agents.add(agent);
				// TODO schedule agent arrival
			}
			nextAgentID += number;
		}
	}
	
	// TODO change to protected. External things shouldn't be able to add agents
	public void addAgent(Agent agent) {
		agents.add(agent);
		nextAgentID++;
	}
	
	protected void addAgents(Collection<Agent> agents) {
		agents.addAll(agents);
		nextAgentID += agents.size();
	}

	public MarketModel(int modelID, ObjectProperties p, SystemData d) {
		// reorg
		fundamentalGenerator = d.getFundamenalValue();
		markets = new ArrayList<Market>();
		trans = new ArrayList<Transaction>();
		rand = new RandPlus();
		agents = new ArrayList<Agent>();
		// reorg

		this.modelID = modelID;
		data = d;
		modelProperties = p;

		agentIDs = new ArrayList<Integer>();
		marketIDs = new ArrayList<Integer>();
		modelMarketConfig = new ArrayList<MarketObjectPair>();
		agentConfig = new ArrayList<AgentPropsPair>();

	}

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
	public void addAgentPropertyPair(AgentType agType,
			ObjectProperties agProperties) {
		AgentPropsPair app = new AgentPropsPair(agType, agProperties);
		agentConfig.add(app);
	}

	/**
	 * Add an agent with default property settings to the MarketModel.
	 * 
	 * @param agType
	 */
	public void addAgentPropertyPair(AgentType agType) {
		ObjectProperties agProperties = Consts.getProperties(agType);
		AgentPropsPair mpp = new AgentPropsPair(agType, agProperties);
		agentConfig.add(mpp);
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
			ObjectProperties mktProperties) {
		MarketObjectPair mpp = new MarketObjectPair(mktType.toString(),
				mktProperties);
		modelMarketConfig.add(mpp);
	}

	/**
	 * Add a market with default property settings to the MarketModel.
	 * 
	 * @param mktType
	 */
	public void addMarketPropertyPair(MarketType mktType) {
		ObjectProperties mktProperties = Consts.getProperties(mktType);
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
	public void editMarketPropertyPair(int idx, ObjectProperties mktProperties) {
		MarketObjectPair mpp = modelMarketConfig.get(idx);
		modelMarketConfig.set(idx, new MarketObjectPair(mpp.getMarketType(),
				mktProperties));
	}

	public Price getFundamentalAt(TimeStamp ts) {
		if (fundamentalGenerator == null)
			// return new Price(0);
			throw new IllegalStateException("No Fundamental Value...");
		return fundamentalGenerator.getValueAt(ts);
	}

	/**
	 * @return modelMarketConfig
	 */
	public ArrayList<MarketObjectPair> getMarketConfig() {
		return modelMarketConfig;
	}

	/**
	 * @return agentIDs
	 */
	public ArrayList<Integer> getAgentIDs() {
		return agentIDs;
	}

	/**
	 * @return marketIDs
	 */
	public ArrayList<Integer> getMarketIDs() {
		return marketIDs;
	}

	/**
	 * @return number of markets in the model
	 */
	public int getNumMarkets() {
		return marketIDs.size();
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

	public TreeMap<Integer, TimeStamp> getExecutionTimes() {
		TreeMap<Integer, TimeStamp> executionTimes = new TreeMap<Integer, TimeStamp>();
		for (Market market : markets) {
			executionTimes.putAll(market.getExecutionTimes());
		}
		return executionTimes;
	}

	@Override
	public String toString() {
		return new String("{" + getID() + "}");
	}

	public Collection<Agent> getAgents() {
		return Collections.unmodifiableCollection(agents);
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
		int fund = this.getFundamentalAt(tr.getTimestamp()).getPrice();
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
}
