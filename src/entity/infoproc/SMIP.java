package entity.infoproc;

import static com.google.common.base.Preconditions.checkArgument;
import static logger.Logger.Level.INFO;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import logger.Logger;
import activity.Activity;
import entity.market.Market;
import entity.market.Quote;
import entity.market.Transaction;
import event.TimeStamp;

/**
 * Class that updates pertinent information for the system. Generally used for creating NBBO update
 * events. Serves the purpose of the Information Processor for a single market
 * 
 * @author cnris
 */
public class SMIP extends IP {

	private static final long serialVersionUID = 827960237754648780L;
	
	protected final Market assocaitedMarket;
	protected Quote quote;

	/**
	 * Constructor
	 * 
	 * @param ID
	 * @param d
	 */
	public SMIP(TimeStamp latency, Market market) {
		super(latency);
		this.assocaitedMarket = market;
		this.quote = new Quote(null, null, 0, null, 0, TimeStamp.ZERO);
	}

	@Override
	public Collection<? extends Activity> processQuote(Market market,
			Quote quote, List<Transaction> newTransactions,
			TimeStamp currentTime) {
		checkArgument(market.equals(assocaitedMarket),
				"Can't update an SM Market with anything but it's market");

		this.quote = quote;
		Logger.log(INFO, market + " -> " + this + " ProcessQuote: " + quote);
		return Collections.emptySet();
	}

	public Quote getQuote() {
		return quote;
	}

	public String toString() {
		return "(SMIP " + id + " in " + assocaitedMarket + ")";
	}
}