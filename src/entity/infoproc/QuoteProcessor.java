package entity.infoproc;

import activity.Activity;
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
public interface QuoteProcessor {

	public Iterable<? extends Activity> sendToQuoteProcessor(Market market, MarketTime quoteTime, 
			Quote quote, TimeStamp currentTime);

	public Iterable<? extends Activity> processQuote(Market market,
			MarketTime quoteTime, Quote quote, TimeStamp currentTime);

}