package edu.umich.srg.marketsim.fundamental;

import com.google.common.primitives.Ints;

import edu.umich.srg.collect.SparseList;
import edu.umich.srg.collect.SparseList.Entry;
import edu.umich.srg.distributions.Binomial;
import edu.umich.srg.distributions.Gaussian;
import edu.umich.srg.distributions.Hypergeometric;
import edu.umich.srg.marketsim.Price;
import edu.umich.srg.marketsim.TimeStamp;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * FIXME
 *
 * if F(t) is the fundamental at time t s^2 is the shock variance m is the fundamental mean and kc
 * is 1 - kappa then
 *
 * F(t+1) ~ N(m*(1-kc) + F(t)*kc, s^2)
 *
 * which implies that
 *
 * F(t+d) ~ N(m*(1-kc^d) + F(t)*kc^d, s^2 * (1 - kc^(2d)) / (1 - kc^2)) if kc in [0, 1)
 *
 * or
 *
 * F(t+d) ~ N(F(t), d * s^2) if kc = 1.
 * 
 * In other words, if kappa > 0, so (1 - kappa) < 1, F(t+d) has expected value (1 - kappa)^d F(t) +
 * (1 - (1 - kappa)^d) m. It is distributed as a Gaussian, with variance: s^2 * (1 - kc^(2d)) / (1 -
 * kc^2).
 * 
 * If kappa == 0, so (1 - kappa) == 1, F(t+d) has expected value F(t), is still a Gaussian, and has
 * variance d s^2 (Bienayme formula, variance of sum of uncorrelated variables is sum of the
 * variances).
 * 
 * If shockProb < 1, then mean reversion occurs in exactly those time steps in which a jump occurs.
 * This means that if we advance 5 time steps and have jumps in 2 and 4, the pdf of future
 * fundamental value is the same as when advancing 2 time steps and jumping in both. That is, we
 * simply replace "d" with "numJumps" in the pdfs above, where "numJumps" is a binomial random
 * variable, with n := d and p := shockVar.
 *
 * @author ewah
 */

// FIXME have IID Gaussians only track if there was a jump. not how many to make sampling more
// efficient

public abstract class GaussianMeanReverting implements Fundamental, Serializable {

  public static GaussianMeanReverting create(Random rand, double mean, double meanReversion,
      double shockVar, double shockProb) {
    if (shockProb == 0)
      return new ConstantFundamental(Price.of(mean));
    else if (shockProb == 1 && meanReversion == 0)
      return new JumpEvery(mean, new RandomWalk(rand, shockVar));
    else if (shockProb == 1 && meanReversion == 1)
      return new JumpEvery(mean, new IIDGaussian(rand, mean, shockVar));
    else if (shockProb == 1)
      return new JumpEvery(mean, new MeanReverting(rand, mean, shockVar, meanReversion));
    else if (meanReversion == 0)
      return new JumpRandomlyCount(mean, new RandomWalk(rand, shockVar), shockProb, rand);
    else if (meanReversion == 1)
      return new JumpRandomlyCount(mean, new IIDGaussian(rand, mean, shockVar), shockProb, rand);
    else
      return new JumpRandomlyCount(mean, new MeanReverting(rand, mean, shockVar, meanReversion),
          shockProb, rand);
  }

  private static class ConstantFundamental extends GaussianMeanReverting {

    private final Price constant;

    private ConstantFundamental(Price constant) {
      this.constant = constant;
    }

    @Override
    public Price getValueAt(TimeStamp time) {
      return constant;
    }

    @Override
    public FundamentalInfo getInfo() {
      return new FundamentalInfo() {

        @Override
        public Iterable<? extends Entry<? extends Number>> getFundamentalValues() {
          return Collections.singleton(SparseList.immutableEntry(0, constant));
        }

      };
    }

    private static final long serialVersionUID = 1;

  }

