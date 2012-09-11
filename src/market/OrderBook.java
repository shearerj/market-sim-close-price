package market;

import java.util.HashMap;
import java.util.ArrayList;

import systemmanager.*;
import event.*;

/**
 * Base class for orderbooks. Each orderbook is associated with a single market.
 * Note that each agent is only allowed one active bid in a market at a time.
 * 
 * @author ewah
 */
public abstract class OrderBook {

	public int marketID;
	public Log log;
	public SystemData data;
	
	// hashed by agent ID
	public HashMap<Integer,Bid> activeBids;
	public HashMap<Integer,Bid> clearedBids;
	
	public OrderBook(int id) {
		this.marketID = id;
		activeBids = new HashMap<Integer,Bid>();
	}
	
	public abstract Bid getBidQuote();
	
	public abstract Bid getAskQuote();
	
	public abstract void removeBid(int agentID);
	
	public abstract ArrayList<Transaction> earliestPriceClear(TimeStamp ts);
	
	public abstract ArrayList<Transaction> uniformPriceClear(TimeStamp ts, float pricingPolicy);
	
	
	public void insertBid(Bid newBid) {
		Integer key = newBid.agentID;
		activeBids.put(key, newBid);
	}
	
	public Bid getBid(int agentID) {
		return activeBids.get(agentID);
	}
	
	public HashMap<Integer,Bid> getClearedBids() {
		return clearedBids;
	}
	
	public HashMap<Integer,Bid> getActiveBids() {
		return activeBids;
	}

	
}
