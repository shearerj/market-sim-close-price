package entity;

import market.*;
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
	public CDAMarket(int marketID, SystemData d, ObjectProperties p, Log l) {
		super(marketID, d, p, l);
		marketType = Consts.getMarketType(this.getName());
		orderbook = new PQOrderBook(ID);
		orderbook.setParams(ID, l, d);
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
		data.addDepth(ID, ts, orderbook.getDepth());
		data.addSubmissionTime(b.getBidID(), ts);
		return clear(ts);
	}
	
	
	public Collection<Activity> removeBid(int agentID, TimeStamp ts) {
		orderbook.removeBid(agentID);
		data.addDepth(this.ID, ts, orderbook.getDepth());
		return clear(ts);
	}
	
	
	public HashMap<Integer,Bid> getBids() {
		return orderbook.getActiveBids();
	}

	
	public Collection<Activity> clear(TimeStamp clearTime) {
		Collection<Activity> actMap = new ArrayList<Activity>();
		orderbook.logActiveBids(clearTime);
		orderbook.logFourHeap(clearTime);
		
		log.log(Log.INFO, clearTime + " | " + this + " Prior-clear Quote" + 
				this.quote(clearTime));
		ArrayList<Transaction> transactions = orderbook.earliestPriceClear(clearTime);
		
		if (transactions == null) {
			lastClearTime = clearTime;
			
			orderbook.logActiveBids(clearTime);
			orderbook.logFourHeap(clearTime);
			data.addDepth(ID, clearTime, orderbook.getDepth());
			
			log.log(Log.INFO, clearTime + " | ....." + this + " " + 
					this.getName() + "::clear: No change. Post-clear Quote" +  
					this.quote(clearTime));
			actMap.add(new SendToSIP(this, clearTime));
			return actMap;
		}
		
		// Add bid execution speed
		ArrayList<Integer> IDs = orderbook.getClearedBidIDs();
		for (Iterator<Integer> id = IDs.iterator(); id.hasNext(); ) {
			data.addTimeToExecution(id.next(), clearTime);
		}
		
		// Add transactions to SystemData
		TreeSet<Integer> transactingIDs = new TreeSet<Integer>();
		for (Iterator<Transaction> i = transactions.iterator(); i.hasNext();) {
			PQTransaction t = (PQTransaction) i.next();
			// track which agents were involved in the transactions
			transactingIDs.add(t.buyerID);
			transactingIDs.add(t.sellerID);
			
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
		data.addDepth(this.ID, clearTime, orderbook.getDepth());
		log.log(Log.INFO, clearTime + " | ....." + toString() + " " + 
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
				data.addSpread(ID, quoteTime, Consts.INF_PRICE);
				data.addMidQuotePrice(ID, quoteTime, Consts.INF_PRICE, Consts.INF_PRICE);
				
			} else if (bp.compareTo(ap) == 1 && ap.getPrice() > 0) {
				log.log(Log.ERROR, this.getName() + "::quote: ERROR bid > ask");
				data.addSpread(ID, quoteTime, Consts.INF_PRICE);
				data.addMidQuotePrice(ID, quoteTime, Consts.INF_PRICE, Consts.INF_PRICE);
				
			} else {
				// valid bid-ask
				data.addQuote(ID, q);
				data.addSpread(ID, quoteTime, q.getSpread());
				data.addMidQuotePrice(ID, quoteTime, bp.getPrice(), ap.getPrice());
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

