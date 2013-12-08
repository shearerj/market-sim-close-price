package entity.infoproc;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;

import activity.Activity;
import entity.Entity;
import entity.market.Market;
import entity.market.MarketTime;
import entity.market.Quote;
import entity.market.Transaction;
import event.TimeStamp;

/**
 * Superclass for Information Processors that either work for one model or multiple.
 * 
 * @author cnris
 * 
 */
public abstract class IP extends Entity {

	private static final long serialVersionUID = -1150989788412631133L;
	public static int nextID = 1;
	
	protected TimeStamp latency;

	public IP(TimeStamp latency) {
		super(nextID++);
		this.latency = checkNotNull(latency, "Latency");
	}
	
	public abstract Iterable<? extends Activity> sendToIP(Market market, MarketTime quoteTime, 
			Quote quote, List<Transaction> newTransactions, TimeStamp currentTime);

	protected abstract Iterable<? extends Activity> processInformation(Market market,
			MarketTime quoteTime, Quote quote, List<Transaction> newTransactions, 
			TimeStamp currentTime);

	
//	public Iterable<? extends Activity> sendToIP(Market market, MarketTime quoteTime, 
//			Quote quote, List<Transaction> newTransactions, TimeStamp currentTime) {
//		TimeStamp nextTime = latency.equals(TimeStamp.IMMEDIATE) ? TimeStamp.IMMEDIATE : currentTime.plus(latency);
//		return ImmutableList.of(new ProcessQuote(this, market, quoteTime, quote,
//				newTransactions, nextTime));
//	}
//
//	public abstract Iterable<? extends Activity> processQuote(Market market, 
//			MarketTime quoteTime, Quote quote, List<Transaction> newTransactions, 
//			TimeStamp currentTime);


}
