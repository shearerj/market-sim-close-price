package edu.umich.srg.distributions;

import java.util.Random;

/**
 * Interface the represents a distribution that can be sampled from. The source of randomness is
 * independent of the distribution. This distribution just contains the knowledge necessary to
 * produce a sample.
 */
public interface Distribution<T> {

  T sample(Random rand);

  /** A distribution that returns integers */
  public interface IntDistribution {
    int sample(Random rand);
  }

  /** A distribution that returns longs */
  public interface LongDistribution {
    long sample(Random rand);
  }

  /** A distribution that returns doubles */
  public interface DoubleDistribution {
    double sample(Random rand);
  }

}
