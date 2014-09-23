package data;

import static logger.Log.Level.ERROR;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

import systemmanager.Simulation;
import utils.Rands;

import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;

import entity.Entity;
import entity.View;
import entity.market.Price;
import event.TimeStamp;

/**
 * Class to store and compute a stochastic process used as a base to determine
 * the private valuations of background agents.
 * 
 * @author ewah
 */
public class FundamentalValue extends Entity implements Iterable<Double>, Serializable {

	private static final long serialVersionUID = 6764216196138108452L;
	protected static final Ordering<TimeStamp> ord = Ordering.natural();
	
	protected final ArrayList<Double> meanRevertProcess;
	protected final double kappa;
	protected final int meanValue;
	protected final double shockVar;
	protected final Random rand;

	/**
	 * @param kap rate which the process reverts to the mean value
	 * @param meanVal mean process
	 * @param var Gaussian Process variance
	 * @param rand Random generator
	 */
	protected FundamentalValue(Simulation sim, double kap, int meanVal, double var, Random rand) {
		super(0, sim);
		this.rand = rand;
		this.kappa = kap;
		this.meanValue = meanVal;
		this.shockVar = var;

		// stochastic initial conditions for random process
		meanRevertProcess = Lists.newArrayList();
		meanRevertProcess.add(Rands.nextGaussian(rand, meanValue, shockVar));
		postStat(0, Iterables.getOnlyElement(meanRevertProcess));
	}
	
	/**
	 * Creates a mean reverting Gaussian Process that supports random access to small (int) TimeStamps
	 * 
	 * @param kap
	 * @param meanVal
	 * @param var
	 * @param rand
	 * @return
	 */
	public static FundamentalValue create(Simulation sim, double kap, int meanVal, double var, Random rand) {
		return new FundamentalValue(sim, kap, meanVal, var, rand);
	}

	/**
	 * Helper method to ensure that maxQuery exists in the data structure.
	 * 
	 * @param maxQuery
	 */
	protected void computeFundamentalTo(int maxQuery) {
		for (int i = meanRevertProcess.size(); i <= maxQuery; i++) {
			double prevValue = Iterables.getLast(meanRevertProcess);
			double nextValue = Rands.nextGaussian(rand, meanValue * kappa + (1 - kappa) * prevValue, shockVar);
			meanRevertProcess.add(nextValue);
			postStat(i, nextValue);
		}
	}

	/**
	 * Returns the global fundamental value at time ts. If undefined, return 0.
	 */
	public Price getValueAt(TimeStamp t) {
		int index = (int) t.getInTicks();
		if (index < 0) { // In case of overflow
			sim.log(ERROR, "Tried to access out of bounds TimeStamp: %s (%d)", t, index);
			return Price.ZERO;
		} else {
			computeFundamentalTo(index);
			return Price.of(meanRevertProcess.get(index)).nonnegative();
		}
	}

	@Override
	public Iterator<Double> iterator() {
		return Iterators.unmodifiableIterator(meanRevertProcess.iterator());
	}
	
	// XXX These aren't rounded the way the price will be
	protected void postStat(int index, double value) {
		sim.postTimedStat(TimeStamp.of(index), Stats.FUNDAMENTAL, value);
		sim.postStat(Stats.CONTROL_FUNDAMENTAL, value);
	}

	public FundamentalValueView getView(final TimeStamp latency) {
		// TODO memeoize?
		return new FundamentalValueView(ord.max(latency, TimeStamp.ZERO));
	}
	
	public class FundamentalValueView implements View {
		private TimeStamp latency;
		
		protected FundamentalValueView(TimeStamp latency) {
			this.latency = latency;
		}
		
		public Price getValue() {
			return FundamentalValue.this.getValueAt(ord.max(FundamentalValue.this.currentTime().minus(latency), TimeStamp.ZERO));
		}
		
		@Override
		public TimeStamp getLatency() {
			return latency;
		}
	}
}
