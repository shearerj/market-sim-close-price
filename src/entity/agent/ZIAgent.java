package entity.agent;

import static java.lang.Math.signum;

import java.util.Random;

import com.google.common.collect.ImmutableList;

import systemmanager.Keys;
import activity.Activity;
import activity.SubmitNMSOrder;
import data.EntityProperties;
import data.FundamentalValue;
import entity.infoproc.SIP;
import entity.market.Market;
import entity.market.Price;
import event.TimeStamp;

/**
 * ZIAGENT
 * 
 * A zero-intelligence (ZI) agent.
 * 
 * This agent bases its private value on a stochastic process, the parameters of which are specified
 * at the beginning of the simulation by the spec file. The agent's private valuation is determined
 * by value of the random process at the time it enters, with some randomization added by using an
 * individual variance parameter. The private value is used to calculate the agent's surplus (and
 * thus the market's allocative efficiency).
 * 
 * This agent submits only ONE limit order during its lifetime.
 * 
 * NOTE: The limit order price is uniformly distributed over a range that is twice the size of
 * bidRange in either a positive or negative direction from the agent's private value.
 * 
 * @author ewah
 */
public class ZIAgent extends BackgroundAgent {

	private static final long serialVersionUID = 1148707664467962927L;
	
	protected final int bidRange; // range for limit order

	public ZIAgent(TimeStamp arrivalTime, FundamentalValue fundamental, SIP sip,
			Market market, Random rand, int bidRange, double pvVar,
			int tickSize) {
		super(arrivalTime, fundamental, sip, market, new PrivateValue(1, pvVar,
				rand), rand, tickSize);
		this.bidRange = bidRange;
	}

	public ZIAgent(TimeStamp arrivalTime, FundamentalValue fundamental, SIP sip,
			Market market, Random rand, EntityProperties props) {
		this(arrivalTime, fundamental, sip, market, rand, props.getAsInt(
				Keys.BID_RANGE, 2000), props.getAsDouble(Keys.PRIVATE_VALUE_VAR, 100000000),
				props.getAsInt(Keys.TICK_SIZE, 1));
	}

	@Override
	public Iterable<? extends Activity> agentStrategy(TimeStamp currentTime) {
		// 50% chance of being either long or short
		int quantity = rand.nextBoolean() ? 1 : -1;
		Price val = fundamental.getValueAt(currentTime).plus(
				privateValue.getValueFromQuantity(positionBalance, quantity)).nonnegative();

		// basic ZI behavior
		Price price = new Price((int) (val.getInTicks() - signum(quantity)
				* rand.nextDouble() * 2 * bidRange)).nonnegative().quantize(tickSize);

		return ImmutableList.of(new SubmitNMSOrder(this, price, quantity,
				primaryMarket, TimeStamp.IMMEDIATE));
	}
}
