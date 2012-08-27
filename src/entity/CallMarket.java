package entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeSet;

import market.*;
import activity.*;
import event.TimeStamp;
import systemmanager.*;

/**
 * Class for a call market.
 * 
 * @author ewah
 */
public class CallMarket extends Market {

	public float pricingPolicy;
	public PQOrderBook orderbook;
	private TimeStamp clearLatency;
	
	/**
	 * Overloaded constructor.
	 * @param marketID
	 */
	public CallMarket(int marketID, SystemData d, Log l) {
		super(marketID, d, l);
		marketType = "CALL";
		orderbook = new PQOrderBook(this.ID);
		orderbook.setParams(l, this.ID);
		clearLatency = d.clearLatency;
		nextClearTime = clearLatency;
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
	}
	
	
	public HashMap<Integer,Bid> getBids() {
		return orderbook.getActiveBids();
	}
	
	
	public Activity getClearActivity(TimeStamp currentTime) {
		return null;
	}
	
	
	public ActivityHashMap clear(TimeStamp clearTime) {
		ActivityHashMap actMap = new ActivityHashMap();
		nextClearTime = clearTime.sum(clearLatency);
		
		orderbook.logActiveBids();
		orderbook.logFourHeap();
		
		Quote q = new Quote(this);
		log.log(Log.INFO, clearTime.toString() + " | " + this.toString() + ": Quote" + q);
		ArrayList<Transaction> transactions = orderbook.uniformPriceClear(clearTime, (float) 0.5);
		if (transactions == null) {
			lastClearTime = clearTime; 
			log.log(Log.INFO, clearTime.toString() + " | " + this.getClass().getSimpleName() + 
					"::clear: Nothing transacted.");
			actMap.insertActivity(new Clear(this, nextClearTime));
			return actMap;
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
		log.log(Log.INFO, clearTime.toString() + " | " + this.toString() + " " + 
				this.getClass().getSimpleName() + " cleared: Quote" + q);
		
//		TimeStamp tsNew = ts.sum(new TimeStamp(getRandSleepTime(sleepTime, data.sleepVar)));
		actMap.insertActivity(new Clear(this, nextClearTime));
		return actMap;
	}
	
	
	
	public Quote quote(TimeStamp quoteTime) {
		Quote q = new Quote(this);
		Price bp = q.lastBidPrice;
		Price ap = q.lastAskPrice;
		
		if (bp != null && ap != null) {
			if (bp.compareTo(ap) == 1 && ap.getPrice() > 0) {
				log.log(Log.ERROR, "CallMarket::quote: ERROR bid > ask");
			} else {
				this.data.addQuote(this.ID, q);
			}
		}
		this.lastQuoteTime = quoteTime;
		
		if (bp != null) lastBidQuote = bp;
	    if (ap != null) lastAskQuote = ap;
	    
	    return q;
	}
	

//	/**
//	 * Computes a randomized time based on time & variance.
//	 * @param time
//	 * @param var
//	 * @return
//	 */
//	public int getRandClearTime(int time, double var) {
//		return (int) Math.round(getNormalRV(time, var));
//	}
}
