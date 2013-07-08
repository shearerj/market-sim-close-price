package entity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import logger.Logger;
import market.Bid;
import market.PQBid;
import market.Price;
import market.Quote;
import model.MarketModel;
import systemmanager.Consts;
import activity.Activity;
import activity.Clear;
import event.TimeStamp;

/**
 * Class for a call market. The order book is closed, therefore agents will only
 * be able to see the price of the last clear as well as the bid/ask immediately
 * after the clear, i.e. they will be able to see the best available buy and sell
 * prices for the bids left in the order book after each market clear.
 * 
 * NOTE: First Clear Activity is initialized in the SystemManager.
 * 
 * @author ewah
 */
public class CallMarket extends Market {

	public final static String CLEAR_FREQ_KEY = "clearFreq";
	public final static String PRICING_POLICY_KEY = "pricingPolicy";
	
	public float pricingPolicy; // XXX Unused?
	protected final TimeStamp clearFreq;
	protected TimeStamp nextClearTime;
	
	public CallMarket(int marketID, MarketModel model, float pricingPolicy, TimeStamp clearFreq) {
		super(marketID, model);
		this.pricingPolicy = pricingPolicy;
		this.clearFreq = clearFreq;
		this.nextClearTime = clearFreq;
	}
	
	public Bid getBidQuote() {
		return this.orderbook.getBidQuote();
	}
	
	public Bid getAskQuote() {
		return this.orderbook.getAskQuote();
	}

	public Price getBidPrice() {
		return lastBidPrice;
	}
	
	public Price getAskPrice() {
		return lastAskPrice;
	}
	
	public Collection<Activity> addBid(Bid b, TimeStamp ts) {
		// Unlike continuous auction market, no Clear inserted unless clear freq = 0
		Collection<Activity> actMap = new ArrayList<Activity>();
		orderbook.insertBid((PQBid) b);
		bids.add(b);
		this.addDepth(ts, orderbook.getDepth());
		if (clearFreq.longValue() == 0) {
			// return clear(ts);
			actMap.add(new Clear(this, Consts.INF_TIME));
		} // else, Clear activities are chained and continue that way
		return actMap;
	}
	
	public Collection<Activity> removeBid(int agentID, TimeStamp ts) {
		// Unlike continuous auction market, no Clear inserted unless clear freq = 0
		Collection<Activity> actMap = new ArrayList<Activity>();
		orderbook.removeBid(agentID);
		this.addDepth(ts, orderbook.getDepth());
		if (clearFreq.longValue() == 0) {
			// return clear(ts);
			actMap.add(new Clear(this, Consts.INF_TIME));
		} // else, Clear activities are chained and continue that way
		return actMap;
	}
	
	
	public HashMap<Integer,Bid> getBids() {
		return orderbook.getActiveBids();
	}
	

	public Collection<Activity> clear(TimeStamp clearTime) {
		// Update the next clear time
		nextClearTime = clearTime.sum(clearFreq);
		Collection<Activity> actList = super.clear(clearTime);
		
		// Insert next clear activity at some time in the future
		if (clearFreq.longValue() > 0) {
			actList.add(new Clear(this, nextClearTime));				
		}
		return actList;
	}
	
	
	public Quote quote(TimeStamp quoteTime) {
		// updates market's quote only immediately after a Clear activity
		// otherwise revises the given quote to be the last known prices
		
		Quote q = new Quote(this);

		// Retrieve the newest quote if quote is requested right after clearing
		if (quoteTime.compareTo(lastClearTime) == 0) {
			Price bp = q.lastBidPrice;
			Price ap = q.lastAskPrice;
			
			if (bp != null && ap != null) {
				if (bp.getPrice() == -1 || ap.getPrice() == -1) {
					// either bid or ask are undefined
					this.addSpread(quoteTime, Consts.INF_PRICE);
					this.addMidQuote(quoteTime, Consts.INF_PRICE, Consts.INF_PRICE);	
					
				} else if (bp.compareTo(ap) == 1 && ap.getPrice() > 0) {
					Logger.log(Logger.ERROR, this.getName() + "::quote: ERROR bid > ask");
					this.addSpread(quoteTime, Consts.INF_PRICE);
					this.addMidQuote(quoteTime, Consts.INF_PRICE, Consts.INF_PRICE);
					
				} else {
					// valid bid-ask
					data.addQuote(id, q);
					this.addSpread(quoteTime, q.getSpread());
					this.addMidQuote(quoteTime, bp.getPrice(), ap.getPrice());
				}
			}
			lastQuoteTime = quoteTime;
			nextQuoteTime = quoteTime.sum(clearFreq);
			
			if (bp != null) {
				lastBidPrice = bp;
				lastBidQuantity = q.lastBidQuantity;
			}
		    if (ap != null) {
		    	lastAskPrice = ap;
		    	lastAskQuantity = q.lastAskQuantity;
		    }
		    
		} else {
			// Otherwise, retrieve the last known quote & overwrite current quote
			q.lastBidPrice = lastBidPrice;
			q.lastAskPrice = lastAskPrice;
			q.lastBidQuantity = lastBidQuantity;
			q.lastAskQuantity = lastAskQuantity;
		}
		
		return q;
	}
}
