package entity;

import java.util.HashMap;
import java.util.ArrayList;

import event.*;
import activity.*;
import activity.market.*;
import systemmanager.*;

/**
 * Base class for all markets.
 * 
 * @author ewah
 */
public abstract class Market extends Entity {
	
	// Agent information
	protected ArrayList<Integer> buyers;
	protected ArrayList<Integer> sellers;
	protected ArrayList<Integer> agentIDs;
	
	// Market information
	public TimeStamp finalClearTime;
	public TimeStamp nextQuoteTime;
	public TimeStamp nextClearTime;
	public TimeStamp lastQuoteTime;
	public TimeStamp lastClearTime;
	public TimeStamp lastBidTime;
	public Price lastClearPrice;
	public Price lastAskQuote;
	public Price lastBidQuote;
	public PQOrderBook orderbook;
	
	public MarketConfig config;
	
	public Market(int marketID, SystemData d) {
		super(marketID, d);

		orderbook = new PQOrderBook();
		
		agentIDs = new ArrayList<Integer>();
		buyers = new ArrayList<Integer>();
		sellers = new ArrayList<Integer>();
		
		finalClearTime = new TimeStamp(-1);
	    lastQuoteTime = new TimeStamp(-1);
	    lastClearTime = new TimeStamp(-1);
	    lastClearPrice = new Price(-1);
	    lastAskQuote = new Price(0);
	    lastBidQuote = new Price(0);
	    
	    // TODO log? config?
	}

	/**
	 * Clears all the market's data structures.
	 */
	protected void clearAll() {
		buyers.clear();
		sellers.clear();
		agentIDs.clear();
	}
	
	/**
	 * Publish quotes.
	 * 
	 * @param quoteTime
	 */
	public abstract void quote(TimeStamp quoteTime);

	/**
	 * Clears the orderbook.
	 * 
	 * @param clearTime
	 * @return
	 */
	public abstract ActivityHashMap clear(TimeStamp clearTime);
	
	/**
	 * Process a submitted bid.
	 * 
	 * @param b
	 * @return
	 */
	public abstract ActivityHashMap processBid(Bid b);

	
	
	public ActivityHashMap addBid(double price, int quantity) {
		// TODO
		return null;
	}
	
	public ActivityHashMap addBid(Bid b) {
		return null;
	}
	
	public ActivityHashMap removeBid(Bid b) {
		return null;
	}
	
	public ActivityHashMap publishQuotes() {
		// TODO
		return null;
	}
	
	
	/**
	 * Returns true if agent can sell in this market.
	 * 
	 * @param agentID
	 * @return boolean
	 */
	public boolean canSell(int agentID) {
		return sellers.contains(agentID);
	}
	
	/**
	 * Returns true if agent can buy in this market.
	 * 
	 * @param agentID
	 * @return boolean
	 */
	public boolean canBuy(int agentID) {
		return buyers.contains(agentID);
	}
	
//	public void updateTransactions() {
////return null;
//}
	
//	public Event addOrUpdateBid() {
//		// TODO
//		return null;
//	}
	
	protected Quote getLatestQuote() {
		return null;
	}
//
//	public abstract boolean processPredicate(Message m);
//
//	public abstract void subClassInit();
	
	
	/**
	 * Place bidInfo for a bid that is transacted
	 * @return
	 */
//	public int putBid() {
//		
//		
//	}
	
	
	/**
	 * @return number of agents active in the market
	 */
	public int getNumAgents() {
		return agentIDs.size();
	}
	
	/**
	 * @return number of buyers in the market
	 */
	public int getNumBuyers() {
		return buyers.size();
	}
	
	/**
	 * @return number of sellers in the market
	 */
	public int getNumSellers() {
		return sellers.size();
	}
	
	/**
	 * @return array of agentIDs
	 */
	public ArrayList<Integer> getAgentIDs() {
		return agentIDs;
	}
}
