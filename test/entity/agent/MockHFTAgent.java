package entity.agent;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;
import java.util.Random;

import com.google.common.collect.ImmutableList;

import activity.Activity;
import data.FundamentalValue;
import entity.infoproc.HFTQuoteProcessor;
import entity.infoproc.SIP;
import entity.market.Market;
import event.TimeStamp;

public class MockHFTAgent extends HFTAgent {

	private static final long serialVersionUID = 1L;

	public MockHFTAgent(TimeStamp latency, FundamentalValue fundamental, 
			SIP sip, Collection<Market> markets) {
		super(latency, new TimeStamp(0), fundamental, sip, markets, new Random(), 1);
	}

	@Override
	public Iterable<? extends Activity> agentStrategy(TimeStamp currentTime) {
		return ImmutableList.of();
	}

	/**
	 * Get HFTIP directly (for testing purposes)
	 * @param market
	 * @return
	 */
	public HFTQuoteProcessor getHFTQuoteProcessor(Market market) {
		checkNotNull(market);
		if (ips.containsKey(market)) return ips.get(market);
		return null;
	}
}
