package data;

import event.*;
import entity.*;
import market.*;
import model.*;
import systemmanager.*;
import systemmanager.Consts.AgentType;
import systemmanager.Consts.ModelType;

import java.text.DecimalFormat;
import java.util.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;

/**
 * SYSTEMDATA
 * 
 * This class stores all simulation data (agents, markets, quotes, bid, etc.) for
 * access by other classes/objects. It also stores the parameters read from the 
 * environment configuration file.
 * 
 * In addition, the SystemData class stores observation data to be written to the
 * output file, e.g. it computes the surplus for all agents and aggregates other 
 * features for observations.
 * 
 * The primary market model is the one whose payoff is stored in the observation
 * file.
 *
 * Note: For the lists containing time series, a new value is added to each list
 * when the value has a chance to change (generally after a clear event in a market).
 * This applies to the execution speed and market depth/spread. For the NBBO spread,
 * a new value is added whenever the NBBO is updated (even if it is a repeated value).
 * 
 * @author ewah
 */
public class SystemData {

	public int simNum;						// observation number
	public File simDir;						// simulations directory
	public boolean EGTA;					// true if EGTA use case
	
	// Model information
	public MarketModel primaryModel;
	public String primaryModelDesc;						// description of primary model
	public HashMap<Integer,MarketModel> models;			// models hashed by ID
	public HashMap<Integer,Integer> marketIDModelIDMap;	// hashed by market ID
	public HashMap<Integer,List<Integer>> modelTransID; // hashed by model ID	
	
	// Market information
	public HashMap<Integer,Price> privateValues;		// private values hashed by bid ID
	public HashMap<Integer,Quote> quotes;				// hashed by market ID
	public HashMap<Integer,Agent> agents;				// (all) agents hashed by ID
	public HashMap<Integer,Agent> players;				// players (for EGTA)
	public HashMap<Integer,Market> markets;				// markets hashed by ID
	public List<Integer> modelIDs;

	// Agent information
	public int numEnvAgents;
	public int numPlayers;
	public HashMap<AgentPropsPair, Integer> envAgentMap;
	public HashMap<AgentPropsPair, Integer> playerMap;
	// hashed by model ID
	public HashMap<Integer,List<AgentPropsPair>> modelAgentMap;
	
	private FundamentalValue fundamentalGenerator;
	
	// hashed by type, gives # of that type
	public HashMap<ModelType,Integer> numModelType;
	
	// Parameters set by specification file
	public TimeStamp simLength;
	public int tickSize;
	public TimeStamp nbboLatency;
	public TimeStamp hftLatency;
	public TimeStamp smLatency;
	public double arrivalRate;
	public double reentryRate;
	public int meanValue;
	public double kappa;
	public double shockVar;
	public double pvVar;	// agent variance from PV random process
		
	/**
	 * Constructor
	 */
	public SystemData(int simNum, File simDir) {
		this.simNum = simNum;
		this.simDir = simDir;
		
		privateValues = new HashMap<Integer,Price>();
		quotes = new HashMap<Integer,Quote>();
		agents = new HashMap<Integer,Agent>();
		players = new HashMap<Integer,Agent>();
		markets = new HashMap<Integer,Market>();
		models = new HashMap<Integer,MarketModel>();
		numModelType = new HashMap<ModelType,Integer>();
		modelIDs = new ArrayList<Integer>();
		primaryModel = null;
		marketIDModelIDMap = new HashMap<Integer,Integer>();
		modelTransID = new HashMap<Integer, List<Integer>>();
		
		modelAgentMap = new HashMap<Integer, List<AgentPropsPair>>();
		playerMap = new HashMap<AgentPropsPair, Integer>();
		envAgentMap = new HashMap<AgentPropsPair, Integer>(); 
	
		// Initialize containers for observations/features
	}
	
	
	/***********************************
	 * Accessor methods
	 *
	 **********************************/
	
	/**
	 * @return true if EGTA use case
	 */
	public boolean isEGTAUseCase() {
		return EGTA;
	}
	
	/**
	 * @return
	 */
	public MarketModel getPrimaryModel() {
		return primaryModel;
	}
	
	/**
	 * @return marketIDs of primary model
	 */
//	public ArrayList<Integer> getPrimaryMarketIDs() {
//		return primaryModel.getMarketIDs();
//	}
	
	/**
	 * @return agentIDs of primary model
	 */
//	public ArrayList<Integer> getPrimaryAgentIDs() {
//		return primaryModel.getAgentIDs();
//	}
	