  private static abstract class AbstractGaussianMeanReverting<F extends FundamentalObservation>
      extends GaussianMeanReverting {

    private final List<F> fundamental;

    private AbstractGaussianMeanReverting(F initial) {
      this.fundamental = new ArrayList<>(Collections.singleton(initial));
    }

    @Override
    public Price getValueAt(TimeStamp timeStamp) {
      long time = timeStamp.get();
      F last = fundamental.get(fundamental.size() - 1);
      double newPrice;
      if (time == last.time) { // Get the last update
        newPrice = last.price;

      } else if (time > last.time) { // Get a future value
        F observation = observeFuture(last, time);
        newPrice = observation.price;
        fundamental.add(observation);

      } else { // Get a value before the most current value
        int index = Collections.binarySearch(fundamental, new FundamentalObservation(time, 0));
        if (index >= 0) { // We've already calculated the value at time t
          newPrice = fundamental.get(index).price;

        } else { // Compute a new intermediate value
          index = -index - 1; // Insertion point
          F before = fundamental.get(index - 1), after = fundamental.get(index);
          F observation = observeIntermediate(before, after, time);
          newPrice = observation.price;
          fundamental.add(index, observation);
        }
      }
      return Price.of(newPrice).nonnegative();
    }

    @Override
    public FundamentalInfo getInfo() {
      return new FundamentalInfo() {

        @Override
        public Iterable<? extends Entry<? extends Number>> getFundamentalValues() {
          return Collections.unmodifiableCollection(fundamental);
        }

      };
    }

    protected abstract F observeFuture(F last, long time);

    protected abstract F observeIntermediate(F before, F after, long time);

    private static final long serialVersionUID = 1;

  }

  private static class JumpEvery extends AbstractGaussianMeanReverting<FundamentalObservation> {

    private final Sampler sampler;

    private JumpEvery(double mean, Sampler sampler) {
      super(new FundamentalObservation(0, mean));
      this.sampler = sampler;
    }

    @Override
    protected FundamentalObservation observeFuture(FundamentalObservation last, long time) {
      double price = sampler.getFutureValue(last.price, time - last.time);
      return new FundamentalObservation(time, price);
    }

    @Override
    protected FundamentalObservation observeIntermediate(FundamentalObservation before,
        FundamentalObservation after, long time) {
      double newPrice = sampler.getIntermediateValue(before.price, time - before.time, after.price,
          after.time - time);
      return new FundamentalObservation(time, newPrice);
    }

    private static final long serialVersionUID = 1;

  }

  private static class JumpRandomlyCount
      extends AbstractGaussianMeanReverting<JumpFundamentalObservation> {

    private final Random rand;
    private final Sampler sampler;
    private final double shockProb;

    private JumpRandomlyCount(double mean, Sampler sampler, double shockProb, Random rand) {
      super(new JumpFundamentalObservation(0, mean, 0));
      this.shockProb = shockProb;
      this.sampler = sampler;
      this.rand = rand;
    }

    @Override
    protected JumpFundamentalObservation observeFuture(JumpFundamentalObservation last, long time) {
      long jumps = Binomial.with(time - last.time, shockProb).sample(rand);
      double price = jumps == 0 ? last.price : sampler.getFutureValue(last.price, jumps);
      return new JumpFundamentalObservation(time, price, jumps);
    }

    @Override
    protected JumpFundamentalObservation observeIntermediate(JumpFundamentalObservation before,
        JumpFundamentalObservation after, long time) {
      int jumpsBefore = Hypergeometric.with(Ints.checkedCast(after.time - before.time),
          Ints.checkedCast(after.jumps), Ints.checkedCast(time - before.time)).sample(rand);
      after.jumps -= jumpsBefore;

      double newPrice;
      if (jumpsBefore == 0)
        newPrice = before.price;
      else if (after.jumps == 0)
        newPrice = after.price;
      else
        newPrice =
            sampler.getIntermediateValue(before.price, jumpsBefore, after.price, after.jumps);

      return new JumpFundamentalObservation(time, newPrice, jumpsBefore);
    }

    private static final long serialVersionUID = 1;

  }

  private interface Sampler {

    double getFutureValue(double lastPrice, long jumps);

    double getIntermediateValue(double priceBefore, long jumpsBefore, double priceAfter,
        long jumpsAfter);

  }

  private static class RandomWalk implements Sampler, Serializable {

    private final Random rand;
    private final double shockVar;

    private RandomWalk(Random rand, double shockVar) {
      this.rand = rand;
      this.shockVar = shockVar;
    }

