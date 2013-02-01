package systemmanager;

import java.util.Random;
import java.util.ArrayList;

import market.Price;

/**
 * Class to store and compute a stochastic process used as a base to determine the
 * private valuations of background agents.
 * 
 * @author ewah
 */
public class FundamentalValue {
	
	private ArrayList<Price> meanRevertProcess;
	private double kappa;
	private int meanValue;
	private double shockVar;
	private int length;			// length of random process (in time-steps)
	private Random rand;
	
	/**
	 * Constructor
	 * @param meanVal
	 * @param var
	 * @param k
	 */
	public FundamentalValue(double kap, int meanVal, double var, int l) {
		rand = new Random();
		kappa = kap;
		meanValue = meanVal;
		shockVar = var;
		length = l;
		
		// stochastic initial conditions for random process
		meanRevertProcess = new ArrayList<Price>();
		meanRevertProcess.add(new Price((int) getNormalRV(meanValue, shockVar)));
		computeAllFundamentalValues();
	}
	
	/**
	 * Compute global fundamental values across entire time horizon.
	 */
	public void computeAllFundamentalValues() {
		// iterate through all the time steps & generate
		for (int i = 1; i <= length; i++) {
			int prevValue = meanRevertProcess.get(i-1).getPrice();
			int nextValue = (int) (getNormalRV(0, shockVar) + (meanValue * kappa) +
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
	public Price getValueAt(int t) {
		// for positive time stamp that is a valid index
		if (meanRevertProcess.size() >= t && t > 0) {
			return meanRevertProcess.get(t);
		}
		return new Price(0);
	}
	
	/** 
	 * Generate normal random variable
	 * @param mu
	 * @param var
	 * @return
	 */
	private double getNormalRV(double mu, double var) {
	    double r1 = rand.nextDouble();
	    double r2 = rand.nextDouble();
	    double z = Math.sqrt(-2*Math.log(r1))*Math.cos(2*Math.PI*r2);
	    return mu + z * Math.sqrt(var);
	}
}
