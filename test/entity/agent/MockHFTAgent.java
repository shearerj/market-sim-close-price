package entity.agent;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;
import java.util.Random;

import com.google.common.collect.ImmutableList;

import activity.Activity;
import data.FundamentalValue;
import entity.infoproc.HFTQuoteProcessor;
import entity.infoproc.HFTTransactionProcessor;
import entity.infoproc.SIP;
import entity.market.Market;
import event.TimeStamp;

public class MockHFTAgent extends HFTAgent {

	private static final long serialVersionUID = 1L;

	public MockHFTAgent(TimeStamp latency, FundamentalValue fundamental, 
			SIP sip, Collection<Market> markets) {
		this(latency, latency, fundamental, sip, markets);
	}
	
	public MockHFTAgent(TimeStamp quoteLatency, TimeStamp transactionLatency,
			FundamentalValue fundamental, SIP sip, Collection<Market> markets) {
		super(quoteLatency, transactionLatency, new TimeStamp(0), fundamental, 
				sip, markets, new Random(), 1);
	}

	@Override
	public Iterable<? extends Activity> agentStrategy(TimeStamp currentTime) {
		return ImmutableList.of();
	}

	/**
	 * Get HFTQuoteProcessor directly (for testing purposes)
	 * @param market
	 * @return
	 */
	public HFTQuoteProcessor getHFTQuoteProcessor(Market market) {
		checkNotNull(market);
		if (quoteProcessors.containsKey(market)) return quoteProcessors.get(market);
		return null;
	}
	
	/**
	 * Get HFTTransactionProcessor directly (for testing purposes)
	 * @param market
	 * @return
	 */
	public HFTTransactionProcessor getHFTTransactionProcessor(Market market) {
		checkNotNull(market);
		if (transactionProcessors.containsKey(market)) return transactionProcessors.get(market);
		return null;
	}
}
