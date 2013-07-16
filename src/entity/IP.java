package entity;

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

	public ProcessQuote scheduleProcessQuote(Market market, Price bid,
			Price ask, TimeStamp ts) {
		return new ProcessQuote(this, market, bid, ask, ts.plus(latency));
	}

	public abstract Collection<Activity> processQuote(Market mkt, Price bid,
			Price ask, TimeStamp ts);

	/**
	 * Get BestBidAsk quote for the given model, or market if SMIP
	 * 
	 * @param modelID
	 * @return BestBidAsk
	 */
	// TODO This should be moved to SIP
	public BestBidAsk getBBOQuote() {
		return lastQuotes;
	}

	// TODO this should be moved to SIP
	public abstract Collection<Activity> updateBBO(MarketModel model,
			TimeStamp ts);
}
