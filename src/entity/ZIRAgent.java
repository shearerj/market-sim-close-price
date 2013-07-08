package entity;

import static logger.Logger.log;
import static logger.Logger.Level.INFO;

import java.util.ArrayList;
import java.util.Collection;

import market.Price;
import market.PrivateValue;
import model.MarketModel;
import utils.RandPlus;
import activity.Activity;
import activity.AgentStrategy;
import data.ArrivalTime;
import data.EntityProperties;
import event.TimeStamp;

/**
 * ZIRAGENT
 * 
 * A zero-intelligence agent with re-submission (ZIR).
 * 
 * The ZIR agent is primarily associated with a single market. It wakes up
 * periodically to submit a new bid (if its previous bid has transacted) or it
 * does nothing.
 * 
 * This agent bases its private value on a stochastic process, the parameters of
 * which are specified at the beginning of the simulation by the spec file. The
 * agent's private valuation is determined by value of the random process at the
 * time it enters, with some randomization added by using an individual variance
 * parameter. The private value is used to calculate the agent's surplus (and
 * thus the market's allocative efficiency).
 * 
 * This agent submits a single limit order at a time. It will modify its private
 * value if its bid has transacted by the time it wakes up.
 * 
 * NOTE: Each limit order price is uniformly distributed over a range that is
 * twice the size of bidRange in either a positive or negative direction from
 * the agent's private value.
 * 
 * @author ewah
 */
public class ZIRAgent extends BackgroundAgent {

	protected int bidRange; // range for limit order
	protected ArrivalTime reentry; // re-entry times
	protected int lastPositionBalance; // last position balance
	protected int maxAbsPosition; // max quantity for position
	// for computing discounted surplus
	protected ArrayList<TimeStamp> submissionTimes; // TODO currently unused

	public ZIRAgent(int agentID, TimeStamp arrivalTime, MarketModel model,
			Market market, RandPlus rand, int bidRange, int maxAbsPosition,
			int reentryRate, double pvVar) {
		// TODO replace null with proper private value initialization
		super(agentID, arrivalTime, model, market, getPrivateValue(
				maxAbsPosition, pvVar, rand), rand);
		this.bidRange = bidRange;
		this.maxAbsPosition = maxAbsPosition;
		this.reentry = new ArrivalTime(arrivalTime, reentryRate, rand);
		this.lastPositionBalance = positionBalance;
		this.submissionTimes = new ArrayList<TimeStamp>();
	}

	public ZIRAgent(int agentID, TimeStamp arrivalTime, MarketModel model,
			Market market, RandPlus rand, EntityProperties props) {
		// TODO get keys and default value for reentry rate and pvvar
		this(agentID, arrivalTime, model, market, rand, props.getAsInt(
				BIDRANGE_KEY, 5000), props.getAsInt(MAXQUANTITY_KEY, 10),
				props.getAsInt("reentry_rate", 100), props.getAsDouble("pvVar",
						100));
	}

	/**
	 * Initialize list of private values given max quantity.
	 */
	protected static PrivateValue getPrivateValue(int maxAbsPosition,
			double pvVar, RandPlus rand) {
		ArrayList<Integer> alphas = new ArrayList<Integer>();
		for (int i = -maxAbsPosition; i <= maxAbsPosition; i++) {
			if (i != 0)
				alphas.add((int) Math.round(rand.nextGaussian(0, pvVar)));
		}
		return new PrivateValue(alphas);
	}

	@Override
	public Collection<Activity> agentStrategy(TimeStamp ts) {
		Collection<Activity> actMap = new ArrayList<Activity>();

		this.updateQuotes(market, ts);

		String s = ts + " | " + this + " " + agentType + ":";
		if (!ts.equals(arrivalTime)) {
			s += " wake up.";
			if (positionBalance == lastPositionBalance) {
				s += " last order has not transacted, go back to sleep";
			}
		}
		if (positionBalance != lastPositionBalance || ts.equals(arrivalTime)) {
			// If either first arrival or if last order has already transacted
			// then should
			// submit a new order. Otherwise, do nothing (does not cancel
			// orders).

			Price price;
			// 0.50% chance of being either long or short
			int quantity = rand.nextBoolean() ? 1 : -1;

			int val = 0;
			int newPosition = quantity + positionBalance;
			// check that will not exceed max absolute position
			if (newPosition <= maxAbsPosition && newPosition >= -maxAbsPosition) {
				val = Math.max(
						0,
						model.getFundamentalAt(ts).plus(
								getPrivateValueAt(quantity)).getPrice());
				s += " position=" + positionBalance + ", for q=" + quantity
						+ ", value=" + model.getFundamentalAt(ts) + " + "
						+ getPrivateValueAt(quantity) + "=" + val;

				if (quantity > 0) {
					price = new Price((int) Math.max(0,
							((val - 2 * bidRange) + rand.nextDouble() * 2
									* bidRange)));
				} else {
					price = new Price((int) Math.max(0,
							(val + rand.nextDouble() * 2 * bidRange)));
				}
				log(INFO, s);
				actMap.addAll(executeSubmitNMSBid(price, quantity, ts));
				submissionTimes.add(ts);

				lastPositionBalance = positionBalance; // update position
														// balance

			} else {
				s += "new order would exceed max position " + maxAbsPosition
						+ "; no submission";
				log(INFO, s);
			}
			// if exceed max position, then don't submit a new bid
			// TODO - stay the same for now (position balance)
		} else {
			log(INFO, s);
		}

		actMap.add(new AgentStrategy(this, reentry.next()));
		return actMap;
	}

}
