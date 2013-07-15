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
		super(marketID, model, ipID);
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

	public Collection<? extends Activity> addBid(Bid bid, TimeStamp curentTime) {
		Collection<Activity> activities = new ArrayList<Activity>(super.addBid(
				bid, curentTime));
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
