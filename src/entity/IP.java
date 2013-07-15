package entity;

import java.util.ArrayList;
import java.util.HashMap;

import logger.Logger;
import static logger.Logger.Level.INFO;
import market.BestBidAsk;
import market.Price;
import model.MarketModel;
import java.util.Collection;
import event.TimeStamp;
import activity.Activity;
import activity.ProcessQuote;

/**
 * Superclass for Information Processors that either work
 * for one model or multiple.
 * 
 * @author cnris
 *
 */
public abstract class IP extends Entity {
	
	protected TimeStamp latency;
	protected BestBidAsk lastQuotes;

	/**
	 * Constructor
	 * @param ID
	 * @param d
	 */
	public IP(int ID, TimeStamp latency) {
		super(ID);
		this.latency = latency;
		lastQuotes = new BestBidAsk(null, null, null, null);
	}
	
	public ProcessQuote scheduleProcessQuote(Market market, Price bid, Price ask, TimeStamp ts) {
		return new ProcessQuote(this, market, bid, ask, ts.plus(latency));
	}
	 
	public abstract Collection<Activity> processQuote(Market mkt, Price bid, Price ask, TimeStamp ts);
	
	/**
	 * Get BestBidAsk quote for the given model,
	 * or market if SMIP
	 * 
	 * @param modelID
	 * @return BestBidAsk
	 */
	public BestBidAsk getBBOQuote() {
			return lastQuotes;
	}
	
	public abstract Collection<Activity> updateBBO(MarketModel model, TimeStamp ts);
}
