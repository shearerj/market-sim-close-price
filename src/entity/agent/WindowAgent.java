package entity.agent;

import java.util.List;
import java.util.Random;

import data.FundamentalValue;
import entity.infoproc.SIP;
import entity.market.Market;
import entity.market.Price;
import entity.market.Transaction;
import event.TimeStamp;

/**
 * This agent looks at the past window period at each reentry to
 * execute its agent strategy.
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
 * those values?
 * 
 * @author ewah
 *
 */
public abstract class WindowAgent extends BackgroundAgent {
	
	private static final long serialVersionUID = -8112884516819617629L;

	protected int windowLength;
	protected TimeStamp lastTimeToCheck; // last time to include
	protected Transaction lastSeenTrans; // last seen transaction to not include
	
	public WindowAgent(TimeStamp arrivalTime, FundamentalValue fundamental,
			SIP sip, Market market, Random rand, double reentryRate,
			PrivateValue pv, int tickSize, int bidRangeMin, int bidRangeMax,
			int windowLength) {
		super(arrivalTime, fundamental, sip, market, rand, reentryRate, pv, tickSize,
				bidRangeMin, bidRangeMax);
		
		this.windowLength = windowLength;
	}
	
	// TODO stuff for handling transactions
	/**
	 * Transactions within the window.
	 *
	 * @return
	 */
	public List<Transaction> getTransactionsInWindow(TimeStamp ts) {
		return null;
	}
//		getLastTimeToCheck(ts);
//		
//		TreeSet<Transaction> trans = data.getTrans(modelID);
//		List<PQTransaction> transInWindow = new ArrayList<PQTransaction>();
//		
//		// iterate through all trans and add the ones equal to or after lastTimeToCheck
//		// and up to current time ts
//		for (Transaction tr : trans) {
//			if (tr.timestamp.compareTo(lastTimeToCheck) >= 0 &&
//					tr.timestamp.compareTo(ts) <= 0) {
//				transInWindow.add((PQTransaction) tr);
//			}
//		}
//		return transInWindow;
//	}
//	
//	protected void getLastTimeToCheck(TimeStamp currentTime) {
//		lastTimeToCheck = new TimeStamp(Math.max(0, currentTime.longValue()-windowLength+1));
//	}
}
