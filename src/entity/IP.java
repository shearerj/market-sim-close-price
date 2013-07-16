package entity;

import java.util.ArrayList;
import java.util.Collection;

import market.BestBidAsk;
import market.Price;
import model.MarketModel;
import activity.Activity;
import activity.ProcessQuote;
import event.TimeStamp;

/**
 * Superclass for Information Processors that either work for one model or multiple.
 * 
 * @author cnris
 * 
 */
public abstract class IP extends Entity {

	protected TimeStamp latency;
	protected BestBidAsk lastQuotes; // TODO This shouldn't have a BestBid ask, that's noly
										// something an SIP has

	/**
	 * Constructor
	 * 
	 * @param ID
	 * @param d
	 */
	public IP(int ID, TimeStamp latency) {
		super(ID);
		this.latency = latency;
		lastQuotes = new BestBidAsk(null, null, null, null);
	}

	public Collection<ProcessQuote> sendToIP(Market market, Price bid,
			Price ask, TimeStamp ts) {
		Collection<ProcessQuote> activities = new ArrayList<ProcessQuote>();
		activities.add(new ProcessQuote(this, market, bid, ask, ts.plus(latency)));
		return activities;
	}

	public abstract Collection<Activity> processQuote(Market mkt, Price bid,
			Price ask, TimeStamp ts);

	/**
	 * Get BestBidAsk quote for the given model, or market if SMIP
	 * 
	 * @param modelID
	 * @return BestBidAsk
	 */
	public BestBidAsk getBBOQuote() {
		return lastQuotes;
	}
}
