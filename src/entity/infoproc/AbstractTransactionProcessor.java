package entity.infoproc;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collections;
import java.util.List;

import activity.Activity;
import activity.ProcessTransactions;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import entity.Entity;
import entity.market.Market;
import entity.market.Transaction;
import event.TimeStamp;

abstract class AbstractTransactionProcessor extends Entity implements TransactionProcessor {

	private static final long serialVersionUID = -8130023032097833791L;
	
	protected TimeStamp latency;
	protected final Market associatedMarket;
	protected final List<Transaction> transactions;

	public AbstractTransactionProcessor(TimeStamp latency, Market associatedMarket) {
		super(ProcessorIDs.nextID++);
		this.latency = latency;
		this.associatedMarket = checkNotNull(associatedMarket);
		this.transactions = Lists.newArrayList();
	}

	@Override
	public Iterable<? extends Activity> sendToTransactionProcessor(Market market, List<Transaction> newTransactions, TimeStamp currentTime) {
		TimeStamp nextTime = latency.equals(TimeStamp.IMMEDIATE) ? TimeStamp.IMMEDIATE : currentTime.plus(latency); 
		return ImmutableList.of(new ProcessTransactions(this, market, newTransactions, nextTime));
	}

	@Override
	public Iterable<? extends Activity> processTransactions(Market market, List<Transaction> newTransactions, TimeStamp currentTime) {
		transactions.addAll(newTransactions);
		return ImmutableList.of();
	}

	public List<Transaction> getTransactions() {
		// So that we don't copy the list a bunch of times
		return Collections.unmodifiableList(transactions);
	}

}
