package edu.umich.srg.distributions;

import edu.umich.srg.distributions.Distribution.DoubleDistribution;
import edu.umich.srg.distributions.Distribution.IntDistribution;
import edu.umich.srg.distributions.Distribution.LongDistribution;

import java.util.Random;
import java.util.function.Function;

public final class Distributions {

  public static Distribution<Integer> asDistribution(IntDistribution dist) {
    return new Distribution<Integer>() {
      @Override
      public Integer sample(Random rand) {
        return dist.sample(rand);
      }
    };
  }

  public static Distribution<Long> asDistribution(LongDistribution dist) {
    return new Distribution<Long>() {
      @Override
      public Long sample(Random rand) {
        return dist.sample(rand);
      }
    };
  }

  public static Distribution<Double> asDistribution(DoubleDistribution dist) {
    return new Distribution<Double>() {
      @Override
      public Double sample(Random rand) {
        return dist.sample(rand);
      }
    };
  }

  public static <T, R> Distribution<R> map(Distribution<T> initial, Function<T, R> func) {
    return new Distribution<R>() {
      @Override
      public R sample(Random rand) {
        return func.apply(initial.sample(rand));
      }
    };
  }

  // Remove constructor
  private Distributions() {};

}
