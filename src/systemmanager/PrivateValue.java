package systemmanager;

import event.TimeStamp;
import market.Price;

import java.util.Random;
import java.util.ArrayList;

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
	private Random rand;
	
	/**
	 * Constructor with defaults
	 */
	public PrivateValue() {
		kappa = 0.1;
		meanPV = 50000;
		shockVar = 2;
		rand = new Random();
		privateValues = new ArrayList<Price>();
	}
	
	public PrivateValue(double k, int meanValue, double var) {
		kappa = k;
		meanPV = meanValue;
		shockVar = var;
		privateValues = new ArrayList<Price>();
		
		// stochastic initial conditions for random process
		privateValues.add(new Price((int) getNormalRV(meanPV, shockVar)));
	}
	
	public int next() {
		int nextPV = (int) ((meanPV * kappa) + ((1 - kappa) * 
						privateValues.get(privateValues.size()-1).getPrice()) +
	                    getNormalRV(0, shockVar));
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
}
