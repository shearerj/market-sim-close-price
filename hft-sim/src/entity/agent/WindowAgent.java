package entity.agent;

import java.util.List;

import logger.Log;
import systemmanager.Keys.WindowLength;
import utils.Rand;
import data.FundamentalValue;
import data.Props;
import data.Stats;
import entity.market.Market;
import entity.market.Transaction;
import entity.sip.MarketInfo;
import event.TimeStamp;
import event.Timeline;

/**
 * This agent looks at the past window period at each reentry to execute its 
 * agent strategy.
 * 
 * If current time is T, it looks at activities occurring in between
 * T-window+1 to T, inclusive.
 * 
 * WindowAgents base all the updates on whatever's happened in the past window.
 * For each reentry, they essentially "reset." This is a way to address
 * the issue of employing trading strategies reliant on fixed valuations.
 * 
 * XXX Question: using windowing, should it use the same estimated values? Or reset
 * every time? Probably should reset every time...? Or can treat as initialized to 
 * those values? (could try either way)
 * 
 * @author ewah
 *
 */
public abstract class WindowAgent extends BackgroundAgent {
	
	private static final long serialVersionUID = -8112884516819617629L;

	private TimeStamp windowLength;

	protected WindowAgent(int id, Stats stats, Timeline timeline, Log log, Rand rand, MarketInfo sip, FundamentalValue fundamental,
			Market market, Props props) {
		super(id, stats, timeline, log, rand, sip, fundamental, market, props);
		
		this.windowLength = props.get(WindowLength.class);
	}

	/**
	 * Get all transactions (from SIP [XXX not true, but intended?] plus its own transactions)
	 * in the window that is of period windowLength prior to currentTime, i.e.
	 * from currentTime-windowLength+1 to currentTime, inclusive.
	 * 
	 * @param currentTime
	 * @return
	 */
	// TODO Should this take into account latency? Latency is effectively limiting size of window...
	// TODO This could use binary search
	protected List<Transaction> getWindowTransactions() {
		TimeStamp firstTimeInWindow = getCurrentTime().minus(windowLength);

		List<Transaction> allTransactions = primaryMarket.getTransactions();
		
		int limit = 0;
		for (Transaction trans : allTransactions) {
			if (!trans.getExecTime().after(firstTimeInWindow))
				break;
			++limit;
		}
		return allTransactions.subList(0, limit);
	}
	
	protected TimeStamp getWindowLength() {
		return windowLength;
	}
}
