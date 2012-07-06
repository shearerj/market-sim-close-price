package entity;

import event.TimeStamp;
import activity.*;
import activity.market.*;
import systemmanager.*;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;

/**
 * Class for a continuous double auction.
 * 
 * @author ewah
 */
public class CDAMarket extends Market {

	/**
	 * Overloaded constructor.
	 * @param marketID
	 */
	public CDAMarket(int marketID, SystemData d) {
		super(marketID, d);
	}

	public ActivityHashMap clear(TimeStamp clearTime) {
		
		ArrayList<Transaction> transactions = null;

		if (config.matching_fn.equals("earliest")) {
//			log(Log.INFO, "ScriptedAuction::clear earliest " + getID());
			transactions = orderbook.earliestPriceClear(clearTime);
		} else if (config.matching_fn.equals("uniform")) {
//			log(Log.INFO, "ScriptedAuction::clear uniform,  " + getID());
			transactions = orderbook.uniformPriceClear(clearTime, config.pricing_k);
//		} else if (config.matching_fn.equals("bbvickrey")) {
////			log(Log.INFO, "ScriptedAuction::clear bbvickrey, " + getID());
//			transactions = orderbook.BBVickreyPriceClear((clearTime)); // TODO - not implemented
		} else {
//			log(Log.INFO, "ScriptedAuction::clear, undefined matching function-->" + config.matching_fn + "<--");
		}

		if (transactions == null) {
			this.lastClearTime = clearTime;  // replaces postClear
			return null; // TODO
		}
		
		// add transactions to SystemData
		for (Iterator<Transaction> i = transactions.iterator(); i.hasNext();) {
			PQTransaction t = (PQTransaction) i.next();
			this.data.addTransaction(t);
			lastClearPrice = t.price;
		}

		// add bids to SystemData
		for (Enumeration e = orderbook.getClearedBids(); e != null && e.hasMoreElements();) {
			PQBid b = (PQBid) e.nextElement();
			TimeStamp closeTime;
			if (!b.containsBuyOffers() && !b.containsSellOffers())
				closeTime = clearTime;
			else closeTime = new TimeStamp(0);
			this.data.addBid(b.getBidID(), b);
		}
		this.lastClearTime = clearTime;

		return null;
	}

	
	public void quote(TimeStamp quoteTime) {

		PQBid b = (PQBid) orderbook.getBidQuote(0);
		PQBid a = (PQBid) orderbook.getAskQuote(0);
		
		PQPoint bq = null, aq = null;
		if (b != null && a != null) {
			bq = b.bidArray[0];
			aq = a.bidArray[0];

			if (bq.comparePrice(aq) == 1 && aq.getprice().getPrice() > 0) {
				//log(Log.ERROR, "ScriptedAuction::quote bid > ask " + bq.getprice() + ">" + aq.getprice());
				orderbook.logBids();
			}
		}
//		gameCache.putQuotes(orderbook.getQuoteString(config.bid_btq_delta),
//                orderbook.getAgentQuoteStrings(),
//                getLastClearPrice(),
//                ts,
//                getLastClearTime(),
//                getStatus());

		
		this.data.addQuote(this.ID, null);
		
		this.lastQuoteTime = quoteTime; // replaces postQuote
		
		if (bq != null) lastBidQuote = bq.getprice();
	    if (aq != null) lastAskQuote = aq.getprice();
	}

	public ActivityHashMap processBid(Bid b) {

		PQBid bid = (PQBid) b;

		/* does the bid have buy and sell offers? */
		int agentid = bid.getAgentID();
		
		//SELL PERMISSIONS
		if (bid.containsSellOffers() && !canSell(agentid)) {
//			log(Log.DEBUG, "processBid, sell permission reject");
			//notify system data? TODO
			return null;
		}

		//BUY PERMISSIONS
		if (bid.containsBuyOffers() && !canBuy(agentid)) {
//			log(Log.DEBUG, "processBid, buy permission reject");
			// notify system data
			return null;
		}

		Bid priorBid = orderbook.getBid(agentid);
		//REPLACE BID, check that bidID matches (not bidHash)
		if (bid.bidID != null) {
			if (priorBid == null) {
//				log(Log.DEBUG, "trying to replace null prior bid");
//				gameCache.notifyCacheBidRejected //NOT ACTIVE?
//				(bid, "replace, not found", TACProtocol.RR_BID_NOT_FOUND);
				return null;
			} else if (!priorBid.bidID.equals(bid.bidID)) {
//				log(Log.DEBUG, "processBid, bid changed, this // previous: "
//				gameCache.notifyCacheBidRejected
//				(bid, "replace, bid changed ", TACProtocol.RR_ACTIVE_BID_CHANGED);
				return null;
			}
		}

		if (priorBid != null) {
//			log(Log.DEBUG, "ScriptedAuction::processBid, prior bid not null: ");

			//BUY DOMINANCE
			int b_dom = bid.bDominates(priorBid);
			if ((config.bid_dominance_buy.equals("ascending") && b_dom < 1) ||
					(config.bid_dominance_buy.equals("descending") && b_dom > -1)) {
//				log(Log.DEBUG, "processBid, buy dominance reject");
//				gameCache.notifyCacheBidRejected
//				(bid, "buy dominance", TACProtocol.RR_BID_NOT_IMPROVED);
				return null;
			}
			//SELL DOMINANCE
			int s_dom = bid.sDominates(priorBid); // 1, 0, -1  (1 if true)
			if ((config.bid_dominance_sell.equals("ascending") && s_dom < 1) ||
					(config.bid_dominance_sell.equals("descending") && s_dom > -1)) {
				// LOGGING / notify system data
				return null;
			}
		}
		
		return null;
	}

	public Price getLastClearPrice() {
		return lastClearPrice;
	}

	public TimeStamp getFinalClearTime() {
		return finalClearTime;
	}

	public TimeStamp getNextClearTime() {
		return nextClearTime;
	}

	public TimeStamp getLastClearTime() {
		return lastClearTime;
	}

	public TimeStamp getNextQuoteTime() {
		return nextQuoteTime;
	}

	public TimeStamp getLastQuoteTime() {
		return lastQuoteTime;
	}


}

