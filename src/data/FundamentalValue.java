package data;

import java.util.ArrayList;

import event.TimeStamp;

import utils.RandPlus;

import logger.Logger;
import market.Price;

/**
 * Class to store and compute a stochastic process used as a base to determine the
 * private valuations of background agents.
 * 
 * @author ewah
 */
public class FundamentalValue {
	
	protected ArrayList<Price> meanRevertProcess;
	protected double kappa;
	protected int meanValue;
	protected double shockVar;
	protected int length;			// length of random process (in time-steps)
	protected RandPlus rand;
	
	/**
	 * Constructor
	 * @param meanVal
	 * @param var
	 * @param k
	 */
	public FundamentalValue(double kap, int meanVal, double var, int l) {
		rand = new RandPlus();
		kappa = kap;
		meanValue = meanVal;
		shockVar = var;
		length = l;
		
		// stochastic initial conditions for random process
		meanRevertProcess = new ArrayList<Price>();
		meanRevertProcess.add(new Price((int) rand.nextGaussian(meanValue, shockVar)));
		computeAllFundamentalValues();
	}
	
	/**
	 * Compute global fundamental values across entire time horizon.
	 */
	public void computeAllFundamentalValues() {
		// iterate through all the time steps & generate
		for (int i = 1; i <= length; i++) {
			int prevValue = meanRevertProcess.get(i-1).getPrice();
			int nextValue = (int) (rand.nextGaussian(0, shockVar) + (meanValue * kappa) +
					((1 - kappa) *	prevValue));
			// truncate at zero
			nextValue = Math.max(nextValue, 0);
			meanRevertProcess.add(new Price(nextValue));
		}
	}

	/**
	 * @return list of all private values in the stochastic process
	 */
	public ArrayList<Price> getProcess() {
		return meanRevertProcess;
	}
	
	/**
	 * Returns the global fundamental value at time ts. If undefined, return 0.
	 * 
	 * @param ts
	 * @return
	 */
	public Price getValueAt(TimeStamp t) {
		if (meanRevertProcess.size() >= t.longValue() && t.after(TimeStamp.startTime)) {
			return meanRevertProcess.get((int) t.longValue());
		}
		Logger.log(Logger.ERROR, "Tried to access out of bounds TimeStamp: " + t);
		return new Price(0);
	}
	
}
