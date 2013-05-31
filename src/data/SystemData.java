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

	public int obsNum;									// observation number
	public boolean EGTA;								// true if EGTA use case
	
	// Model information
	public MarketModel primaryModel;
	public String primaryModelDesc;						// description of primary model
	public HashMap<Integer,MarketModel> models;			// models hashed by ID
	public HashMap<Integer,Integer> marketIDModelIDMap;	// hashed by market ID
	public HashMap<Integer,ArrayList<Integer>> modelTransID; // hashed by model ID	
	
	// Market information
	public HashMap<Integer,PQBid> bids;					// all bids ever, hashed by bid ID
	public HashMap<Integer,Price> privateValues;		// private values hashed by bid ID
	public HashMap<Integer,PQTransaction> transactions;	// hashed by transaction ID
	public HashMap<Integer, ArrayList<PQTransaction> > transactionLists; //hashmap of in order market transactions
	public HashMap<Integer,Quote> quotes;				// hashed by market ID
	public HashMap<Integer,Agent> agents;				// (all) agents hashed by ID
	public HashMap<Integer,Agent> players;				// players (for EGTA)
	public HashMap<Integer,Market> markets;				// markets hashed by ID
	public ArrayList<Integer> modelIDs;

	public int numEnvAgents;
	public int numPlayers;
	public HashMap<AgentPropsPair, Integer> envAgentMap;
	public HashMap<AgentPropsPair, Integer> playerMap;
	// hashed by model ID
	public HashMap<Integer, ArrayList<AgentPropsPair>> modelAgentMap;
	
	private SIP sip;
	private Sequence transIDSequence;	
	private FundamentalValue fundamentalGenerator;
	
	// hashed by type, gives # of that type
	public HashMap<String,Integer> numModelType;
	
	// Parameters set by specification file
	public TimeStamp simLength;
	public int tickSize;
	public TimeStamp centralCallClearFreq;
	public TimeStamp nbboLatency;
	public double arrivalRate;
	public double reentryRate;
	public int meanValue;
	public double kappa;
	public double shockVar;
	public double pvVar;								// agent variance from PV random process
	
	// Variables of time series for observation file
	public HashMap<Integer,HashMap<TimeStamp,Double>> marketDepth;		//hashed by market ID, then TimeStamp	
	public HashMap<Integer,HashMap<TimeStamp,Double>> marketSpread;		//hashed by market ID, then TimeStamp
	public HashMap<Integer,HashMap<TimeStamp,Double>> NBBOSpread;		//hashed by model ID, then TimeStamp
	public HashMap<Integer,HashMap<TimeStamp,Double>> marketMidQuote;	//hashed by market ID, then TimeStamp
	public HashMap<Integer,TimeStamp> timeToExecution;		 	// hashed by bid ID
	public HashMap<Integer,TimeStamp> submissionTime;			// hashed by bid ID
	public HashMap<Double,HashMap<Integer,Double>> allSurplus;	// hashed by rho & agent ID
	
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
		modelTransID = new HashMap<Integer, ArrayList<Integer>>();
		
		modelAgentMap = new HashMap<Integer, ArrayList<AgentPropsPair>>();
		playerMap = new HashMap<AgentPropsPair, Integer>();
		envAgentMap = new HashMap<AgentPropsPair, Integer>(); 
	
		transactionLists = new HashMap<Integer, ArrayList<PQTransaction>>();
		
		// Initialize containers for observations/features
		marketDepth = new HashMap<Integer,HashMap<TimeStamp,Double>>();
		marketSpread = new HashMap<Integer,HashMap<TimeStamp,Double>>();
		NBBOSpread = new HashMap<Integer,HashMap<TimeStamp,Double>>();
		marketMidQuote = new HashMap<Integer,HashMap<TimeStamp,Double>>();
		timeToExecution = new HashMap<Integer,TimeStamp>();
		submissionTime = new HashMap<Integer,TimeStamp>();
		allSurplus = new HashMap<Double,HashMap<Integer,Double>>();
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
	
	/**
	 * @return
	 */
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
	
