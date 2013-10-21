package entity.infoproc;

import java.util.List;

import activity.Activity;
import activity.AgentStrategy;

import com.google.common.collect.ImmutableList;

import entity.agent.HFTAgent;
import entity.market.Market;
import entity.market.MarketTime;
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

	public HFTIP(TimeStamp latency, Market mkt, HFTAgent hftAgent) {
		super(latency, mkt);
		this.hftAgent = hftAgent;
	}

	@Override
	public Iterable<? extends Activity> processQuote(Market market, MarketTime quoteTime, Quote quote,
			List<Transaction> newTransactions, TimeStamp currentTime) {
		return ImmutableList.<Activity> builder().addAll(
				super.processQuote(market, quoteTime, quote, newTransactions, currentTime)).add(
				new AgentStrategy(hftAgent, TimeStamp.IMMEDIATE)).build();
	}

	@Override
	public String toString() {
		return "(HFTIP " + id + " in " + associatedMarket + " for " + hftAgent + ")"; 
	}
}