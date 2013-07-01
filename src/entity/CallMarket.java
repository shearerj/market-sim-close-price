package entity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeSet;

import logger.Logger;
import market.Bid;
import market.PQBid;
import market.PQOrderBook;
import market.PQTransaction;
import market.Price;
import market.Quote;
import market.Transaction;
import model.MarketModel;
import systemmanager.Consts;
import activity.Activity;
import activity.Clear;
import activity.SendToSIP;
import data.ObjectProperties;
import data.SystemData;
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

	public float pricingPolicy;
	public PQOrderBook orderbook;
	private TimeStamp clearFreq;
	
	public final static String CLEAR_FREQ_KEY = "clearFreq";
	public final static String PRICING_POLICY_KEY = "pricingPolicy";
	
	/**
	 * Overloaded constructor.
	 * @param marketID
	 */
	public CallMarket(int marketID, SystemData d, ObjectProperties p, MarketModel model) {
		super(marketID, d, p, model);
		marketType = Consts.getMarketType(this.getName());
		orderbook = new PQOrderBook(id);
		orderbook.setParams(id, d);
		pricingPolicy = params.getAsFloat(CallMarket.PRICING_POLICY_KEY);
		clearFreq = new TimeStamp(params.getAsInt(CallMarket.CLEAR_FREQ_KEY));
		nextClearTime = clearFreq;
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
		data.addDepth(id, ts, orderbook.getDepth());
		submissionTimes.put(b.getBidID(), ts);
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
		data.addDepth(id, ts, orderbook.getDepth());
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
		Collection<Activity> actMap = new ArrayList<Activity>();
		
		// Update the next clear time
		nextClearTime = clearTime.sum(clearFreq);
		orderbook.logFourHeap(clearTime);
		
		// Return prior quote (works b/c lastClearTime has not been updated yet)
		Logger.log(Logger.INFO, clearTime + " | " + this + " Prior-clear Quote" + 
				this.quote(clearTime));
		ArrayList<Transaction> trans = orderbook.uniformPriceClear(clearTime, pricingPolicy);
		
		if (trans == null) {
			lastClearTime = clearTime;
			
			orderbook.logActiveBids(clearTime);
			orderbook.logFourHeap(clearTime);
			data.addDepth(id, clearTime, orderbook.getDepth());
			
			// Now update the quote
			Logger.log(Logger.INFO, clearTime + " | ....." + this + " " + 
					this.getName() + "::clear: No change. Post-clear Quote" 
					+ this.quote(clearTime));
			actMap.add(new SendToSIP(this, clearTime));

			if (clearFreq.longValue() > 0) {
				actMap.add(new Clear(this, nextClearTime));				
			}
			return actMap;
		}
		
		// Add bid execution speed
		ArrayList<Integer> IDs = orderbook.getClearedBidIDs();
		for (Iterator<Integer> id = IDs.iterator(); id.hasNext(); ) {
			addExecutionTime(id.next(), clearTime);
		}
		
		// Add transactions to SystemData
		for (Iterator<Transaction> i = trans.iterator(); i.hasNext();) {
			PQTransaction t = (PQTransaction) i.next();
			model.addTrans(t);
			lastClearPrice = t.price;
			//update and log transactions
			t.getBuyer().updateTransactions(clearTime);
			t.getBuyer().logTransactions(clearTime);
			t.getSeller().updateTransactions(clearTime);
			t.getSeller().logTransactions(clearTime);
		}
		lastClearTime = clearTime;

		orderbook.logActiveBids(clearTime);
		orderbook.logClearedBids(clearTime);
		orderbook.logFourHeap(clearTime);
		data.addDepth(id, clearTime, orderbook.getDepth());
		Logger.log(Logger.INFO, clearTime.toString() + " | ....." + this + " " + 
				this.getName() + "::clear: Order book cleared: Post-clear Quote" 
				+ this.quote(clearTime));
		actMap.add(new SendToSIP(this, clearTime));

		// Insert next clear activity at some time in the future
		if (clearFreq.longValue() > 0) {
			actMap.add(new Clear(this, nextClearTime));				
		}
		return actMap;
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
					data.addSpread(id, quoteTime, Consts.INF_PRICE);
					data.addMidQuotePrice(id, quoteTime, Consts.INF_PRICE, Consts.INF_PRICE);	
					
				} else if (bp.compareTo(ap) == 1 && ap.getPrice() > 0) {
					Logger.log(Logger.ERROR, this.getName() + "::quote: ERROR bid > ask");
					data.addSpread(id, quoteTime, Consts.INF_PRICE);
					data.addMidQuotePrice(id, quoteTime, Consts.INF_PRICE, Consts.INF_PRICE);
					
				} else {
					// valid bid-ask
					data.addQuote(id, q);
					data.addSpread(id, quoteTime, q.getSpread());
					data.addMidQuotePrice(id, quoteTime, bp.getPrice(), ap.getPrice());
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
