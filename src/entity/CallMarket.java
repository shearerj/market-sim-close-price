package entity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import market.Bid;
import model.MarketModel;
import systemmanager.Consts;
import activity.Activity;
import activity.Clear;
import event.TimeStamp;

/**
 * Class for a call market. The order book is closed, therefore agents will only
 * be able to see the price of the last clear as well as the bid/ask immediately
 * after the clear, i.e. they will be able to see the best available buy and
 * sell prices for the bids left in the order book after each market clear.
 * 
 * NOTE: First Clear Activity is initialized in the SystemManager.
 * 
 * @author ewah
 */
public class CallMarket extends Market {

	public final static String CLEAR_FREQ_KEY = "clearFreq";
	public final static String PRICING_POLICY_KEY = "pricingPolicy";

	public float pricingPolicy; // XXX Unused?
	protected final TimeStamp clearFreq;
	protected TimeStamp nextClearTime, nextQuoteTime;

	public CallMarket(int marketID, MarketModel model, float pricingPolicy,
			TimeStamp clearFreq) {
		super(marketID, model, model.getipIDgen());

		if (clearFreq.after(Consts.START_TIME))
			throw new IllegalArgumentException(
					"Can't create a call market with 0 clear frequency. Create a CDA instead.");
		this.pricingPolicy = pricingPolicy;
		this.clearFreq = clearFreq;
		this.nextClearTime = clearFreq;
	}
	
	@Override
	public Map<Agent, Bid> getBids() {
		return orderbook.getActiveBids();
	}

	@Override
	public Collection<? extends Activity> clear(TimeStamp currentTime) {
		// Update the next clear time
		nextClearTime = currentTime.plus(clearFreq);
		Collection<Activity> activities = new ArrayList<Activity>(
				super.clear(currentTime));
		// Insert next clear activity at some time in the future
		activities.add(new Clear(this, nextClearTime));
		return activities;
	}
	
}
