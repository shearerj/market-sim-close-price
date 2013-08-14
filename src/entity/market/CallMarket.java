package entity.market;

import java.util.ArrayList;
import java.util.Collection;

import systemmanager.Keys;

import data.EntityProperties;

import activity.Activity;
import activity.Clear;
import entity.infoproc.SIP;
import entity.market.clearingrule.UniformPriceClear;
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
	
	private static final long serialVersionUID = -1736458709580878467L;
	
	protected final TimeStamp clearFreq;
	protected TimeStamp nextClearTime;

	public CallMarket(SIP sip, double pricingPolicy,
			TimeStamp clearFreq, TimeStamp latency, int tickSize) {
		super(sip, new UniformPriceClear(pricingPolicy, tickSize), latency);

		if (!clearFreq.after(TimeStamp.ZERO))
			throw new IllegalArgumentException(
					"Can't create a call market with 0 clear frequency. Create a CDA instead.");
		
		this.clearFreq = clearFreq;
		this.nextClearTime = TimeStamp.ZERO;
	}
	
	public CallMarket(SIP sip, EntityProperties props) {
		this(sip, props.getAsDouble(Keys.PRICING_POLICY, 0.5), new TimeStamp(
				props.getAsInt(Keys.CLEAR_FREQ, 100)), new TimeStamp(
				props.getAsInt(Keys.MARKET_LATENCY, -1)), props.getAsInt(
				Keys.TICK_SIZE, 1));
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
