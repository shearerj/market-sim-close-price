package edu.umich.srg.distributions;

import static com.google.common.base.Preconditions.checkArgument;

import edu.umich.srg.distributions.Distribution.LongDistribution;

import java.io.Serializable;
import java.util.Random;

/**
 * Generate samples from a Binomail. Method is taken from:
 * 
 * "Computer Methods for Sampling from Gamma, Beta, Poisson and Binomial Distributions" - J. H.
 * Ahrens and U. Dieter (1973)
 * 
 * It replies on the approximate beta sampling, so is not perfect.
 */
public abstract class Binomial implements LongDistribution, Serializable {

  public static Binomial with(long numDraws, double successProb) {
    checkArgument(numDraws >= 0, "Can't sample Binomial with less than one trial");
    checkArgument(0 <= successProb && successProb <= 1, "p must be a probility");
    return binomialSwitch(numDraws, successProb);
  }

  private static Binomial binomialSwitch(long numDraws, double successProb) {
    if (successProb == 0 || numDraws == 0)
      return new ConstantBinomial(0);
    else if (successProb == 1)
      return new ConstantBinomial(numDraws);
    else if (numDraws < 37)
      return new BernoulliBinomial(numDraws, successProb);
    else if (numDraws % 2 == 0)
      return new EvenBinomial(numDraws, successProb);
    else
      return new BetaBinomial(numDraws, successProb);
  }

  /*
   * TODO The fact that these classes are public is an artifact of java 8 and should be fixed when
   * java 9 allows these to be private. Currently they just muddle the API.
   */

  private static class ConstantBinomial extends Binomial {

    private final long constant;

    private ConstantBinomial(long constant) {
      this.constant = constant;
    }

    @Override
    public long sample(Random rand) {
      return constant;
    }

    private static final long serialVersionUID = 1;

  }

  /**
   * When n is small, a Binomial is best sampled by just generating n Bernoulli trials and summing
   * the successes.
   * 
   * @author erik
   *
   */
  private static class BernoulliBinomial extends Binomial {

    private final long n;
    private final double p;

    private BernoulliBinomial(long n, double p) {
      this.n = n;
      this.p = p;
    }

    @Override
    public long sample(Random rand) {
      long binomial = 0;
      for (int i = 0; i < n; ++i)
        if (rand.nextDouble() < p)
          binomial++;
      return binomial;
    }

    private static final long serialVersionUID = 1;

  }

  /**
   * If n is large, then we can use the Beta method for generating binomial samples. The idea of the
   * method is simple. The jth order statistic of n Bernoulli trials is Beta(j, n - j + 1). Thus, if
   * j = (n + 1) / 2 and Y ~ Beta(j, j), then if Y < p at least j of the trials have to be larger
   * than p (at least j successes). The conditional distribution for the trials that were less than
   * Y but greater than p is also Binomial, and so we get a recursive definition that takes O(log
   * n).
   */
  private static class BetaBinomial extends Binomial {

    private final long j;
    private final double p;
    private final Beta orderDistribution;

    private BetaBinomial(long n, double p) {
      this.p = p;
      this.j = (n + 1) / 2;
      this.orderDistribution = Beta.with(j, j);
    }

    @Override
    public long sample(Random rand) {
      double order = orderDistribution.sample(rand);
      // double order = DoubleStream.generate(rand::nextDouble).limit(2 * j - 1).sorted().skip(j -
      // 1)
      // .findFirst().getAsDouble();
      if (order < p)
        return j + Binomial.binomialSwitch(j - 1, (p - order) / (1 - order)).sample(rand);
      else
        return Binomial.binomialSwitch(j - 1, p / order).sample(rand);
    }

    private static final long serialVersionUID = 1;

  }

  /**
   * The Beta method works best when n is odd, thus if it's even, we do one independent Bernoulli
   * trial, and add its result to the recursive definition.
   */
  private static class EvenBinomial extends Binomial {

    private final double p;
    private final Binomial oddBernoulli;

    private EvenBinomial(long n, double p) {
      this.p = p;
      this.oddBernoulli = Binomial.binomialSwitch(n - 1, p);
    }

    @Override
    public long sample(Random rand) {
      return (rand.nextDouble() < p ? 1 : 0) + oddBernoulli.sample(rand);
    }

    private static final long serialVersionUID = 1;

  }

  private static final long serialVersionUID = 1;


}
