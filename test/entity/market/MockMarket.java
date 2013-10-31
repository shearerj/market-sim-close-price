package entity.market;

import java.util.Collection;
import java.util.Random;

import activity.Activity;

import com.google.common.collect.ImmutableList;

import entity.agent.Agent;
import entity.infoproc.SIP;
import entity.market.clearingrule.UniformPriceClear;
import event.TimeStamp;

/**
 * MockMarket for testing purposes.
 * 
 * NOTE: A market clear must be explicitly called or scheduled. Orders match
 * at a uniform price at the midpoint of the BID/ASK.
 */
public class MockMarket extends Market {

	private static final long serialVersionUID = 1L;

	public MockMarket(SIP sip) {
		this(sip, TimeStamp.IMMEDIATE);
	}

	public MockMarket(SIP sip, TimeStamp latency) {
		super(sip, latency, new UniformPriceClear(0.5, 1), new Random());
	}
	
	@Override
	public Collection<? extends Activity> submitOrder(Agent agent, Price price,
			int quantity, TimeStamp currentTime) {
		return ImmutableList.<Activity> builder().addAll(
				super.submitOrder(agent, price, quantity, currentTime)).addAll(
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
