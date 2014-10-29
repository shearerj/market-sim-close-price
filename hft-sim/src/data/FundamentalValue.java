package data;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

import utils.Rands;

import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;

import entity.View;
import entity.market.Price;
import event.TimeLine;
import event.TimeStamp;

/**
 * Class to store and compute a stochastic process used as a base to determine
 * the private valuations of background agents.
 * 
 * @author ewah
 */
public class FundamentalValue implements Iterable<Double>, Serializable {

	protected static final Ordering<TimeStamp> ord = Ordering.natural();
	
	private final TimeLine timeline;
	private final Stats stats;
	
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
	protected FundamentalValue(Stats stats, TimeLine timeline, double kap, int meanVal, double var, Random rand) {
		this.stats = stats;
		this.timeline = timeline;
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
	 */
	public static FundamentalValue create(Stats stats, TimeLine timeline, double kap, int meanVal, double var, Random rand) {
		return new FundamentalValue(stats, timeline, kap, meanVal, var, rand);
	}

	/**
	 * Helper method to ensure that maxQuery exists in the data structure.
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
		checkArgument(!t.before(TimeStamp.ZERO), "Can't query before time zero");
		int index = (int) t.getInTicks();
		computeFundamentalTo(index);
		return Price.of(meanRevertProcess.get(index)).nonnegative();
	}

	@Override
	public Iterator<Double> iterator() {
		return Iterators.unmodifiableIterator(meanRevertProcess.iterator());
	}
	
	// XXX These aren't rounded the way the prices will be
	protected void postStat(int index, double value) {
		stats.postTimed(TimeStamp.of(index), Stats.FUNDAMENTAL, value);
		stats.post(Stats.CONTROL_FUNDAMENTAL, value);
	}

	/** Get a possibly delayed view of the fundamental process */
	public FundamentalValueView getView(final TimeStamp latency) {
		// TODO memeoize?
		return new FundamentalValueView(ord.max(latency, TimeStamp.ZERO));
	}
	
	/** A view of this process from some entity with limited information */
	public class FundamentalValueView implements View {
		private TimeStamp latency;
		
		protected FundamentalValueView(TimeStamp latency) {
			this.latency = latency;
		}
		
		/** Gets most recent known value of the fundamental for an entity in the simulation */
		public Price getValue() {
			return FundamentalValue.this.getValueAt(ord.max(timeline.getCurrentTime().minus(latency), TimeStamp.ZERO));
		}
		
		@Override
		public TimeStamp getLatency() {
			return latency;
		}
	}

	private static final long serialVersionUID = 6764216196138108452L;
}
