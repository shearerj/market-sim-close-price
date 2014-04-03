package entity.market;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Random;

import systemmanager.Keys;
import systemmanager.Scheduler;
import activity.Clear;
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

	public CallMarket(Scheduler scheduler, SIP sip, Random rand,
			TimeStamp latency, int tickSize, double pricingPolicy,
			TimeStamp clearFreq) {
		
		this(scheduler, sip, rand, latency, latency, tickSize, pricingPolicy,
				clearFreq);
	}

	public CallMarket(Scheduler scheduler, SIP sip, Random rand,
			TimeStamp quoteLatency, TimeStamp transactionLatency, int tickSize,
			double pricingPolicy, TimeStamp clearFreq) {
		
		super(scheduler, sip, quoteLatency, transactionLatency,
				new UniformPriceClear(pricingPolicy, tickSize), rand);
		checkArgument(clearFreq.after(TimeStamp.ZERO),
				"Can't create a call market with 0 clear frequency. Create a CDA instead.");

		this.clearFreq = clearFreq;
		this.nextClearTime = TimeStamp.ZERO;
	}

	public CallMarket(Scheduler scheduler, SIP sip, Random rand,
			EntityProperties props) {
		
		this(scheduler, sip, rand,
				TimeStamp.create(props.getAsInt(Keys.QUOTE_LATENCY, props.getAsInt(Keys.MARKET_LATENCY, -1))),
				TimeStamp.create(props.getAsInt(Keys.TRANSACTION_LATENCY, props.getAsInt(Keys.MARKET_LATENCY, -1))),
				props.getAsInt(Keys.TICK_SIZE, 1),
				props.getAsDouble(Keys.PRICING_POLICY, 0.5),
				TimeStamp.create(props.getAsInt(Keys.CLEAR_FREQ, 100)));
	}

	@Override
	public void clear(TimeStamp currentTime) {
		nextClearTime = currentTime.plus(clearFreq);
		super.clear(currentTime);
		scheduler.scheduleActivity(nextClearTime, new Clear(this));
	}

}