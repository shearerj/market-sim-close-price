package market;

import java.util.HashMap;
import java.util.ArrayList;

import data.SystemData;

import entity.Market;
import event.*;

/**
 * Base class for orderbooks. Each orderbook is associated with a single market.
 * Note that each agent is only allowed one active bid in a market at a time.
 * 
 * @author ewah
 */
public abstract class OrderBook {

	// reorg
	protected final Market market;
	// reorg
	
	public int marketID;
	public SystemData data;
	
	// hashed by agent ID FIXME Can only store one bid for each agent including HFT
	public HashMap<Integer,Bid> activeBids;
	public HashMap<Integer,Bid> clearedBids;
	
	public OrderBook(Market market) {
		this.market = market;
		this.marketID = market.getID();
		this.activeBids = new HashMap<Integer,Bid>();
		this.clearedBids = new HashMap<Integer,Bid>();
	}
	
	@Deprecated
	public OrderBook(int id, SystemData data) {
		this.marketID = id;
		this.market = data.getMarket(id);
		activeBids = new HashMap<Integer,Bid>();
	}
	
	public abstract int getDepth();
	
	public abstract Bid getBidQuote();
	
	public abstract Bid getAskQuote();
	
	public abstract void removeBid(int agentID);
	
	public abstract ArrayList<Transaction> earliestPriceClear(TimeStamp ts);
	
	public abstract ArrayList<Transaction> uniformPriceClear(TimeStamp ts, float pricingPolicy);
	
	
	public void insertBid(Bid newBid) {
		Integer key = newBid.getAgent().getID();
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
