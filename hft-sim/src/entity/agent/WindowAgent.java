package entity.agent;

import java.util.List;
import java.util.Random;

import systemmanager.Keys;
import systemmanager.Simulation;
import data.Props;
import entity.market.Market;
import entity.market.Transaction;
import event.TimeStamp;

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

	protected TimeStamp windowLength;

	protected WindowAgent(Simulation sim, TimeStamp arrivalTime, Market market, Random rand, Props props) {
		super(sim, arrivalTime, market, rand, props);
		
		this.windowLength = TimeStamp.of(props.getAsLong(Keys.WINDOW_LENGTH));
	}

	/**
	 * Get all transactions (from SIP [XXX not true, but intended?] plus its own transactions)
	 * in the window that is of period windowLength prior to currentTime, i.e.
	 * from currentTime-windowLength+1 to currentTime, inclusive.
	 * 
	 * @param currentTime
	 * @return
	 */
	// FIXME Assert that the ording and searching are correct
	// FIXME Should this take into account latency? Latency is effectively limitng size of window...
	// Maybe get window from latest transaction time backwards?
	public List<Transaction> getWindowTransactions() {
		TimeStamp firstTimeInWindow = currentTime().minus(windowLength);

		List<Transaction> allTransactions = primaryMarket.getTransactions();
		
		// TODO Binary Search?
		int limit = 0;
		for (Transaction trans : allTransactions) {
			if (!trans.getExecTime().after(firstTimeInWindow))
				break;
			++limit;
		}
		return allTransactions.subList(0, limit);
	}
	
	public TimeStamp getWindowLength() {
		return windowLength;
	}
}
