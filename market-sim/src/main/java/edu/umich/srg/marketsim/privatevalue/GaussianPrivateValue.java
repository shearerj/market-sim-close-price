package edu.umich.srg.marketsim.privatevalue;

import java.util.Random;

import edu.umich.srg.distributions.Gaussian;

public class GaussianPrivateValue extends AbstractListPrivateValue {

	private GaussianPrivateValue(Random rand, int maxPosition, double variance) {
		super(Gaussian.withMeanVariance(0, variance), maxPosition, rand);
	}
	
	public static GaussianPrivateValue generate(Random rand, int maxPosition, double variance) {
		return new GaussianPrivateValue(rand, maxPosition, variance);
	}

}
