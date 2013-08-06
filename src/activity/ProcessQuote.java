package activity;

import java.util.Collection;
import java.util.List;


import org.apache.commons.lang3.builder.HashCodeBuilder;

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

	public Collection<? extends Activity> execute(TimeStamp currentTime) {
		return this.ip.processQuote(market, quote, newTransactions, currentTime);
	}

	public String toString() {
		return new String(getName() + "::" + market + ":" + quote);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof ProcessQuote)) return false;
		ProcessQuote other = (ProcessQuote) obj;
		return super.equals(other) && this.ip.equals(other.ip)
				&& this.market.equals(other.market)
				&& this.quote.equals(other.quote);
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder(19, 37).append(market).append(ip).append(
				quote).append(scheduledTime).toHashCode();
	}
}
