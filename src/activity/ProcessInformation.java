package activity;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;

import entity.infoproc.SIP;
import entity.market.Market;
import entity.market.MarketTime;
import entity.market.Quote;
import entity.market.Transaction;
import event.TimeStamp;

public class ProcessInformation extends Activity {

	protected final SIP ip;
	protected final Market market;
	protected final MarketTime quoteTime;
	protected final Quote quote;
	protected final List<Transaction> newTransactions;

	public ProcessInformation(SIP ip, Market market, MarketTime quoteTime, Quote quote,
			List<Transaction> newTransactions, TimeStamp scheduledTime) {
		super(scheduledTime);
		this.ip = checkNotNull(ip, "IP");
		this.quoteTime = checkNotNull(quoteTime, "Market Time");
		this.market = checkNotNull(market, "Market");
		this.quote = checkNotNull(quote, "Quote");
		this.newTransactions = checkNotNull(newTransactions, "New Transactions");
	}

	@Override
	public Iterable<? extends Activity> execute(TimeStamp currentTime) {
		return this.ip.processNewInformation(market, quoteTime, quote, newTransactions,
				currentTime);
	}
	
	@Override
	public String toString() {
		return super.toString() + market + " -> " + ip + " : " + quote;
	}
	
}
