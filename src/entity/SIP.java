package entity;

import event.*;
import model.*;
import market.*;
import activity.Activity;
import systemmanager.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.HashMap;


/**
 * Class that updates pertinent information for the system. 
 * Generally used for creating NBBO update events. Always has ID = 0.
 * Serves the purpose of the Security Information Processor in Regulation NMS.
 * 
 * @author ewah
 */
public class SIP extends Entity {

	private int tickSize;
	private HashMap<Integer, BestBidAsk> lastQuotes;		// hashed by model ID
	private HashMap<Integer, BestBidAsk> marketQuotes;		// hashed by market ID

	/**
	 * Constructor
	 * @param ID
	 * @param d
	 */
	public SIP(int ID, SystemData d, Log l) {
		super(ID, d, new ObjectProperties(), l);
		tickSize = d.tickSize;
		lastQuotes = new HashMap<Integer,BestBidAsk>();
		marketQuotes = new HashMap<Integer,BestBidAsk>();
	}
	
	/**
	 * Get BestBidAsk quote for the given model.
	 * 
	 * @param modelID
	 * @return BestBidAsk
	 */
	public BestBidAsk getNBBOQuote(int modelID) {
		if (!lastQuotes.containsKey(modelID)) {
			BestBidAsk b = new BestBidAsk();
			lastQuotes.put(modelID, b);
			return b;
		} else {
			return lastQuotes.get(modelID);
		}
	}

	/**
 	 * Get global BestBidAsk quote for the given model.
 	 *
 	 * @param modelID
 	 * @return BestBidAsk
 	 */
	public BestBidAsk getGlobalQuote(int modelID) {
		return this.computeBestBidOffer(data.getModel(modelID).getMarketIDs(), false);
	}

	/**
 	 * Store market's best bid/ask & insert Activity to UpdateNBBO at some amount of time 
 	 *
 	 * @param mkt
 	 * @param bid
 	 * @param ask
 	 * @param ts
 	 * @return Collection<Activity>
 	 */
	public Collection<Activity> processQuote(Market mkt, int bid, int ask, TimeStamp ts) {
		int mktID = mkt.getID();
		BestBidAsk q = new BestBidAsk();
		q.bestBid = bid;
		q.bestAsk = ask;
		q.bestBidMarket = mktID;
		q.bestAskMarket = mktID;
		marketQuotes.put(mktID, q);
		log.log(Log.INFO, ts + " | " + data.getMarket(mktID) + " " + 
				"ProcessQuote: " + q);
		return Collections.emptyList();
	}
	
	/**
	 * Method to update the NBBO values.
	 * 
	 * @param model
	 * @param ts
	 * @return
	 */
	public Collection<Activity> updateNBBO(MarketModel model, TimeStamp ts) {
		
		Collection<Activity> actMap = new ArrayList<Activity>();
		
		int modelID = model.getID();
		ArrayList<Integer> ids = model.getMarketIDs();
		String s = ts + " | " + ids + " UpdateNBBO: current " + getNBBOQuote(modelID)
				+ " --> ";
	
		BestBidAsk lastQuote = computeBestBidOffer(ids, true);
			
		int bestBid = lastQuote.bestBid;
		int bestAsk = lastQuote.bestAsk;
		if ((bestBid != -1) && (bestAsk != -1)) {
			// check for inconsistency in buy/sell prices & fix if found
			if (lastQuote.bestBid > lastQuote.bestAsk) {
				int mid = (lastQuote.bestBid + lastQuote.bestAsk) / 2;
				bestBid = mid - this.tickSize;
				bestAsk = mid + this.tickSize;
				s += " (before fix) " + lastQuote + " --> ";
				
				// Add spread of INF if inconsistent NBBO quote
				this.data.addNBBOSpread(modelID, ts, Consts.INF_PRICE);
			} else {
				// if bid-ask consistent, store the spread
				this.data.addNBBOSpread(modelID, ts, lastQuote.getSpread());
			}
		} else {
			// store spread of INF since no bid-ask spread
			this.data.addNBBOSpread(modelID, ts, Consts.INF_PRICE);
		}
		lastQuote.bestBid = bestBid;
		lastQuote.bestAsk = bestAsk;
		lastQuotes.put(modelID, lastQuote);
		log.log(Log.INFO, s + "updated " + lastQuote);
		return actMap;
	}

	/**
	 * Find best quote across given markets (lowest ask & highest bid).
	 * 
	 * @param marketIDs
	 * @param nbbo		true if getting NBBO, false if getting global quote
	 * @return
	 */
	private BestBidAsk computeBestBidOffer(ArrayList<Integer> marketIDs, boolean nbbo) {
		
	    int bestBid = -1;
	    int bestBidMkt = 0;
	    int bestAsk = -1;
	    int bestAskMkt = 0;
	    
	    for (Iterator<Integer> it = marketIDs.iterator(); it.hasNext(); ) {
			int mktID = it.next();
			int bid = -1, ask = -1;
			
			if (nbbo) {
				// NBBO quote (may be delayed)
				BestBidAsk ba = new BestBidAsk();
				if (marketQuotes.containsKey(mktID)) {
					ba  = marketQuotes.get(mktID);
				}
				bid = ba.bestBid;
				ask = ba.bestAsk;
			} else {
				// global quote
				bid = data.getMarket(mktID).getBidPrice().getPrice();
				ask = data.getMarket(mktID).getAskPrice().getPrice();
			}

			// Best bid quote is highest BID
			if (bestBid == -1 || bestBid < bid) {
				if (bid != -1) bestBid = bid;
				bestBidMkt = mktID;
			}
			// Best ask quote is lowest ASK
			if (bestAsk == -1 || bestAsk > ask) {
				if (ask != -1) bestAsk = ask;
				bestAskMkt = mktID;
			}
	    }
	    BestBidAsk q = new BestBidAsk();
	    q.bestBidMarket = bestBidMkt;
	    q.bestBid = bestBid;
	    q.bestAskMarket = bestAskMkt;
	    q.bestAsk = bestAsk;
	    return q;
	}
}
