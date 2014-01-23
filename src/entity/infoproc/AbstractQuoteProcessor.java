package entity.infoproc;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import systemmanager.Scheduler;
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

	protected final TimeStamp latency;
	protected final Market associatedMarket;
	protected Quote quote;
	protected MarketTime lastQuoteTime;

	public AbstractQuoteProcessor(Scheduler scheduler, TimeStamp latency, Market associatedMarket) {
		super(ProcessorIDs.nextID++, scheduler);
		this.latency = checkNotNull(latency);
		this.associatedMarket = checkNotNull(associatedMarket);
		this.quote = new Quote(null, null, 0, null, 0, TimeStamp.ZERO);
	}

	private static final long serialVersionUID = 4487935082860406953L;

	// TODO better way to handle immediate execution
	@Override
	public void sendToQuoteProcessor(Market market, 
			MarketTime quoteTime, Quote quote, TimeStamp currentTime) {
		Activity act = new ProcessQuote(this, market, quoteTime, quote);
		if (latency.equals(TimeStamp.IMMEDIATE))
			scheduler.executeActivity(act);
		else
			scheduler.scheduleActivity(currentTime.plus(latency), act);
	}

	@Override
	public void processQuote(Market market, MarketTime quoteTime, Quote quote,
			TimeStamp currentTime) {
		checkArgument(market.equals(associatedMarket),
				"Can't update a QuoteProcessor with anything but its market");
		checkArgument(quote.getMarket().equals(associatedMarket),
				"Can't update a QuoteProcessor with quote from another market");

		// Do nothing for a stale quote
		if (lastQuoteTime != null && lastQuoteTime.compareTo(quoteTime) > 0)
			return;

		this.quote = quote;
		this.lastQuoteTime = quoteTime;
	}

	public Quote getQuote() {
		return quote;
	}

	@Override
	public TimeStamp getLatency() {
		return latency;
	}
}