package entity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import market.Quote;
import market.Transaction;
import activity.Activity;
import activity.ProcessQuote;
import event.TimeStamp;

/**
 * Superclass for Information Processors that either work for one model or multiple.
 * 
 * @author cnris
 * 
 */
public abstract class IP extends Entity {

	protected TimeStamp latency;
	protected Collection<Transaction> trans;

	/**
	 * Constructor
	 * 
	 * @param ID
	 * @param d
	 */
	public IP(int ID, TimeStamp latency) {
		super(ID);
		this.latency = latency;
		this.trans = new ArrayList<Transaction>();
	}
	
	public Collection<Transaction> getTranses() {
		return this.trans;
	}

	public Collection<ProcessQuote> sendToIP(Market market, Quote quote,
			TimeStamp ts, Collection<Transaction> transes) {
		this.trans = transes;
		return Collections.singleton(new ProcessQuote(this, market, quote,
				ts.plus(latency)));
	}

	public abstract Collection<Activity> processQuote(Market mkt, Quote quote,
			TimeStamp ts);

}
