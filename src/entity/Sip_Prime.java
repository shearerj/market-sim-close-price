package entity;

import event.*;
import market.*;
import model.MarketModel;
import systemmanager.*;
import data.SystemData;
import logger.Logger;

import java.util.ArrayList;
import java.util.Collections;

import java.util.Collection;

import activity.Activity;


/**
 * Class that updates pertinent information for the system. 
 * Generally used for creating NBBO update events.
 * Serves the purpose of the Security Information Processor in Regulation NMS.
 * Is the NBBO for one market model
 * 
 * @author ewah
 */
public class Sip_Prime extends AbstractIP {
	
	private int modelID;
	
	/**
	 * Constructor
	 * @param ID
	 * @param d
	 */
	public Sip_Prime(int ID, SystemData d, int modelID) {
		super(ID, d);
		this.modelID = modelID;
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
	 * Method to update the NBBO values.
	 * 
	 * @param model
	 * @param ts
	 * @return
	 */
	public Collection<Activity> updateNBBO(MarketModel model, TimeStamp ts) {
		int modelID = model.getID();
		ArrayList<Integer> ids = model.getMarketIDs();
		String s = ts + " | " + ids + " UpdateNBBO: current " + getNBBOQuote(modelID)
				+ " --> ";
	
		BestBidAsk lastQuote = computeBestBidOffer(ids, true);
			
		Price bestBid = lastQuote.bestBid;
		Price bestAsk = lastQuote.bestAsk;
		if ((bestBid != null) && (bestAsk != null)) {
			// check for inconsistency in buy/sell prices & fix if found
			if (lastQuote.bestBid.compareTo(lastQuote.bestAsk) > 0) {
				int mid = (lastQuote.bestBid.getPrice() + lastQuote.bestAsk.getPrice()) / 2;
				bestBid = new Price(mid - this.tickSize);
				bestAsk = new Price(mid + this.tickSize);
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
		lastQuote = new BestBidAsk(lastQuote.bestBidMarket, bestBid, lastQuote.bestAskMarket, bestAsk);
		lastQuotes.put(modelID, lastQuote);
		Logger.log(Logger.INFO, s + "updated " + lastQuote);
		return Collections.emptyList();
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return new String("SIP number " + this.getID() + ", model number " 
				+ modelID);
	}
}