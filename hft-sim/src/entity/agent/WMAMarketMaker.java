package entity.agent;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Random;

import systemmanager.Keys;
import systemmanager.Simulation;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import data.Props;
import entity.market.Market;

/**
 * WMAMARKETMAKER
 * 
 * Weighted Moving Average Market Maker
 * 
 * Computes either a linear weighted moving average or an exponential WMA.
 * Linear is selected with a weight factor of 0.
 * 
 * The computation of the exponential weighted moving average is parameterized
 * by a weight factor w in range (0,1). The weight of a general data point with
 * lag i is, before normalization:
 * 
 * w * ( 1-w )^i
 * 
 * A weight factor of 0 reverts the weighting to a linear weighted moving
 * average, which is computed with weight for a data point with lag i:
 * 
 * T-i
 * 
 * where T is the total number of elements being averaged.
 * 
 * NOTE: Because the prices are stored in an EvictingQueue, which does not
 * accept null elements, the number of elements in the bid/ask queues may not be
 * equivalent.
 * 
 * @author zzy, ewah
 * 
 */
// FIXME Instead of 0 being a special value, 0 should be zero (e.g. normal mean)
// and a linear weighting should be a separate class or something.
public class WMAMarketMaker extends MAMarketMaker {
	private static final long serialVersionUID = -8566264088391504213L;

	protected double weightFactor;

	protected WMAMarketMaker(Simulation sim, Market market, Random rand, Props props) {
		super(sim, market, rand, props);

		this.weightFactor = props.getAsDouble(Keys.WEIGHT_FACTOR);
		checkArgument(weightFactor >= 0 && weightFactor < 1, "Weight factor must be in range [0,1)!");
	}
	
	public static WMAMarketMaker create(Simulation sim, Market market, Random rand, Props props) {
		return new WMAMarketMaker(sim, market, rand, props);
	}
	
	protected double average(Iterable<? extends Number> numbers) {
		return weightFactor == 0 ? linearWeight(numbers) : exponentialWeight(numbers, 1 - weightFactor);
	}
		
	protected static double linearWeight(Iterable<? extends Number> numbers) {
		double sum = 0;
		double totalWeight = 0;
		int weight = 1;
		for (Number n : numbers) {
			sum += weight * n.doubleValue();
			totalWeight += weight;
			weight += 1;
		}
		return sum / totalWeight; // TODO Numerically unstable?
	}
	
	protected static double exponentialWeight(Iterable<? extends Number> numbers, double weightUpdate) {
		double sum = 0;
		double totalWeight = 0;
		double weight = 1;
		for (Number n : Lists.reverse(ImmutableList.copyOf(numbers))) {
			sum += weight * n.doubleValue();
			totalWeight += weight;
			weight *= weightUpdate;
		}
		return sum / totalWeight; // TODO Numerically unstable?
	}
	
}