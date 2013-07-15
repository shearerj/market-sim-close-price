package market;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import data.SystemData;

import entity.Agent;
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

	public SystemData data;

	// hashed by agent ID FIXME Can only store one bid for each agent including
	// HFT
	public Map<Agent, Bid> activeBids;
	public Map<Agent, Bid> clearedBids;

	public OrderBook(Market market) {
		this.market = market;
		this.activeBids = new HashMap<Agent, Bid>();
		this.clearedBids = new HashMap<Agent, Bid>();
	}

	public abstract int getDepth();

	public abstract Bid getBidQuote();

	public abstract Bid getAskQuote();

	public abstract void removeBid(int agentID);

	public abstract Collection<Transaction> earliestPriceClear(TimeStamp ts);

	public abstract Collection<Transaction> uniformPriceClear(TimeStamp ts,
			float pricingPolicy);

	public void insertBid(Bid newBid) {
		activeBids.put(newBid.getAgent(), newBid);
	}

	public Bid getBid(int agentID) {
		return activeBids.get(agentID);
	}

	public Map<Agent, Bid> getClearedBids() {
		return clearedBids;
	}

	public Map<Agent, Bid> getActiveBids() {
		return activeBids;
	}

}
