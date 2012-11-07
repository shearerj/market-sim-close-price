package entity;

import market.*;
import event.TimeStamp;
import activity.*;
import systemmanager.*;

import java.util.ArrayList;
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
	public CDAMarket(int marketID, SystemData d, Log l) {
		super(marketID, d, l);
		marketType = Consts.getMarketType(this.getClass().getSimpleName());
		orderbook = new PQOrderBook(this.ID);
		orderbook.setParams(this.ID, l, d);
	}

	public Bid getBidQuote() {
		return this.orderbook.getBidQuote();
	}
	
	public Bid getAskQuote() {
		return this.orderbook.getAskQuote();
	}
	
	public Price getBidPrice() {	
		return ((PQBid) getBidQuote()).bidTreeSet.first().getPrice();
	}
	
	public Price getAskPrice() {
		return ((PQBid) getAskQuote()).bidTreeSet.last().getPrice();
	}
	
	public ActivityHashMap addBid(Bid b, TimeStamp ts) {
		orderbook.insertBid((PQBid) b);
		ActivityHashMap actMap = new ActivityHashMap();
		actMap.insertActivity(Consts.CDA_CLEAR_PRIORITY, new Clear(this, ts));
		this.data.addDepth(this.ID, ts, orderbook.getDepth());
		this.data.addSubmissionTime(b.getBidID(), ts);
		return actMap;
	}
	
	
	public ActivityHashMap removeBid(int agentID, TimeStamp ts) {
		orderbook.removeBid(agentID);
		orderbook.logActiveBids(ts);
		orderbook.logFourHeap(ts);
		this.data.addDepth(this.ID, ts, orderbook.getDepth());
		return null;
	}
	
	
	public HashMap<Integer,Bid> getBids() {
		return orderbook.getActiveBids();
	}

	
	public ActivityHashMap clear(TimeStamp clearTime) {
		orderbook.logActiveBids(clearTime);
		orderbook.logFourHeap(clearTime);
		
		log.log(Log.INFO, clearTime.toString() + " | " + this.toString() + " Prior Quote" + 
				this.quote(clearTime));	
		ArrayList<Transaction> transactions = orderbook.earliestPriceClear(clearTime);
		
		if (transactions == null) {
			lastClearTime = clearTime;
			
			orderbook.logActiveBids(clearTime);
			orderbook.logFourHeap(clearTime);
			log.log(Log.INFO, clearTime.toString() + " | " + this.toString() + " " + 
					this.getClass().getSimpleName() + "::clear: Nothing transacted. Post Quote" 
					+ this.quote(clearTime));
			return null;
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
		this.data.addDepth(this.ID, clearTime, orderbook.getDepth());
		log.log(Log.INFO, clearTime.toString() + " | " + this.toString() + " " +
				this.getClass().getSimpleName() + " cleared: Post Quote" + this.quote(clearTime));
		return null;
	}
	
	
	public Quote quote(TimeStamp quoteTime) {
		Quote q = new Quote(this);
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

