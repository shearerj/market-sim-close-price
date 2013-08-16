package activity;

import java.util.Collection;
import java.util.List;

import entity.infoproc.IP;
import entity.market.Market;
import entity.market.Quote;
import entity.market.Transaction;
import event.TimeStamp;

/**
 * Class for Activity of SIP processing a quote received from a given Market.
 * 
 * @author ewah
 */
public class ProcessQuote extends Activity {

	protected final IP ip;
	protected final Market market;
	protected final Quote quote;
	protected final List<Transaction> newTransactions;

	public ProcessQuote(IP ip, Market market, Quote quote,
			List<Transaction> newTransactions, TimeStamp scheduledTime) {
		super(scheduledTime);
		this.ip = ip;
		this.market = market;
		this.quote = quote;
		this.newTransactions = newTransactions;
	}

	@Override
	public Collection<? extends Activity> execute(TimeStamp currentTime) {
		return this.ip.processQuote(market, quote, newTransactions, currentTime);
	}
	
	@Override
	public String toString() {
		return super.toString() + market + " -> " + ip + " : " + quote;
	}
	
}
