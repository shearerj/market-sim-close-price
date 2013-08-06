package activity;

import java.util.Collection;
import java.util.List;


import org.apache.commons.lang3.builder.HashCodeBuilder;

import entity.IP;
import entity.market.Market;
import entity.market.Quote;
import entity.market.Transaction;
import event.TimeStamp;

/**
 * Class for Activity of sending new quote information to the Security Information Processor (SIP).
 * 
 * @author ewah
 */
public class SendToIP extends Activity {

	protected final Market market;
	protected final IP ip;
	protected final Quote quote;
	List<Transaction> transactions;

	public SendToIP(Market market, Quote quote, List<Transaction> transactions, IP ip, TimeStamp scheduledTime) {
		super(scheduledTime);
		this.market = market;
		this.ip = ip;
		this.quote = quote;
		this.transactions = transactions;
	}

	public Collection<? extends Activity> execute(TimeStamp time) {
		return ip.sendToIP(market, quote, transactions, time);
	}

	public String toString() {
		return new String(getName() + "::" + market);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof SendToIP)) return false;
		SendToIP other = (SendToIP) obj;
		return super.equals(other) && this.market.equals(other.market)
				&& this.ip.equals(other.ip) && this.quote.equals(other.quote);
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder(19, 37).append(market).append(scheduledTime).append(
				quote).append(ip).toHashCode();
	}
}
