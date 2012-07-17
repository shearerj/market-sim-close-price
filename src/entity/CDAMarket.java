package entity;

import event.TimeStamp;
import activity.*;
import systemmanager.*;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;

import market.*;

/**
 * Class for a continuous double auction.
 * 
 * @author ewah
 */
public class CDAMarket extends Market {

	public PQOrderBook orderbook;
	
	/**
	 * Overloaded constructor.
	 * @param marketID
	 */
	public CDAMarket(int marketID, SystemData d) {
		super(marketID, d);
		orderbook = new PQOrderBook(this.ID);
		marketType = "CDA";
	}

	public Bid getBidQuote() {
		return this.orderbook.getBidQuote();
	}
	
	public Bid getAskQuote() {
		return this.orderbook.getAskQuote();
	}
	
	
	public void addBid(Bid b) {
		orderbook.insertBid((PQBid) b);
		this.data.addBid(b.getBidID(), (PQBid) b);
	}
	
	
	public void removeBid(Bid b) {
		orderbook.removeBid(b.getAgentID());
		// replace with empty bid
		PQBid emptyBid = new PQBid(b.getAgentID(), this.ID);
		emptyBid.addPoint(0, new Price(0));	
		this.data.bidData.put(b.getBidID(), emptyBid);
	}
	
	
	
	public ActivityHashMap clear(TimeStamp clearTime) {
//		System.out.print("Market " + this.ID + " ACTIVITY: Clear: ");
		orderbook.logActiveBids();
		
		ArrayList<Transaction> transactions = orderbook.earliestPriceClear(clearTime);
		if (transactions == null) {
			this.lastClearTime = clearTime; 
			System.out.println("-------------------Nothing transacted.");
			return null;
		}
		// add transactions to SystemData
		for (Iterator<Transaction> i = transactions.iterator(); i.hasNext();) {
			PQTransaction t = (PQTransaction) i.next();
			this.data.addTransaction(t);
			lastClearPrice = t.price;
		}
		// add bids to SystemData
		for (Iterator<Bid> i = orderbook.getClearedBids().values().iterator(); i.hasNext(); ) {
			PQBid b = (PQBid) i.next();
//			TimeStamp closeTime = new TimeStamp(0);
//			if (!b.containsBuyOffers() && !b.containsSellOffers()) closeTime = clearTime;
			this.data.addBid(b.getBidID(), b);
		}
		lastClearTime = clearTime;

		orderbook.logClearedBids();
		return null;
	}

	
	
	public Quote quote(TimeStamp quoteTime) {
		Quote q = new Quote(this);
		Price bp = q.lastBidPrice;
		Price ap = q.lastAskPrice;
		
		if (bp != null && ap != null) {
			if (bp.compareTo(ap) == 1 && ap.getPrice() > 0) {
				System.out.println("ERROR bid > ask");
				// TODO log
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

