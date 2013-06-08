package data;

import event.*;
import entity.*;
import market.*;
import model.*;
import systemmanager.*;

import java.util.*;

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

	public int num;										// observation number
	public String simDir;							// simulations directory
	public boolean EGTA;								// true if EGTA use case
	
	// Model information
	public MarketModel primaryModel;
	public String primaryModelDesc;						// description of primary model
	public HashMap<Integer,MarketModel> models;			// models hashed by ID
	public HashMap<Integer,Integer> marketIDModelIDMap;	// hashed by market ID
	public HashMap<Integer,List<Integer>> modelTransID; // hashed by model ID	
	
	// Market information
	public HashMap<Integer,PQBid> bids;					// all bids ever, hashed by bid ID
	public HashMap<Integer,Price> privateValues;		// private values hashed by bid ID
	public HashMap<Integer,PQTransaction> transactions;	// hashed by transaction ID
	public HashMap<Integer,List<PQTransaction> > transactionLists; // hashed by market ID
	public HashMap<Integer,Quote> quotes;				// hashed by market ID
	public HashMap<Integer,Agent> agents;				// (all) agents hashed by ID
	public HashMap<Integer,Agent> players;				// players (for EGTA)
	public HashMap<Integer,Market> markets;				// markets hashed by ID
	public List<Integer> modelIDs;

	public int numEnvAgents;
	public int numPlayers;
	public HashMap<AgentPropsPair, Integer> envAgentMap;
	public HashMap<AgentPropsPair, Integer> playerMap;
	// hashed by model ID
	public HashMap<Integer,List<AgentPropsPair>> modelAgentMap;
	
	private SIP sip;
	private Sequence transIDSequence;	
	private FundamentalValue fundamentalGenerator;
	
	// hashed by type, gives # of that type
	public HashMap<String,Integer> numModelType;
	
	// Parameters set by specification file
	public TimeStamp simLength;
	public int tickSize;
	public TimeStamp nbboLatency;
	public double arrivalRate;
	public double reentryRate;
	public int meanValue;
	public double kappa;
	public double shockVar;
	public double pvVar;			// agent variance from PV random process
	
	// Variables of time series for observation file
	public HashMap<Integer,TimeSeries> marketSpread;			// hashed by market ID
	public HashMap<Integer,TimeSeries> NBBOSpread;				// hashed by model ID
	public HashMap<Integer,TimeSeries> marketMidQuote;			// hashed by market ID
	public HashMap<Integer,TimeSeries> marketDepth;				// hashed by market ID
	
	public HashMap<Integer,TimeStamp> timeToExecution;		 	// hashed by bid ID
	public HashMap<Integer,TimeStamp> submissionTime;			// hashed by bid ID
	public HashMap<Integer,HashMap<Double,Surplus>> modelSurplus; // hashed by model ID
																  // then by rho
	
	/**
	 * Constructor
	 */
	public SystemData() {
		bids = new HashMap<Integer,PQBid>();
		privateValues = new HashMap<Integer,Price>();
		transactions = new HashMap<Integer,PQTransaction>();
		quotes = new HashMap<Integer,Quote>();
		agents = new HashMap<Integer,Agent>();
		players = new HashMap<Integer,Agent>();
		markets = new HashMap<Integer,Market>();
		models = new HashMap<Integer,MarketModel>();
		numModelType = new HashMap<String,Integer>();
		modelIDs = new ArrayList<Integer>();
		transIDSequence = new Sequence(0);
		primaryModel = null;
		marketIDModelIDMap = new HashMap<Integer,Integer>();
		modelTransID = new HashMap<Integer, List<Integer>>();
		
		modelAgentMap = new HashMap<Integer, List<AgentPropsPair>>();
		playerMap = new HashMap<AgentPropsPair, Integer>();
		envAgentMap = new HashMap<AgentPropsPair, Integer>(); 
	
		transactionLists = new HashMap<Integer,List<PQTransaction>>();
		
		// Initialize containers for observations/features
		marketDepth = new HashMap<Integer,TimeSeries>();
		marketSpread = new HashMap<Integer,TimeSeries>();
		NBBOSpread = new HashMap<Integer,TimeSeries>();
		marketMidQuote = new HashMap<Integer,TimeSeries>();
		timeToExecution = new HashMap<Integer,TimeStamp>();
		submissionTime = new HashMap<Integer,TimeStamp>();
		modelSurplus = new HashMap<Integer,HashMap<Double,Surplus>>();
		
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
	public ArrayList<Integer> getPrimaryMarketIDs() {
		return primaryModel.getMarketIDs();
	}
	
	/**
	 * @return agentIDs of primary model
	 */
	public ArrayList<Integer> getPrimaryAgentIDs() {
		return primaryModel.getAgentIDs();
	}
	
	/**
	 * @param marketID
	 * @return
	 */
	public HashMap<Integer,Bid> getBids(int marketID) {
		return markets.get(marketID).getBids();
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
	
	public SIP getSIP() {
		return sip;
	}
	
	public HashMap<Integer,Agent> getAgents() {
		return agents;
	}

	public ArrayList<Integer> getAgentIDs() {
		return new ArrayList<Integer>(agents.keySet());
	}
	
	public int getAgentLogID(int agentID) {
		return agents.get(agentID).getLogID();
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
	public boolean isSMAgent(String agentType) {
		return Consts.SM_AGENT_TYPES.contains(agentType);
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
	
	public PQBid getBid(int id) {
		return bids.get(id);
	}

	public Price getPrivateValueByBid(int bidID) {
		return privateValues.get(bidID);
	}
	
	/**
	 * Get a specific transaction within a given model.
	 * @param modelID
	 * @param id
	 * @return
	 */
	public PQTransaction getTransaction(int modelID, int id) {
		ArrayList<Integer> mktIDs = getModel(modelID).getMarketIDs();
		// TODO need to get rid of this
		return getTrans(modelID).get(id);
	}
	
	public PQTransaction getTransaction(int id) {
		return transactions.get(id);
	}
	
	public List<Integer> getTransIDByModel(int modelID) {
		return modelTransID.get(modelID);
	}
	
	/**
	 * @return all transactions, across all models
	 */
	public HashMap<Integer, PQTransaction> getTransactions() {
		return transactions;
	}
	
	/**
	 * Get transactions for only the given model.
	 * 
	 * @param modelID 
	 * @return
	 */
	public HashMap<Integer,PQTransaction> getTrans(int modelID) {
		ArrayList<Integer> mktIDs = getModel(modelID).getMarketIDs();
		
		HashMap<Integer,PQTransaction> trans = new HashMap<Integer,PQTransaction>();
		for (Map.Entry<Integer,PQTransaction> entry : transactions.entrySet()) {
			if (mktIDs.contains(entry.getValue().marketID)) {
				trans.put(entry.getKey(), entry.getValue());
			}
		}
		return trans;
	}
	
	/**
	 * @return list of all transaction IDs
	 */
	public ArrayList<Integer> getTransIDs(int modelID) {
		return new ArrayList<Integer>(getTrans(modelID).keySet());
	}
	
	/**
	 * @param bidID
	 * @return
	 */
	public TimeStamp getTimeToExecution(int bidID) {
		return timeToExecution.get(bidID);
	}
	
	public HashMap<Double,Surplus> getSurplus(int modelID) {
		return modelSurplus.get(modelID);
	}
	
	public Surplus getSurplus(int modelID, double rho) {
		if (modelSurplus.containsKey(modelID))
			return modelSurplus.get(modelID).get(rho);
		else 
			return null;
	}
	
//	public HashMap<Integer,Double> getSurplusByRho(double rho) {
//		return allSurplus.get(rho);
//	}
	

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
	
	public void setSIP(SIP sip) {
		this.sip = sip;
	}
	
	public void addQuote(int mktID, Quote q) {
		quotes.put(mktID, q);
	}
	
	public void addBid(PQBid b) {
		bids.put(b.getBidID(), b);
	}
	
	public void addPrivateValue(int bidID, Price pv) {
		privateValues.put(bidID, pv);
	}
	
	/**
	 * Add bid-ask spread value.
	 * 
	 * @param mktID
	 * @param ts 
	 * @param spread
	 */
	public void addSpread(int mktID, TimeStamp ts, int spread) {
		if(!marketSpread.containsKey(mktID)) {
			marketSpread.put(mktID, new TimeSeries());
		}
		marketSpread.get(mktID).add(ts, (double) spread);
	}	
	
	/**
	 * Add the mid-quote price (midpoint of BID-ASK quote). For computing
	 * price volatility & returns.
	 * 
	 * @param mktID
	 * @param ts
	 * @param bid
	 * @param ask
	 */
	public void addMidQuotePrice(int mktID, TimeStamp ts, int bid, int ask) {
		double midQuote = (bid + ask) / 2;
		
		if (!marketMidQuote.containsKey(mktID)) {
			marketMidQuote.put(mktID, new TimeSeries());
		}
		marketMidQuote.get(mktID).add(ts, midQuote);
	}
	
	/**
	 * Add NBBO bid-ask spread value.
	 * 
	 * @param modelID
	 * @param ts 
	 * @param spread
	 */
	public void addNBBOSpread(int modelID, TimeStamp ts, int spread) {
		if(!NBBOSpread.containsKey(modelID)) {
			NBBOSpread.put(modelID, new TimeSeries());
		}
		NBBOSpread.get(modelID).add(ts, (double) spread);
	}	
		
	/**
	 * Add depth (number of orders waiting to be fulfilled).
	 * 
	 * @param mktID
	 * @param ts
	 * @param depth
	 */
	public void addDepth(int mktID, TimeStamp ts, int depth) {
		if(!marketDepth.containsKey(mktID)) {
			marketDepth.put(mktID, new TimeSeries());
		}
		marketDepth.get(mktID).add(ts, (double) depth);
	}
	
	/**
	 * Add bid submission time to the hash map container.
	 * @param bidID
	 * @param ts
	 */
	public void addSubmissionTime(int bidID, TimeStamp ts) {
		submissionTime.put(bidID, ts);
	}


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
	
	/**
	 * Add transaction.
	 * @param tr
	 */
	public void addTransaction(PQTransaction tr) {
		int id = transIDSequence.increment();
		tr.transID = id;
		transactions.put(id, tr);
		if (!transactionLists.containsKey(tr.marketID)) {
			transactionLists.put(tr.marketID, new ArrayList<PQTransaction>());
		}
		transactionLists.get(tr.marketID).add(tr);
				
				
		MarketModel model = getModelByMarketID(tr.marketID);
		addSurplus(model.getID(), tr);
		addModelTransID(model.getID(), tr.transID);
	}
	
	/**
	 * Add bid time to execution (difference between transaction and submission times).
	 * @param bidID
	 * @param ts
	 */
	public void addTimeToExecution(int bidID, TimeStamp ts) {
		// check if submission time contains it (if not, there is an error)
		if (submissionTime.containsKey(bidID)) {
			timeToExecution.put(bidID, ts.diff(submissionTime.get(bidID)));
		} else {
			System.err.println(this.getClass().getSimpleName() + 
					":: submission time does not contain bidID " + bidID);
		}
	}

	/***********************************
	 * Surplus computations
	 *
	 **********************************/
	
	/**
	 * Add surplus. Update surplus for given model, given value of rho, 
	 * and given a new transaction to process.
	 * 
	 * @param modelID
	 * @param t
	 */
	public void addSurplus(int modelID, PQTransaction t) {
		if(!modelSurplus.containsKey(modelID)) {
			modelSurplus.put(modelID, new HashMap<Double,Surplus>());
		}
		// compute surplus for all values of rho
		for (double rho : Consts.rhos) {
			if (!modelSurplus.get(modelID).containsKey(rho)) {
				modelSurplus.get(modelID).put(rho, new Surplus(rho));
			}
			Surplus s = getSurplus(modelID, rho);
			Price rt = getFundamentalAt(t.timestamp);
			
			// Compute buyer surplus
			Agent buyer = getAgent(t.buyerID);
			TimeStamp buyTime = getTimeToExecution(t.buyBidID);
			if (buyer.getPrivateValue() != null) {
				double cs = getPrivateValueByBid(t.buyBidID).sum(rt).diff(t.price).getPrice();
				// System.out.println(modelID + ": " + t + " cs=" + cs + ", buyTime=" + buyT);
				s.addCumulative(t.buyerID, Math.exp(-rho * buyTime.longValue()) * cs);
			} else {
				double cs = rt.diff(t.price).getPrice();
				s.add(t.buyerID, Math.exp(-rho * buyTime.longValue()) * cs);
			}
			
			// Compute seller surplus
			Agent seller = getAgent(t.sellerID);
			TimeStamp sellTime = getTimeToExecution(t.sellBidID);
			if (seller.getPrivateValue() != null) {
				double ps = t.price.diff(getPrivateValueByBid(t.sellBidID).sum(rt)).getPrice();
				// System.out.println(modelID + ": " + t + " ps=" + ps + ", sellTime=" + sellT);
				s.addCumulative(t.sellerID, Math.exp(-rho * sellTime.longValue()) * ps);
			} else {
				double ps = t.price.diff(rt).getPrice();
				s.add(t.sellerID, Math.exp(-rho * sellTime.longValue()) * ps);
			}
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
		return fundamentalGenerator.getValueAt((int) ts.longValue());
	}
	
	/**
	 * @return time series of the global fundamental generated by the 
	 * 			mean-reverting stochastic process
	 */
	public ArrayList<Price> getFundamentalValueProcess() {
		return fundamentalGenerator.getProcess();
	}
}

