package edu.umich.srg.distributions;

import edu.umich.srg.distributions.Distribution.LongDistribution;

import java.util.Random;

/**
 * FIXME Add documentation range = [0, inf)
 * 
 * @author erik
 *
 */
public class Geometric implements LongDistribution {

  private final double weight;

  private Geometric(double successProb) {
    this.weight = Math.log1p(-successProb);
  }

  public static Geometric withSuccessProbability(double successProb) {
    return new Geometric(successProb);
  }

  @Override
  public long sample(Random rand) {
    return (long) (Math.log1p(-rand.nextDouble()) / weight);
  }

}
