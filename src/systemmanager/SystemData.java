package systemmanager;

import event.*;
import entity.*;
import market.*;

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
 * Note: For the lists containing time series, a new value is added to each list
 * when the value has a chance to change (generally after a clear event in a market).
 * This applies to the execution speed and market depth/spread. For the NBBO spread,
 * a new value is added whenever the NBBO is updated (even if it is a repeated value).
 * 
 * @author ewah
 */
public class SystemData {

	public int obsNum;									// observation number
	
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
	
	// Does not include the central market
	public HashMap<String,Integer> numMarketType;		// hashed by type, gives # of that type
	public HashMap<String,Integer> numAgentType;
	public int numMarkets;
	public int numAgents;
	public ArrayList<Integer> roleAgentIDs;				// IDs of agents in a role
	
	// Parameters set by specification file
	public TimeStamp simLength;
	public int tickSize;
	public TimeStamp clearFreq;
	public TimeStamp nbboLatency;
	public double arrivalRate;
	public int meanPV;
	public double kappa;
	public double shockVar;
	public double expireRate;
	public int bidRange;
	public double privateValueVar;				// agent variance from PV random process
	
	// Two-market model containers
	public HashMap<Integer, ArrayList<Market>> twoMarketModels;	// hashed by 2M model ID
	
	// Central market type; if invalid type, no central market will be created
	public String centralMarketFlag;
	public HashMap<Integer,Market> centralMarkets;
	
	// Variables of time series for observation file
	public HashMap<Integer,HashMap<TimeStamp,Integer>> marketDepth;		// hashed by market ID
	public HashMap<Integer,HashMap<TimeStamp,Integer>> marketSpread;	// hashed by market ID
	public HashMap<TimeStamp,Integer> NBBOSpread;				// time series of NBBO spreads
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
		numMarketType = new HashMap<String,Integer>();
		numAgentType = new HashMap<String,Integer>();
		numAgents = 0;
		numMarkets = 0;
		roleAgentIDs = new ArrayList<Integer>();
		transIDSequence = new Sequence(0);
	
		// Initialize containers for observations/features
		marketDepth = new HashMap<Integer,HashMap<TimeStamp,Integer>>();
		marketSpread = new HashMap<Integer,HashMap<TimeStamp,Integer>>();
		NBBOSpread = new HashMap<TimeStamp,Integer>();
		executionTime = new HashMap<Integer,TimeStamp>();
		submissionTime = new HashMap<Integer,TimeStamp>();
		
