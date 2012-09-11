package systemmanager;

import java.util.Random;
import java.util.ArrayList;

import market.Price;

/**
 * Class to store and compute private values for background agents.
 * 
 * @author ewah
 */
public class PrivateValue {
	
	private ArrayList<Price> privateValues;
	private double kappa;
	private int meanPV;
	private double shockVar;
	private int tickSize;
	private Random rand;
	
	/**
	 * Constructor with defaults
	 */
	public PrivateValue() {
		rand = new Random();
		kappa = 0.1;
		meanPV = 50000;
		shockVar = 2;
		tickSize = 1;
		privateValues = new ArrayList<Price>();
	}
	
	/**
	 * Constructor
	 * @param k
	 * @param meanValue
	 * @param var
	 */
	public PrivateValue(double kap, int meanValue, double var, int tick) {
		rand = new Random();
		kappa = kap;
		meanPV = meanValue;
		shockVar = var;
		privateValues = new ArrayList<Price>();
		tickSize = tick;
		
		// stochastic initial conditions for random process
		privateValues.add(new Price((int) getNormalRV(meanPV, shockVar)));
	}
	
	/**
	 * Compute the next private value based on the most recent value.
	 * @return
	 */
	public int next() {
		int nextPV = (int) ((meanPV * kappa) + ((1 - kappa) * 
						privateValues.get(privateValues.size()-1).getPrice()) +
	                    getNormalRV(0, shockVar) * tickSize);
		// truncate at zero
		nextPV = Math.max(nextPV, 0);
	    privateValues.add(new Price(nextPV));
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
	 * @return list of all private values
	 */
	public ArrayList<Price> getPrivateValues() {
		return privateValues;
	}
}
