package entity;

import event.*;
import activity.*;
import activity.market.*;
import systemmanager.*;

import java.util.Iterator;
import java.util.Vector;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.TreeSet;
import java.io.IOException;

/**
 * Base class for all agents.
 * 
 * @author ewah
 */
public abstract class Agent extends Entity {

	public static final int INF_PRICE = 99999;

	// Market information (hashed by market ID, as ID may be negative)
	protected ArrayList<Integer> marketIDs;
	protected HashMap<Integer,Price> bidPrice;
	protected HashMap<Integer,Price> askPrice;
	protected HashMap<Integer,Bid> currentBid;
	protected HashMap<Integer,TimeStamp> lastQuoteTime;
	protected HashMap<Integer,TimeStamp> nextQuoteTime;
	protected HashMap<Integer,TimeStamp> lastClearTime;
	protected HashMap<Integer,Double> initBid; // or should this be Bid?
	protected HashMap<Integer,Double> initAsk;
	protected HashMap<Integer,Vector<Quote>> quotes;
	protected HashMap<Integer,Double> prevBid;
	protected HashMap<Integer,Double> prevAsk;

	// Agent parameters
	protected int positionLimit;
	protected double tickSize;
	protected int latency;				// how frequently agentStrategy is called
	protected String agentType;
	
	protected Integer lastTransID;		// ID of the last transaction fetched
	
	// Tracking cash flow
	protected double cashBalance;
	protected double positionSize;
	
	// Execution speed (hashed by market ID)
	protected HashMap<Integer,ArrayDeque<Integer>> matchingInterval;

	/**
	 * Constructor
	 * @param agentID
	 */
	public Agent(int agentID, SystemData d) {
		super(agentID, d);

		// initialize all containers
		marketIDs = new ArrayList<Integer>();
		bidPrice = new HashMap<Integer,Price>();
		askPrice = new HashMap<Integer,Price>();
		currentBid = new HashMap<Integer,Bid>();
		lastQuoteTime = new HashMap<Integer,TimeStamp>();
		nextQuoteTime = new HashMap<Integer,TimeStamp>();
		lastClearTime = new HashMap<Integer,TimeStamp>();
		quotes = new HashMap<Integer,Vector<Quote>>();
		
		initBid = new HashMap<Integer,Double>();
		initAsk = new HashMap<Integer,Double>();
		prevBid = new HashMap<Integer,Double>();
		prevAsk = new HashMap<Integer,Double>();
		
		try {
			log = new Log(Log.INFO, ".", agentType + agentID + ".log", true);
		} catch (IOException e) {
		}
	}

	/**
	 * Clears all the agent's data structures.
	 */
	protected void clearAll() {
		marketIDs.clear();
		bidPrice.clear();
		askPrice.clear();
		currentBid.clear();
		lastQuoteTime.clear();
		nextQuoteTime.clear();
		lastClearTime.clear();
		quotes.clear();
		
		initBid.clear();
		initAsk.clear();
		prevBid.clear();
		prevAsk.clear();
	}

	// Abstract function specifying agent's strategy for participating in the market.
	public abstract ActivityHashMap agentStrategy();

	/**
	 * Agent arrives in a single market.
	 * 
	 * @param marketID
	 * @param arrivalTime
	 * @return ActivityHashMap
	 */
	public ActivityHashMap agentArrival(Market mkt, TimeStamp ts) {

		System.out.println("ACTIVITY: AgentArrival");
		
		// buyer/seller based on the config file -- TODO
		mkt.agentIDs.add(this.ID);
		mkt.buyers.add(this.ID);
		mkt.sellers.add(this.ID);
		marketIDs.add(mkt.ID);
		quotes.put(mkt.ID, new Vector<Quote>());

		// Initialize bid/ask containers
		prevBid.put(mkt.ID, 0.0);
		prevAsk.put(mkt.ID, 0.0);
		initBid.put(mkt.ID, -1.0);
		initAsk.put(mkt.ID, -1.0);
		
		// Create activity map
		ActivityHashMap actMap = new ActivityHashMap();
		actMap.insertActivity(new UpdateQuotes(this, mkt, ts));
		actMap.insertActivity(new AgentStrategy(this, ts));

		return actMap;
	}

	/**
	 * Agent arrives in multiple markets.
	 * 
	 * @param mkts
	 * @param ts	arrival time
	 * @return
	 */
	public ActivityHashMap agentArrival(ArrayList<Market> mkts, TimeStamp ts) {
		
		System.out.println("ACTIVITY: AgentArrival");
		
		ActivityHashMap actMap = new ActivityHashMap();
		
		for (Iterator<Market> i = mkts.iterator(); i.hasNext();) {
			Market mkt = i.next();
			mkt.agentIDs.add(this.ID);
			mkt.buyers.add(this.ID);
			mkt.sellers.add(this.ID);
			marketIDs.add(mkt.ID);
			quotes.put(mkt.ID, new Vector<Quote>());
			
			prevBid.put(mkt.ID, 0.0);
			prevAsk.put(mkt.ID, 0.0);
			initBid.put(mkt.ID, -1.0);
			initAsk.put(mkt.ID, -1.0);
			
			actMap.insertActivity(new UpdateQuotes(this, mkt, ts));
			actMap.insertActivity(new AgentStrategy(this, ts));
		}
		return actMap;
	}

	/**
	 * Agent departs a specified market, if it is active.
	 * 
	 * @param departureTime
	 * @return ActivityHashMap
	 */
	public ActivityHashMap agentDeparture(Market mkt) {

		System.out.println("ACTIVITY: AgentDeparture");
		
		mkt.agentIDs.remove(this.ID);
		mkt.buyers.remove(this.ID);
		mkt.sellers.remove(this.ID);
		this.exitMarket(mkt.ID);
		
		return null;
	}
	
