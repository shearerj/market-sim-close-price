/**
 * $Id: OrderBook.java,v 1.39 2005/03/29 18:19:54 chengsf Exp $
 * 
 * Edited 2012/06/13 by ewah
 */
package activity.market;

import event.TimeStamp;
import systemmanager.Log;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.ArrayList;

import activity.market.OrderBook.AgentDataEntry;

/**
 * Base class for orderbooks
 * holds bids for agents, performs quoting/clearing operations
 *
 * @see ab3d.auction.Auction
 */
public abstract class OrderBook {

	static final boolean DEBUG = false;

//	protected int HTSize = 30;
	int aucID;
	protected HashMap<Integer,OrderBook.AgentDataEntry> dataEntries;   // key is agent id, value is agentDataEntry
	protected HashMap<Integer,Bid> clearedBids;  // key is agent id, value is a bid
	public Log log;

	/**
	 * set the log/auctionID associated with this orderbook for logging
	 *
	 * @param l
	 * @param aID
	 */
	public void setParams(Log l, int aID) {
		aucID = aID;
		log = l;
	}

	protected void log(int level, String s) {
		s = String.valueOf(aucID) + "," + "OrderBook::" + s;
		if (log == null) {
			//System.out.println(level+"|"+AB3DConsts.AUCTION+"|"+s);
		} else {
//			log.log(level, AB3DConsts.AUCTION, s); // TODO to fix
			log.log(level, s, s);
		}
	}

	protected void log(int level, String loc, String s) {
		s = aucID + "," + s;
		if (log == null) {
			//System.out.println(level+"|"+loc+"|"+s);
		} else {
			log.log(level, loc, s);
		}

	}

	protected class AgentDataEntry {
		public Bid bid;
		public Bid alloc;

		public AgentDataEntry() {
			bid = null;
			alloc = null;
		}
	}

	public OrderBook() {
		dataEntries = new HashMap<Integer, AgentDataEntry>();
	}

	/**
	 * create an orderbook with a given initial # of agent entries
	 *
	 * @param size number of entries
	 */
	public OrderBook(int size) {
		dataEntries = new HashMap<Integer, AgentDataEntry>(size);

	}

	/**
	 * create an orderbook with default initial size
	 */
	public OrderBook(MarketConfig config) {
		dataEntries = new HashMap<Integer, AgentDataEntry>();
	}

	/**
	 * get list of recently cleared bids
	 *
	 * @return the list of most recently cleared bids
	 */
	public Enumeration getClearedBids() {
		if (clearedBids == null)
			return null;
		else
			return (Enumeration) clearedBids.values();
//			return clearedBids.elements();
	}

	/**
	 * get the current bid for a given agent
	 *
	 * @param agent_id
	 * @return agent's current bid
	 */
	public Bid getBid(int agent_id) {

		AgentDataEntry ade = (AgentDataEntry) dataEntries.get(new Integer(agent_id));
		if (ade == null) {
			log(log.INFO, "no agent data entry found for agent id" + agent_id + ", keys:");
			return null;
		} else {
			if (ade.bid == null)
				log(Log.INFO, "bid was null for agent id " + agent_id);
			return ade.bid;
		}
	}

	protected final void debug(String message) {
		if (DEBUG) {
			System.out.print(message);
		}
	}

	protected final void debugn(String message) {
		if (DEBUG) {
			System.out.println(message);
		}
	}

	/**
	 * flush out all bids from orderbook
	 */
	public abstract void reset();

	/**
	 * compute allocations for all agents having an active bid
	 * in the OrderBook
	 * allocations are saved into their OrderBook entries
	 *
	 * @return hashmap of real time allocations for all agents with bids
	 */
	public abstract HashMap<Integer,Bid> computeAllocations();

	/**
	 * get hashtable of last computed allocations for all agents
	 *
	 * @return hashtable of last-computed allocations
	 */
	public abstract HashMap<Integer,Bid> getAllocations();

	/**
	 * last computed allocation for a given agent
	 *
	 * @param agentid
	 * @return portion of bid that would transact
	 */
	public abstract Bid lastAlloc(int agentid);

	/**
	 * clear the orderbook, pricing based on earlier submitted bid
	 *
	 * @return Vector of Transactions
	 */
	public abstract ArrayList<Transaction> earliestPriceClear(TimeStamp ts);

	/**
	 * clear the orderbook
	 *
	 * @param ts
	 */
//	public abstract ArrayList<Transaction> BBVickreyPriceClear(TimeStamp ts);


	/**
	 * clear the auction at a uniform price between bid/ask based on pricing_k
	 *
	 * @return Vector of Transactions
	 */
	public abstract ArrayList<Transaction> uniformPriceClear(TimeStamp ts, float pricing_k);
	
	
	
	/**
	 * @return Hashtable of personalized xml strings for agents
	 */
//	public abstract Hashtable getAgentQuoteStrings();
	
//	public abstract String getQuoteString(float delta);
//
//	public abstract String getQuoteString();
	
	public abstract ArrayList<Bid> getQuote();
	
	public abstract ArrayList<Bid> getQuote(float delta);

	public abstract Bid getBidQuote(float delta);

	public abstract Bid getAskQuote(float delta);
	
	public abstract Bid getBidQuote();
	
	public abstract Bid getAskQuote();
	
	/**
	 * what is the weakest acceptable bid for buy
	 *
	 * @return weakest acceptable bid for buy
	 */

	/**
	 * insert a bid into the orderbook, replacing agents old bid (if any)
	 *
	 * @param newBid
	 */
	public void insertBid(Bid newBid) {
		Integer key = newBid.agentID;
		// look up the agent's entry

		OrderBook.AgentDataEntry ade = dataEntries.get(key);
		//create entry if non-existent, remove old bid if exists
		if (ade == null) {
			log(Log.INFO, "insertBid, agent id " + newBid.agentID);
			ade = new OrderBook.AgentDataEntry();
			dataEntries.put(key, ade);
		}
		ade.bid = newBid;
	}

	/**
	 * remove the active bid for this agentID
	 *
	 * @param agentID id of agent whose bid will be removed
	 */
	public abstract void removeBid(int agentID);


	/**
	 * log the bids for the orderbook gui
	 */
	public abstract void logBids();


}
