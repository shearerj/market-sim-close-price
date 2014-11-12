package utils;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Class with extra random methods
 * 
 * @author erik
 * 
 */
public final class Rand extends Random {
	
	private Rand() {
		super();
	}

	private Rand(long seed) {
		super(seed);
	}
	
	public static Rand create() {
		return new Rand();
	}
	
	public static Rand from(long seed) {
		return new Rand(seed);
	}
	
	public static Rand from(Random random) {
		return new Rand(random.nextLong());
	}

	public synchronized double nextGaussian(double mean, double variance) {
		return mean + nextGaussian() * Math.sqrt(variance);
	}
	
	public double nextExponential(double rate) {
		if (rate == 0)
			return Double.POSITIVE_INFINITY;
		return -Math.log(nextDouble()) / rate;
	}

	public double nextUniform(double a, double b) {
		return nextDouble()*(b-a) + a;
	}
	
	public <T> T nextElement(T... elements) {
		return nextElement(Arrays.asList(elements));
	}
	
	public <T> T nextElement(List<T> elements) {
		return elements.get(nextInt(elements.size()));
	}

	private static final long serialVersionUID = -2509122458513641197L;
}
