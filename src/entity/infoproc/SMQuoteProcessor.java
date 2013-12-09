package entity.infoproc;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import activity.Activity;
import activity.ProcessQuote;

import com.google.common.collect.ImmutableList;

import entity.Entity;
import entity.market.Market;
import entity.market.MarketTime;
import entity.market.Quote;
import event.TimeStamp;

/**
 * Class that updates pertinent information for the system. Generally used for 
 * creating NBBO update events. Serves the purpose of the Information Processor 
 * for a single market.
 * 
 * @author cnris
 */
public class SMQuoteProcessor extends Entity implements QuoteProcessor {

	private static final long serialVersionUID = 827960237754648780L;
	public static int nextID = 1;
	
	protected TimeStamp latency;
	protected final Market associatedMarket;
	protected Quote quote;
	protected MarketTime lastQuoteTime;
	
	public SMQuoteProcessor(TimeStamp latency, Market market) {
		super(nextID++);
		this.latency = checkNotNull(latency);
		this.associatedMarket = checkNotNull(market);
		this.quote = new Quote(null, null, 0, null, 0, TimeStamp.ZERO);
	}

	@Override
	public Iterable<? extends Activity> sendToQP(Market market,
			MarketTime quoteTime, Quote quote, TimeStamp currentTime) {
		TimeStamp nextTime = latency.equals(TimeStamp.IMMEDIATE) ? TimeStamp.IMMEDIATE : currentTime.plus(latency);
		return ImmutableList.of(new ProcessQuote(this, market, quoteTime, quote, nextTime));
	}

	@Override
	public Iterable<? extends Activity> processQuote(Market market,
			MarketTime quoteTime, Quote quote, TimeStamp currentTime) {
		checkArgument(market.equals(associatedMarket),
				"Can't update a QuoteProcessor with anything but its market");
		checkArgument(quote.getMarket().equals(associatedMarket),
				"Can't update a QuoteProcessor with quote from another market");

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
		return "(QuoteProcessor " + id + " in " + associatedMarket + ")";
	}

}