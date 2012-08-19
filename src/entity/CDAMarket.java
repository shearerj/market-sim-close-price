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
		marketType = "CDA";
		orderbook = new PQOrderBook(this.ID);
		orderbook.setParams(l, this.ID);
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
	
	public void addBid(Bid b) {
		orderbook.insertBid((PQBid) b);
	}
	
	
	public void removeBid(int agentID) {
		orderbook.removeBid(agentID);
		orderbook.logActiveBids();
		orderbook.logFourHeap();
		
//		// replace with empty bid
//		PQBid emptyBid = new PQBid(agentID, this.ID);
//		emptyBid.addPoint(0, new Price(0));
//		this.data.bidData.put(agentID, emptyBid);
	}
	
	
	public HashMap<Integer,Bid> getBids() {
		return orderbook.getActiveBids();
	}
	
	
	public ActivityHashMap clear(TimeStamp clearTime) {
		orderbook.logActiveBids();
		orderbook.logFourHeap();
		
		Quote q = new Quote(this);
		ArrayList<Transaction> transactions = orderbook.earliestPriceClear(clearTime);
		if (transactions == null) {
			lastClearTime = clearTime;
			log.log(Log.INFO, clearTime.toString() + " | STATUS: Nothing transacted.");
			log.log(Log.INFO, clearTime.toString() + " | " + this.toString() + ": Quote" + q);
			return null;
		}
		TreeSet<Integer> transactingIDs = new TreeSet<Integer>();
		// add transactions to SystemData
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
		orderbook.logActiveBids();
		orderbook.logClearedBids();
		orderbook.logFourHeap();
		log.log(Log.INFO, clearTime.toString() + " | " + this.toString() + ": Quote" + q);
		return null;
	}

	
	
	public Quote quote(TimeStamp quoteTime) {
		Quote q = new Quote(this);
		Price bp = q.lastBidPrice;
		Price ap = q.lastAskPrice;
		
		if (bp != null && ap != null) {
			if (bp.compareTo(ap) == 1 && ap.getPrice() > 0) {
				log.log(Log.ERROR, "CDAMArket::quote: ERROR bid > ask");
			} else {
				this.data.addQuote(this.ID, q);
			}
		}
		this.lastQuoteTime = quoteTime;
		
		if (bp != null) lastBidQuote = bp;
	    if (ap != null) lastAskQuote = ap;
	    
	    return q;
	}

}

