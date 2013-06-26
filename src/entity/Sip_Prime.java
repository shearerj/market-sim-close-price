package entity;

import event.*;
import market.*;
import model.MarketModel;
import systemmanager.*;
import data.SystemData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import activity.ActivityHashMap;


/**
 * Class that updates pertinent information for the system. 
 * Generally used for creating NBBO update events.
 * Serves the purpose of the Security Information Processor in Regulation NMS.
 * Is the NBBO for one market model
 * 
 * @author ewah
 */
public class Sip_Prime extends IP_Super {
	
	private int modelID;
	
	/**
	 * Constructor
	 * @param ID
	 * @param d
	 */
	public Sip_Prime(int ID, SystemData d, Log l, int modelID) {
		super(ID, d, l);
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
	public ActivityHashMap updateNBBO(MarketModel model, TimeStamp ts) {
		
		ActivityHashMap actMap = new ActivityHashMap();
		
		int modelID = model.getID();
		ArrayList<Integer> ids = model.getMarketIDs();
		String s = ts + " | " + ids + " UpdateNBBO: current " + getNBBOQuote(modelID)
				+ " --> ";
	
		BestBidAsk lastQuote = computeBestBidOffer(ids, true);
			
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
	public String toString() {
		return new String("SIP number " + this.getID() + ", model number " 
				+ modelID);
	}
}