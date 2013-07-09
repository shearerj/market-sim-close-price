package entity;

import static java.lang.Math.signum;

import java.util.Collection;

import market.Price;
import market.PrivateValue;
import model.MarketModel;
import utils.RandPlus;
import activity.Activity;
import data.EntityProperties;
import event.TimeStamp;

/**
 * ZIAGENT
 * 
 * A zero-intelligence (ZI) agent.
 * 
 * This agent bases its private value on a stochastic process, the parameters of
 * which are specified at the beginning of the simulation by the spec file. The
 * agent's private valuation is determined by value of the random process at the
 * time it enters, with some randomization added by using an individual variance
 * parameter. The private value is used to calculate the agent's surplus (and
 * thus the market's allocative efficiency).
 * 
 * This agent submits only ONE limit order during its lifetime.
 * 
 * NOTE: The limit order price is uniformly distributed over a range that is
 * twice the size of bidRange in either a positive or negative direction from
 * the agent's private value.
 * 
 * @author ewah
 */
public class ZIAgent extends BackgroundAgent {

	protected final int bidRange; // range for limit order

	public ZIAgent(int agentID, TimeStamp arrivalTime, MarketModel model,
			Market market, RandPlus rand, int bidRange, double pvVar) {
		super(agentID, arrivalTime, model, market, new PrivateValue(1, pvVar,
				rand), rand);
		this.bidRange = bidRange;
	}

	public ZIAgent(int agentID, TimeStamp arrivalTime, MarketModel model,
			Market market, RandPlus rand, EntityProperties props) {
		this(agentID, arrivalTime, model, market, rand, props.getAsInt(
				BIDRANGE_KEY, 2000), props.getAsDouble("pvVar", 100));
		// FIXME get KEY for PVVar and and proper default
	}

	@Override
	public Collection<? extends Activity> agentStrategy(TimeStamp currentTime) {
		// 50% chance of being either long or short
		int quantity = rand.nextBoolean() ? 1 : -1;
		Price val = model.getFundamentalAt(currentTime).plus(
				privateValue.getValueFromQuantity(positionBalance, quantity)).nonnegative();

		// basic ZI behavior
		Price price = new Price((int) (val.getPrice() - signum(quantity)
				* rand.nextDouble() * 2 * bidRange)).nonnegative();

		// bid does not expire
		return executeSubmitNMSBid(price, quantity, currentTime);
	}
}
