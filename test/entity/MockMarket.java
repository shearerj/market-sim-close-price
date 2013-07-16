package entity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import event.TimeStamp;

import activity.Activity;

import market.Bid;
import market.Price;
import model.MarketModel;

public class MockMarket extends Market {
	
	public MockMarket(int marketID, MarketModel model) {
		super(marketID, model);
	}
	
	public MockMarket(MarketModel model) {
		this(0, model);
	}


	@Override
	public Map<Agent, Bid> getBids() {
		return orderbook.getActiveBids();
	}
	
	@Override
	public Collection<? extends Activity> submitBid(Agent agent, Price price,
			int quantity, TimeStamp currentTime) {
		Collection<Activity> acts = new ArrayList<Activity>
			(super.submitBid(agent, price, quantity, currentTime));
			this.updateQuote(currentTime);
		return acts;
	}


}
