package entity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.PriorityQueue;

import systemmanager.Consts;

import market.Bid;
import market.PQBid;
import market.Price;
import market.Quote;
import model.MarketModel;
import activity.Activity;
import activity.Clear;
import event.TimeStamp;

public class MockMarket extends Market {
	
	public MockMarket(int marketID, MarketModel model) {
		super(marketID, model);
		// TODO Auto-generated constructor stub
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

	@Override
	public Collection<? extends Activity> removeBid(Agent agent,
			TimeStamp currentTime) {
		orderbook.removeBid(agent.getID());
		return new ArrayList<Activity>();
	}
}
