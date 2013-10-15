package entity.market;

import java.util.Collection;
import java.util.Random;

import activity.Activity;

import com.google.common.collect.ImmutableList;

import entity.agent.Agent;
import entity.infoproc.SIP;
import entity.market.clearingrule.UniformPriceClear;
import event.TimeStamp;

public class MockMarket extends Market {

	private static final long serialVersionUID = 1L;

	public MockMarket(SIP sip) {
		super(sip, TimeStamp.IMMEDIATE, new UniformPriceClear(0.5, 1), new Random());
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
