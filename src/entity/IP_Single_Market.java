package entity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

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

	protected int marketID;
	
	/**
	 * Constructor
	 * @param ID
	 * @param d
	 */
	public IP_Single_Market(int ID, SystemData d, int marketID) {
		super(ID, d);
		this.marketID = marketID;
	}
	
	public int getMarketID() {
		return marketID;
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
			
		int bestBid = lastQuote.bestBid.getPrice();
		int bestAsk = lastQuote.bestAsk.getPrice();
		if ((bestBid != -1) && (bestAsk != -1)) {
			// check for inconsistency in buy/sell prices & fix if found
			if (bestBid > bestAsk) {
				int mid = (bestBid+ bestAsk) / 2;
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
		lastQuote = new BestBidAsk(lastQuote.bestBidMarket, new Price(bestBid), lastQuote.bestAskMarket, new Price(bestAsk));
		lastQuotes.put(modelID, lastQuote);
		Logger.log(Logger.INFO, s + "updated " + lastQuote);
		return Collections.emptyList();
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public abstract String toString();
}