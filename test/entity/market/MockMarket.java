package entity.market;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import activity.Activity;
import entity.agent.Agent;
import entity.infoproc.SIP;
import entity.market.Market;
import entity.market.Order;
import entity.market.Price;
import entity.market.Transaction;
import entity.market.clearingrule.UniformPriceClear;
import event.TimeStamp;

public class MockMarket extends Market {

	private static final long serialVersionUID = 1L;

	public MockMarket(SIP sip) {
		super(sip, new UniformPriceClear(0.5, 1), TimeStamp.IMMEDIATE);
	}

	@Override
	public Collection<? extends Activity> submitOrder(Agent agent, Price price,
			int quantity, TimeStamp currentTime) {
		Collection<Activity> acts = new ArrayList<Activity>(2);
		acts.addAll(super.submitOrder(agent, price, quantity, currentTime));
		acts.addAll(updateQuote(Collections.<Transaction> emptyList(), currentTime));
		return acts;
	}

	@Override
	public Collection<? extends Activity> withdrawOrder(Order order,
			TimeStamp currentTime) {
		Collection<Activity> acts = new ArrayList<Activity>(2);
		acts.addAll(super.withdrawOrder(order, currentTime));
		acts.addAll(updateQuote(Collections.<Transaction> emptyList(), currentTime));
		return acts;
	}

}
