package entity.infoproc;

import activity.Activity;
import activity.AgentStrategy;

import com.google.common.collect.ImmutableList;

import entity.agent.HFTAgent;
import entity.market.Market;
import entity.market.MarketTime;
import entity.market.Quote;
import event.TimeStamp;

/**
 * Specific Information Processor for a single market, used by HFT agents
 * 
 * @author cnris
 */
public class HFTQuoteProcessor extends SMQuoteProcessor {

	private static final long serialVersionUID = -4104375974647291881L;
	
	protected final HFTAgent hftAgent;

	public HFTQuoteProcessor(TimeStamp latency, Market mkt, HFTAgent hftAgent) {
		super(latency, mkt);
		this.hftAgent = hftAgent;
	}

	@Override
	public Iterable<? extends Activity> processQuote(Market market, MarketTime quoteTime, 
			Quote quote, TimeStamp currentTime) {
		return ImmutableList.<Activity> builder().addAll(
				super.processQuote(market, quoteTime, quote, currentTime)).add(
				new AgentStrategy(hftAgent, TimeStamp.IMMEDIATE)).build();
	}

	@Override
	public String toString() {
		return "(HFTQuoteProcessor " + id + " in " + associatedMarket + " for " + hftAgent + ")"; 
	}
	
}