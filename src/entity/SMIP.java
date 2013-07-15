package entity;

import static logger.Logger.Level.INFO;

import java.util.Collection;
import java.util.Collections;

import logger.Logger;
import market.BestBidAsk;
import market.Price;
import model.MarketModel;
import activity.Activity;
import event.TimeStamp;


/**
 * Class that updates pertinent information for the system. 
 * Generally used for creating NBBO update events. 
 * Serves the purpose of the Information Processor for a single market
 * 
 * @author cnris
 */
public class SMIP extends IP {

	protected Market market;
	
	/**
	 * Constructor
	 * @param ID
	 * @param d
	 */
	public SMIP(int ID, TimeStamp latency, Market market) {
		super(ID, latency);
		this.market = market;
	}
	
	Market getMarket() {
		return this.market;
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
	public Collection<Activity> processQuote(Market mkt, Price bid, Price ask, TimeStamp ts) {
		BestBidAsk q = new BestBidAsk(mkt, bid, mkt, ask);
		lastQuotes = q;
		Logger.log(INFO, ts + " | " + this + " | "+ mkt + " " + 
				"ProcessQuote: " + q);
		return null;
	}
	
	/**
	 * Method to update the BBO values, in this case for only ONE market
	 * 
	 * @param model
	 * @param ts
	 * @return
	 */
	// TODO This class shouldn't need this, as it's only returning the quote for a given market
	public Collection<Activity> updateBBO(MarketModel model, TimeStamp ts) {
		String s = ts + " | " + this.market + " UpdateNBBO: current " + getBBOQuote()
				+ " --> ";
	
		BestBidAsk lastQuote = new BestBidAsk(market, lastQuotes.getBestBid(), market, lastQuotes.getBestAsk());
			
		int bestBid = lastQuote.getBestBid().getPrice();
		int bestAsk = lastQuote.getBestAsk().getPrice();
		if ((bestBid != -1) && (bestAsk != -1)) {
			// FIXME figure out best method for price disparity
			// check for inconsistency in buy/sell prices & fix if found
			if (bestBid > bestAsk) {
				//int mid = (bestBid+ bestAsk) / 2;
				//bestBid = mid - this.tickSize;
				//bestAsk = mid + this.tickSize;
				//s += " (before fix) " + lastQuote + " --> ";
				
				// Add spread of INF if inconsistent NBBO quote
				model.addNBBOSpread(ts, Price.INF.getPrice()); // FIXME may not want to add this to MARKET????
			} else {
				// if bid-ask consistent, store the spread
				model.addNBBOSpread(ts, lastQuote.getSpread());
			}
		} else {
			// store spread of INF since no bid-ask spread
			model.addNBBOSpread(ts, Price.INF.getPrice());
		}
		//lastQuote = new BestBidAsk(mkt, new Price(bestBid), mkt, new Price(bestAsk));
		//lastQuotes = lastQuote;
		Logger.log(INFO, s + "updated " + lastQuotes);
		return Collections.emptyList();
	}
	
	public String toString() {
		return "SMIP number " + id + ", " + "market" + market;
	}
}