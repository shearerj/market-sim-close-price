package entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import market.BestBidAsk;
import market.Price;
import model.MarketModel;
import java.util.Collection;
import logger.Logger;

import data.ObjectProperties;
import data.SystemData;
import event.TimeStamp;
import activity.Activity;

/**
 * Superclass for Information Processors that either work
 * for one model or multiple.
 * 
 * @author ewah
 *
 */
public abstract class AbstractIP extends Entity {
	
	protected int tickSize;
	protected HashMap<Integer, BestBidAsk> lastQuotes;		// hashed by model ID
	protected HashMap<Integer, BestBidAsk> marketQuotes;		// hashed by market ID

	/**
	 * Constructor
	 * @param ID
	 * @param d
	 */
	public AbstractIP(int ID, SystemData d) {
		super(ID, d, new ObjectProperties());
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
		Collection<Market> markets = new ArrayList<Market>(marketIDs.size());
		for (int mktID : marketIDs)
			markets.add(data.getMarket(mktID));
		return computeBestBidOffer(markets, nbbo);
	}
	
	/**
	 * Find best quote across given markets (lowest ask & highest bid).
	 * 
	 * @param marketIDs
	 * @param nbbo		true if getting NBBO, false if getting global quote
	 * @return
	 */
	protected BestBidAsk computeBestBidOffer(Collection<Market> markets, boolean nbbo) {
	    Price bestBid = null, bestAsk = null;
	    Market bestBidMkt = null, bestAskMkt = null;
	    
	    for (Market mkt : markets) {
			Price bid, ask;
			
			if (nbbo) {
				// NBBO quote (may be delayed)
				BestBidAsk ba = new BestBidAsk();
				if (marketQuotes.containsKey(mkt.getID())) {
					ba  = marketQuotes.get(mkt.getID());
				}
				bid = ba.bestBid;
				ask = ba.bestAsk;
			} else {
				// global quote
				bid = mkt.getBidPrice();
				ask = mkt.getAskPrice();
			}

			// FIXME This seems wrong, should the conditional also assume bid != null?
			// Best bid quote is highest BID
			if (bestBid == null || bestBid.compareTo(bid) < 0) {
				if (bid != null) bestBid = bid;
				bestBidMkt = mkt;
			}
			// Best ask quote is lowest ASK
			if (bestAsk == null || bestAsk.compareTo(ask) > 0) {
				if (ask != null) bestAsk = ask;
				bestAskMkt = mkt;
			}
	    }
	    return new BestBidAsk(bestBidMkt, bestBid, bestAskMkt, bestAsk);
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
	public Collection<Activity> processQuote(Market mkt, int bid, int ask, TimeStamp ts) {
		int mktID = mkt.getID();
		BestBidAsk q = new BestBidAsk(mkt, new Price(bid), mkt, new Price(ask));
		marketQuotes.put(mktID, q);
		Logger.log(Logger.INFO, ts + " | " + this + " | "+ data.getMarket(mktID) + " " + 
				"ProcessQuote: " + q);
		return null;
	}
	
	public abstract String toString();
	
	public abstract Collection<Activity> updateNBBO(MarketModel model, TimeStamp ts);
}
