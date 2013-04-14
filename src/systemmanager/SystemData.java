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
	
	// Model information
	public HashMap<Integer,MarketModel> models;					// models hashed by ID
	public HashMap<Integer,ArrayList<Integer>> modelToMarketList;	// hashed by model ID
	public HashMap<Integer,Integer> marketIDModelIDMap;			// hashed by market ID
	public MarketModel primaryModel;
	public String primaryModelDesc;						// description of primary model
	
	// Market information
	public HashMap<Integer,PQBid> bidData;				// all bids ever, hashed by bid ID
	public HashMap<Integer,PQTransaction> transData;		// hashed by transaction ID
	public HashMap<Integer,Quote> quoteData;			// hashed by market ID
	public HashMap<Integer,Agent> agents;				// agents hashed by ID
	public HashMap<Integer,Market> markets;				// markets hashed by ID
	public ArrayList<Integer> modelIDs;

	private SIP sip;
	private Sequence transIDSequence;	
	private ArrivalTime arrivalTimeGenerator;
	private FundamentalValue fundamentalGenerator;
	
	// hashed by type, gives # of that type
	public HashMap<String,Integer> numModelType;
	public HashMap<String,Integer> numMarketType;
	public HashMap<String,Integer> numAgentType;
	
	public int numMarkets;
	public int numAgents;
	
	// Parameters set by specification file
	public TimeStamp simLength;
	public int tickSize;
	public TimeStamp centralCallClearFreq;
	public TimeStamp nbboLatency;
	public double arrivalRate;
	public int meanValue;
	public double kappa;
	public double shockVar;
	public int bidRange;
	public double privateValueVar;				// agent variance from PV random process
	public int marketmaker_numRungs;			// market maker number of rungs (per side)
	public int marketmaker_rungSize;			// market maker rung size
	public int marketmaker_sleepTime;			// market maker sleep time
	
	// Variables of time series for observation file
	public HashMap<Integer,HashMap<TimeStamp,Double>> marketDepth;		// hashed by market ID
	public HashMap<Integer,HashMap<TimeStamp,Double>> marketSpread;		// hashed by market ID
	public HashMap<Integer,HashMap<TimeStamp,Double>> NBBOSpread;		// hashed by model ID, time series
	public HashMap<Integer,HashMap<TimeStamp,Double>> marketMidQuote;	// hashed by market ID
	public HashMap<Integer,TimeStamp> timeToExecution;		 	// hashed by bid ID
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
		modelIDs = new ArrayList<Integer>();
		transIDSequence = new Sequence(0);
		modelToMarketList = new HashMap<Integer,ArrayList<Integer>>();
		primaryModel = null;
		marketIDModelIDMap = new HashMap<Integer,Integer>();
	
		// Initialize containers for observations/features
		marketDepth = new HashMap<Integer,HashMap<TimeStamp,Double>>();
		marketSpread = new HashMap<Integer,HashMap<TimeStamp,Double>>();
		NBBOSpread = new HashMap<Integer,HashMap<TimeStamp,Double>>();
		marketMidQuote = new HashMap<Integer,HashMap<TimeStamp,Double>>();
		timeToExecution = new HashMap<Integer,TimeStamp>();
		submissionTime = new HashMap<Integer,TimeStamp>();
	}
	
	
	/***********************************
	 * Accessor methods
	 *
	 **********************************/
	
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
	
	
	public int getNumAgentType(String type) {
		return numAgentType.get(type);
	}
	
	/**
	 * Returns true if agent with the specified ID is a background agent, i.e.
	 * not a player in one of the market models.
	 * 
	 * @param id
	 * @return
	 */
	public boolean isBackgroundAgent(int id) {
		return !(agents.get(id) instanceof HFTAgent);
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
		return bidData.get(id);
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
	
	/**
	 * Get transactions for only the given model.
	 * 
	 * @param modelID 
	 * @return
	 */
	public HashMap<Integer,PQTransaction> getTrans(int modelID) {
		ArrayList<Integer> mktIDs = getModel(modelID).getMarketIDs();
		
		HashMap<Integer,PQTransaction> trans = new HashMap<Integer,PQTransaction>();
		for (Map.Entry<Integer,PQTransaction> entry : transData.entrySet()) {
			if (mktIDs.contains(entry.getValue().marketID)) {
				trans.put(entry.getKey(), entry.getValue());
			}
		}
		return trans;
	}
	
	
	/**
	 * Modifies transactions for the given model by setting TimeStamp to be constant, 
	 * and using agent log IDs rather than agent IDs for indicating buyer or seller.
	 * Also sets constant price (because surplus will be same regardless of price), as
	 * well as constant bid IDs since different bids are submitted to each model.
	 * 
	 * @param modelID
	 * @return
	 */
	public ArrayList<PQTransaction> getComparableTrans(int modelID) {
		ArrayList<Integer> mktIDs = getModel(modelID).getMarketIDs();
				
		ArrayList<PQTransaction> trans = new ArrayList<PQTransaction>();
		for (Map.Entry<Integer,PQTransaction> entry : transData.entrySet()) {
			if (mktIDs.contains(entry.getValue().marketID)) {
				PQTransaction tr = entry.getValue();
				
				// Modify values to be comparable between models
				// - Use logID rather than agentID
				// - Set constant TimeStamp
				// - Set constant marketID
				// - Set constant price
				// - Set constant bid IDs
				// TODO - for now, ignore quantity
				int bID = getAgent(tr.buyerID).getLogID();
				int sID = getAgent(tr.sellerID).getLogID();
				Price p = new Price(0);
				TimeStamp ts = new TimeStamp(-1);
				int mktID = 0;
				int bBidID = 0;
				int sBidID = 0;
				
				PQTransaction trNew = new PQTransaction(tr.quantity, p, bID, sID, bBidID, sBidID, ts, mktID);
				trans.add(trNew);
			}
		}
		return trans;
	}
	
	/**
	 * Modifies all transactions in transData to be comparable between models.
	 * 
	 * @return
	 */
	public ArrayList<PQTransaction> getUniqueComparableTrans() {
		Set<PQTransaction> trans = new HashSet<PQTransaction>();
		
		for (Map.Entry<Integer,PQTransaction> entry : transData.entrySet()) {
			PQTransaction tr = entry.getValue();
			
			// Modify values to be comparable between models
			// - Use logID rather than agentID
			// - Set constant TimeStamp
			// - Set constant marketID
			// - Set constant price
			// - Set constant bid IDs
			// TODO - for now, ignore quantity
			int bID = getAgent(tr.buyerID).getLogID();
			int sID = getAgent(tr.sellerID).getLogID();
			Price p = new Price(0);
			TimeStamp ts = new TimeStamp(-1);
			int mktID = 0;
			int bBidID = 0;
			int sBidID = 0;
			
			PQTransaction trNew = new PQTransaction(tr.quantity, p, bID, sID, bBidID, sBidID, ts, mktID);
			trans.add(trNew);
		}
		ArrayList<PQTransaction> uniqueTrans = new ArrayList<PQTransaction>();
		uniqueTrans.addAll(trans);
		return uniqueTrans;
	}
	
//	/**
//	 * @return list of expirations for all ZI agent bids (only ZIAgent limit orders expire).
//	 */
//	public ArrayList<TimeStamp> getExpirations() {
//		ArrayList<TimeStamp> exps = new ArrayList<TimeStamp>();
//		for (Iterator<Integer> ag = getAgentIDs().iterator(); ag.hasNext(); ) {
//			int id = ag.next();
//			if (agents.get(id) instanceof ZIAgent) {
//				exps.add(new TimeStamp(((ZIAgent) agents.get(id)).getExpiration()));
//			}
//		}
//		return exps;
//	}
	
	
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
	
	
	/***********************************
	 * Surplus computations
	 *
	 **********************************/
	
	/**
	 * Iterates through all transactions and sums up surplus for all agents of a 
	 * specified type. For agents that do not have a private valuation, use their
	 * realized profit instead.
	 * 
	 * CS = PV - p, PS = p - PV
	 * 
	 * @param modelID 		model id to check
	 * @param agentType
	 * @return hash map of agent surplus, hashed by agent ID
	 */
	public HashMap<Integer,Integer> getSurplusForType(int modelID, String agentType) {

		ArrayList<Integer> ids = getModel(modelID).getMarketIDs();
		HashMap<Integer,Integer> allSurplus = new HashMap<Integer,Integer>();
		
		for (Map.Entry<Integer,PQTransaction> trans : getTrans(modelID).entrySet()) {
			PQTransaction t = trans.getValue();
			
			if (ids.contains(t.marketID)) {
				Agent buyer = agents.get(t.buyerID);
				Agent seller = agents.get(t.sellerID);
				
				if (agentType.equals(buyer.getType())) {
					int surplus = 0;
					if (allSurplus.containsKey(buyer.getID())) {
						surplus = allSurplus.get(buyer.getID());
					}
					if (buyer.getPrivateValue() != -1) {
						allSurplus.put(buyer.getID(), surplus + (buyer.getPrivateValue() - t.price.getPrice()));	
					} else {
						allSurplus.put(buyer.getID(), buyer.getRealizedProfit());
					}
				}
				if (agentType.equals(seller.getType())) {
					int surplus = 0;
					if (allSurplus.containsKey(seller.getID())) {
						surplus = allSurplus.get(seller.getID());
					}
					if (seller.getPrivateValue() != -1) {
						allSurplus.put(seller.getID(), surplus + (t.price.getPrice() - seller.getPrivateValue()));
					} else {
						allSurplus.put(seller.getID(), seller.getRealizedProfit());
					}
				}
			}
		}
		return allSurplus;
	}
	
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
					if (buyer.getPrivateValue() != -1) {
						surplus += (buyer.getPrivateValue() - t.price.getPrice());	
					} else {
						surplus = buyer.getRealizedProfit(); // already summed
					}
				}
				if (seller.getID() == agentID) {
					if (seller.getPrivateValue() != -1) {
						surplus += (t.price.getPrice() - seller.getPrivateValue());
					} else {
						surplus = seller.getRealizedProfit(); // already summed
					}
				}
			}
		}
		return surplus;
	}
	
	
	/**
	 * Iterates through all transactions and sums up surplus for all background agents
	 * that have a private valuation.
	 * 
	 * CS = PV - p, PS = p - PV
	 * 
	 * @param modelID 		model id to check
	 * @return hash map of background agent surplus, hashed by agent ID
	 */
	public HashMap<Integer,Integer> getBackgroundSurplus(int modelID) {
		// basically the same as discounted surplus, but with discount 0.0

		ArrayList<Integer> ids = getModel(modelID).getMarketIDs();
		HashMap<Integer,Integer> allSurplus = new HashMap<Integer,Integer>();
		
		for (Map.Entry<Integer,PQTransaction> trans : getTrans(modelID).entrySet()) {
			PQTransaction t = trans.getValue();
			
			if (ids.contains(t.marketID)) {
				Agent buyer = getAgent(t.buyerID);
				Agent seller = getAgent(t.sellerID);
				
				// Check that PV is defined & that it is a background agent
				if (isBackgroundAgent(buyer.getID())) {
					int surplus = 0;
					if (allSurplus.containsKey(buyer.getID())) {
						surplus = allSurplus.get(buyer.getID());
					}
					if (buyer.getPrivateValue() != -1) {
						int rt = getFundamentalAt(t.timestamp).getPrice();
						int val = (buyer.getPrivateValue() + rt) - t.price.getPrice();
						allSurplus.put(buyer.getID(), surplus + val);	
					} else {
						allSurplus.put(buyer.getID(), buyer.getRealizedProfit());	// already summed
					}
				}
				if (isBackgroundAgent(seller.getID())) {
					int surplus = 0;
					if (allSurplus.containsKey(seller.getID())) {
						surplus = allSurplus.get(seller.getID());
					}
					if (seller.getPrivateValue() != -1) {
						int rt = getFundamentalAt(t.timestamp).getPrice();
						int val = t.price.getPrice() - (seller.getPrivateValue() + rt);
						allSurplus.put(seller.getID(), surplus + val);
					} else {
						allSurplus.put(seller.getID(), seller.getRealizedProfit());	// already summed
					}
				}
			}
		}
		return allSurplus;
	}
	
	/**
	 * Iterates through all transactions and sums up discounted surplus for all
	 * background agents. (Note that HFT agent transactions will naturally execute
	 * with zero transaction time, so they do not need to be discounted).
	 * 
	 * Each transaction's surplus is discounted by exp{-rho * T}, where T is the 
	 * execution speed of that transaction.
	 * 
	 * TODO - should the returned hashmap be hashed by transaction ID instead?
	 * 
	 * CS = PV - p, PS = p - PV
	 * 
	 * @param modelID 		model id to check
	 * @param rho			discount factor
	 * @return discounted surplus, hashed by (background) agent ID
	 */
	public HashMap<Integer,Double> getDiscountedSurplus(int modelID, double rho) {
		ArrayList<Integer> ids = getModel(modelID).getMarketIDs();
		HashMap<Integer,Double> discSurplus = new HashMap<Integer,Double>();
		
		for (Map.Entry<Integer,PQTransaction> trans : getTrans(modelID).entrySet()) {
			PQTransaction t = trans.getValue();
			
			if (ids.contains(t.marketID)) {
				Agent buyer = agents.get(t.buyerID);
				Agent seller = agents.get(t.sellerID);
				TimeStamp buyTime = timeToExecution.get(t.buyBidID);
				TimeStamp sellTime = timeToExecution.get(t.sellBidID);
				
				// Check that PV is defined & that it is a background agent
				if (isBackgroundAgent(buyer.getID())) {
					double surplus = 0;
					if (discSurplus.containsKey(buyer.getID())) {
						surplus = discSurplus.get(buyer.getID());
					}
					if (buyer.getPrivateValue() != -1) {
						double cs = (buyer.getPrivateValue() - t.price.getPrice());
//						System.out.println(modelID + ": " + t + " cs=" + cs + ", buyTime=" + buyTime);
						discSurplus.put(buyer.getID(), surplus + Math.exp(-rho * buyTime.longValue()) * cs);
					} else {
						discSurplus.put(buyer.getID(), Math.exp(-rho * buyTime.longValue()) * buyer.getRealizedProfit());
					}
				}
				if (isBackgroundAgent(seller.getID())) {
					double surplus = 0;
					if (discSurplus.containsKey(seller.getID())) {
						surplus = discSurplus.get(seller.getID());
					}
					if (seller.getPrivateValue() != -1) {
						double ps = (t.price.getPrice() - seller.getPrivateValue());
//						System.out.println(modelID + ": " + t + " ps=" + ps + ", sellTime=" + sellTime);
						discSurplus.put(seller.getID(), surplus + Math.exp(-rho * sellTime.longValue()) * ps);
					} else {
						discSurplus.put(seller.getID(), Math.exp(-rho * sellTime.longValue()) * seller.getRealizedProfit());
					}
				}
			}
		}
		return discSurplus;
	}
	
	/**
	 * @return list of all transaction IDs
	 */
	public ArrayList<Integer> getTransactionIDs(int modelID) {
		return new ArrayList<Integer>(getTrans(modelID).keySet());
	}
	
	/***********************************
	 * Add agent, market, model, spread, etc.
	 *
	 **********************************/
	
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
	
	public void setSIP(SIP sip) {
		this.sip = sip;
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
		
//		System.out.println("PRE marketSpread: " + marketSpread.get(mktID));
//		Market mkt = markets.get(mktID);
		if (marketSpread.get(mktID) != null) {
			marketSpread.get(mktID).put(ts, (double) spread);
//			System.out.println(ts + " " + mkt.toString() + ": " + spread + " added.");
//			System.out.println("POST marketSpread: " + marketSpread.get(mktID));
		} else {
			HashMap<TimeStamp,Double> tmp = new HashMap<TimeStamp,Double>();
			tmp.put(ts, (double) spread);
			marketSpread.put(mktID, tmp);
//			System.out.println(ts + " " + mkt.toString() + ": " + spread + " added.");
//			System.out.println("POST marketSpread: " + marketSpread.get(mktID));
		}
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
		if (NBBOSpread.get(modelID) != null) {
			NBBOSpread.get(modelID).put(ts, (double)spread);
		} else {
			HashMap<TimeStamp,Double> tmp = new HashMap<TimeStamp,Double>();
			tmp.put(ts, (double) spread);
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
		if (marketDepth.get(mktID) != null) {
			marketDepth.get(mktID).put(ts, (double) depth);
		} else {
			HashMap<TimeStamp,Double> tmp = new HashMap<TimeStamp,Double>();
			tmp.put(ts, (double) depth);
			marketDepth.put(mktID, tmp);
		}
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
	
	
	
	
	/***********************************
	 * Arrival times & global fundamental generation
	 *
	 **********************************/
	
	/**
	 * Set up arrival times generation for background agents.
	 */
	public void arrivalTimes() {
		arrivalTimeGenerator = new ArrivalTime(new TimeStamp(0), arrivalRate);
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
	 * @return time series of the global fundamental generated by the stochastic process
	 */
	public ArrayList<Price> getFundamentalValueProcess() {
		return fundamentalGenerator.getProcess();
	}
}

