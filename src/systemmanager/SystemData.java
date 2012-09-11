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
 * @author ewah
 */
public class SystemData {

	// Market information
	public HashMap<Integer,PQTransaction> transData;	// hashed by transaction ID
	public HashMap<Integer,Quote> quoteData;			// hashed by market ID
	public HashMap<Integer,Agent> agents;				// agents hashed by ID
	public HashMap<Integer,Market> markets;				// markets hashed by ID
	public Quoter quoter;
	
	public ArrayList<String> marketTypes;
	public ArrayList<String> agentTypes;
	public HashMap<String,Integer> numMarketType;		// hashed by type, gives # of that type
	public HashMap<String,Integer> numAgentType;
	public int numMarkets;
	public int numAgents;
	
	// Parameters set by config file
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
	public double valueVar;			// agent variance from PV random process
	
	// Internal variables
	private Sequence transIDSequence;
	private ArrivalTime arrivalTimeGenerator;
	private PrivateValue privateValueGenerator;
	
	/**
	 * Constructor
	 */
	public SystemData() {
		transData = new HashMap<Integer,PQTransaction>();
		quoteData = new HashMap<Integer,Quote>();
		agents = new HashMap<Integer,Agent>();
		markets = new HashMap<Integer,Market>();
		numMarketType = new HashMap<String,Integer>();
		numAgentType = new HashMap<String,Integer>();
		numAgents = 0;
		numMarkets = 0;
		transIDSequence = new Sequence(0);
	}

	
	// Access variables

	public HashMap<Integer,Bid> getBids(int marketID) {
		return markets.get(marketID).getBids();
	}

	public HashMap<Integer,PQTransaction> getTrans() {
		return transData;
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
		return markets.get(id);
	}

