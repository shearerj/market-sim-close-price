package entity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import activity.Activity;
import activity.AgentStrategy;
import entity.market.Market;
import entity.market.Quote;
import entity.market.Transaction;
import event.TimeStamp;

/**
 * Specific Information Processor for a single market, used by HFT agents
 * 
 * @author cnris
 */
public class LAIP extends SMIP {

	protected final LAAgent laAgent;

	/**
	 * Constructor
	 * 
	 * @param ID
	 * @param d
	 */
	public LAIP(int ID, TimeStamp latency, Market mkt, LAAgent laagent) {
		super(ID, latency, mkt);
		this.laAgent = laagent;
	}

	@Override
	public Collection<? extends Activity> processQuote(Market market, Quote quote,
			List<Transaction> newTransactions, TimeStamp currentTime) {
		Collection<Activity> acts = new ArrayList<Activity>(super.processQuote(
				market, quote, newTransactions, currentTime));
		acts.add(new AgentStrategy(laAgent, TimeStamp.IMMEDIATE));
		return acts;
	}

	@Override
	public String toString() {
		return super.toString() + ", LA: " + laAgent;
	}
}