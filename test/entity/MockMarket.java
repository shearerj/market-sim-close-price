package entity;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import market.Price;
import market.Transaction;
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
		updateQuote(Collections.<Transaction>emptyList(), currentTime);
		return acts;
	}

	@Override
	public Collection<? extends Activity> withdrawBid(Agent agent,
			TimeStamp currentTime) {
		Collection<? extends Activity> acts = super.withdrawBid(agent,
				currentTime);
		updateQuote(Collections.<Transaction>emptyList(), currentTime);
		return acts;
	}

	@Override
	protected List<Transaction> clearPricing(TimeStamp currentTime) {
		return orderbook.uniformPriceClear(currentTime, 0.5f);
	}

}
