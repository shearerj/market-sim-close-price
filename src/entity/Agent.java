package entity;

import event.*;
import activity.*;
import systemmanager.*;

import java.util.Iterator;
import java.util.Vector;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.TreeSet;
import java.io.IOException;

import market.*;

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

	protected HashMap<Integer,Double> initBid;
	protected HashMap<Integer,Double> initAsk;
	protected HashMap<Integer,Vector<Quote>> quotes;
	protected HashMap<Integer,Double> prevBid;
	protected HashMap<Integer,Double> prevAsk;

	// Agent parameters
	protected int positionLimit;
	protected double tickSize;
	protected int sleepTime;			// how frequently agentStrategy is called
	protected String agentType;

// 	Tracking cash flow
	protected double cashBalance;
	protected double positionSize;

	// Execution speed (hashed by market ID)
//	protected HashMap<Integer,ArrayDeque<Integer>> matchingInterval;

//	protected Integer lastTransID;		// ID of the last transaction fetched

	
	
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
	public abstract ActivityHashMap agentStrategy(TimeStamp ts);

	/**
	 * Agent arrives in a single market.
	 * 
	 * @param marketID
	 * @param arrivalTime
	 * @return ActivityHashMap
	 */
	public ActivityHashMap agentArrival(Market mkt, TimeStamp ts) {

		System.out.println("Agent " + this.ID + " ACTIVITY: AgentArrival in Market " + mkt.ID);

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

//		return setupStrategyFrequency(mkt, ts); // runs out of memory!
		
		ActivityHashMap actMap = new ActivityHashMap();
		actMap.insertActivity(new UpdateAllQuotes(this, ts));
		actMap.insertActivity(new AgentStrategy(this, ts));
		return actMap;
	}

	/**
	 * Specifies when the agent will call its agentStrategy method.
	 * Times will depend on the time of agent arrival into the market
	 * and the agent's latency.
	 * 
	 * @param mkt
	 * @param ts
	 * @return ActivityHashMap
	 */
	public ActivityHashMap setupStrategyFrequency(Market mkt, TimeStamp ts) {
		ActivityHashMap actMap = new ActivityHashMap();
		for (long i = ts.longValue(); i < data.gameLength.longValue(); i+=(long)sleepTime) {
			TimeStamp time = ts.sum(new TimeStamp(i));
			actMap.insertActivity(new UpdateAllQuotes(this, time));
			actMap.insertActivity(new AgentStrategy(this, time));
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

		System.out.println("Agent " + this.ID + " ACTIVITY: AgentDeparture from Market " + mkt.ID);

		mkt.agentIDs.remove(mkt.agentIDs.indexOf(this.ID));
		mkt.buyers.remove(mkt.buyers.indexOf(this.ID));
		mkt.sellers.remove(mkt.sellers.indexOf(this.ID));
		this.exitMarket(mkt.ID);
		return null;
	}

	/**
	 * Exits market by removing all entries hashed by the specified market ID.
	 *  
	 * @param mktID
	 */
	protected void exitMarket(int mktID) {
		marketIDs.remove(marketIDs.indexOf(mktID));
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
	 * Wrapper method to group activities of 1) submit bid & add bid to market,
	 * 2) process/clear bid.
	 * 
	 * @param mkt
	 * @param price
	 * @param quantity
	 * @param ts
	 * @return
	 */
	public ActivityHashMap addBid(Market mkt, double price, int quantity, TimeStamp ts) {

		System.out.println("Agent " + this.ID + " ACTIVITY LIST: AddBid (" + price + ", " + quantity + ") to Market " + mkt.ID);

		ActivityHashMap actMap = new ActivityHashMap();
		actMap.insertActivity(new SubmitBid(this, mkt, price, quantity, ts));
		actMap.insertActivity(new Clear(mkt, ts));
		return actMap;
	}

	/**
	 * Submit a bid to the specified market.
	 * 
	 * @param b
	 * @param mkt
	 * @return
	 */
	public ActivityHashMap submitBid(Market mkt, double price, int quantity, TimeStamp ts) {

		System.out.println("Agent " + this.ID + " ACTIVITY: SubmitBid (" + price + ", " + quantity + ") to Market " + mkt.ID);

		if (quantity == 0) return null;

		PQBid pqBid = new PQBid(this.ID, mkt.ID);
		pqBid.addPoint(quantity, new Price(price));
		pqBid.timestamp = ts;
		mkt.addBid(pqBid);
		currentBid.put(mkt.ID, pqBid);
		return null;
	}	

	/**
	 * Withdraws a bid by replacing it with an empty bid.
	 * 
	 * @param b
	 * @param mkt
	 * @return
	 */
	public ActivityHashMap withdrawBid(Bid b, Market mkt, TimeStamp ts) {

		System.out.println("Agent " + this.ID + " ACTIVITY: WithdrawBid " + " id=" + b.getBidID() + " from Market " + mkt.ID);
		
		mkt.removeBid(b);
		return null;
	}


	public ActivityHashMap getNBBO(TimeStamp ts) {
		// TODO		
		return null;
	}

	
	
	/**
	 * Update quotes from all markets that the agent is active in.
	 * 
	 * @param ts
	 * @return
	 */
	public ActivityHashMap updateAllQuotes(TimeStamp ts) {
		
		System.out.println("Agent " + this.ID + " ACTIVITY: UpdateAllQuotes");
		
		for (Iterator<Integer> i = marketIDs.iterator(); i.hasNext(); ) {
			Market mkt = data.markets.get(i.next());
			updateQuotes(mkt, ts);
		}
		return null;
	}
	
	/**
	 * Updates quotes for the given market.
	 * 
	 * @param mkt
	 * @param ts
	 */
	public void updateQuotes(Market mkt, TimeStamp ts) {
		Quote q = mkt.quote(ts);
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
				lastClearTime.put(mkt.ID, q.lastClearTime);
			}
		} else {
			//	TODO - add ot log
		}
		quotes.get(mkt.ID).add(q);
	}

	
	public int getQuoteSize(int mktID) {
		return quotes.get(mktID).size();
	}

	public Quote getLatestQuote(int mktID) {
		if (quotes.get(mktID).isEmpty())
			return null;
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

	/**
	 * Find best market to buy in (i.e. lowest ASK) and to sell in (i.e. highest BID)
	 * @return BestQuote
	 */
	protected BestQuote findBestBuySell() {
		double bestBuy = -1;
		double bestSell = -1;
		int bestBuyMkt = -1;
		int bestSellMkt = -1;

		for (Iterator<Integer> i = marketIDs.iterator(); i.hasNext(); ) {
			Market mkt = data.markets.get(i.next());
			Price bid = getBidPrice(mkt.ID);
			Price ask = getAskPrice(mkt.ID);

			// in case the bid/ask disappears
			Vector<Price> price = new Vector<Price>();
			price.add(bid);
			price.add(ask);
			
			if (checkBidAsk(mkt.ID, price)) {
				System.out.println("issue with bid ask");
				// TODO - put in log
			}
			
			// Best market to buy in is the one with the lowest ASK
			if (bestBuy == -1 || bestBuy > ask.getPrice()) {
				if (ask.getPrice() != -1) bestBuy = ask.getPrice();
				bestBuyMkt = mkt.ID;
			}
			// Best market to sell in is the one with the highest BID
			if (bestSell == -1 || bestSell < bid.getPrice()) {
				if (bid.getPrice() != -1) bestSell = bid.getPrice();
				bestSellMkt = mkt.ID;
			}
		}
		BestQuote q = new BestQuote();
		q.bestBuy = bestBuy;
		q.bestSell = bestSell;
		q.bestBuyMarket = bestBuyMkt;
		q.bestSellMarket = bestSellMkt;
		
		return q;
	}


	/**
	 * Checks the bid/ask prices for errors. TODO - still need to finish
	 *  
	 * @param aucID
	 * @param price
	 * @return
	 */
	boolean checkBidAsk(int mktID, Vector<Price> price) {
		double bid = price.get(0).toDouble();
		double ask = price.get(1).toDouble();
		boolean flag = true;
		
//		if (ask <= 0) countMissingAsk.get(aucID).addLast(1); else countMissingAsk.get(aucID).addLast(0);
//		if (bid <= 0) countMissingBid.get(aucID).addLast(1); else countMissingBid.get(aucID).addLast(0);
//		if (countMissingAsk.get(aucID).size() > windowSizeCount) countMissingAsk.get(aucID).removeLast();
//		if (countMissingBid.get(aucID).size() > windowSizeCount) countMissingBid.get(aucID).removeLast();

		if (ask > 0 && bid > 0) {
			prevAsk.put(mktID, ask);
			prevBid.put(mktID, bid);
			flag = false;
		} else if (ask <= 0 && bid > 0) {
			double oldask = ask;
//			ask = bid + randomSpread(countMissingAsk.get(aucID))*tickSize;
			//
//			log(Log.INFO, "MMAgent::fixBidAsk: ask: " + oldask + " to " + ask);
			//
			prevAsk.put(mktID, ask);
			prevBid.put(mktID, bid);
		} else if (bid <= 0 && ask > 0) {
			double oldbid = bid;
//			bid = ask - randomSpread(countMissingBid.get(aucID))*tickSize;
			//
//			log(Log.INFO, "MMAgent::fixBidAsk: bid: " + oldbid + " to " + bid);
			//
			prevAsk.put(mktID, ask);
			prevBid.put(mktID, bid);
		} else {
			double oldbid = bid;
			double oldask = ask;
//			ask = (prevAsk[aucID]+prevBid[aucID])/2 + (randomSpread(countMissingAsk.get(aucID))+randomSpread(countMissingBid.get(aucID)))*tickSize/2;
//			bid = (prevAsk[aucID]+prevBid[aucID])/2 - (randomSpread(countMissingAsk.get(aucID))+randomSpread(countMissingBid.get(aucID)))*tickSize/2;
			//
//			log(Log.INFO, "MMAgent::fixBidAsk: bid: " + oldbid + " to " + bid +
//					", ask: " + oldask + " to " + ask);
		}

		bid = Math.max(bid, 1);
		ask = Math.max(ask, 1);

//		price[0] = Math.min(bid, ask);
//		price[1] = Math.max(bid, ask);

		return flag;
	}

	
	//	public ArrayList<PQTransaction> getTransactions(Integer lastID) {
	//		ArrayList<PQTransaction> transactions = new ArrayList<PQTransaction>();
	//		
	//		TreeSet<Integer> t = getTransIDs(lastID);
	//		if (t == null || t.size() == 0) return null;
	//		
	//		for (Iterator<Integer> i = t.iterator(); i.hasNext();) {
	//			Integer id = (Integer) i.next();
	//			PQTransaction record = this.data.transData.get(id);
	//			// transInfo replaced by this.data.transData.get()
	//			transactions.add(record);
	//		}
	//		return transactions;
	//	}
	//	
	//	/**
	//	 * @return sorted set of all transaction IDs later than the last one
	//	 */
	//	public TreeSet<Integer> getTransIDs() {
	//		if (lastTransID != null) return getTransIDs(lastTransID);
	//		return null;
	//	}
	//	
	//	/**
	//	 * @param lastID - last ID that don't want transIDs for
	//	 * @return sorted set of transaction IDs later that lastID
	//	 */
	//	public TreeSet<Integer> getTransIDs(Integer lastID) {
	//		TreeSet<Integer> transIDs = new TreeSet<Integer>();
	//		for (Iterator<Integer> i = this.data.transData.keySet().iterator(); i.hasNext();) {
	//			transIDs.add(i.next());
	//		}
	//		if (transIDs.size() == 0) return null;
	//		
	//		return transIDs; 
	//	}
}