	/**
	 * Exits market by removing all entries hashed by the specified market ID.
	 *  
	 * @param mktID
	 */
	protected void exitMarket(int mktID) {
		marketIDs.remove(mktID);
		bidPrice.remove(mktID);
		askPrice.remove(mktID);
		currentBid.remove(mktID);
		lastQuoteTime.remove(mktID);
		nextQuoteTime.remove(mktID);
		lastClearTime.remove(mktID);
		quotes.remove(mktID);
		initBid.remove(mktID);
		initAsk.remove(mktID);
		prevBid.remove(mktID);
		prevAsk.remove(mktID);
	}

	/**
	 * Submit a bid to the specified market.
	 * 
	 * @param b
	 * @param mkt
	 * @return
	 */
	public ActivityHashMap submitBid(Bid b, Market mkt) {

		System.out.println("ACTIVITY: SubmitBid");

		if (b == null) return null;
		
		// TODO - need to now add the bid to the market & process it
		// look at bid ID
		
		return null;
	}	

	/**
	 * Withdraws a bid by replacing it with an empty bid.
	 * 
	 * @param b
	 * @param mkt
	 * @return
	 */
	public ActivityHashMap withdrawBid(Bid b, Market mkt) {		
		PQBid emptyBid = new PQBid();
		emptyBid.addPoint(0, new Price(0));
		this.data.bidData.put(b.getBidID(), emptyBid);
		return null;
	}


	public ActivityHashMap getNBBO() {
		// TODO
		return null;
	}

	public ActivityHashMap getQuote(Market mkt) { // gets the latest quote

		// after it gets the quote, where is it stored? in the quotes vec
		// how does the agent interact with this?

		// mkt.getLatestQuote();

		return null;
	}

	public Quote getQuote(Bid b, int mktID) {
		//TODO - Agent.java
		
		// this function is needed to grab the quote data from the appropriate market
		// want hqw data! bid must be active in the auction???
		
		return null;
	}
	
	
	public ActivityHashMap updateQuotes(Market mkt) {

		System.out.println("ACTIVITY: UpdateQuotes");
		
		Quote q = getQuote(null, mkt.ID);
		
		if (q != null) {
			if (q.lastAskPrice == null)
				askPrice.put(mkt.ID, new Price(INF_PRICE));
			else
				askPrice.put(mkt.ID, q.lastAskPrice);
			if (q.lastBidPrice == null)
				bidPrice.put(mkt.ID, new Price(0));
			else
				bidPrice.put(mkt.ID, q.lastBidPrice);
			if (q.lastQuoteTime != null)
				lastQuoteTime.put(mkt.ID, q.lastQuoteTime);
			if (q.lastClearTime != null && lastClearTime.get(mkt.ID) != q.lastClearTime) {
				int t = (int)(q.lastClearTime.longValue() - lastClearTime.get(mkt.ID).longValue());
				lastClearTime.put(mkt.ID, q.lastClearTime);
				// Computing matching intervals using moving average of 2 clearings.
				if (matchingInterval.get(mkt.ID).size() > 2*getNumMarkets()) {
					matchingInterval.get(mkt.ID).removeLast();
				}
				matchingInterval.get(mkt.ID).addFirst(t);
			}
		} else {
//			log(Log.ERROR, "MMAgent::updateQuotes, ERROR: couldn't get quote");
		}
		quotes.get(mkt.ID).add(q);

		return null;
	}

	public int getQuoteSize(int mktID) {
		return quotes.get(mktID).size();
	}

	public Quote getLatestQuote(int mktID) {
		if (quotes.get(mktID).isEmpty())
			return null;
		//return (Quote) this.quotes.getLast();
		return quotes.get(mktID).lastElement();
	}

	public Quote getQuoteAt(int mktID, int idx) {
		return quotes.get(mktID).get(idx);
	}

	public Price getBidPrice(int mktID) {
		return bidPrice.get(mktID);
	}

	public Price getAskPrice(int mktID) {
		return askPrice.get(mktID);
	}

	/**
	 * @return number of markets that agent is active in
	 */
	public int getNumMarkets() {
		return marketIDs.size();
	}
	
	/**
	 * @return array of marketIDs
	 */
	public ArrayList<Integer> getMarketIDs() {
		return marketIDs;
	}
	
	public ArrayList<PQTransaction> getTransactions(Integer lastID) {
		ArrayList<PQTransaction> transactions = new ArrayList<PQTransaction>();
		
		TreeSet<Integer> t = getTransIDs(lastID);
		if (t == null || t.size() == 0) return null;
		
		for (Iterator<Integer> i = t.iterator(); i.hasNext();) {
			Integer id = (Integer) i.next();
			PQTransaction record = this.data.transData.get(id);
			// transInfo replaced by this.data.transData.get()
			transactions.add(record);
		}
		return transactions;
	}
	
	/**
	 * @return sorted set of all transaction IDs later than the last one
	 */
	public TreeSet<Integer> getTransIDs() {
		if (lastTransID != null) return getTransIDs(lastTransID);
		return null;
	}
	
	/**
	 * @param lastID - last ID that don't want transIDs for
	 * @return sorted set of transaction IDs later that lastID
	 */
	public TreeSet<Integer> getTransIDs(Integer lastID) {
		TreeSet<Integer> transIDs = new TreeSet<Integer>();
		for (Iterator<Integer> i = this.data.transData.keySet().iterator(); i.hasNext();) {
			transIDs.add(i.next());
		}
		if (transIDs.size() == 0) return null;
		
		return transIDs; 
	}
}
