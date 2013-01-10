package model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import entity.*;
import systemmanager.*;

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
 * specified, all background agents are assumed to be enter all available
 * markets in the model.
 * 
 * Note:
 * 
 * Configuration:
 *  - Each model may have various configurations, e.g. specifying the players 
 *    allowed in that instance of the model.
 *  - Each configuration is a string, and they are separated commas in the spec
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

	protected int modelID;
	protected String config;
	protected SystemData data;
	protected ArrayList<Integer> agentIDs;		// IDs of associated agents
	protected ArrayList<Integer> permittedAgentIDs;
	protected ObjectProperties modelProperties;
	
	// Specify market configuration & properties for the model
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
		this.modelID = modelID;
		data = d;
		modelMarketConfig = new ArrayList<MarketObjectPair>();
		marketIDs = new ArrayList<Integer>();
		modelProperties = p;
		agentIDs = new ArrayList<Integer>();
		permittedAgentIDs = new ArrayList<Integer>();
	}
	
	/**
	 * Executed after agents are created; sets which HFTAgents can submit bids to markets
	 * in this model.
	 *
	 * Note that all SMAgents are, by default, permitted in all markets.
	 */
	public abstract void setAgentPermissions();
	
	/**
	 * @return configuration string for this model.
	 */
	public abstract String getConfig();
	
	
	/**
	 * @return model name (format "MODELTYPE-CONFIG")
	 */
	public String getFullName() {
		return this.getClass().getSimpleName().toUpperCase() + "-" + config;
	}
	
	/**
	 * @param id
	 * @return true if agent with the given id is permitted in this model.
	 */
	public boolean checkAgentPermissions(int id) {
		return permittedAgentIDs.contains(id);
	}
	
	/**
	 * Add all SM agent IDs to the permissions list.
	 */
	public void permitAllSMAgents() {
		for (Iterator<Integer> it = data.getAgentIDs().iterator(); it.hasNext(); ) {
			Agent ag = data.getAgent(it.next());
			// Check if the agent is a single market agent
			if (Arrays.asList(Consts.SMAgentTypes).contains(ag.getType())) {
				permittedAgentIDs.add(ag.getID());
			}
		}
	}
	
	/**
	 * Adds an agent to the list of agents for the model.
	 * @param id
	 */
	public void linkAgent(int id) {
		if (!agentIDs.contains(id)) agentIDs.add(id);
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
	 * @return agentIDs
	 */
	public ArrayList<Integer> getPermittedAgentIDs() {
		return permittedAgentIDs;
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
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return new String("{" + getID() + "}");
	}
}
