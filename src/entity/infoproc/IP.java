package entity.infoproc;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;

import com.google.common.collect.ImmutableList;

import activity.Activity;
import activity.ProcessQuote;
import entity.Entity;
import entity.market.Market;
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
	private static int nextID;
	
	protected TimeStamp latency;

	public IP(TimeStamp latency) {
		super(nextID++);
		this.latency = checkNotNull(latency, "Latency");
	}

	public Iterable<? extends Activity> sendToIP(Market market, Quote quote,
			List<Transaction> newTransactions, TimeStamp currentTime) {
		return ImmutableList.of(new ProcessQuote(this, market, quote,
				newTransactions, currentTime.plus(latency)));
	}

	public abstract Iterable<? extends Activity> processQuote(Market market, Quote quote,
			List<Transaction> newTransactions, TimeStamp currentTime);

}
