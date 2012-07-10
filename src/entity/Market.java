package entity;

import java.util.ArrayList;

import market.*;
import event.*;
import activity.*;
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
	
	public Market(int marketID, SystemData d) {
		super(marketID, d);
		
		agentIDs = new ArrayList<Integer>();
		buyers = new ArrayList<Integer>();
		sellers = new ArrayList<Integer>();
		
		finalClearTime = new TimeStamp(-1);
	    lastQuoteTime = new TimeStamp(-1);
	    lastClearTime = new TimeStamp(-1);
	    lastClearPrice = new Price(-1);
	    lastAskQuote = new Price(0);
	    lastBidQuote = new Price(0);
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
	 * @return bid (highest buy offer)
	 */
	public abstract Bid getBidQuote();
	
	/**
	 * @return ask (lowest sell offer)
	 */
	public abstract Bid getAskQuote();
	
	/**
	 * Publish quotes.
	 * 
	 * @param quoteTime
	 */
	public abstract Quote quote(TimeStamp quoteTime);

	/**
	 * Clears the orderbook.
	 * 
	 * @param clearTime
	 * @return
	 */
	public abstract ActivityHashMap clear(TimeStamp clearTime);
	
//	public abstract ActivityHashMap processBid(Bid b);
	
	/**
	 * Add bid to the market.
	 * @param b
	 */
	public abstract void addBid(Bid b);
	
	/**
	 * Remove bid from the market.
	 * @param b
	 */
	public abstract void removeBid(Bid b);
	
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
	

	public Price getLastClearPrice() {
		return lastClearPrice;
	}

	public TimeStamp getFinalClearTime() {
		return finalClearTime;
	}

	public TimeStamp getNextClearTime() {
		return nextClearTime;
	}

	public TimeStamp getLastClearTime() {
		return lastClearTime;
	}

	public TimeStamp getNextQuoteTime() {
		return nextQuoteTime;
	}

	public TimeStamp getLastQuoteTime() {
		return lastQuoteTime;
	}
	
	
//	public void updateTransactions() {
////return null;
//}
	
//	public Event addOrUpdateBid() {
//		return null;
//	}
	
//	protected Quote getLatestQuote() {
//		return null;
//	}
	
}
