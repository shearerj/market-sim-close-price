package model;

import systemmanager.*;

import java.util.ArrayList;

import market.Price;
import market.Transaction;

import data.AgentPropsPair;
import data.FundamentalValue;
import data.ObjectProperties;
import data.SystemData;
import entity.Market;
import event.TimeStamp;

/**
 * MARKETMODEL
 * 
 * Base class for specifying a market model (e.g. two-market model, 
 * centralized call market, etc.).
 * 
 * Multiple types of market models can be included in a single simulation
 * trial. Agents present in the simulation will reside in a primary market 
 * model (for payoff output purposes in the observation files), but agents
 * behave independently within each model.
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
 * Configuration:
 *  - Each model may have various configurations, e.g. specifying the players 
 *    allowed in that instance of the model.
 *  - Each configuration is a string, and they are separated by commas.
 *  
 *  For example, in the spec file:
 *  
 *  	"MARKETMODEL": "A,B"
 *  
 *  would indicate that for the given model, there is one instance of 
 *  configuration A and one instance of configuration B. The system, in this 
 *  case, would also determine that it needs to create two instances of this 
 *  model.
 * 
 * @author ewah
 */
public abstract class MarketModel {
	
	// -- begin reorg --
	
	protected FundamentalValue fundamentalGenerator;
	protected ArrayList<Market> markets;
	protected ArrayList<Transaction> trans;
	
	// -- end reorg --

	protected final int modelID;
	protected String config; // TODO Does this need to be saved? or just used at construction?
	protected SystemData data;
	protected ArrayList<Integer> agentIDs;		// IDs of associated agents
	protected ObjectProperties modelProperties;
	protected ArrayList<AgentPropsPair> agentConfig;
	protected ArrayList<MarketObjectPair> modelMarketConfig;
	
	// Store information on market IDs for each market specified in modelProperties
	protected ArrayList<Integer> marketIDs;
	
	
	/**
	 * Constructor
	 * 
	 * @param modelID
	 * @param p
	 * @param d
	 */
	public MarketModel(int modelID, ObjectProperties p, SystemData d) {
		// reorg
		fundamentalGenerator = d.getFundamenalValue();
		markets = new ArrayList<Market>();
		trans = new ArrayList<Transaction>();
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
	 * If configuration string has a colon, i.e. CONFIG:PARAMS, then only include
	 * the CONFIG portion.
	 * 
	 * @return model name
	 */
	public String getFullName() {
		String configStr = this.getConfig();
		if (!this.getConfig().equals("")) configStr = "-" + configStr;
		String [] configs = configStr.split("[:]+");
		return this.getClass().getSimpleName().toUpperCase() + configs[0];
	}
	
	/**
	 * @return model name for observation file (format "modeltypeconfig")
	 */
	public String getLogName() {
		//return this.getClass().getSimpleName().toLower Case() + this.getConfig().toLowerCase();
		return getFullName().toLowerCase().replace("-", "");
	}
	
	/**
	 * Adds an agent to the list of agents for the model.
	 * @param id
	 */
	public void linkAgent(int id) {
		if (!agentIDs.contains(id)) agentIDs.add(id);
	}
	
	/**
	 * Add an agent-property pair to the MarketModel.
	 * 
	 * @param agType
	 * @param agProperties
	 */
	public void addAgentPropertyPair(String agType, ObjectProperties agProperties) {
		AgentPropsPair app = new AgentPropsPair(agType, agProperties);
		agentConfig.add(app);
	}
	
	/**
	 * Add an agent with default property settings to the MarketModel.
	 * 
	 * @param agType
	 */
	public void addAgentPropertyPair(String agType) {
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
	 * @return number of (additional, non-environment) agents specified by config
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
	public void addMarketPropertyPair(String mktType, ObjectProperties mktProperties) {
		MarketObjectPair mpp = new MarketObjectPair(mktType, mktProperties);
		modelMarketConfig.add(mpp);
	}
	
	/**
	 * Add a market with default property settings to the MarketModel.
	 * 
	 * @param mktType
	 */
	public void addMarketPropertyPair(String mktType) {
		ObjectProperties mktProperties = Consts.getProperties(mktType);
		MarketObjectPair mpp = new MarketObjectPair(mktType, mktProperties);
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
		modelMarketConfig.set(idx, new MarketObjectPair(mpp.getMarketType(), mktProperties));
	}
	
	public Price getFundamentalAt(TimeStamp ts) {
		if (fundamentalGenerator == null)
			// return new Price(0);
			throw new IllegalStateException("No Fundamental Value...");
		return fundamentalGenerator.getValueAt((int) ts.longValue());
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
	
	public ArrayList<Transaction> getTrans() {
		return trans;
	}

	public void addTrans(Transaction tr) {
		this.trans.add(tr);
	}

	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return new String("{" + getID() + "}");
	}
}
