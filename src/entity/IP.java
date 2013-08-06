package entity;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import activity.Activity;
import activity.ProcessQuote;
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

	protected TimeStamp latency;

	/**
	 * Constructor
	 * 
	 * @param ID
	 * @param d
	 */
	public IP(int ID, TimeStamp latency) {
		super(ID);
		this.latency = latency;
	}

	public Collection<? extends Activity> sendToIP(Market market, Quote quote,
			List<Transaction> newTransactions, TimeStamp currentTime) {
		return Collections.singleton(new ProcessQuote(this, market, quote,
				newTransactions, currentTime.plus(latency)));
	}

	public abstract Collection<? extends Activity> processQuote(Market market, Quote quote,
			List<Transaction> newTransactions, TimeStamp currentTime);

}
