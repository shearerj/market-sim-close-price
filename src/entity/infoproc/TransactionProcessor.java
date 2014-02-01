package entity.infoproc;

import java.util.List;

import activity.Activity;
import entity.market.Market;
import entity.market.Transaction;
import event.TimeStamp;

public interface TransactionProcessor {

	public static int nextID = 1;
	
	/*
	 * TODO May run into issue here with Transactions being slightly out of
	 * order with respect to MarketTime. However, this isn't an absolute time
	 * measurement, so maybe it doesn't make sense?
	 */
	/* TODO Maybe these methods don't need the associatedMarket? */
	public Iterable<? extends Activity> sendToTransactionProcessor(Market market,
			List<Transaction> newTransactions, TimeStamp currentTime);
	
	public Iterable<? extends Activity> processTransactions(Market market, 
			List<Transaction> newTransactions, TimeStamp currentTime);
	
	public List<Transaction> getTransactions();

	public TimeStamp getLatency();
}
