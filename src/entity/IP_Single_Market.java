package entity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import static logger.Logger.Level.INFO;

import data.SystemData;

import java.util.Collection;
import activity.Activity;
import event.*;
import market.*;
import logger.Logger;
import model.MarketModel;
import systemmanager.*;


/**
 * Class that updates pertinent information for the system. 
 * Generally used for creating NBBO update events. 
 * Serves the purpose of the Information Processor for a single market
 * 
 * @author ewah
 */
public abstract class IP_Single_Market extends AbstractIP {

	protected Market mkt;
	protected int marketID;
	
	/**
	 * Constructor
	 * @param ID
	 * @param d
	 */
	public IP_Single_Market(int ID, int marketID, TimeStamp latency, Market mkt) {
		super(ID, latency);
		this.mkt = mkt;
		this.marketID = marketID;
	}
	
	public int getMarketID() {
		return marketID;
	}
	
	Market getMarket() {
		return this.mkt;
	}

	/**
 	 * Get global BestBidAsk quote for the given MARKET
 	 *
 	 * @param modelID
 	 * @return BestBidAsk
 	 */
	public BestBidAsk getGlobalQuote() {
		Integer[] array = {marketID};
		return this.computeBestBidOffer(new ArrayList<Integer>(Arrays.asList(array)), false);
	}
	
	/**
	 * Method to update the NBBO values, in this case for only ONE market
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
	
		Integer[] array = {marketID};
		BestBidAsk lastQuote = computeBestBidOffer(new ArrayList<Integer>(Arrays.asList(array)), true);
			
		int bestBid = lastQuote.getBestBid().getPrice();
		int bestAsk = lastQuote.getBestAsk().getPrice();
		if ((bestBid != -1) && (bestAsk != -1)) {
			// check for inconsistency in buy/sell prices & fix if found
			if (bestBid > bestAsk) {
				int mid = (bestBid+ bestAsk) / 2;
				bestBid = mid - this.tickSize;
				bestAsk = mid + this.tickSize;
				s += " (before fix) " + lastQuote + " --> ";
				
				// Add spread of INF if inconsistent NBBO quote
				model.addNBBOSpread(ts, Consts.INF_PRICE.getPrice()); // may not want to add this to MARKET????
			} else {
				// if bid-ask consistent, store the spread
				model.addNBBOSpread(ts, lastQuote.getSpread());
			}
		} else {
			// store spread of INF since no bid-ask spread
			model.addNBBOSpread(ts, Consts.INF_PRICE.getPrice());
		}
		lastQuote = new BestBidAsk(lastQuote.getBestBidMarket(), new Price(bestBid), lastQuote.getBestAskMarket(), new Price(bestAsk));
		lastQuotes.put(modelID, lastQuote);
		Logger.log(INFO, s + "updated " + lastQuote);
		return Collections.emptyList();
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public abstract String toString();
}