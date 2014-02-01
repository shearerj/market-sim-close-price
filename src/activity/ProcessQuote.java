package activity;

import static com.google.common.base.Preconditions.checkNotNull;

import entity.infoproc.QuoteProcessor;
import entity.market.Market;
import entity.market.MarketTime;
import entity.market.Quote;
import event.TimeStamp;

/**
 * Class for Activity of IP processing a quote received from a given Market.
 * 
 * @author ewah
 */
public class ProcessQuote extends Activity {

	protected final QuoteProcessor ip;
	protected final Market market;
	protected final MarketTime quoteTime;
	protected final Quote quote;

	public ProcessQuote(QuoteProcessor ip, Market market, MarketTime quoteTime, Quote quote,
			TimeStamp scheduledTime) {
		super(scheduledTime);
		this.ip = checkNotNull(ip, "IP");
		this.quoteTime = checkNotNull(quoteTime, "Market Time");
		this.market = checkNotNull(market, "Market");
		this.quote = checkNotNull(quote, "Quote");
	}

	@Override
	public Iterable<? extends Activity> execute(TimeStamp currentTime) {
		return this.ip.processQuote(market, quoteTime, quote, currentTime);
	}
	
	@Override
	public String toString() {
		return super.toString() + market + " -> " + ip + " : " + quote;
	}
	
}
