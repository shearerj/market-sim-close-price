package entity;

import java.util.ArrayList;
import java.util.HashMap;
import static logger.Logger.Level.INFO;

import market.BestBidAsk;
import market.Price;
import model.MarketModel;
import java.util.Collection;
import logger.Logger;
import event.TimeStamp;
import activity.Activity;
import activity.ProcessQuote;

/**
 * Superclass for Information Processors that either work
 * for one model or multiple.
 * 
 * @author ewah
 *
 */
public abstract class AbstractIP extends Entity {
	
	protected TimeStamp latency;
	protected BestBidAsk lastQuotes;		// hashed by model ID
	protected HashMap<Integer, BestBidAsk> marketQuotes;		// hashed by market ID

	/**
	 * Constructor
	 * @param ID
	 * @param d
	 */
	public AbstractIP(int ID, TimeStamp latency) {
		super(ID);
		this.latency = latency;
		lastQuotes = new BestBidAsk(null, null, null, null);
		marketQuotes = new HashMap<Integer,BestBidAsk>();
	}
	
	public ProcessQuote scheduleProcessQuote(Market market, int bid, int ask, TimeStamp ts) {
		return new ProcessQuote(this, market, bid, ask, ts.plus(latency));
	}
	 
	/**
	 * Get BestBidAsk quote for the given model.
	 * 
	 * @param modelID
	 * @return BestBidAsk
	 */
	public BestBidAsk getNBBOQuote() {
			return lastQuotes;
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
	 * Note: for Single Markets, only one market will contain data and key
	 * thus it will simply get BBO for one market.
	 * 
	 * @param marketIDs
	 * @param nbbo		true if getting NBBO, false if getting global quote
	 * @return
	 */
	protected BestBidAsk computeBestBidOffer(Collection<Market> markets, boolean nbbo) {
	    Price bestBid = null, bestAsk = null;
	    Market bestBidMkt = null, bestAskMkt = null;
	    
	    for (Market mkt1 : markets) {
			Price bid, ask;
			
			if (nbbo) {
				// NBBO quote (may be delayed)
				BestBidAsk ba = marketQuotes.get(mkt1.getID());
				if (marketQuotes.containsKey(mkt1.getID())) {
					ba  = marketQuotes.get(mkt1.getID());
					bid = ba.getBestBid();
					ask = ba.getBestAsk();
				}
				else {
					bid = null;
					ask = null;
				}
			} else {
				// global quote
				BestBidAsk ba = marketQuotes.get(mkt1.getID()); // unsure, think should be same
				if (marketQuotes.containsKey(mkt1.getID())) {
					ba  = marketQuotes.get(mkt1.getID());
					bid = ba.getBestBid();
					ask = ba.getBestAsk();
				}
				else {
					bid = null;
					ask = null;
				}
			}

			// FIXME This seems wrong, should the conditional also assume bid != null?
			// Best bid quote is highest BID
			if (bestBid == null || bestBid.compareTo(bid) < 0) {
				if (bid != null) bestBid = bid;
				bestBidMkt = mkt1;
			}
			// Best ask quote is lowest ASK
			if (bestAsk == null || bestAsk.compareTo(ask) > 0) {
				if (ask != null) bestAsk = ask;
				bestAskMkt = mkt1;
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
		Logger.log(INFO, ts + " | " + this + " | "+ data.getMarket(mktID) + " " + 
				"ProcessQuote: " + q);
		return null;
	}
	
	public abstract String toString();
	
	public abstract Collection<Activity> updateNBBO(MarketModel model, TimeStamp ts);
}