//	/**
//	 * @return modelAgentMap
//	 */
//	public HashMap<Integer, ArrayList<AgentPropsPair>> getModelAgentMap() {
//		return modelAgentMap;
//	}
	
	/**
	 * @param modelID
	 * @return list of AgentPropertiesPairs for model agents within the specified model
	 */
	public ArrayList<AgentPropsPair> getModelAgentByModel(int modelID) {
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
		
		for (ArrayList<AgentPropsPair> list : modelAgentMap.values()) {		
			for (Iterator<AgentPropsPair> it = list.iterator(); it.hasNext(); ) {
				AgentPropsPair app = it.next();
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
	public boolean isNonPlayer(int id) {
		return !(players.containsKey(id));
	}
	
	/**
	 * Return true if that agent type is a single-market agent (SMAgent).
	 * 
	 * @param agentType
	 * @return
	 */
	public boolean isSMAgent(String agentType) {
		return Arrays.asList(Consts.SM_AGENT_TYPES).contains(agentType);
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
		return getTrans(modelID).get(id);
	}
	
	public PQTransaction getTransaction(int id) {
		return transactions.get(id);
	}
	
	public ArrayList<Integer> getTransIDByModel(int modelID) {
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
	 * TODO - eventually remove
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
	
	public double getSurplus(double rho, int agentID) {
		return allSurplus.get(rho).get(agentID);
	}
	
	public HashMap<Integer,Double> getSurplusByRho(double rho) {
		return allSurplus.get(rho);
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
	
	public void setSIP(SIP sip) {
		this.sip = sip;
	}

	public void addModelTransID(int modelID, int transID) {
		if (modelTransID.containsKey(modelID)) {
			modelTransID.get(modelID).add(transID);
		} else {
			ArrayList<Integer> tmp = new ArrayList<Integer>();
			tmp.add(transID);
			modelTransID.put(modelID, tmp);
		}
	}
	
	public void addTransaction(PQTransaction tr) {
		int id = transIDSequence.increment();
		tr.transID = id;
		transactions.put(id, tr);
		addSurplus(tr);
		// insert based on model ID
		addModelTransID(getModelByMarketID(tr.marketID).getID(), tr.transID);
		
		//Adding to transactionList
		//If the market has not been added, create the ArrayList
		if(!transactionLists.containsKey(tr.marketID)) {
			transactionLists.put(tr.marketID, new ArrayList<PQTransaction>());
		}
		transactionLists.get(tr.marketID).add(tr);
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
	 * Add bid-ask spread value to the HashMap containers.
	 * 
	 * @param mktID
	 * @param ts 
	 * @param spread
	 */
	public void addSpread(int mktID, TimeStamp ts, int spread) {
		//If the market has not been added, create the HashMap
		if(!marketSpread.containsKey(mktID)) {
			marketSpread.put(mktID, new HashMap<TimeStamp,Double>());
		}
		//Add the spread to the corresponding market
		marketSpread.get(mktID).put(ts, (double) spread);
		
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
		
		if (marketMidQuote.get(mktID) != null) {
			marketMidQuote.get(mktID).put(ts,  midQuote);
		} else {
			HashMap<TimeStamp,Double> tmp = new HashMap<TimeStamp,Double>();
			tmp.put(ts, midQuote);
			marketMidQuote.put(mktID, tmp);
		}
	}
	
	
	/**
	 * Add NBBO bid-ask spread value to the HashMap containers.
	 * 
	 * @param modelID
	 * @param ts 
	 * @param spread
	 */
	public void addNBBOSpread(int modelID, TimeStamp ts, int spread) {
		//If the model has not been addressed, create the HashMap
		if(!NBBOSpread.containsKey(modelID)) {
			NBBOSpread.put(modelID, new HashMap<TimeStamp,Double>());
		}
		//Add the spread to the corresponding model
		NBBOSpread.get(modelID).put(ts, (double) spread);
	}	
		
	/**
	 * Add depth (number of orders waiting to be fulfilled) to the the container.
	 * 
	 * @param mktID
	 * @param ts
	 * @param depth
	 */
	public void addDepth(int mktID, TimeStamp ts, int depth) {
		//If the market has not been addressed, add the HashMap
		if(!marketDepth.containsKey(mktID)) {
			marketDepth.put(mktID, new HashMap<TimeStamp,Double>());
		}
		//Add the depth to the corresponding market
		marketDepth.get(mktID).put(ts, (double) depth);
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
	 * Add bid time to execution (difference between transaction and submission times).
	 * @param bidID
	 * @param ts
	 */
	public void addTimeToExecution(int bidID, TimeStamp ts) {
		// check if submission time contains it (if not, there is an error)
		if (submissionTime.containsKey(bidID)) {
			timeToExecution.put(bidID, ts.diff(submissionTime.get(bidID)));
		} else {
			System.err.print("ERROR: submission time does not contain bidID " + bidID);
		}
	}


	/**
	 * Add surplus to data structure, for all given values of rho.
	 * 
	 * @param t
	 */
	public void addSurplus(PQTransaction t) {
		// in case 0 not included in rhos list
		double rho = 0;
		updateSurplus(t, rho);
		for (int i = 0; i < Consts.rhos.length; i++) {
			rho = Consts.rhos[i];
			if (rho != 0) {
				updateSurplus(t, rho);
			}
		}
	}
	
	/**
	 * @param t
	 * @param rho
	 */
	private void updateSurplus(PQTransaction t, double rho) {
		if (!allSurplus.containsKey(rho)) {
			HashMap<Integer,Double> tmp = new HashMap<Integer,Double>();
			allSurplus.put(rho, tmp);
		}
		allSurplus.get(rho).put(t.buyerID, computeBuyerSurplus(t, rho));
		allSurplus.get(rho).put(t.sellerID, computeSellerSurplus(t, rho));
	}
	
	/**
	 * Returns updated surplus for buyer in given transaction.
	 * 
	 * @param t
	 * @param rho
	 * @return
	 */
	public double computeBuyerSurplus(PQTransaction t, double rho) {
		double val = 0;

		Agent buyer = getAgent(t.buyerID);
		TimeStamp buyTime = getTimeToExecution(t.buyBidID);
		Price rt = getFundamentalAt(t.timestamp);
		
		double surplus = 0;
		if (allSurplus.get(rho).containsKey(buyer.getID())) {
			surplus = allSurplus.get(rho).get(buyer.getID());
		}
		if (buyer.getPrivateValue() != null) {
			double cs = getPrivateValueByBid(t.buyBidID).sum(rt).diff(t.price).getPrice();
			// System.out.println(modelID + ": " + t + " cs=" + cs + ", buyTime=" + buyT);
			val = surplus + Math.exp(-rho * buyTime.longValue()) * cs;
		} else {
			double cs = rt.diff(t.price).getPrice();
			val = Math.exp(-rho * buyTime.longValue()) * cs;
		}
		return val;
	}
	
	/**
	 * Returns updated surplus for seller in given transaction.
	 * 
	 * @param t
	 * @param rho
	 * @return
	 */
	public double computeSellerSurplus(PQTransaction t, double rho) {
		double val = 0;

		Agent seller = getAgent(t.sellerID);
		TimeStamp sellTime = getTimeToExecution(t.sellBidID);
		Price rt = getFundamentalAt(t.timestamp);
		
		double surplus = 0;
		if (allSurplus.get(rho).containsKey(seller.getID())) {
			surplus = allSurplus.get(rho).get(seller.getID());
		}
		if (seller.getPrivateValue() != null) {
			double ps = t.price.diff(getPrivateValueByBid(t.sellBidID).sum(rt)).getPrice();
			// System.out.println(modelID + ": " + t + " ps=" + ps + ", sellTime=" + sellT);
			val = surplus + Math.exp(-rho * sellTime.longValue()) * ps;
		} else {
			double ps = t.price.diff(rt).getPrice();
			val = Math.exp(-rho * sellTime.longValue()) * ps;
		}
		return val;
	}
	
	
	/***********************************
	 * Global fundamental generation
	 *
	 **********************************/
	
	/**
	 * Set up global fundamental generation.
	 */
	public void globalFundamentalValues() {
		fundamentalGenerator = new FundamentalValue(kappa, meanValue, shockVar, (int) simLength.longValue());	
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
	 * @return time series of the global fundamental generated by the stochastic process
	 */
	public ArrayList<Price> getFundamentalValueProcess() {
		return fundamentalGenerator.getProcess();
	}
	
	
	
	/***********************************
	 * Surplus computations
	 *
	 **********************************/
	
//	/**
//	 * Iterates through all transactions and sums up surplus for all agents of a 
//	 * specified type. For agents that do not have a private valuation, use their
//	 * realized profit instead.
//	 * 
//	 * Note that private values are a deviation from the fundamental (v).
//	 * 
//	 * CS = (PV + v) - p, PS = p - (PV + v)
//	 * 
//	 * @param modelID 		model id to check
//	 * @param agentType
//	 * @return hash map of agent surplus, hashed by agent ID
//	 */
//	public HashMap<Integer,Integer> getSurplusForType(int modelID, String agentType) {
//
//		ArrayList<Integer> ids = getModel(modelID).getMarketIDs();
//		HashMap<Integer,Integer> allSurplus = new HashMap<Integer,Integer>();
//		
//		for (Map.Entry<Integer,PQTransaction> trans : getTrans(modelID).entrySet()) {
//			PQTransaction t = trans.getValue();
//			
//			if (ids.contains(t.marketID)) {
//				Agent buyer = agents.get(t.buyerID);
//				Agent seller = agents.get(t.sellerID);
//				
//				if (agentType.equals(buyer.getType())) {
//					int surplus = 0;
//					if (allSurplus.containsKey(buyer.getID())) {
//						surplus = allSurplus.get(buyer.getID());
//					}
//					if (buyer.getPrivateValue() != null) {
//						Price rt = getFundamentalAt(t.timestamp);
//						int val = (buyer.getPrivateValueAt(t.quantity).sum(rt)).diff(t.price).getPrice();
//						allSurplus.put(buyer.getID(), surplus + val);
//					} else {
//						allSurplus.put(buyer.getID(), buyer.getRealizedProfit());
//					}
//				}
//				if (agentType.equals(seller.getType())) {
//					int surplus = 0;
//					if (allSurplus.containsKey(seller.getID())) {
//						surplus = allSurplus.get(seller.getID());
//					}
//					if (seller.getPrivateValue() != null) {
//						Price rt = getFundamentalAt(t.timestamp);
//						int val = t.price.diff(seller.getPrivateValueAt(-t.quantity).sum(rt)).getPrice();
//						allSurplus.put(seller.getID(), surplus + val);
//					} else {
//						allSurplus.put(seller.getID(), seller.getRealizedProfit());
//					}
//				}
//			}
//		}
//		return allSurplus;
//	}
	
	
	/**
	 * Get total surplus for a specific agent within a model.
	 * 
	 * @param modelID
	 * @param agentID
	 * @return
	 */
	public int getSurplusForAgent(int modelID, int agentID) {
		ArrayList<Integer> ids = getModel(modelID).getMarketIDs();
		int surplus = 0;
		
		for (Map.Entry<Integer,PQTransaction> trans : getTrans(modelID).entrySet()) {
			PQTransaction t = trans.getValue();
			
			if (ids.contains(t.marketID)) {
				Agent buyer = agents.get(t.buyerID);
				Agent seller = agents.get(t.sellerID);
				
				if (buyer.getID() == agentID) {
					if (buyer.getPrivateValue() != null) {
						Price rt = getFundamentalAt(t.timestamp);
						//surplus += (buyer.getPrivateValueAt(t.quantity).sum(rt)).diff(t.price).getPrice();
						surplus += getPrivateValueByBid(t.buyBidID).sum(rt).diff(t.price).getPrice();
					} else {
						surplus = buyer.getRealizedProfit(); // already summed
					}
				}
				if (seller.getID() == agentID) {
					if (seller.getPrivateValue() != null) {
						Price rt = getFundamentalAt(t.timestamp);
						//surplus += t.price.diff(seller.getPrivateValueAt(-t.quantity).sum(rt)).getPrice();
						surplus += t.price.diff(getPrivateValueByBid(t.sellBidID).sum(rt)).getPrice();
					} else {
						surplus = seller.getRealizedProfit(); // already summed
					}
				}
			}
		}
		return surplus;
	}
	
	
//	/**
//	 * Iterates through all transactions and sums up surplus for all background agents
//	 * that have a private valuation.
//	 * 
//	 * Note that private values are a deviation from the fundamental (v).
//	 * 
//	 * CS = (PV + v) - p, PS = p - (PV + v)
//	 * 
//	 * @param modelID 		model id to check
//	 * @return hash map of background agent surplus, hashed by agent ID
//	 */
//	public HashMap<Integer,Integer> getBackgroundSurplus(int modelID) {
//		// basically the same as discounted surplus, but with discount 0.0
//		// also Integers instead of Doubles
//
//		ArrayList<Integer> ids = getModel(modelID).getMarketIDs();
//		HashMap<Integer,Integer> allSurplus = new HashMap<Integer,Integer>();
//		
//		for (Map.Entry<Integer,PQTransaction> trans : getTrans(modelID).entrySet()) {
//			PQTransaction t = trans.getValue();
//			
//			if (ids.contains(t.marketID)) {
//				Agent buyer = getAgent(t.buyerID);
//				Agent seller = getAgent(t.sellerID);
//				
//				// Check that PV is defined & that it is a background agent
//				if (isNonPlayer(buyer.getID())) {
//					int surplus = 0;
//					if (allSurplus.containsKey(buyer.getID())) {
//						surplus = allSurplus.get(buyer.getID());
//					}
//					if (buyer.getPrivateValue() != null) {
//						Price rt = getFundamentalAt(t.timestamp);
//						//int val = (buyer.getPrivateValueAt(t.quantity).sum(rt)).diff(t.price).getPrice();
//						int val = getPrivateValueByBid(t.buyBidID).sum(rt).diff(t.price).getPrice();
//						allSurplus.put(buyer.getID(), surplus + val);	
//					} else {
//						allSurplus.put(buyer.getID(), buyer.getRealizedProfit());	// already summed
//					}
//				}
//				if (isNonPlayer(seller.getID())) {
//					int surplus = 0;
//					if (allSurplus.containsKey(seller.getID())) {
//						surplus = allSurplus.get(seller.getID());
//					}
//					if (seller.getPrivateValue() != null) {
//						Price rt = getFundamentalAt(t.timestamp);
//						//int val = t.price.diff(seller.getPrivateValueAt(-t.quantity).sum(rt)).getPrice();
//						int val = t.price.diff(getPrivateValueByBid(t.sellBidID).sum(rt)).getPrice();
//						allSurplus.put(seller.getID(), surplus + val);
//					} else {
//						allSurplus.put(seller.getID(), seller.getRealizedProfit());	// already summed
//					}
//				}
//			}
//		}
//		return allSurplus;
//	}
//	
//	/**
//	 * Iterates through all transactions and sums up discounted surplus for all
//	 * background agents. (Note that HFT agent transactions will naturally execute
//	 * with zero transaction time, so they do not need to be discounted).
//	 * 
//	 * Each transaction's surplus is discounted by exp{-rho * T}, where T is the 
//	 * execution speed of that transaction.
//	 * 
//	 * TODO - should the returned hashmap be hashed by transaction ID instead?
//	 * 
//	 * CS = (PV + v) - p, PS = p - (PV + v)
//	 * 
//	 * @param modelID 		model id to check
//	 * @param rho			discount factor
//	 * @return discounted surplus, hashed by (background) agent ID
//	 */
//	public HashMap<Integer,Double> getDiscountedSurplus(int modelID, double rho) {
//		ArrayList<Integer> ids = getModel(modelID).getMarketIDs();
//		HashMap<Integer,Double> discSurplus = new HashMap<Integer,Double>();
//		
//		for (Map.Entry<Integer,PQTransaction> trans : getTrans(modelID).entrySet()) {
//			PQTransaction t = trans.getValue();
//			
//			if (ids.contains(t.marketID)) {
//				Agent buyer = agents.get(t.buyerID);
//				Agent seller = agents.get(t.sellerID);
//				TimeStamp buyTime = timeToExecution.get(t.buyBidID);
//				TimeStamp sellTime = timeToExecution.get(t.sellBidID);
//				
//				// Check that PV is defined & that it is a background agent
//				if (isNonPlayer(buyer.getID())) {
//					double surplus = 0;
//					if (discSurplus.containsKey(buyer.getID())) {
//						surplus = discSurplus.get(buyer.getID());
//					}
//					if (buyer.getPrivateValue() != null) {
//						Price rt = getFundamentalAt(t.timestamp);
//						double cs = getPrivateValueByBid(t.buyBidID).sum(rt).diff(t.price).getPrice();
////						System.out.println(modelID + ": " + t + " cs=" + cs + ", buyTime=" + buyTime);
//						discSurplus.put(buyer.getID(), surplus + Math.exp(-rho * buyTime.longValue()) * cs);
//					} else {
//						discSurplus.put(buyer.getID(), Math.exp(-rho * buyTime.longValue()) * buyer.getRealizedProfit());
//					}
//				}
//				if (isNonPlayer(seller.getID())) {
//					double surplus = 0;
//					if (discSurplus.containsKey(seller.getID())) {
//						surplus = discSurplus.get(seller.getID());
//					}
//					if (seller.getPrivateValue() != null) {
//						Price rt = getFundamentalAt(t.timestamp);
//						double ps = t.price.diff(getPrivateValueByBid(t.sellBidID).sum(rt)).getPrice();
////						System.out.println(modelID + ": " + t + " ps=" + ps + ", sellTime=" + sellTime);
//						discSurplus.put(seller.getID(), surplus + Math.exp(-rho * sellTime.longValue()) * ps);
//					} else {
//						discSurplus.put(seller.getID(), Math.exp(-rho * sellTime.longValue()) * seller.getRealizedProfit());
//					}
//				}
//			}
//		}
//		return discSurplus;
//	}

	
	
	
	
//	/**
//	 * Modifies transactions for the given model by setting TimeStamp to be constant, 
//	 * and using agent log IDs rather than agent IDs for indicating buyer or seller.
//	 * Also sets constant price (because surplus will be same regardless of price), as
//	 * well as constant bid IDs since different bids are submitted to each model.
//	 * 
//	 * @param modelID
//	 * @return
//	 */
//	public ArrayList<PQTransaction> getComparableTrans(int modelID) {
//		ArrayList<Integer> mktIDs = getModel(modelID).getMarketIDs();
//				
//		ArrayList<PQTransaction> trans = new ArrayList<PQTransaction>();
//		for (Map.Entry<Integer,PQTransaction> entry : transactions.entrySet()) {
//			if (mktIDs.contains(entry.getValue().marketID)) {
//				PQTransaction tr = entry.getValue();
//				
//				// Modify values to be comparable between models
//				// - Use logID rather than agentID
//				// - Set constant TimeStamp
//				// - Set constant marketID
//				// - Set constant price
//				// - Set constant bid IDs
//				// TODO - for now, ignore quantity
//				int bID = getAgent(tr.buyerID).getLogID();
//				int sID = getAgent(tr.sellerID).getLogID();
//				Price p = new Price(0);
//				TimeStamp ts = new TimeStamp(-1);
//				int mktID = 0;
//				int bBidID = 0;
//				int sBidID = 0;
//				
//				PQTransaction trNew = new PQTransaction(tr.quantity, p, bID, sID, bBidID, sBidID, ts, mktID);
//				trans.add(trNew);
//			}
//		}
//		return trans;
//	}
	
//	/**
//	 * Modifies all transactions in transData to be comparable between models.
//	 * 
//	 * @return
//	 */
//	public ArrayList<PQTransaction> getUniqueComparableTrans() {
//		Set<PQTransaction> trans = new HashSet<PQTransaction>();
//		
//		for (Map.Entry<Integer,PQTransaction> entry : transactions.entrySet()) {
//			PQTransaction tr = entry.getValue();
//			
//			// Modify values to be comparable between models
//			// - Use logID rather than agentID
//			// - Set constant TimeStamp
//			// - Set constant marketID
//			// - Set constant price
//			// - Set constant bid IDs
//			// TODO - for now, ignore quantity
//			int bID = getAgent(tr.buyerID).getLogID();
//			int sID = getAgent(tr.sellerID).getLogID();
//			Price p = new Price(0);
//			TimeStamp ts = new TimeStamp(-1);
//			int mktID = 0;
//			int bBidID = 0;
//			int sBidID = 0;
//			
//			PQTransaction trNew = new PQTransaction(tr.quantity, p, bID, sID, bBidID, sBidID, ts, mktID);
//			trans.add(trNew);
//		}
//		ArrayList<PQTransaction> uniqueTrans = new ArrayList<PQTransaction>();
//		uniqueTrans.addAll(trans);
//		return uniqueTrans;
//	}
	
//	/**
//	 * @return list of actual private values of all agents
//	 */
//	public ArrayList<Price> getPrivateValues() {
//		ArrayList<Price> pvs = new ArrayList<Price>(numAgents);
//		for (Iterator<Integer> ag = getAgentIDs().iterator(); ag.hasNext(); ) {
//			Price val = agents.get(ag.next()).getPrivateValue();
//			// PV will be null if it doesn't exist for the agent
//			if (val != null) {
//				pvs.add(val);
//			}
//		}
//		return pvs;
//	}
	
	
}

