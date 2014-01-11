package entity.infoproc;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collections;
import java.util.List;

import activity.Activity;
import activity.ProcessTransactions;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import entity.Entity;
import entity.agent.Agent;
import entity.market.Market;
import entity.market.Order;
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
	public Iterable<? extends Activity> sendToTransactionProcessor(Market market, 
			List<Transaction> newTransactions, TimeStamp currentTime) {
		if (latency.equals(TimeStamp.IMMEDIATE)) {
			return ImmutableList.<Activity> builder().addAll(
					processTransactions(market, newTransactions, currentTime)).build();
		}
		return ImmutableList.of(new ProcessTransactions(this, market, 
				newTransactions, currentTime.plus(latency)));
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

	@Override
	public TimeStamp getLatency() {
		return latency;
	}
	
	// Tells an agent about a transaction and removes the order if everything has transacted
	protected void updateAgent(Agent agent, Order order, Transaction transaction) {
		if (order.getQuantity() == 0)
			agent.removeOrder(order);
		agent.processTransaction(transaction);
	}

}
