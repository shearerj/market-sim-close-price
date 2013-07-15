package entity;

import event.*;
import market.*;
import model.MarketModel;
import logger.Logger;
import static logger.Logger.Level.INFO;

import java.util.Collections;
import java.util.HashMap;

import java.util.Collection;

import activity.Activity;

/**
 * Class that updates pertinent information for the system. Generally used for creating NBBO update
 * events. Serves the purpose of the Security Information Processor in Regulation NMS. Is the NBBO
 * for one market model
 * 
 * @author ewah
 */
public class SIP extends IP {

	protected final int modelID; // for logging only
	protected HashMap<Market, BestBidAsk> marketQuotes; // hashed by market

	/**
	 * Constructor
	 * 
	 * @param ID
	 * @param d
	 */
	public SIP(int ID, int modelID, TimeStamp latency) {
		super(ID, latency);
		this.modelID = modelID;
		marketQuotes = new HashMap<Market, BestBidAsk>();
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
	public Collection<Activity> processQuote(Market mkt, Price bid, Price ask,
			TimeStamp ts) {
		BestBidAsk q = new BestBidAsk(mkt, bid, mkt, ask);
		marketQuotes.put(mkt, q);
		Logger.log(INFO, ts + " | " + this + " | " + mkt + " "
				+ "ProcessQuote: " + q);
		return null;
	}

	/**
	 * Find best quote across given markets (lowest ask & highest bid). Note: for Single Markets,
	 * only one market will contain data and key thus it will simply get BBO for one market.
	 * 
	 * @param marketIDs
	 * @param nbbo
	 *            true if getting NBBO, false if getting global quote
	 * @return
	 */
	protected BestBidAsk computeBestBidOffer(Collection<Market> markets) {
		Price bestBid = null, bestAsk = null;
		Market bestBidMkt = null, bestAskMkt = null;

		for (Market mkt1 : markets) {
			Price bid, ask;
			BestBidAsk ba = marketQuotes.get(mkt1.getID());
			if (marketQuotes.containsKey(mkt1.getID())) {
				ba = marketQuotes.get(mkt1.getID());
				bid = ba.getBestBid();
				ask = ba.getBestAsk();
			} else {
				bid = null;
				ask = null;
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
	 * Method to update the BBO values. In this case, for the NBBO.
	 */
	// TODO Since SIP is the only multimarket IP we have, these should probably all be called NBBO
	public Collection<Activity> updateBBO(MarketModel model, TimeStamp ts) {
		String s = ts + " | " + model.getMarkets() + " UpdateNBBO: current "
				+ getBBOQuote() + " --> ";

		BestBidAsk lastQuote = computeBestBidOffer(model.getMarkets());

		Price bestBid = lastQuote.getBestBid();
		Price bestAsk = lastQuote.getBestAsk();
		if ((bestBid != null) && (bestAsk != null)) {
			// check for inconsistency in buy/sell prices & fix if found
			if (lastQuote.getBestBid().compareTo(lastQuote.getBestAsk()) > 0) {
				// FIXME should figure out best way to handle price discrepencies.
				int mid = (lastQuote.getBestBid().getPrice() + lastQuote.getBestAsk().getPrice()) / 2;
				// Removed the tick increment from old fix, mainly for ease of use. What would the
				// appropriate tick size for the SIP be anyways, in terms of this fix? Seems like
				// just the midpoint will work fine.
				bestBid = new Price(mid);
				bestAsk = new Price(mid);
				s += " (before fix) " + lastQuote + " --> ";

				// Add spread of INF if inconsistent NBBO quote
				model.addNBBOSpread(ts, Price.INF.getPrice());
			} else {
				// if bid-ask consistent, store the spread
				model.addNBBOSpread(ts, lastQuote.getSpread());
			}
		} else {
			// store spread of INF since no bid-ask spread
			model.addNBBOSpread(ts, Price.INF.getPrice());
		}
		lastQuote = new BestBidAsk(lastQuote.getBestBidMarket(), bestBid,
				lastQuote.getBestAskMarket(), bestAsk);
		lastQuotes = lastQuote;
		Logger.log(INFO, s + "updated " + lastQuotes);
		return Collections.emptyList();
	}

	public String toString() {
		return "SIP number " + id + ", model number " + modelID;
	}
}