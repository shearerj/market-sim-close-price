package entity;

import static logger.Logger.log;
import static logger.Logger.Level.INFO;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;

import market.BestBidAsk;
import market.Price;
import model.MarketModel;
import systemmanager.Consts;
import activity.Activity;
import event.TimeStamp;

/**
 * Class that updates pertinent information for the system. Generally used for
 * creating NBBO update events. Always has ID = 0. Serves the purpose of the
 * Security Information Processor in Regulation NMS.
 * 
 * @author ewah
 */
public class SIP extends Entity {

	private int tickSize;
	private HashMap<Integer, BestBidAsk> lastQuotes; // hashed by model ID
	private HashMap<Integer, BestBidAsk> marketQuotes; // hashed by market ID

	public SIP(int id, int tickSize) {
		super(id);
		this.tickSize = tickSize;
		this.lastQuotes = new HashMap<Integer, BestBidAsk>();
		this.marketQuotes = new HashMap<Integer, BestBidAsk>();
	}

	/**
	 * Get BestBidAsk quote for the given model.
	 * 
	 * @param modelID
	 * @return BestBidAsk
	 */
	public BestBidAsk getNBBOQuote(int modelID) {
		if (!lastQuotes.containsKey(modelID)) {
			// FIXME This shouldn't be done / shouldn't happen
			BestBidAsk b = new BestBidAsk(null, null, null, null);
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
		return this.computeBestBidOffer(data.getModel(modelID).getMarketIDs(),
				false);
	}

	/**
	 * Process and store new quotes for the given market.
	 * 
	 * @param mkt
	 * @param bid
	 * @param ask
	 * @param ts
	 * @return Collection<Activity>
	 */
	public Collection<Activity> processQuote(Market mkt, int bid, int ask,
			TimeStamp ts) {
		BestBidAsk q = new BestBidAsk(mkt, new Price(bid), mkt, new Price(ask));
		marketQuotes.put(mkt.getID(), q);
		log(INFO, ts + " | " + mkt + " " + "ProcessQuote: " + q);
		return updateNBBO(mkt.model, ts);
	}

	/**
	 * Method to update the NBBO values.
	 * 
	 * @param model
	 * @param ts
	 * @return
	 */
	public Collection<Activity> updateNBBO(MarketModel model, TimeStamp ts) {
		int modelID = model.getID();
		ArrayList<Integer> ids = model.getMarketIDs();
		String s = ts + " | " + ids + " UpdateNBBO: current "
				+ getNBBOQuote(modelID) + " --> ";

		BestBidAsk lastQuote = computeBestBidOffer(ids, true);

		Price bestBid = lastQuote.getBestBid();
		Price bestAsk = lastQuote.getBestAsk();
		if ((bestBid != null) && (bestAsk != null)) {
			// check for inconsistency in buy/sell prices & fix if found
			if (lastQuote.getBestBid().compareTo(lastQuote.getBestAsk()) > 0) {
				int mid = (lastQuote.getBestBid().getPrice() + lastQuote.getBestAsk().getPrice()) / 2;
				bestBid = new Price(mid - this.tickSize);
				bestAsk = new Price(mid + this.tickSize);
				s += " (before fix) " + lastQuote + " --> ";

				// Add spread of INF if inconsistent NBBO quote
				model.addNBBOSpread(ts, Consts.INF_PRICE.getPrice());
			} else {
				// if bid-ask consistent, store the spread
				model.addNBBOSpread(ts, lastQuote.getSpread());
			}
		} else {
			// store spread of INF since no bid-ask spread
			model.addNBBOSpread(ts, Consts.INF_PRICE.getPrice());
		}
		lastQuote = new BestBidAsk(lastQuote.getBestBidMarket(), bestBid,
				lastQuote.getBestAskMarket(), bestAsk);
		lastQuotes.put(modelID, lastQuote);
		log(INFO, s + "updated " + lastQuote);
		return Collections.emptyList();
	}

	/**
	 * Find best quote across given markets (lowest ask & highest bid).
	 * 
	 * @param marketIDs
	 * @param nbbo
	 *            true if getting NBBO, false if getting global quote
	 * @return
	 */
	private BestBidAsk computeBestBidOffer(ArrayList<Integer> marketIDs,
			boolean nbbo) {
		Collection<Market> markets = new ArrayList<Market>(marketIDs.size());
		for (int mktID : marketIDs)
			markets.add(data.getMarket(mktID));
		return computeBestBidOffer(markets, nbbo);
	}

	/**
	 * Find best quote across given markets (lowest ask & highest bid).
	 * 
	 * @param marketIDs
	 * @param nbbo
	 *            true if getting NBBO, false if getting global quote
	 * @return
	 */
	private BestBidAsk computeBestBidOffer(Collection<Market> markets,
			boolean nbbo) {
		Price bestBid = null, bestAsk = null;
		Market bestBidMkt = null, bestAskMkt = null;

		for (Market mkt : markets) {
			Price bid, ask;

			if (nbbo) {
				// NBBO quote (may be delayed)
				if (marketQuotes.containsKey(mkt.getID())) {
					BestBidAsk ba = marketQuotes.get(mkt.getID());
					bid = ba.getBestBid();
					ask = ba.getBestAsk();
				} else {
					bid = null;
					ask = null;
				}

			} else {
				// global quote
				bid = mkt.getBidPrice();
				ask = mkt.getAskPrice();
			}

			// FIXME This seems wrong, should the conditional also assume bid !=
			// null?
			// Best bid quote is highest BID
			if (bestBid == null || bestBid.compareTo(bid) < 0) {
				if (bid != null)
					bestBid = bid;
				bestBidMkt = mkt;
			}
			// Best ask quote is lowest ASK
			if (bestAsk == null || bestAsk.compareTo(ask) > 0) {
				if (ask != null)
					bestAsk = ask;
				bestAskMkt = mkt;
			}
		}
		return new BestBidAsk(bestBidMkt, bestBid, bestAskMkt, bestAsk);
	}
}
