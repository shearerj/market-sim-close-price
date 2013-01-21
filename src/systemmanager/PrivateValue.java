package systemmanager;

import java.util.Random;
import java.util.ArrayList;

import market.Price;

/**
 * Class to store and compute a stochastic process used as a base to determine the
 * private values of background agents.
 * 
 * @author ewah
 */
public class PrivateValue {
	
	private ArrayList<Price> privateValueProcess;
	private double kappa;
	private int meanPV;
	private double shockVar;
	private Random rand;
	
	/**
	 * Constructor with defaults
	 */
	public PrivateValue() {
		rand = new Random();
		kappa = 0.1;
		meanPV = 50000;
		shockVar = 2;
		privateValueProcess = new ArrayList<Price>();
	}
	
	/**
	 * Constructor
	 * @param meanValue
	 * @param var
	 * @param k
	 */
	public PrivateValue(double kap, int meanValue, double var) {
		rand = new Random();
		kappa = kap;
		meanPV = meanValue;
		shockVar = var;
		privateValueProcess = new ArrayList<Price>();
		// stochastic initial conditions for random process
		privateValueProcess.add(new Price((int) getNormalRV(meanPV, shockVar)));
	}
	
	/**
	 * Compute the next private value based on the most recent value.
	 * @return
	 */
	public int next() {
		int nextPV = (int) ((meanPV * kappa) + ((1 - kappa) * 
						privateValueProcess.get(privateValueProcess.size()-1).getPrice()) + getNormalRV(0, shockVar));
		// truncate at zero
		nextPV = Math.max(nextPV, 0);
	    privateValueProcess.add(new Price(nextPV));
	    return nextPV;
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
	
	
	/**
	 * @return list of all private values in the stochastic process
	 */
	public ArrayList<Price> getPrivateValueProcess() {
		return privateValueProcess;
	}
}
