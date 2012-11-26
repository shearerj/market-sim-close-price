package systemmanager;

import event.*;
import entity.*;
import market.*;
import model.*;

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
 * The primary market model is the one in which agents primarily interact. Bids
 * are then duplicated in the other market models, based on those submitted in the
 * primary model.
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
	
	// Model information
	public HashMap<Integer,MarketModel> models;			// models hashed by ID
	public HashMap<Integer,ArrayList<Integer>> modelToMarketList;	// hashed by model ID
	public HashMap<Integer,Integer> marketToModel;		// hashed by market ID
	public MarketModel primaryModel;
	public String primaryModelDesc;						// description of primary model
	
	// Market information
	public HashMap<Integer,PQBid> bidData;				// all bids ever, hashed by bid ID
	public HashMap<Integer,PQTransaction> transData;	// hashed by transaction ID
	public HashMap<Integer,Quote> quoteData;			// hashed by market ID
	public HashMap<Integer,Agent> agents;				// agents hashed by ID
	public HashMap<Integer,Market> markets;				// markets hashed by ID
	public Quoter quoter;
	private Sequence transIDSequence;
	
	private ArrivalTime arrivalTimeGenerator;
	private PrivateValue processGenerator;
	
	// hashed by type, gives # of that type
	public HashMap<String,Integer> numModelType;
	public HashMap<String,Integer> numMarketType;
	public HashMap<String,Integer> numAgentType;
	
	public int numMarkets;
	public int numAgents;
	public ArrayList<Integer> roleAgentIDs;				// IDs of agents in a role
	public ArrayList<Integer> modelIDs;
	
	// Parameters set by specification file
	public TimeStamp simLength;
	public int tickSize;
	public TimeStamp centralCallClearFreq;
	public TimeStamp nbboLatency;
	public double arrivalRate;
	public int meanPV;
	public double kappa;
	public double shockVar;
	public double expireRate;
	public int bidRange;
	public double privateValueVar;				// agent variance from PV random process
	
	// Variables of time series for observation file
	public HashMap<Integer,HashMap<TimeStamp,Integer>> marketDepth;		// hashed by market ID
	public HashMap<Integer,HashMap<TimeStamp,Integer>> marketSpread;	// hashed by market ID
	public HashMap<Integer,HashMap<TimeStamp,Integer>> NBBOSpread;		// hashed by model ID, time series
	public HashMap<Integer,TimeStamp> executionTime;		 	// hashed by bid ID
	public HashMap<Integer,TimeStamp> submissionTime;			// hashed by bid ID
	
	/**
	 * Constructor
	 */
	public SystemData() {
		bidData = new HashMap<Integer,PQBid>();
		transData = new HashMap<Integer,PQTransaction>();
		quoteData = new HashMap<Integer,Quote>();
		agents = new HashMap<Integer,Agent>();
		markets = new HashMap<Integer,Market>();
		models = new HashMap<Integer,MarketModel>();
		numAgentType = new HashMap<String,Integer>();
		numMarketType = new HashMap<String,Integer>();
		numModelType = new HashMap<String,Integer>();
		numAgents = 0;
		numMarkets = 0;
		roleAgentIDs = new ArrayList<Integer>();
		modelIDs = new ArrayList<Integer>();
		transIDSequence = new Sequence(0);
		modelToMarketList = new HashMap<Integer,ArrayList<Integer>>();
		primaryModel = null;
		marketToModel = new HashMap<Integer,Integer>();
	
		// Initialize containers for observations/features
		marketDepth = new HashMap<Integer,HashMap<TimeStamp,Integer>>();
		marketSpread = new HashMap<Integer,HashMap<TimeStamp,Integer>>();
		NBBOSpread = new HashMap<Integer,HashMap<TimeStamp,Integer>>();
		executionTime = new HashMap<Integer,TimeStamp>();
		submissionTime = new HashMap<Integer,TimeStamp>();
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
		return quoteData;
	}

	/**
	 * Get quote for a given market.
	 * @param mktID
	 * @return
	 */
	public Quote getQuote(int mktID) {
		return quoteData.get(mktID);
	}
	
	/**
	 * Returns quoter entity.
	 * @return
	 */
	public Quoter getQuoter() {
		return quoter;
	}
	
	/**
	 * @return
	 */
	public HashMap<Integer,Agent> getAgents() {
		return agents;
	}

	/**
	 * @return HashMap of uncentralized markets
	 */
	public HashMap<Integer,Market> getMarkets() {
		return markets;
	}

	/**
	 * @return
	 */
	public HashMap<Integer,MarketModel> getModels() {
		return models;
	}
	
	/**
	 * @return
	 */
	public ArrayList<Integer> getAgentIDs() {
		return new ArrayList<Integer>(agents.keySet());
	}
	
	/**
	 * @return ArrayList of uncentralized market IDs
	 */
	public ArrayList<Integer> getMarketIDs() {
		return new ArrayList<Integer>(markets.keySet());
	}
	
	public Agent getAgent(int id) {
		return agents.get(id);
	}

	public Market getMarket(int id) {
		return markets.get(id);
	}

	public MarketModel getMarketModel(int id) {
		return models.get(id);
	}
	
	public MarketModel getModelByMarket(int mktID) {
		return models.get(marketToModel.get(mktID));
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
	 * Returns true if agent with the specified ID is a background agent, i.e.
	 * not a player in one of the market models.
	 * 
	 * @param id
	 * @return
	 */
	public boolean isBackgroundAgent(int id) {
		return !roleAgentIDs.contains(id);
	}
	
	/**
	 * Given a market ID, find the markets in other markets that are linked.
	 * Results includes the current market ID.
	 * 
	 * @param id
	 * @return
	 */
	public ArrayList<Integer> getLinkedMarkets(int mktID) {
		ArrayList<Integer> ids = new ArrayList<Integer>();
		int idx = modelToMarketList.get(primaryModel.getID()).indexOf(mktID);
		
		// need to cycle through all models to determine the correct index?
		for (Map.Entry<Integer,ArrayList<Integer>> entry : modelToMarketList.entrySet()) {
			ArrayList<Integer> mktIDs = entry.getValue();
			
			if (idx < mktIDs.size()) {
				// add the ID at the given index
				ids.add(mktIDs.get(idx));
			} else if (mktIDs.size() == 1) {
				// always add the centralized market
				ids.add(mktIDs.get(0));
			}
		}
		return ids;
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
	
	
	public PQTransaction getTransaction(int id) {
		return transData.get(id);
	}
	

	public HashMap<Integer,PQTransaction> getAllTrans() {
		return transData;
	}
	
	/**
	 * Get transactions for only the given market(s).
	 * @param mktID
	 * @return
	 */
	public HashMap<Integer,PQTransaction> getTrans(ArrayList<Integer> mktIDs) {
		HashMap<Integer,PQTransaction> trans = new HashMap<Integer,PQTransaction>();
		for (Map.Entry<Integer,PQTransaction> entry : transData.entrySet()) {
			if (mktIDs.contains(entry.getValue().marketID))
				trans.put(entry.getKey(), entry.getValue());
		}
		return trans;
	}
	
	/**
	 * @param id
	 * @return
	 */
	public PQBid getBid(int id) {
		return bidData.get(id);
	}

	
	/**
	 * @return list of expirations for all ZI agent bids (only ZIAgent limit orders expire).
	 */
	public ArrayList<TimeStamp> getExpirations() {
		ArrayList<TimeStamp> exps = new ArrayList<TimeStamp>();
		for (Iterator<Integer> ag = getAgentIDs().iterator(); ag.hasNext(); ) {
			int id = ag.next();
			if (agents.get(id) instanceof ZIAgent) {
				exps.add(new TimeStamp(((ZIAgent) agents.get(id)).getExpiration()));
			}
		}
		return exps;
	}
	
	
	/**
	 * @return list of actual private values of all agents
	 */
	public ArrayList<Price> getPrivateValues() {
		ArrayList<Price> pvs = new ArrayList<Price>(numAgents);
		for (Iterator<Integer> ag = getAgentIDs().iterator(); ag.hasNext(); ) {
			int val = agents.get(ag.next()).getPrivateValue();
			// PV will be negative if it doesn't exist for the agent; only add positive PVs
			if (val >= 0) {
				pvs.add(new Price(val));
			}
		}
		return pvs;
	}
	
	/**
	 * Gets the realized profit of all background agents.
	 * 
	 * @return hash map of background agent profits, hashed by agent ID
	 */
	public HashMap<Integer,Integer> getAllProfit() {
		HashMap<Integer,Integer> allProfit = new HashMap<Integer,Integer>();
		for (Iterator<Integer> it = getAgentIDs().iterator(); it.hasNext(); ) {
			int id = it.next();
			if (isBackgroundAgent(id)) {
				allProfit.put(id, getAgent(id).getRealizedProfit());
			}
		}
		return allProfit;
	}
	
	/**
	 * Iterates through all transactions and sums up surplus for all agents.
	 * CS = PV - p, PS = p - PV
	 * 
	 * @param ids 		market ids to check
	 * @return hash map of background agent surplus, hashed by agent ID
	 */
	public HashMap<Integer,Integer> getSurplus(ArrayList<Integer> ids) {
		
		HashMap<Integer,Integer> allSurplus = new HashMap<Integer,Integer>();
		
		for (Map.Entry<Integer,PQTransaction> trans : transData.entrySet()) {
			PQTransaction t = trans.getValue();
			
			if (ids.contains(t.marketID)) {
				Agent buyer = agents.get(t.buyerID);
				Agent seller = agents.get(t.sellerID);
				
				// Check that PV is defined & that it is a background agent
				if (buyer.getPrivateValue() != -1 && isBackgroundAgent(buyer.getID())) {
					int surplus = 0;
					if (allSurplus.containsKey(buyer.getID())) {
						surplus = allSurplus.get(buyer.getID());					
					}
					allSurplus.put(buyer.getID(),
							surplus + buyer.getPrivateValue() - t.price.getPrice());		
				}
				if (seller.getPrivateValue() != -1 && isBackgroundAgent(seller.getID())) {
					int surplus = 0;
					if (allSurplus.containsKey(seller.getID())) {
						surplus = allSurplus.get(seller.getID());
					}
					allSurplus.put(seller.getID(),
							surplus + t.price.getPrice() - seller.getPrivateValue());		
				}
			}
		}
		return allSurplus;
	}
	
	/**
	 * @return list of all transaction IDs
	 */
	public ArrayList<Integer> getTransactionIDs() {
		return new ArrayList<Integer>(transData.keySet());
	}

	public void addAgent(Agent ag) {
		agents.put(ag.getID(), ag);
	}
	
	public void addMarket(Market mkt) {
		markets.put(mkt.getID(), mkt);
	}

	public void addModel(MarketModel mdl) {
		int id = mdl.getID();
		models.put(id, mdl);
		modelIDs.add(id);
	}
	
	public void addTransaction(PQTransaction tr) {
		int id = transIDSequence.increment();
		tr.transID = id;
		transData.put(id, tr);
	}

	public void addQuote(int mktID, Quote q) {
		quoteData.put(mktID, q);
	}
	
	public void addBid(PQBid b) {
		bidData.put(b.getBidID(), b);
	}
	
	/**
	 * Add bid-ask spread value to the HashMap containers.
	 * 
	 * @param mktID
	 * @param ts 
	 * @param spread
	 */
	public void addSpread(int mktID, TimeStamp ts, int spread) {
		if (marketSpread.get(mktID) != null) {
			marketSpread.get(mktID).put(ts, spread);
		} else {
			HashMap<TimeStamp,Integer> tmp = new HashMap<TimeStamp,Integer>();
			tmp.put(ts, spread);
			marketSpread.put(mktID, tmp);
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
		if (NBBOSpread.get(modelID) != null) {
			NBBOSpread.get(modelID).put(ts, spread);
		} else {
			HashMap<TimeStamp,Integer> tmp = new HashMap<TimeStamp,Integer>();
			tmp.put(ts, spread);
			NBBOSpread.put(modelID, tmp);
		}
	}	
	
	
	/**
	 * Add depth (number of orders waiting to be fulfilled) to the the container.
	 * 
	 * @param mktID
	 * @param ts
	 * @param depth
	 */
	public void addDepth(int mktID, TimeStamp ts, int depth) {
		if (mktID >= 0) {
			System.err.print("ERROR: mktID must be < 0");
		} else {
			if (marketDepth.get(mktID) != null) {
				marketDepth.get(mktID).put(ts, depth);
			} else {
				HashMap<TimeStamp,Integer> tmp = new HashMap<TimeStamp,Integer>();
				tmp.put(ts, depth);
				marketDepth.put(mktID, tmp);
			}
		}
	}
	
	/**
	 * Add bid submission time to the hashmap container.
	 * @param bidID
	 * @param ts
	 */
	public void addSubmissionTime(int bidID, TimeStamp ts) {
		submissionTime.put(bidID, ts);
	}
	
	/**
	 * Add bid execution time to the hashmap container.
	 * @param bidID
	 * @param ts
	 */
	public void addExecutionTime(int bidID, TimeStamp ts) {
		executionTime.put(bidID, ts);
	}
	
	/**
	 * Set up arrival times generation for background agents.
	 */
	public void backgroundArrivalTimes() {
		arrivalTimeGenerator = new ArrivalTime(new TimeStamp(0), arrivalRate);
	}
	
	/**
	 * Set up private value generation for background agents.
	 */
	public void backgroundPrivateValues() {
		processGenerator = new PrivateValue(kappa, meanPV, shockVar);	
	}
	
	/**
	 * @return next generated arrival time
	 */
	public TimeStamp nextArrival() {
		if (arrivalTimeGenerator == null) {
			return null;
		}
		return arrivalTimeGenerator.next();
	}
	
	/**
	 * @return next generated private value
	 */
	public int nextPrivateValue() {
		if (processGenerator == null) {
			return 0;
		}
		return processGenerator.next();
	}
	
	/**
	 * @return list of all arrival times
	 */
	public ArrayList<TimeStamp> getArrivalTimes() {
		return arrivalTimeGenerator.getArrivalTimes();
	}
	
	/**
	 * @return list of all intervals
	 */
	public ArrayList<TimeStamp> getIntervals() {
		return arrivalTimeGenerator.getIntervals();
	}
	
	/**
	 * @return list of all private values generated in the stochastic process
	 */
	public ArrayList<Price> getPrivateValueProcess() {
		return processGenerator.getPrivateValueProcess();
	}	
}

