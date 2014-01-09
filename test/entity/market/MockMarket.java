package entity.market;

import java.util.Collection;
import java.util.Random;

import activity.Activity;

import com.google.common.collect.ImmutableList;

import entity.agent.Agent;
import entity.infoproc.SIP;
import entity.market.clearingrule.MockClearingRule;
import event.TimeStamp;
import fourheap.Order.OrderType;

/**
 * MockMarket for testing purposes.
 * 
 * NOTE: A market clear must be explicitly called or scheduled.
 */
public class MockMarket extends Market {

	private static final long serialVersionUID = 1L;

	public MockMarket(SIP sip) {
		this(sip, TimeStamp.IMMEDIATE);
	}

	public MockMarket(SIP sip, TimeStamp latency) {
		super(sip, latency, new MockClearingRule(), new Random());
	}
	
	public MockMarket(SIP sip, TimeStamp quoteLatency, TimeStamp transactionLatency) {
		super(sip, quoteLatency, transactionLatency, new MockClearingRule(), new Random());
	}
	
	@Override
	public Collection<? extends Activity> submitOrder(Agent agent, OrderType type,
			Price price, int quantity, TimeStamp currentTime) {
		return ImmutableList.<Activity> builder().addAll(
				super.submitOrder(agent, type, price, quantity, currentTime)).addAll(
				updateQuote(currentTime)).build();
	}

	@Override
	public Collection<? extends Activity> withdrawOrder(Order order,
			TimeStamp currentTime) {
		return ImmutableList.<Activity> builder().addAll(
				super.withdrawOrder(order, currentTime)).addAll(
				updateQuote(currentTime)).build();
	}

	@Override
	public String toString() {
		return "MockMarket " + super.toString();
	}

}
