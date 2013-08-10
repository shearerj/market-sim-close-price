package data;

import static logger.Logger.log;
import static logger.Logger.Level.ERROR;

import java.util.ArrayList;

import utils.RandPlus;
import entity.market.Price;
import event.TimeStamp;

/**
 * Class to store and compute a stochastic process used as a base to determine
 * the private valuations of background agents.
 * 
 * @author ewah
 */
// XXX Potentially need a way to do this that will work for longs / arbitrary time stamps
public class FundamentalValue {

	protected final ArrayList<Double> meanRevertProcess;
	protected final double kappa;
	protected final int meanValue;
	protected final double shockVar;
	protected RandPlus rand;

	/**
	 * Creates a mean reverting Gaussian Process that supports random access to small (int) TimeStamps
	 * @param kap rate which the process reverts to the mean value
	 * @param meanVal mean process
	 * @param var Gaussian Process variance
	 * @param rand Random generator
	 */
	public FundamentalValue(double kap, int meanVal, double var, RandPlus rand) {
		this.rand = rand;
		this.kappa = kap;
		this.meanValue = meanVal;
		this.shockVar = var;

		// stochastic initial conditions for random process
		meanRevertProcess = new ArrayList<Double>();
		meanRevertProcess.add(rand.nextGaussian(meanValue, shockVar));
	}

	protected void computeFundamentalTo(int length) {
		while (meanRevertProcess.size() < length + 1) {
			double prevValue = meanRevertProcess.get(meanRevertProcess.size() - 1);
			double nextValue = rand.nextGaussian(meanValue * kappa
					+ (1 - kappa) * prevValue, shockVar);
			meanRevertProcess.add(nextValue);
		}
	}

	/**
	 * Returns the global fundamental value at time ts. If undefined, return 0.
	 */
	public Price getValueAt(TimeStamp t) {
		int index = (int) t.getInTicks();
		if (index < 0) { // Incase of overflow
			log(ERROR, "Tried to access out of bounds TimeStamp: " + t + " ("
					+ index + ")");
			return new Price(0);
		}
		computeFundamentalTo(index);
		return new Price((int) (double) meanRevertProcess.get(index)).nonnegative();
	}

}
