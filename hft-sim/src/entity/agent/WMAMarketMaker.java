package entity.agent;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Random;

import logger.Log;
import systemmanager.Keys.WeightFactor;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import data.FundamentalValue;
import data.Props;
import data.Stats;
import entity.market.Market;
import entity.sip.MarketInfo;
import event.Timeline;

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

	protected WMAMarketMaker(int id, Stats stats, Timeline timeline, Log log, Random rand, MarketInfo sip, FundamentalValue fundamental,
			Market market, Props props) {
		super(id, stats, timeline, log, rand, sip, fundamental, market, props);

		this.weightFactor = props.get(WeightFactor.class);
		checkArgument(weightFactor >= 0 && weightFactor < 1, "Weight factor must be in range [0,1)!");
	}
	
	public static WMAMarketMaker create(int id, Stats stats, Timeline timeline, Log log, Random rand, MarketInfo sip, FundamentalValue fundamental,
			Market market, Props props) {
		return new WMAMarketMaker(id, stats, timeline, log, rand, sip, fundamental, market, props);
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