package activity;

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
		this.market = market;
		this.ip = ip;
		this.quote = quote;
		this.transactions = transactions;
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
