package entity.infoproc;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;

import activity.Activity;
import activity.ProcessQuote;
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
abstract class AbstractQuoteProcessor extends Entity implements QuoteProcessor {

	protected TimeStamp latency;
	protected final Market associatedMarket;
	protected Quote quote;
	protected MarketTime lastQuoteTime;

	public AbstractQuoteProcessor(TimeStamp latency, Market associatedMarket) {
		super(ProcessorIDs.nextID++);
		this.latency = checkNotNull(latency);
		this.associatedMarket = checkNotNull(associatedMarket);
		this.quote = new Quote(null, null, 0, null, 0, TimeStamp.ZERO);
	}

	private static final long serialVersionUID = 4487935082860406953L;

	@Override
	public Iterable<? extends Activity> sendToQuoteProcessor(Market market, 
			MarketTime quoteTime, Quote quote, TimeStamp currentTime) {
		if (latency.equals(TimeStamp.IMMEDIATE)) {
			return ImmutableList.<Activity> builder().addAll(
					processQuote(market, quoteTime, quote, currentTime)).build();
		}
		return ImmutableList.of(new ProcessQuote(this, market, quoteTime, quote, 
				currentTime.plus(latency)));
	}

	@Override
	public Iterable<? extends Activity> processQuote(Market market, MarketTime quoteTime, Quote quote,
			TimeStamp currentTime) {
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

	@Override
	public TimeStamp getLatency() {
		return latency;
	}
}