	/**
	 * @param marketID
	 * @return
	 */
	public Map<Integer,Bid> getBids(int marketID) {
		return null; //markets.get(marketID).getBids();
	}
	
	public HashMap<Integer,Quote> getQuotes() {
		return quotes;
	}

	/**
	 * Get quote for a given market.
	 * @param mktID
	 * @return
	 */
	public Quote getQuote(int mktID) {
		return quotes.get(mktID);
	}
	
	public HashMap<Integer,Agent> getAgents() {
		return agents;
	}

	public ArrayList<Integer> getAgentIDs() {
		return new ArrayList<Integer>(agents.keySet());
	}
	
	public ArrayList<Integer> getPlayerIDs() {
		return new ArrayList<Integer>(players.keySet());
	}
	
	public HashMap<Integer,Market> getMarkets() {
		return markets;
	}
	
	/**
	 * Given array of market IDs, returns ArrayList of associated Markets.
	 * @param IDs
	 * @return
	 */
	public ArrayList<Market> getMarketsByIDs(int[] IDs) {
		ArrayList<Market> mkts = new ArrayList<Market>();
		for (int i = 0; i < IDs.length; i++) {
			if (markets.keySet().contains(IDs[i])) {
				mkts.add(markets.get(IDs[i]));
			}
		}
		return mkts;
	}
	
	public ArrayList<Integer> getMarketIDs() {
		return new ArrayList<Integer>(markets.keySet());
	}
	
	public Agent getAgent(int id) {
		return agents.get(id);
	}
	
	public Market getMarket(int id) {
		return markets.get(id);
	}
	
	public HashMap<Integer,MarketModel> getModels() {
		return models;
	}
	
	public ArrayList<Integer> getModelIDs() {
		return new ArrayList<Integer>(models.keySet());
	}

	public MarketModel getModel(int id) {
		return models.get(id);
	}
	
	public MarketModel getModelByMarketID(int mktID) {
		return models.get(marketIDModelIDMap.get(mktID));
	}
	
	public int getModelIDByMarketID(int mktID) {
		return getModelByMarketID(mktID).getID();
	}
	
	/**
	 * Returns configuration string for the given model (specified by ID).
	 * @param id
	 * @return
	 */
	public String getModelConfig(int id) {
		return models.get(id).getConfig();
	}
	
	/**
	 * @return number of environment agents
	 */
	public int getNumEnvAgents() {
		return numEnvAgents;
	}
	
	/**
	 * @return number of players
	 */
	public int getNumPlayers() {
		return numPlayers;
	}	
	
	/**
	 * @return playerMap
	 */
	public HashMap<AgentPropsPair, Integer> getPlayerMap() {
		return playerMap;
	}
	
	/**
	 * @param modelID
	 * @return list of AgentPropertiesPairs for model agents within the specified model
	 */
	public List<AgentPropsPair> getModelAgentByModel(int modelID) {
		return modelAgentMap.get(modelID);
	}
	
	
	/**
	 * Iterates through all models. Forms HashMap hashed by unique AgentPropertiesPairs
	 * and with the number of each type, across all models.
	 * 
	 * @return HashMap hashed by AgentPropertiesPair
	 */
	public HashMap<AgentPropsPair, Integer> getModelAgentMap() {
		HashMap<AgentPropsPair,Integer> map = new HashMap<AgentPropsPair,Integer>();
		
		for (List<AgentPropsPair> list : modelAgentMap.values()) {		
			for (AgentPropsPair app : list) {
				if (map.containsKey(app)) {
					int n = map.get(app);
					map.put(app, ++n);
				} else {
					map.put(app, 1);
				}
			}
		}
		return map;
	}
	
	/**
	 * @return envAgentMap
	 */
	public HashMap<AgentPropsPair, Integer> getEnvAgentMap() {
		return envAgentMap;
	}
	
	/**
	 * Returns true if agent with the specified ID is  not a player.
	 * 
	 * @param id
	 * @return
	 */
	public boolean isEnvironmentAgent(int id) {
		return !(players.containsKey(id));
	}
	
	public boolean isPlayer(int id) {
		return players.containsKey(id);
	}
	
	/**
	 * Return true if that agent type is a single-market agent (SMAgent).
	 * 
	 * @param agentType
	 * @return
	 */
	public boolean isSMAgent(AgentType agentType) {
//		return Consts.SM_AGENTS.contains(agentType);
		return false;
	}
	
