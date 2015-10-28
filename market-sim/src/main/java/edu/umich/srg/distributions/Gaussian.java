package edu.umich.srg.distributions;

import edu.umich.srg.distributions.Distribution.DoubleDistribution;

import java.util.Random;

public class Gaussian implements DoubleDistribution {

  private final double mean, standardDeviation;

  private Gaussian(double mean, double standardDeviation) {
    this.mean = mean;
    this.standardDeviation = standardDeviation;
  }

  public static Gaussian withMeanVariance(double mean, double variance) {
    return new Gaussian(mean, Math.sqrt(variance));
  }

  public static Gaussian withMeanStandardDeviation(double mean, double standardDeviation) {
    return new Gaussian(mean, standardDeviation);
  }

  @Override
  public double sample(Random rand) {
    return rand.nextGaussian() * standardDeviation + mean;
  };

}
