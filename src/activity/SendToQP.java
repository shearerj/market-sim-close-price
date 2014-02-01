package activity;

import static com.google.common.base.Preconditions.checkNotNull;
import entity.infoproc.QuoteProcessor;
import entity.market.Market;
import entity.market.MarketTime;
import entity.market.Quote;
import event.TimeStamp;

/**
 * Class for Activity of sending new quote information to an information
 * processor, including the Security Information Processor (SIP). This should
 * happen as soon as a quote is generated.
 * 
 * @author ewah
 */
public class SendToQP extends Activity {

	protected final Market market;
	protected final QuoteProcessor qp;
	protected final MarketTime quoteTime;
	protected final Quote quote;

	public SendToQP(Market market, MarketTime quoteTime, Quote quote, 
			QuoteProcessor qp, TimeStamp scheduledTime) {
		super(scheduledTime);
		this.market = checkNotNull(market, "Market");
		this.qp = checkNotNull(qp, "QP");
		this.quoteTime = checkNotNull(quoteTime, "Market Time");
		this.quote = checkNotNull(quote, "Quote");
	}

	@Override
	public Iterable<? extends Activity> execute(TimeStamp currentTime) {
		return qp.sendToQuoteProcessor(market, this.quoteTime, quote, currentTime);
	}
	
	@Override
	public String toString() {
		return super.toString() + market + " -> " + qp;
	}
}