	/**
	 * @param id
	 * @return true if agent with given ID has a non-null private value.
	 */
	public boolean hasPrivateValue(int id) {
		 return (getAgent(id).getPrivateValue() != null);
	}
	
	
	public ArrayList<Integer> getAgentIDsOfType(String type) {
		ArrayList<Integer> ids = new ArrayList<Integer>();
		for (Map.Entry<Integer,Agent> entry : agents.entrySet()) {
			if (entry.getValue().getType().equals(type)) {
				ids.add(entry.getKey());
			}
		}
		return ids;
	}
	
	public ArrayList<Integer> getMarketIDsOfType(String type) {
		ArrayList<Integer> ids = new ArrayList<Integer>();
		for (Map.Entry<Integer,Market> entry : markets.entrySet()) {
			if (entry.getValue().getType().equals(type)) {
				ids.add(entry.getKey());
			}
		}
		return ids;
	}

	public Price getPrivateValueByBid(int bidID) {
		return privateValues.get(bidID);
	}
	

	/***********************************
	 * Add agent, market, model, spread, etc.
	 *
	 **********************************/
	
	public void addAgent(Agent ag) {
		agents.put(ag.getID(), ag);
	}

	public void addPlayer(Agent ag) {
		players.put(ag.getID(), ag);
	}
	
	public void addMarket(Market mkt) {
		markets.put(mkt.getID(), mkt);
	}

	public void addModel(MarketModel mdl) {
		int id = mdl.getID();
		models.put(id, mdl);
		modelIDs.add(id);
		addModelAgentsToMap(id, mdl.getAgentConfig());
	}
	
	public void addModelAgentsToMap(int modelID, ArrayList<AgentPropsPair> list) {
		if (modelAgentMap.containsKey(modelID)) {
			modelAgentMap.get(modelID).addAll(list);
		} else {
			modelAgentMap.put(modelID, list);
		}
	}
	
	/**
	 * @param a AgentPropertiesPair
	 */
	public void addPlayerProperties(AgentPropsPair a) {
		if (playerMap.containsKey(a)) {
			playerMap.put(a, playerMap.get(a)+1);
		} else {
			playerMap.put(a, 1);
		}
		numPlayers++;
	}
	
	/**
	 * @param a AgentPropertiesPair
	 * @param n number of agents
	 */
	public void addEnvAgentNumber(AgentPropsPair a, int n) {
		envAgentMap.put(a, n);
		numEnvAgents += n;
	}
	
	public void addQuote(int mktID, Quote q) {
		quotes.put(mktID, q);
	}
	
	public void addPrivateValue(int bidID, Price pv) {
		privateValues.put(bidID, pv);
	}
	
	/**

	/**
	 * Add transaction ID to modelTransID container.
	 * @param modelID
	 * @param transID
	 */
	public void addModelTransID(int modelID, int transID) {
		if (modelTransID.containsKey(modelID)) {
			modelTransID.get(modelID).add(transID);
		} else {
			ArrayList<Integer> tmp = new ArrayList<Integer>();
			tmp.add(transID);
			modelTransID.put(modelID, tmp);
		}
	}
	
	/***********************************
	 * Global fundamental generation
	 *
	 **********************************/
	
	/**
	 * Set up global fundamental generation.
	 */
	public void globalFundamentalValues() {
		fundamentalGenerator = new FundamentalValue(kappa, meanValue, shockVar, 
				(int) simLength.longValue());	
	}
	
	/**
	 * Return generated fundamental value at time ts.
	 * 
	 * @param ts
	 * @return
	 */
	public Price getFundamentalAt(TimeStamp ts) {
		if (fundamentalGenerator == null) {
			return new Price(0);
		}
		return fundamentalGenerator.getValueAt(ts);
	}
	
	public FundamentalValue getFundamenalValue() {
		return fundamentalGenerator;
	}
	
	
	/***********************************
	 * Outputting (for testing)
	 *
	 **********************************/
	
	/**
	 * Write a double array to a file that's stored in the logs directory.
	 * 
	 * @param values
	 * @param filename
	 */
	public void writeToFile(double[] values, String filename) {
		try {
			DecimalFormat df = new DecimalFormat("#.#######");
			File f = new File(new File(simDir, Consts.LOG_DIR), filename);
			if (!f.isFile()) f.createNewFile();
			FileOutputStream os = new FileOutputStream(f);
			OutputStreamWriter osw = new OutputStreamWriter(os, "UTF-8");
			BufferedWriter bw = new BufferedWriter(osw);
			for (int i = 0; i < values.length; i++) {
				bw.write(df.format(values[i]));
				bw.newLine();
			}
			bw.close();
			osw.close();
			os.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}

