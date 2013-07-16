package entity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import market.Quote;
import market.Transaction;
import activity.Activity;
import activity.AgentStrategy;
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

	// TODO May want to change this... Not great to just leak the market;
	public Market getMarket() {
		return assocaitedMarket;
	}

	@Override
	public String toString() {
		return super.toString() + ", LA: " + laAgent;
	}
}