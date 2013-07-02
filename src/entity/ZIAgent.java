package entity;

import java.util.Collection;
import java.util.HashMap;

import market.Price;
import model.MarketModel;
import utils.RandPlus;
import activity.Activity;
import data.EntityProperties;
import data.Observations;
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
			Market market, RandPlus rand, int bidRange) {
		super(agentID, arrivalTime, model, market, rand);
		this.bidRange = bidRange;
	}

	public ZIAgent(int agentID, TimeStamp arrivalTime, MarketModel model,
			Market market, RandPlus rand, EntityProperties props) {
		this(agentID, arrivalTime, model, market, rand, props.getAsInt(
				BIDRANGE_KEY, 2000));
	}

	@Override
	public HashMap<String, Object> getObservation() {
		HashMap<String, Object> obs = new HashMap<String, Object>();
		obs.put(Observations.ROLE_KEY, getRole());
		obs.put(Observations.PAYOFF_KEY, getRealizedProfit());
		obs.put(Observations.STRATEGY_KEY, getFullStrategy());
		return obs;
	}

	@Override
	public Collection<? extends Activity> agentStrategy(TimeStamp ts) {
		// update quotes
		this.updateQuotes(market, ts);

		Price price;
		int quantity;
		quantity = rand.nextBoolean() ? 1 : -1; // 50% chance of being either long or
											// short
		int val = Math.max(0,
				model.getFundamentalAt(ts).sum(getPrivateValueAt(quantity)).getPrice());

		// basic ZI behavior
		if (quantity > 0)
			price = new Price((int) Math.max(0, ((val - 2 * bidRange) + rand.nextDouble() * 2
					* bidRange)));
		else
			price = new Price((int) Math.max(0, (val + rand.nextDouble() * 2 * bidRange)));

		return executeSubmitNMSBid(price, quantity, ts); // bid does not expire
	}
}
