package entity.infoproc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import activity.Activity;
import activity.AgentStrategy;
import entity.agent.HFTAgent;
import entity.market.Market;
import entity.market.Quote;
import entity.market.Transaction;
import event.TimeStamp;

/**
 * Specific Information Processor for a single market, used by HFT agents
 * 
 * @author cnris
 */
public class HFTIP extends SMIP {

	private static final long serialVersionUID = -4104375974647291881L;
	
	protected final HFTAgent hftAgent;

	/**
	 * Constructor
	 * 
	 * @param ID
	 * @param d
	 */
	public HFTIP(int ID, TimeStamp latency, Market mkt, HFTAgent hftAgent) {
		super(ID, latency, mkt);
		this.hftAgent = hftAgent;
	}

	@Override
	public Collection<? extends Activity> processQuote(Market market, Quote quote,
			List<Transaction> newTransactions, TimeStamp currentTime) {
		Collection<Activity> acts = new ArrayList<Activity>(super.processQuote(
				market, quote, newTransactions, currentTime));
		acts.add(new AgentStrategy(hftAgent, TimeStamp.IMMEDIATE));
		return acts;
	}

	@Override
	public String toString() {
		return "(HFTIP " + id + " in " + assocaitedMarket + " for " + hftAgent + ")"; 
	}
}