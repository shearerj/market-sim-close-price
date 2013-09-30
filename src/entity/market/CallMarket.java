package entity.market;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Random;

import systemmanager.Keys;
import activity.Activity;
import activity.Clear;

import com.google.common.collect.ImmutableList;

import data.EntityProperties;
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

	public CallMarket(SIP sip, TimeStamp latency, Random rand, int tickSize,
			double pricingPolicy, TimeStamp clearFreq) {
		super(sip, latency, new UniformPriceClear(pricingPolicy, tickSize), rand);
		checkArgument(clearFreq.after(TimeStamp.ZERO),
				"Can't create a call market with 0 clear frequency. Create a CDA instead.");	// TODO really? why not?

		this.clearFreq = clearFreq;
		this.nextClearTime = TimeStamp.ZERO;
	}
	
	public CallMarket(SIP sip, Random rand, EntityProperties props) {
		this(sip, new TimeStamp(props.getAsInt(Keys.MARKET_LATENCY, -1)), rand,
				props.getAsInt(Keys.TICK_SIZE, 1),
				props.getAsDouble(Keys.PRICING_POLICY, 0.5),
				new TimeStamp(props.getAsInt(Keys.CLEAR_FREQ, 100)));
	}

	@Override
	public Iterable<? extends Activity> clear(TimeStamp currentTime) {
		nextClearTime = currentTime.plus(clearFreq);
		return ImmutableList.<Activity> builder().addAll(
				super.clear(currentTime)).add(new Clear(this, nextClearTime)).build();
	}

	@Override
	public String toString() {
		return "Call " + super.toString();
	}

}
