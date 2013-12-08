package entity.infoproc;

import java.util.List;

import activity.Activity;
import activity.ProcessTransaction;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import entity.market.Market;
import entity.market.MarketTime;
import entity.market.Quote;
import entity.market.Transaction;
import event.TimeStamp;

public class TransactionProcessor extends IP {

	private static final long serialVersionUID = 4550103178485854572L;

	protected final Market associatedMarket;
	protected final List<Transaction> transactions;
	
	public TransactionProcessor(TimeStamp latency, Market market) {
		super(latency);
		this.associatedMarket = market;
		this.transactions = Lists.newArrayList();
	}

	@Override
	public Iterable<? extends Activity> sendToIP(Market market, MarketTime quoteTime, 
			Quote quote, List<Transaction> newTransactions, TimeStamp currentTime) {
		TimeStamp nextTime = latency.equals(TimeStamp.IMMEDIATE) ? TimeStamp.IMMEDIATE : currentTime.plus(latency); 
		return ImmutableList.of(new ProcessTransaction(this, market,
				newTransactions, nextTime));
	}

	@Override
	protected Iterable<? extends Activity> processInformation(Market market,
			MarketTime quoteTime, Quote quote,
			List<Transaction> newTransactions, TimeStamp currentTime) {
		transactions.addAll(newTransactions); 
		return ImmutableList.of();
	}
	
	public Iterable<? extends Activity> processTransaction(Market market, 
			List<Transaction> newTransactions, TimeStamp currentTime) {
		return this.processInformation(market, null, null, newTransactions, currentTime);
	}
	
	public List<Transaction> getTransactions() {
		return ImmutableList.copyOf(transactions);
	}

	public String toString() {
		return "(TransactionProcessor " + id + " in " + associatedMarket + ")";
	}
}
