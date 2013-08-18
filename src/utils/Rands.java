package utils;

import java.util.Random;

public abstract class Rands {

	public static synchronized double nextGaussian(Random rand, double mean, double variance) {
		return mean + rand.nextGaussian() * Math.sqrt(variance);
	}
	
	public static double nextExponential(Random rand, double rate) {
		return -Math.log(rand.nextDouble()) / rate;
	}

}
