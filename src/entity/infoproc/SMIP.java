package entity.infoproc;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.List;

import activity.Activity;

import com.google.common.collect.ImmutableList;

import entity.market.Market;
import entity.market.MarketTime;
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
	
	protected final Market associatedMarket;
	protected Quote quote;
	protected MarketTime lastQuoteTime;

	/**
	 * Constructor
	 * 
	 * @param ID
	 * @param d
	 */
	public SMIP(TimeStamp latency, Market market) {
		super(latency);
		this.associatedMarket = market;
		this.quote = new Quote(null, null, 0, null, 0, TimeStamp.ZERO);
	}

	@Override
	public Iterable<? extends Activity> processQuote(Market market, MarketTime quoteTime,
			Quote quote, List<Transaction> newTransactions,
			TimeStamp currentTime) {
		checkArgument(market.equals(associatedMarket),
				"Can't update an SM IP with anything but its market");
		checkArgument(quote.getMarket().equals(associatedMarket),
				"Can't update an SM IP with quote from another market");
		
		// Do nothing for a stale quote
		if (lastQuoteTime != null && lastQuoteTime.compareTo(quoteTime) > 0)
			return ImmutableList.of();

		this.quote = quote;
		this.lastQuoteTime = quoteTime;
		return ImmutableList.of();
	}

	public Quote getQuote() {
		return quote;
	}

	public String toString() {
		return "(SMIP " + id + " in " + associatedMarket + ")";
	}
}