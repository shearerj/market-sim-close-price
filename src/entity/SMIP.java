package entity;

import static logger.Logger.Level.INFO;

import java.util.Collection;
import java.util.Collections;

import logger.Logger;
import market.Quote;
import activity.Activity;
import event.TimeStamp;


/**
 * Class that updates pertinent information for the system. 
 * Generally used for creating NBBO update events. 
 * Serves the purpose of the Information Processor for a single market
 * 
 * @author cnris
 */
public class SMIP extends IP {

	protected final Market market;
	protected Quote quote;
	
	/**
	 * Constructor
	 * @param ID
	 * @param d
	 */
	public SMIP(int ID, TimeStamp latency, Market market) {
		super(ID, latency);
		this.market = market;
	}
	
	/**
 	 * Store market's best bid/ask & insert Activity to UpdateNBBO at some amount of time 
 	 *
 	 * @param market
 	 * @param bid
 	 * @param ask
 	 * @param currentTime
 	 * @return ActivityHashMap
 	 */
	public Collection<Activity> processQuote(Market market, Quote quote, TimeStamp currentTime) {
		this.quote = quote;
		Logger.log(INFO, currentTime + " | " + this + " | "+ market + " " + 
				"ProcessQuote: " + quote);
		return Collections.emptySet();
	}
	
	public Quote getQuote() {
		return quote;
	}
	
	public String toString() {
		return "SMIP number " + id + ", " + "market" + market;
	}
}