package entity;

import event.*;
import market.*;
import model.MarketModel;
import logger.Logger;
import static logger.Logger.Level.INFO;

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
public class SIP extends AbstractIP { // FOR TESTS, WE MIGHT NEED TO PASS THIS IN AS ARGUMENT! TODO PASS What?
	
	// FIXME Shouldn't take model ID. Shouldn't even know about the model
	private int modelID;
	
	/**
	 * Constructor
	 * @param ID
	 * @param d
	 */
	public SIP(int ID, int modelID, TimeStamp latency) {
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
		// FIXME
//		return this.computeBestBidOffer(data.getModel(modelID).getMarkets(), false);
		return null;
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
		String s = ts + " | " + model.getMarkets() + " UpdateNBBO: current " + getNBBOQuote()
				+ " --> ";
	
		BestBidAsk lastQuote = computeBestBidOffer(model.getMarkets(), true);
			
		Price bestBid = lastQuote.getBestBid();
		Price bestAsk = lastQuote.getBestAsk();
		if ((bestBid != null) && (bestAsk != null)) {
			// check for inconsistency in buy/sell prices & fix if found
			if (lastQuote.getBestBid().compareTo(lastQuote.getBestAsk()) > 0) {
				// FIXME should figure out best way to handle price discrepencies.
				//int mid = (lastQuote.getBestBid().getPrice() + lastQuote.getBestAsk().getPrice()) / 2;
				//bestBid = new Price(mid - this.tickSize);
				//bestAsk = new Price(mid + this.tickSize);
				//s += " (before fix) " + lastQuote + " --> ";
				
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
		lastQuote = new BestBidAsk(lastQuote.getBestBidMarket(), bestBid, lastQuote.getBestAskMarket(), bestAsk);
		lastQuotes = lastQuote;
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