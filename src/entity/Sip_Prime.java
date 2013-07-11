package entity;

import event.*;
import market.*;
import model.MarketModel;
import systemmanager.*;
import data.SystemData;
import logger.Logger;
import static logger.Logger.Level.INFO;

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
	public Sip_Prime(int ID, int modelID, TimeStamp latency) {
		super(ID, latency);
		this.modelID = modelID;
	}

	/**
 	 * Get global BestBidAsk quote for the given model.
 	 *
 	 * @param modelID
 	 * @return BestBidAsk
 	 */
	public BestBidAsk getGlobalQuote(int modelID) {
		return this.computeBestBidOffer(data.getModel(modelID).getMarkets(), false);
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
		String s = ts + " | " + model.getMarkets() + " UpdateNBBO: current " + getNBBOQuote(modelID)
				+ " --> ";
	
		BestBidAsk lastQuote = computeBestBidOffer(model.getMarkets(), true);
			
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
		lastQuote = new BestBidAsk(lastQuote.getBestBidMarket(), bestBid, lastQuote.getBestAskMarket(), bestAsk);
		lastQuotes.put(modelID, lastQuote);
		Logger.log(INFO, s + "updated " + lastQuote);
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