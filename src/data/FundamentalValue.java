package data;

import static logger.Logger.log;
import static logger.Logger.Level.ERROR;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Random;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import utils.Rands;
import entity.market.Price;
import event.TimeStamp;

/**
 * Class to store and compute a stochastic process used as a base to determine
 * the private valuations of background agents.
 * 
 * @author ewah
 */
// XXX Potentially move this to another package?
public class FundamentalValue implements Serializable {

	private static final long serialVersionUID = 6764216196138108452L;
	
	protected final ArrayList<Double> meanRevertProcess;
	protected final double kappa;
	protected final int meanValue;
	protected final double shockVar;
	protected final Random rand;

	/**
	 * Creates a mean reverting Gaussian Process that supports random access to small (int) TimeStamps
	 * 
	 * @param kap rate which the process reverts to the mean value
	 * @param meanVal mean process
	 * @param var Gaussian Process variance
	 * @param rand Random generator
	 */
	public FundamentalValue(double kap, int meanVal, double var, Random rand) {
		this.rand = rand;
		this.kappa = kap;
		this.meanValue = meanVal;
		this.shockVar = var;

		// stochastic initial conditions for random process
		meanRevertProcess = Lists.newArrayList();
		meanRevertProcess.add(Rands.nextGaussian(rand, meanValue, shockVar));
	}
	
	/**
	 * @param kap
	 * @param meanVal
	 * @param var
	 * @param rand
	 * @return
	 */
	public static FundamentalValue create(double kap, int meanVal, double var, Random rand) {
		return new FundamentalValue(kap, meanVal, var, rand);
	}

	/**
	 * Helper method to ensure that maxQuery exists in the data structure.
	 * 
	 * @param maxQuery
	 */
	protected void computeFundamentalTo(int maxQuery) {
		for (int i = meanRevertProcess.size(); i <= maxQuery; i++) {
			double prevValue = Iterables.getLast(meanRevertProcess);
			double nextValue = Rands.nextGaussian(rand, meanValue * kappa
					+ (1 - kappa) * prevValue, shockVar);
			meanRevertProcess.add(nextValue);
		}
	}

	/**
	 * Returns the global fundamental value at time ts. If undefined, return 0.
	 */
	public Price getValueAt(TimeStamp t) {
		int index = (int) t.getInTicks();
		if (index < 0) { // In case of overflow
			log(ERROR, "Tried to access out of bounds TimeStamp: " + t + " ("
					+ index + ")");
			return new Price(0);
		}
		computeFundamentalTo(index);
		return new Price((int) (double) meanRevertProcess.get(index)).nonnegative();
	}
	
	/**
	 * Returns a TimeSeries copy of the fundamental data. This makes a copy, and
	 * does not return a view into the FundamentalValue. This makes it
	 * expensive, and not super great. This could be fixed if TimeSeries were an
	 * interface, but that would require more thought.
	 */
	public TimeSeries asTimeSeries() {
		TimeSeries copy = TimeSeries.create();
		int time = 0;
		for (double v : meanRevertProcess)
			copy.add(time++, v);
		return copy;
	}
}
