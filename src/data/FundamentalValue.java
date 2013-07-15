package data;

import static logger.Logger.log;
import static logger.Logger.Level.ERROR;

import java.util.ArrayList;

import market.Price;
import utils.RandPlus;
import event.TimeStamp;

/**
 * Class to store and compute a stochastic process used as a base to determine
 * the private valuations of background agents.
 * 
 * @author ewah
 */
// XXX Potentially need a way to do this that will work for longs
public class FundamentalValue {

	protected final ArrayList<Price> meanRevertProcess;
	protected final double kappa;
	protected final int meanValue;
	protected final double shockVar;
	protected RandPlus rand;

	@Deprecated
	public FundamentalValue(double kap, int meanVal, double var, int l) {
		this(kap, meanVal, var, new RandPlus());
	}

	// TODO Documentation with description of parameters
	public FundamentalValue(double kap, int meanVal, double var, RandPlus rand) {
		this.rand = rand;
		this.kappa = kap;
		this.meanValue = meanVal;
		this.shockVar = var;

		// stochastic initial conditions for random process
		meanRevertProcess = new ArrayList<Price>();
		meanRevertProcess.add(new Price((int) rand.nextGaussian(meanValue,
				shockVar)));
	}

	protected void computeFundamentalTo(int length) {
		while (meanRevertProcess.size() < length + 1) {
			int prevValue = meanRevertProcess.get(meanRevertProcess.size() - 1).getPrice();
			int nextValue = (int) (rand.nextGaussian(0, shockVar)
					+ (meanValue * kappa) + ((1 - kappa) * prevValue));
			meanRevertProcess.add(new Price(Math.max(nextValue, 0)));
		}
	}

	/**
	 * Returns the global fundamental value at time ts. If undefined, return 0.
	 */
	public Price getValueAt(TimeStamp t) {
		int index = (int) t.longValue(); // Incase of overflow
		if (index < 0) {
			log(ERROR,
					"Tried to access out of bounds TimeStamp: " + t);
			return new Price(0);
		}
		computeFundamentalTo(index);
		return meanRevertProcess.get(index);
	}

}
