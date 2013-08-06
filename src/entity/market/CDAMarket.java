package entity.market;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import clearingrule.EarliestPriceClear;

import model.MarketModel;
import activity.Activity;
import activity.Clear;
import entity.agent.Agent;
import event.TimeStamp;

/**
 * Class for a continuous double auction market.
 * 
 * @author ewah
 */
public class CDAMarket extends Market {

	public CDAMarket(int marketID, MarketModel model, TimeStamp latency) {
		super(marketID, model, new EarliestPriceClear(), latency);
	}

	@Override
	public Collection<? extends Activity> submitOrder(Agent agent, Price price,
			int quantity, TimeStamp curentTime) {
		Collection<Activity> activities = new ArrayList<Activity>(
				super.submitOrder(agent, price, quantity, curentTime));
		activities.add(new Clear(this, TimeStamp.IMMEDIATE));
		return activities;
	}

	@Override
	public Collection<? extends Activity> withdrawOrder(Order order,
			TimeStamp currentTime) {
		Collection<Activity> acts = new ArrayList<Activity>(2);
		acts.addAll(super.withdrawOrder(order, currentTime));
		acts.addAll(updateQuote(Collections.<Transaction>emptyList(), currentTime));
		return acts;
	}

}
