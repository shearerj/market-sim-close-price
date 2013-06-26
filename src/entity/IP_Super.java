package entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import market.BestBidAsk;
import model.MarketModel;
import systemmanager.Consts;
import systemmanager.Log;
import data.ObjectProperties;
import data.SystemData;
import activity.ActivityHashMap;
import event.TimeStamp;

/**
 * Superclass for Information Processors that either work
 * for one model or multiple.
 * 
 * @author ewah
 *
 */
public abstract class IP_Super extends Entity {
	
	protected int tickSize;
	protected HashMap<Integer, BestBidAsk> lastQuotes;		// hashed by model ID
	protected HashMap<Integer, BestBidAsk> marketQuotes;		// hashed by market ID

	/**
	 * Constructor
	 * @param ID
	 * @param d
	 */
	public IP_Super(int ID, SystemData d, Log l) {
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
	 * Find best quote across given markets (lowest ask & highest bid).
	 * 
	 * @param marketIDs
	 * @param nbbo		true if getting NBBO, false if getting global quote
	 * @return
	 */
	protected BestBidAsk computeBestBidOffer(ArrayList<Integer> marketIDs, boolean nbbo) {
		// check if goes through all models?
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
	
	/**
 	 * Store market's best bid/ask & insert Activity to UpdateNBBO at some amount of time 
 	 *
 	 * @param mkt
 	 * @param bid
 	 * @param ask
 	 * @param ts
 	 * @return ActivityHashMap
 	 */
	public ActivityHashMap processQuote(Market mkt, int bid, int ask, TimeStamp ts) {
		int mktID = mkt.getID();
		BestBidAsk q = new BestBidAsk();
		q.bestBid = bid;
		q.bestAsk = ask;
		q.bestBidMarket = mktID;
		q.bestAskMarket = mktID;
		marketQuotes.put(mktID, q);
		log.log(Log.INFO, ts + " | " + this + " | "+ data.getMarket(mktID) + " " + 
				"ProcessQuote: " + q);
		return null;
	}
	
	public abstract String toString();
	
	public abstract ActivityHashMap updateNBBO(MarketModel model, TimeStamp ts);
}