	public PQTransaction getTransaction(int id) {
		return transData.get(id);
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
		privateValueGenerator = new PrivateValue(kappa, meanPV, shockVar, tickSize);	
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
		if (privateValueGenerator == null) {
			return 0;
		}
		return privateValueGenerator.next();
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
	 * @return list of all private values
	 */
	public ArrayList<Price> getPrivateValues() {
		return privateValueGenerator.getPrivateValues();
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
		arrivalRate = Double.parseDouble(p.getProperty("arrivalRate"));
		meanPV = Integer.parseInt(p.getProperty("meanPV"));
		shockVar = Double.parseDouble(p.getProperty("shockVar"));
		expireRate = Double.parseDouble(p.getProperty("expireRate"));
		bidRange = Integer.parseInt(p.getProperty("bidRange"));
	}
	
	
	/**
	 * Iterates through all transactions and sums up surplus for all agents.
	 * CS = PV - p, PS = p - PV
	 * 
	 * @return surplus hashmap, hashed by agent ID
	 */
	public HashMap<Integer,Integer> getAllSurplus() {
		
		HashMap<Integer,Integer> allSurplus = new HashMap<Integer,Integer>();
		for (Map.Entry<Integer,PQTransaction> trans : transData.entrySet()) {
			PQTransaction t = trans.getValue();
			
			Agent buyer = agents.get(t.buyerID);
			Agent seller = agents.get(t.sellerID);
			
			if (buyer.getPrivateValue() != -1) {
				int surplus = 0;
				if (allSurplus.containsKey(buyer.getID())) {
					surplus = allSurplus.get(buyer.getID());					
				}
				allSurplus.put(buyer.getID(), surplus + buyer.getPrivateValue() - t.price.getPrice());		
			}
			if (seller.getPrivateValue() != -1) {
				int surplus = 0;
				if (allSurplus.containsKey(seller.getID())) {
					surplus = allSurplus.get(seller.getID());					
				}
				allSurplus.put(seller.getID(), surplus + t.price.getPrice() - seller.getPrivateValue());		
			}
		}
		return allSurplus;
	}
	
	

	/**
	 * @return list of all transaction ids
	 */
	public ArrayList<Integer> getTransactionIDs() {
		return new ArrayList<Integer>(transData.keySet());
	}
	
	
	
	
//	/**
//	 * Gets transaction IDs for all transaction after earliestTransID.
//	 * 
//	 * @param earliestTransID
//	 * @param agentID
//	 * @return
//	 */
//	public ArrayList<Integer> getTransactions(int earliestTransID, int agentID) {
//		//"<transIDs>"
//		ArrayList<Integer> transIDs = new ArrayList<Integer>();
//		
//		Map clone = (Map) transData.clone();
//		Set td = clone.entrySet();
//		for (Iterator i = td.iterator(); i.hasNext();) {
//			Map.Entry me = (Map.Entry) i.next();
//
//			PQTransaction trans = (PQTransaction) me.getValue();
//			int transID = (Integer) me.getKey();
//			
//			if (transID > earliestTransID) {
//
//				if (trans == null) {
//					//getTransactions: transID value is null (transID): "
//					return null;
//				}
//				if ((agentID == trans.buyerID) || (agentID == trans.sellerID)) {
//					transIDs.add(transID);
//				}
//			}
//		}
//		return transIDs;
//	}

//	/**
//	 * Gets initial transaction ID for specified agent.
//	 * @param agentID
//	 * @return
//	 */
//	public Integer getInitialTransaction(int agentID) {
//		// "<initialLastTransID>";
//		Set td = transData.entrySet();
//
//		// DO NOT change the -1 initial for minTransID.  It is used as a
//		// return value in case transactions are not found.
//		int minTransID = -1;
//		int cnt = 0;
//		for (Iterator i = td.iterator(); i.hasNext();) {
//			Map.Entry me = (Map.Entry) i.next();
//
//			PQTransaction trans = (PQTransaction) me.getValue();
//			int transID = (Integer) me.getKey();
//
//			// we only want transactions for the agent, no others.
//			if (trans == null) {
//				return null;
//			}
//
//			if (trans.buyerID == null) {
//				//"SystemCacheData::getTransactions: no buyerID tag");
//				return null;
//			}
//			if (trans.sellerID == null) {
//				//"SystemCacheData::getTransactions: no sellerID tag");
//				return null;
//			}
//
//			// Check if match our calling agent's ID
//			if ((agentID == trans.buyerID) || (agentID == trans.sellerID)) {
//				if (cnt == 0) {
//					minTransID = transID;
//					cnt++;
//				}
//				if (transID < minTransID)
//					minTransID = transID;
//			}
//		}
//		// At this point, we either found one, in which case transID is set to
//		// the earliestTransID, or there are no transactions, so earliestTransID
//		// is set to -1 (its initial value).
//		return minTransID;
//	}
//
//	/**
//	 * Get transactions for a given agent.
//	 * 
//	 * @param agentID
//	 * @param type		'b' if buyer, 's' if seller
//	 * @return
//	 */
//	public ArrayList<Integer> getAgentTransactions(int agentID, char type) {
//		//		HashMap<Integer,PQTransaction> transMap = new HashMap<Integer,PQTransaction>();
//		ArrayList<Integer> transIDs = new ArrayList<Integer>();
//
//		Set td = transData.entrySet();
//		for (Iterator i = td.iterator(); i.hasNext();) {
//			Map.Entry me = (Map.Entry) i.next();
//
//			PQTransaction trans = (PQTransaction) me.getValue();
//			int transID = (Integer) me.getKey();
//
//			if (trans == null) {
//				//SystemCacheData::getTransactions: transID value is null
//				return null;
//			}
//			if (trans.buyerID == null) {
//				//"SystemCacheData::getAgentTrans: no buyerID tag");
//				return null;
//			}
//			if (trans.sellerID == null) {
//				//"SystemCacheData::getAgentTrans: no sellerID tag");
//				return null;
//			}
//
//			int id = 0;
//			if (type == 'b') {
//				id = trans.buyerID;
//			} else if (type == 's') {
//				id = trans.sellerID;
//			}
//
//			if (agentID == id) {
//				if (trans.price == null) {
//					//"SystemCacheData::getAgentTrans: no price tag");
//					return null;
//				}
//				if (trans.quantity == null) {
//					//SystemCacheData::getTransactions: no quantity tag");
//					return null;
//				}
//				if (trans.marketID == null) {
//					//"SystemCacheData::getAgentTrans: no auction ID tag");
//					return null;
//				}
//				if (trans.timestamp == null) {
//					//SystemCacheData::getAgentTrans: no timestamp tag");
//					return null;
//				}
//				//				transMap.put((Integer) me.getKey(), trans);
//				transIDs.add((Integer) me.getKey());
//			}
//		}
//		//		return transMap;
//		return transIDs;
//	}


	// Set variables

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
	

	public Quote putQuote(Market market,
						ArrayList<Bid> globalQuote,
						HashMap<Integer,Integer> agentQuotes, 
						Price lastClearPrice,
						TimeStamp lastQuoteTime,
						TimeStamp lastClearTime,
						Integer status) {

		Integer agentID = null;
		int ret;

//		Price lastBid = ((PQBid) globalQuote.get(0)).bidArray[0].price;
//		Bid lastAsk = globalQuote.get(1);
//
//		Quote q = new Quote(market.getID(), lastAsk, lastBid, lastClearPrice, lastQuoteTime, lastClearTime);
//
//
//		for (Enumeration Enum = agentXMLquotes.keys(); Enum.hasMoreElements();) {
//			agentID = (Integer) Enum.nextElement();
//			if (sendToCache(s + "<agentID>" + agentID + "</agentID>" + (String) agentXMLquotes.get(agentID) + "</putQuote>") != 0)
//				ret = -1;
//		}
		return null;
	}
	
}
