package entity;

import logger.Logger;
import market.*;
import data.ObjectProperties;
import data.SystemData;
import event.TimeStamp;
import activity.*;
import systemmanager.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.HashMap;
import java.util.TreeSet;

/**
 * Class for a continuous double auction market.
 * 
 * @author ewah
 */
public class CDAMarket extends Market {

	public PQOrderBook orderbook;
	
	/**
	 * Overloaded constructor.
	 * @param marketID
	 */
	public CDAMarket(int marketID, SystemData d, ObjectProperties p) {
		super(marketID, d, p);
		marketType = Consts.getMarketType(this.getName());
		orderbook = new PQOrderBook(id);
		orderbook.setParams(id, d);
	}

	public Bid getBidQuote() {
		return orderbook.getBidQuote();
	}
	
	public Bid getAskQuote() {
		return orderbook.getAskQuote();
	}
	
	public Price getBidPrice() {	
		return ((PQBid) getBidQuote()).bidTreeSet.first().getPrice();
	}
	
	public Price getAskPrice() {
		return ((PQBid) getAskQuote()).bidTreeSet.last().getPrice();
	}
	
	public Collection<Activity> addBid(Bid b, TimeStamp ts) {
		orderbook.insertBid((PQBid) b);
		data.addDepth(id, ts, orderbook.getDepth());
		data.addSubmissionTime(b.getBidID(), ts);
		return clear(ts);
	}
	
	
	public Collection<Activity> removeBid(int agentID, TimeStamp ts) {
		orderbook.removeBid(agentID);
		data.addDepth(this.id, ts, orderbook.getDepth());
		return clear(ts);
	}
	
	
	public HashMap<Integer,Bid> getBids() {
		return orderbook.getActiveBids();
	}

	
	public Collection<Activity> clear(TimeStamp clearTime) {
		Collection<Activity> actMap = new ArrayList<Activity>();
		orderbook.logActiveBids(clearTime);
		orderbook.logFourHeap(clearTime);
		
		Logger.log(Logger.INFO, clearTime + " | " + this + " Prior-clear Quote" + 
				this.quote(clearTime));
		ArrayList<Transaction> transactions = orderbook.earliestPriceClear(clearTime);
		
		if (transactions == null) {
			lastClearTime = clearTime;
			
			orderbook.logActiveBids(clearTime);
			orderbook.logFourHeap(clearTime);
			data.addDepth(id, clearTime, orderbook.getDepth());
			
			Logger.log(Logger.INFO, clearTime + " | ....." + this + " " + 
					this.getName() + "::clear: No change. Post-clear Quote" +  
					this.quote(clearTime));
			actMap.add(new SendToSIP(this, clearTime));
			return actMap;
		}
		
		// Add bid execution speed
		ArrayList<Integer> IDs = orderbook.getClearedBidIDs();
		for (Iterator<Integer> id = IDs.iterator(); id.hasNext(); ) {
			data.addExecutionTime(id.next(), clearTime);
		}
		
		// Add transactions to SystemData
		TreeSet<Integer> transactingIDs = new TreeSet<Integer>();
		for (Iterator<Transaction> i = transactions.iterator(); i.hasNext();) {
			PQTransaction t = (PQTransaction) i.next();
			// track which agents were involved in the transactions
			transactingIDs.add(t.getBuyer().getID());
			transactingIDs.add(t.getSeller().getID());
			
			data.addTransaction(t);
			lastClearPrice = t.price;
		}
		lastClearTime = clearTime;

		// update and log transactions
		for (Iterator<Integer> it = transactingIDs.iterator(); it.hasNext(); ) {
			int id = it.next();
			data.getAgent(id).updateTransactions(clearTime);
			data.getAgent(id).logTransactions(clearTime); 
		}
		orderbook.logActiveBids(clearTime);
		orderbook.logClearedBids(clearTime);
		orderbook.logFourHeap(clearTime);
		data.addDepth(this.id, clearTime, orderbook.getDepth());
		Logger.log(Logger.INFO, clearTime + " | ....." + toString() + " " + 
				this.getName() + "::clear: Order book cleared: " +
				"Post-clear Quote" + this.quote(clearTime));
		actMap.add(new SendToSIP(this, clearTime));
		return actMap;
	}
	
	
	public Quote quote(TimeStamp quoteTime) {
		Quote q = new Quote(this);
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
		
		if (bp != null) {
			lastBidPrice = bp;
			lastBidQuantity = q.lastBidQuantity;
		}
	    if (ap != null) {
	    	lastAskPrice = ap;
	    	lastAskQuantity = q.lastAskQuantity;
	    }
	    
	    return q;
	}

}

