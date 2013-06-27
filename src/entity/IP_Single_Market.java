package entity;

import java.util.ArrayList;
import java.util.Arrays;
import data.SystemData;

import activity.ActivityHashMap;
import event.*;
import market.*;
import model.MarketModel;
import systemmanager.*;


/**
 * Class that updates pertinent information for the system. 
 * Generally used for creating NBBO update events. 
 * Serves the purpose of the Information Processor for a single market
 * 
 * @author ewah
 */
public abstract class IP_Single_Market extends IP_Super {

	protected int marketID;
	
	/**
	 * Constructor
	 * @param ID
	 * @param d
	 */
	public IP_Single_Market(int ID, SystemData d, Log l, int marketID) {
		super(ID, d, l);
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
	public ActivityHashMap updateNBBO(MarketModel model, TimeStamp ts) {
		
		ActivityHashMap actMap = new ActivityHashMap();
		
		int modelID = model.getID();
		ArrayList<Integer> ids = model.getMarketIDs();
		String s = ts + " | " + ids + " UpdateNBBO: current " + getNBBOQuote(modelID)
				+ " --> ";
	
		Integer[] array = {marketID};
		BestBidAsk lastQuote = computeBestBidOffer(new ArrayList<Integer>(Arrays.asList(array)), true);
			
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
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public abstract String toString();
}