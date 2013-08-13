package generator;

import utils.RandPlus;

/**
 * Poisson Process
 */
public class PoissonProcess extends Generator<Double> {

	private static final long serialVersionUID = 1600945421354708623L;
	
	protected final double rate;
	protected final RandPlus rand;
	protected double lastValue;

	public PoissonProcess(double initialValue, double rate, RandPlus rand) {
		this.lastValue = initialValue;
		this.rate = rate;
		this.rand = rand;
	}

	@Override
	public Double next() {
		double interval = rand.nextExponential(rate);
		lastValue += interval;
		return lastValue;
	}

}
