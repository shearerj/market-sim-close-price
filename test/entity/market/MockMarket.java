package entity.market;

import java.util.Collection;
import java.util.Random;

import systemmanager.Consts.OrderType;
import activity.Activity;

import com.google.common.collect.ImmutableList;

import entity.agent.Agent;
import entity.infoproc.SIP;
import entity.market.clearingrule.MockClearingRule;
import event.TimeStamp;

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
		super(sip, latency, new MockClearingRule(Price.ZERO), new Random());
	}
	
	@Override
	public Collection<? extends Activity> submitOrder(Agent agent, OrderType type,
			Price price, int quantity, TimeStamp currentTime) {
		return ImmutableList.<Activity> builder().addAll(
				super.submitOrder(agent, type, price, quantity, currentTime)).addAll(
				updateQuote(ImmutableList.<Transaction> of(), currentTime)).build();
	}

	@Override
	public Collection<? extends Activity> withdrawOrder(Order order,
			TimeStamp currentTime) {
		return ImmutableList.<Activity> builder().addAll(
				super.withdrawOrder(order, currentTime)).addAll(
				updateQuote(ImmutableList.<Transaction> of(), currentTime)).build();
	}

	@Override
	public String toString() {
		return "MockMarket " + super.toString();
	}

}
