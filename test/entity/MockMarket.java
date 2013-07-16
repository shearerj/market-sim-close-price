package entity;

import java.util.Collection;

import market.Price;
import model.MarketModel;
import activity.Activity;
import event.TimeStamp;

public class MockMarket extends Market {

	public MockMarket(int marketID, MarketModel model) {
		super(marketID, model);
	}

	public MockMarket(MarketModel model) {
		this(0, model);
	}

	@Override
	public Collection<? extends Activity> submitBid(Agent agent, Price price,
			int quantity, TimeStamp currentTime) {
		Collection<? extends Activity> acts = super.submitBid(agent, price,
				quantity, currentTime);
		updateQuote(currentTime);
		return acts;
	}

	@Override
	public Collection<? extends Activity> withdrawBid(Agent agent,
			TimeStamp currentTime) {
		Collection<? extends Activity> acts = super.withdrawBid(agent,
				currentTime);
		updateQuote(currentTime);
		return acts;
	}

}
