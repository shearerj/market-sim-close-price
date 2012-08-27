package entity;

import market.*;
import event.*;
import activity.*;
import systemmanager.*;

import java.util.ArrayList;
import java.util.HashMap;

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
	public TimeStamp nextClearTime;		// Used for Call markets
	public TimeStamp lastQuoteTime;
	public TimeStamp lastClearTime;
	public TimeStamp lastBidTime;
	public Price lastClearPrice;
	public Price lastAskQuote;
	public Price lastBidQuote;

	public String marketType;

	public Market(int marketID, SystemData d, Log l) {
		super(marketID, d, l);

		agentIDs = new ArrayList<Integer>();
		buyers = new ArrayList<Integer>();
		sellers = new ArrayList<Integer>();

		finalClearTime = new TimeStamp(-1);
		lastQuoteTime = new TimeStamp(-1);
		lastClearTime = new TimeStamp(-1);
		nextQuoteTime = new TimeStamp(-1);
		nextClearTime = new TimeStamp(-1);
		lastClearPrice = new Price(-1);
		lastAskQuote = new Price(0);
		lastBidQuote = new Price(0);
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
	 * @return bid price (highest)
	 */
	public abstract Price getBidPrice();

	/**
	 * @return ask price (lowest
	 */
	public abstract Price getAskPrice();

	/**
	 * Publish quotes.
	 * 
	 * @param quoteTime
	 */
	public abstract Quote quote(TimeStamp quoteTime);

	/**
	 * Clears the order book.
	 * 
	 * @param clearTime
	 * @return
	 */
	public abstract ActivityHashMap clear(TimeStamp clearTime);

	/**
	 * @return map of bids (hashed by agent ID)
	 */
	public abstract HashMap<Integer, Bid> getBids();

	/**
	 * Add bid to the market.
	 * 
	 * @param b
	 */
	public abstract void addBid(Bid b);

	/**
	 * Remove bid for given agent from the market.
	 * 
	 * @param agentID
	 */
	public abstract void removeBid(int agentID);

	/**
	 * Returns clearing activity; otherwise returns null (call market)
	 * 
	 * @param currentTime of bid submission
	 * @return Activity for clearing (TimeStamps will vary)
	 */
	public abstract Activity getClearActivity(TimeStamp currentTime);
	
	
	
	/**
	 * Clears all the market's data structures.
	 */
	protected void clearAll() {
		buyers.clear();
		sellers.clear();
		agentIDs.clear();
	}

	/**
	 * Method to get the type of the market.
	 * 
	 * @return
	 */
	public String getType() {
		return marketType;
	}

	public String toString() {
		return new String("[" + this.getID() + "]");
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

}
