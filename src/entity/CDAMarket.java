package entity;

import java.util.ArrayList;
import java.util.Collection;

import market.Price;
import model.MarketModel;
import systemmanager.Consts;
import activity.Activity;
import activity.Clear;
import event.TimeStamp;

/**
 * Class for a continuous double auction market.
 * 
 * @author ewah
 */
public class CDAMarket extends Market {

	public CDAMarket(int marketID, MarketModel model, int ipID) {
		super(marketID, model);
	}

	@Override
	public Collection<? extends Activity> submitBid(Agent agent, Price price,
			int quantity, TimeStamp curentTime) {
		Collection<Activity> activities = new ArrayList<Activity>(
				super.submitBid(agent, price, quantity, curentTime));
		activities.add(new Clear(this, Consts.INF_TIME));
		return activities;
	}

	@Override
	public Collection<? extends Activity> withdrawBid(Agent agent,
			TimeStamp curentTime) {
		Collection<Activity> activities = new ArrayList<Activity>(
				super.withdrawBid(agent, curentTime));
		activities.add(new Clear(this, Consts.INF_TIME));
		return activities;
	}

}