    @Override
    public double getFutureValue(double lastPrice, long jumps) {
      return Gaussian.withMeanVariance(lastPrice, shockVar * jumps).sample(rand);
    }

    @Override
    public double getIntermediateValue(double priceBefore, long jumpsBefore, double priceAfter,
        long jumpsAfter) {
      return Gaussian
          .withMeanVariance(
              (priceBefore * jumpsAfter + priceAfter * jumpsBefore) / (jumpsBefore + jumpsAfter),
              jumpsBefore * jumpsAfter / (double) (jumpsBefore + jumpsAfter) * shockVar)
          .sample(rand);
    }

    private static final long serialVersionUID = 1;

  }

  private static class IIDGaussian implements Sampler, Serializable {

    private final Random rand;
    private final Gaussian dist;

    private IIDGaussian(Random rand, double mean, double shockVar) {
      this.dist = Gaussian.withMeanVariance(mean, shockVar);
      this.rand = rand;
    }

    @Override
    public double getFutureValue(double lastPrice, long jumps) {
      return dist.sample(rand);
    }

    @Override
    public double getIntermediateValue(double priceBefore, long jumpsBefore, double priceAfter,
        long jumpsAfter) {
      return dist.sample(rand);
    }

    private static final long serialVersionUID = 1;

  }

  private static class MeanReverting implements Sampler, Serializable {

    private final Random rand;
    private final double shockVar, mean, kappac;

    private MeanReverting(Random rand, double mean, double shockVar, double meanReversion) {
      this.mean = mean;
      this.shockVar = shockVar;
      this.kappac = 1 - meanReversion;
      this.rand = rand;
    }

    @Override
    public double getFutureValue(double lastPrice, long jumps) {
      double kappacToPower = Math.pow(kappac, jumps),
          stepMean = (1 - kappacToPower) * mean + kappacToPower * lastPrice,
          stepVar = (1 - kappacToPower * kappacToPower) / (1 - kappac * kappac);
      return Gaussian.withMeanVariance(stepMean, shockVar * stepVar).sample(rand);
    }

    @Override
    public double getIntermediateValue(double priceBefore, long jumpsBefore, double priceAfter,
        long jumpsAfter) {
      double kappacPowerBefore = Math.pow(kappac, jumpsBefore),
          kappacPowerAfter = Math.pow(kappac, jumpsAfter),
          stepMean = ((kappacPowerBefore - 1) * (kappacPowerAfter - 1)
              * (kappacPowerBefore * kappacPowerAfter - 1) * mean
              + kappacPowerBefore * (kappacPowerAfter * kappacPowerAfter - 1) * priceBefore
              + kappacPowerAfter * (kappacPowerBefore * kappacPowerBefore - 1) * priceAfter)
              / (kappacPowerBefore * kappacPowerBefore * kappacPowerAfter * kappacPowerAfter - 1),
          stepVariance = (kappacPowerBefore * kappacPowerBefore - 1)
              * (kappacPowerAfter * kappacPowerAfter - 1)
              / ((kappac * kappac - 1)
                  * (kappacPowerBefore * kappacPowerBefore * kappacPowerAfter * kappacPowerAfter
                      - 1));
      return Gaussian.withMeanVariance(stepMean, stepVariance * shockVar).sample(rand);
    }

    private static final long serialVersionUID = 1;

  }

  private static class FundamentalObservation
      implements Comparable<FundamentalObservation>, SparseList.Entry<Double>, Serializable {

    final long time;
    final double price;

    private FundamentalObservation(long time, double price) {
      this.time = time;
      this.price = price;
    }

    @Override
    public int compareTo(FundamentalObservation other) {
      return Long.compare(time, other.time);
    }

    @Override
    public long getIndex() {
      return time;
    }

    @Override
    public Double getElement() {
      return price;
    }

    private static final long serialVersionUID = 1;

  }

  private static class JumpFundamentalObservation extends FundamentalObservation {

    long jumps;

    private JumpFundamentalObservation(long time, double price, long jumps) {
      super(time, price);
      this.jumps = jumps;
    }

    private static final long serialVersionUID = 1;

  }

  private static final long serialVersionUID = 1;

}
