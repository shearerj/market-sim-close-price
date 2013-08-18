package utils;

import java.util.Random;

// FIXME Make methods static
public class Rands extends Random {

	private static final long serialVersionUID = -3289945139751584689L;

	public Rands() {
		super();
	}

	public Rands(long seed) {
		super(seed);
	}
	
	public final synchronized double nextGaussian(double mean, double variance) {
		return mean + nextGaussian() * Math.sqrt(variance);
	}
	
	public final double nextExponential(double rate) {
		return -Math.log(nextDouble()) / rate;
	}

}
