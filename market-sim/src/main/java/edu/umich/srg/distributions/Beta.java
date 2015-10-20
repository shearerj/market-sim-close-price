package edu.umich.srg.distributions;

import java.util.Random;

import edu.umich.srg.distributions.Distribution.DoubleDistribution;

public class Beta implements DoubleDistribution {

	private final Exponential alpha, beta;
	
	private Beta(double alpha, double beta) {
		this.alpha = Exponential.withRate(alpha);
		this.beta = Exponential.withRate(beta);
	}
	
	public static Beta with(double alpha, double beta) {
		return new Beta(alpha, beta);
	}
	
	@Override
	public double sample(Random rand) {
		double alphaSample = alpha.sample(rand);
		double betaSample = beta.sample(rand);
		return alphaSample / (alphaSample + betaSample);
	}

}
