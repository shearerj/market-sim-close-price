package entity;

import static logger.Logger.Level.INFO;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import logger.Logger;
import market.Quote;
import market.Transaction;
import activity.Activity;
import event.TimeStamp;

/**
 * Class that updates pertinent information for the system. Generally used for creating NBBO update
 * events. Serves the purpose of the Information Processor for a single market
 * 
 * @author cnris
 */
public class SMIP extends IP {

	protected final Market assocaitedMarket;
	protected Quote quote;

	/**
	 * Constructor
	 * 
	 * @param ID
	 * @param d
	 */
	public SMIP(int ID, TimeStamp latency, Market market) {
		super(ID, latency);
		this.assocaitedMarket = market;
		this.quote = new Quote(null, null, 0, null, 0, TimeStamp.ZERO);
	}

	@Override
	public Collection<? extends Activity> processQuote(Market market,
			Quote quote, List<Transaction> newTransactions,
			TimeStamp currentTime) {
		if (!market.equals(assocaitedMarket))
			throw new IllegalArgumentException(
					"Can't update an SM Market with anything but it's market");

		this.quote = quote;
		Logger.log(INFO, currentTime + " | " + this + " | " + market + " "
				+ "ProcessQuote: " + quote);
		return Collections.emptySet();
	}

	public Quote getQuote() {
		return quote;
	}

	public String toString() {
		return "SMIP number " + id + ", " + "market" + assocaitedMarket;
	}
}