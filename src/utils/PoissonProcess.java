package utils;

import java.util.Iterator;

/**
 * Poisson Process
 */
public class PoissonProcess implements Iterator<Double> {

	protected final double rate;
	protected final RandPlus rand;
	protected double lastValue;

	public PoissonProcess(double initialValue, double rate, RandPlus rand) {
		this.lastValue = initialValue;
		this.rate = rate;
		this.rand = rand;
	}

	@Override
	public boolean hasNext() {
		return true;
	}

	@Override
	public Double next() {
		double interval = rand.nextExponential(rate);
		lastValue += interval;
		return lastValue;
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException("Can't remove from a process");
	}

}
