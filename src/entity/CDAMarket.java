package entity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import market.Bid;
import market.PQBid;
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

	public Bid getBidQuote() {
		return orderbook.getBidQuote();
	}

	public Bid getAskQuote() {
		return orderbook.getAskQuote();
	}

	public Price getBidPrice() {
		return ((PQBid) getBidQuote()).bidTreeSet.first().getPrice();
	}

	public Price getAskPrice() {
		return ((PQBid) getAskQuote()).bidTreeSet.last().getPrice();
	}

	@Override
	public Collection<? extends Activity> submitBid(Agent agent, Price price,
			int quantity, TimeStamp curentTime) {
		Collection<Activity> activities = new ArrayList<Activity>(
				super.submitBid(agent, price, quantity, curentTime));
		activities.add(new Clear(this, Consts.INF_TIME));
		return activities;
	}

	public Collection<? extends Activity> removeBid(Agent agent,
			TimeStamp curentTime) {
		Collection<Activity> activities = new ArrayList<Activity>(
				super.removeBid(agent, curentTime));
		activities.add(new Clear(this, Consts.INF_TIME));
		return activities;
	}

	public Map<Agent, Bid> getBids() {
		return orderbook.getActiveBids();
	}

}