		// Initialize containers for market configurations
		twoMarketModels = new HashMap<Integer,ArrayList<Market>>();
		centralMarkets = new HashMap<Integer,Market>();
	}


	public HashMap<Integer,Bid> getBids(int marketID) {
		return markets.get(marketID).getBids();
	}
	
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
	
	public HashMap<Integer,Agent> getAgents() {
		return agents;
	}

	public HashMap<Integer,Market> getMarkets() {
		return markets;
	}

	public ArrayList<Integer> getAgentIDs() {
		return new ArrayList<Integer>(agents.keySet());
	}
	
	public ArrayList<Integer> getMarketIDs() {
		return new ArrayList<Integer>(markets.keySet());
	}
	
	public Agent getAgent(int id) {
		return agents.get(id);
	}

	public Market getMarket(int id) {
		if (!centralMarkets.containsKey(id)) {
			return markets.get(id);
		} else {
			return centralMarkets.get(id);
		}
	}

	public ArrayList<Integer> getCentralMarketIDs() {
		return new ArrayList<Integer>(centralMarkets.keySet());
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
	
	public PQBid getBid(int id) {
		return bidData.get(id);
	}
	
	/**
	 * @return true if central markets present, false otherwise
	 */
	public boolean useCentralMarket() {
		if (centralMarketFlag != null) {
			if (centralMarketFlag.equals("on")) {
				return true;
			} else {
				return false;
			}
		}
		return false;	
	}
	
	
	/**
	 * Returns the type of the central market with the given market ID.
	 * 
	 * @param mktID
	 * @return
	 */
	public String getCentralMarketType(int mktID) {
		if (!this.getCentralMarketIDs().contains(mktID))
			return null;
		
		if (getMarket(mktID) instanceof CDAMarket) {
			return new String("CDA");
		}
		if (getMarket(mktID) instanceof CallMarket) {
			return new String("CALL");
		}
		return null;
	}
	

	
	/**
	 * @return list of lifetime of all background trader bids
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
			// PV will be negative if it does not exist for the agent, so only add positive PVs
			if (val >= 0) {
				pvs.add(new Price(val));
			}
		}
		return pvs;
	}
	
	/**
	 * Read in parameters from the env.properties configuration file.
	 * 
	 * @param p
	 */
	public void readEnvProps(Properties p) {
		simLength = new TimeStamp(Long.parseLong(p.getProperty("simLength")));
		nbboLatency = new TimeStamp(Long.parseLong(p.getProperty("nbboLatency")));
		clearFreq = new TimeStamp(Long.parseLong(p.getProperty("clearLatency")));
		tickSize = Integer.parseInt(p.getProperty("tickSize"));
		kappa = Double.parseDouble(p.getProperty("kappa"));
		arrivalRate = Double.parseDouble(p.getProperty("arrivalRate"));
		meanPV = Integer.parseInt(p.getProperty("meanPV"));
		shockVar = Double.parseDouble(p.getProperty("shockVar"));
		expireRate = Double.parseDouble(p.getProperty("expireRate"));
		bidRange = Integer.parseInt(p.getProperty("bidRange"));
	}
	
	
	/**
	 * Gets the realized profit of all BackgroundAgent objects.
	 * 
	 * @return hashmap of background agent profits, hashed by agent ID
	 */
	public HashMap<Integer,Integer> getAllProfit() {
		HashMap<Integer,Integer> allProfit = new HashMap<Integer,Integer>();
		for (Iterator<Integer> it = this.getAgentIDs().iterator(); it.hasNext(); ) {
			int id = it.next();
			if (this.getAgent(id) instanceof ZIAgent) {
				allProfit.put(id, this.getAgent(id).getRealizedProfit());
			}
		}
		return allProfit;
	}
	
	/**
	 * Iterates through all transactions and sums up surplus for all agents.
	 * CS = PV - p, PS = p - PV
	 * 
	 * @param ids 		market ids to check
	 * @return hashmap of background agent surplus, hashed by agent ID
	 */
	public HashMap<Integer,Integer> getSurplus(ArrayList<Integer> ids) {
		
		HashMap<Integer,Integer> allSurplus = new HashMap<Integer,Integer>();
		
		for (Map.Entry<Integer,PQTransaction> trans : transData.entrySet()) {
			PQTransaction t = trans.getValue();
			
			if (ids.contains(t.marketID)) {
				Agent buyer = agents.get(t.buyerID);
				Agent seller = agents.get(t.sellerID);
				
				// Check that PV is defined for the agent & that it's a background agent
				if (buyer.getPrivateValue() != -1 && buyer instanceof ZIAgent) {
					int surplus = 0;
					if (allSurplus.containsKey(buyer.getID())) {
						surplus = allSurplus.get(buyer.getID());					
					}
					allSurplus.put(buyer.getID(), surplus + buyer.getPrivateValue() - t.price.getPrice());		
				}
				if (seller.getPrivateValue() != -1 && seller instanceof ZIAgent) {
					int surplus = 0;
					if (allSurplus.containsKey(seller.getID())) {
						surplus = allSurplus.get(seller.getID());					
					}
					allSurplus.put(seller.getID(), surplus + t.price.getPrice() - seller.getPrivateValue());		
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
	 * Add bid-ask spread value to the HashMap containers. If the mktID is 0, then
	 * inserts as NBBO spread.
	 * 
	 * @param mktID
	 * @param ts 
	 * @param spread
	 */
	public void addSpread(int mktID, TimeStamp ts, int spread) {
		// mktID 0 indicates NBBO spread information
		if (mktID == 0) {
			NBBOSpread.put(ts, spread);
		} else {
			if (marketSpread.get(mktID) != null) {
				marketSpread.get(mktID).put(ts, spread);
			} else {
				HashMap<TimeStamp,Integer> tmp = new HashMap<TimeStamp,Integer>();
				tmp.put(ts, spread);
				marketSpread.put(mktID, tmp);
			}
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

