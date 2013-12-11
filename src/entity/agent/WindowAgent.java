package entity.agent;

import java.util.List;
import java.util.Random;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.Lists;

import data.FundamentalValue;
import entity.infoproc.SIP;
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
	
	public WindowAgent(TimeStamp arrivalTime, FundamentalValue fundamental,
			SIP sip, Market market, Random rand, double reentryRate,
			PrivateValue pv, int tickSize, int bidRangeMin, int bidRangeMax,
			int windowLength) {
		super(arrivalTime, fundamental, sip, market, rand, reentryRate, pv, tickSize,
				bidRangeMin, bidRangeMax);
		
		this.windowLength = new TimeStamp(windowLength);
	}

	/**
	 * Get all transactions (from SIP plus its own transactions) in the window
	 * that is of period windowLength prior to currentTime, i.e. from
	 * currentTime-windowLength+1 to currentTime, inclusive.
	 * 
	 * @param currentTime
	 * @return
	 */
	public ImmutableList<Transaction> getWindowTransactions(TimeStamp currentTime) {
		TimeStamp firstTimeInWindow = currentTime.minus(new TimeStamp(windowLength));
		
		List<Transaction> allTransactions = Lists.newArrayList(); 
		allTransactions.addAll(this.sip.getTransactions());
		allTransactions.addAll(this.transactions);
		
		Builder<Transaction> transactionBuilder = ImmutableList.builder();
		for (Transaction trans : allTransactions) {
			if (!transactionBuilder.build().contains(trans) && 
					trans.getExecTime().after(firstTimeInWindow)) {
				transactionBuilder.add(trans);
			}
		}
		return transactionBuilder.build();
	}
}
