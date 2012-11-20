package entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeSet;

import market.*;
import activity.*;
import event.*;
import systemmanager.*;

/**
 * Class for a call market. The order book is closed, therefore agents will only
 * be able to see the price of the last clear as well as the bid/ask immediately
 * after the clear, i.e. they will be able to see the best available buy and sell
 * prices for the bids left in the order book after each market clear.
 * 
 * NOTE: First clear Activity is initialized in the SystemManager.
 * 
 * @author ewah
 */
public class CallMarket extends Market {

	public float pricingPolicy;
	public PQOrderBook orderbook;
	private TimeStamp clearFreq;
	
	/**
	 * Overloaded constructor.
	 * @param marketID
	 */
	public CallMarket(int marketID, SystemData d, ObjectProperties p, Log l) {
		super(marketID, d, p, l);
		marketType = Consts.getMarketType(this.getClass().getSimpleName());
		orderbook = new PQOrderBook(this.ID);
		orderbook.setParams(this.ID, l, d);
		clearFreq = new TimeStamp(Integer.parseInt(p.get("clearFreq")));
		nextClearTime = clearFreq;
		pricingPolicy = (float) Double.parseDouble(p.get("pricingPolicy"));
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
	
	public ActivityHashMap addBid(Bid b, TimeStamp ts) {
		// Unlike continuous auction market, no Clear Activity inserted unless clear freq = 0
		ActivityHashMap actMap = new ActivityHashMap();
		orderbook.insertBid((PQBid) b);
		this.data.addDepth(this.ID, ts, orderbook.getDepth());
		this.data.addSubmissionTime(b.getBidID(), ts);
		if (clearFreq.longValue() == 0) {
			actMap.insertActivity(Consts.CALL_CLEAR_PRIORITY, new Clear(this, ts));
			return actMap;
		}
		return actMap;
	}
	
	public ActivityHashMap removeBid(int agentID, TimeStamp ts) {
		ActivityHashMap actMap = new ActivityHashMap();
		orderbook.removeBid(agentID);
		orderbook.logActiveBids(ts);
		orderbook.logFourHeap(ts);
		this.data.addDepth(this.ID, ts, orderbook.getDepth());
		return actMap;
	}
	
	
	public HashMap<Integer,Bid> getBids() {
		return orderbook.getActiveBids();
	}
	

	public ActivityHashMap clear(TimeStamp clearTime) {
		ActivityHashMap actMap = new ActivityHashMap();
		
		// Update the next clear time
		this.nextClearTime = clearTime.sum(clearFreq);

		// Return prior quote (works b/c lastClearTime has not been updated yet)
		log.log(Log.INFO, clearTime.toString() + " | " + this.toString() + " Prior Quote" + 
				this.quote(clearTime));
		ArrayList<Transaction> transactions = orderbook.uniformPriceClear(clearTime, (float) 0.5);
		
		if (transactions == null) {
			lastClearTime = clearTime;
			
			orderbook.logActiveBids(clearTime);
			orderbook.logFourHeap(clearTime);
			
			// Now update the quote
			log.log(Log.INFO, clearTime.toString() + " | " + this.toString() + " " + 
					this.getClass().getSimpleName() + "::clear: Nothing transacted. Post Quote" 
					+ this.quote(clearTime));
			
			if (clearFreq.longValue() > 0) {
				actMap.insertActivity(Consts.CALL_CLEAR_PRIORITY, new Clear(this, nextClearTime));				
			}
			return actMap;
		}
		
		// Add bid execution times
		ArrayList<Integer> IDs = orderbook.getClearedBidIDs();
		for (Iterator<Integer> id = IDs.iterator(); id.hasNext(); ) {
			data.addExecutionTime(id.next(), clearTime);
		}
		
		// Add transactions to SystemData
		TreeSet<Integer> transactingIDs = new TreeSet<Integer>();
		for (Iterator<Transaction> i = transactions.iterator(); i.hasNext();) {
			PQTransaction t = (PQTransaction) i.next();
			// track which agents were involved in the transactions
			transactingIDs.add(t.buyerID);
			transactingIDs.add(t.sellerID);

			this.data.addTransaction(t);
			lastClearPrice = t.price;
		}
		this.lastClearTime = clearTime;

		// update and log transactions
		for (Iterator<Integer> it = transactingIDs.iterator(); it.hasNext(); ) {
			int id = it.next();
			data.getAgent(id).updateTransactions(clearTime);
			data.getAgent(id).logTransactions(clearTime);
		}
		orderbook.logActiveBids(clearTime);
		orderbook.logClearedBids(clearTime);
		orderbook.logFourHeap(clearTime);
		this.data.addDepth(this.ID, clearTime, orderbook.getDepth());
		log.log(Log.INFO, clearTime.toString() + " | " + this.toString() + " " + 
				this.getClass().getSimpleName() + " cleared: Post Quote" + this.quote(clearTime));

		// Insert next clear activity at some time in the future
		if (clearFreq.longValue() > 0) {
			actMap.insertActivity(Consts.CALL_CLEAR_PRIORITY, new Clear(this, nextClearTime));				
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
					this.data.addSpread(this.ID, quoteTime, Consts.INF_PRICE);
					
				} else if (bp.compareTo(ap) == 1 && ap.getPrice() > 0) {
					log.log(Log.ERROR, this.getClass().getSimpleName() + "::quote: ERROR bid > ask");
					this.data.addSpread(this.ID, quoteTime, Consts.INF_PRICE);
					
				} else {
					// valid bid-ask
					this.data.addQuote(this.ID, q);
					this.data.addSpread(this.ID, quoteTime, q.getSpread());
				}
			}
			this.lastQuoteTime = quoteTime;
			this.nextQuoteTime = quoteTime.sum(clearFreq);
			
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
