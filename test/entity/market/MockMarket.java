package entity.market;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import clearingrule.UniformPriceClear;

import model.MarketModel;
import activity.Activity;
import entity.agent.Agent;
import entity.market.Market;
import entity.market.Order;
import entity.market.Price;
import entity.market.Transaction;
import event.TimeStamp;

public class MockMarket extends Market {

	public MockMarket(int marketID, MarketModel model) {
		super(marketID, model, new UniformPriceClear(0.5d), TimeStamp.IMMEDIATE);
	}

	public MockMarket(MarketModel model) {
		this(0, model);
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
