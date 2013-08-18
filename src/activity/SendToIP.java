package activity;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;
import java.util.List;

import entity.infoproc.IP;
import entity.market.Market;
import entity.market.Quote;
import entity.market.Transaction;
import event.TimeStamp;

/**
 * Class for Activity of sending new quote information to an information processor, including the
 * Security Information Processor (SIP).
 * 
 * @author ewah
 */
public class SendToIP extends Activity {

	protected final Market market;
	protected final IP ip;
	protected final Quote quote;
	protected final List<Transaction> transactions;

	public SendToIP(Market market, Quote quote, List<Transaction> transactions, IP ip, TimeStamp scheduledTime) {
		super(scheduledTime);
		this.market = checkNotNull(market, "Market");
		this.ip = checkNotNull(ip, "IP");
		this.quote = checkNotNull(quote, "Quote");
		this.transactions = checkNotNull(transactions, "Transactions");
	}

	@Override
	public Collection<? extends Activity> execute(TimeStamp time) {
		return ip.sendToIP(market, quote, transactions, time);
	}
	
	@Override
	public String toString() {
		return super.toString() + market + " -> " + ip;
	}
}
