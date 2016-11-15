package edu.umich.srg.marketsim.fundamental;

import com.google.common.primitives.Ints;

import edu.umich.srg.collect.Sparse;
import edu.umich.srg.distributions.Binomial;
import edu.umich.srg.distributions.Gaussian;
import edu.umich.srg.distributions.Hypergeometric;
import edu.umich.srg.marketsim.Price;
import edu.umich.srg.marketsim.TimeStamp;
import edu.umich.srg.util.PositionalSeed;

import java.io.Serializable;
import java.util.AbstractMap;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Random;
import java.util.TreeMap;

/**
 * This class models a Guassian mean reverting process that doesn't necessarily make a jump at every
 * time step. It has two important implementation features. First, fundamental values are computed
 * lazily and on demand, so that even if you ask for the fundamental value at time 10000000, it
 * should return reasonable fast, without having to generate all 1000000 values. It is also randomly
 * stable, that is, two fundamentals with the same random generator will produce the same value at
 * every point independent of query order. This costs a logarithmic factor to do, but the stability
 * is generally worth it, and the log factor is tiny in terms of actual time costs. More detail on
 * the math for sampling from the fundamental is in the docs folder.
 */

/*
 * FIXME have IID Gaussians only track "if there was a jump", not "how many jumps" to make sampling
 * more efficient
 */

/*
 * There are two major design decisions that will help understand how this works. The first is that
 * to avoid constantly checking for special cases, these are broken into subclasses, and since the
 * cases are two dimensional (one for how to process the jumps, and one for when they occur) there
 * is a class nesting. This is where the Sampler interface comes in.
 * 
 * The second is that to keep the draws consistent independent of order, we do some extra sampling.
 * The extra sampling comes in the form of binary search. With existing points, if the time 15 is
 * queried, this will first sample points at 1, 2, 4, 8, 16, 12, 14, 15. This may seems like a lot,
 * but it's generally log in the distance. The second piece necessary is to condition the sameple on
 * the time when the occur. That way, the random generator at time t is always the same, and always
 * conditioned on the same draws, thus given consistent draws independent of order. To facilitate
 * the final part, the positional seed class is used which generates more uniform seeds for
 * sequential values, making sure that this is close uniform pseudo random.
 * 
 * Calculating the equations for the rmsd were non trivial and were done using mathematica's
 * symbolic toolbox. See the resources directory for the notebooks that calculated them.
 */

public abstract class GaussianMeanReverting implements Fundamental, Serializable {

  /** Create a standard gaussian mean reverting fundamental stochastic process. */
  public static GaussianMeanReverting create(Random rand, double mean, double meanReversion,
      double shockVar, double shockProb) {
    if (shockProb == 0) {
      return ConstantFundamental.create(Price.of(mean));
    } else {
      Sampler sampler;
      if (meanReversion == 0) {
        sampler = new RandomWalk(rand, shockVar);
      } else if (meanReversion == 1) {
        sampler = new IidGaussian(rand, mean, shockVar);
      } else {
        sampler = new MeanReverting(rand, mean, shockVar, meanReversion);
      }
      if (shockProb == 1) {
        return new JumpEvery(mean, sampler);
      } else {
        return new JumpRandomlyCount(mean, sampler, shockProb, rand);
      }
    }
  }

  private abstract static class AbstractGaussianMeanReverting<F extends FundamentalObservation>
      extends GaussianMeanReverting {

    private final NavigableMap<Long, F> fundamental;

    private AbstractGaussianMeanReverting(F initial) {
      // Put in zero and one, so doubling works
      this.fundamental = new TreeMap<>();
      fundamental.put(0L, initial);
    }

    @Override
    public Price getValueAt(TimeStamp timeStamp) {
      long time = timeStamp.get();

      // First make sure that time is in the map by binary searching up
      Entry<Long, F> last = fundamental.lastEntry();
      while (time > last.getKey()) {
        long nextTime = last.getKey() == 0 ? 1 : last.getKey() * 2;
        F observation = observeFuture(last, nextTime);
        fundamental.put(nextTime, observation);
        last = new AbstractMap.SimpleImmutableEntry<>(nextTime, observation);
      }

      Entry<Long, F> before = fundamental.floorEntry(time);
      Entry<Long, F> after = fundamental.ceilingEntry(time);

      while (before.getKey() != time && after.getKey() != time) {
        long midTime = (before.getKey() + after.getKey()) / 2;
        F observation = observeIntermediate(before, after, midTime);
        fundamental.put(midTime, observation);
        Entry<Long, F> entry = new AbstractMap.SimpleImmutableEntry<>(midTime, observation);
        if (midTime > time) {
          after = entry;
        } else {
          before = entry;
        }
      }

      if (before.getKey() == time) {
        return Price.of(before.getValue().price).nonnegative();
      } else {
        return Price.of(after.getValue().price).nonnegative();
      }
    }

    @Override
    public Iterable<Sparse.Entry<Number>> getFundamentalValues(TimeStamp finalTime) {
      // TODO Replace filter with takeWhile
      long longTime = finalTime.get();
      return () -> fundamental.entrySet().stream().filter(e -> e.getKey() <= longTime)
          .map(e -> Sparse.<Number>immutableEntry(e.getKey(), Price.of(e.getValue().doubleValue())))
          .iterator();
    }

    protected abstract F observeFuture(Entry<Long, F> last, long time);

    protected abstract F observeIntermediate(Entry<Long, F> before, Entry<Long, F> after,
        long time);

    private static final long serialVersionUID = 1;

  }

  private static class JumpEvery extends AbstractGaussianMeanReverting<FundamentalObservation> {

    private final Sampler sampler;

    private JumpEvery(double mean, Sampler sampler) {
      super(new FundamentalObservation(mean));
      this.sampler = sampler;
    }

    @Override
    protected FundamentalObservation observeFuture(Entry<Long, FundamentalObservation> last,
        long time) {
      double price = sampler.getFutureValue(time, last.getValue().price, time - last.getKey());
      return new FundamentalObservation(price);
    }

    @Override
    protected FundamentalObservation observeIntermediate(Entry<Long, FundamentalObservation> before,
        Entry<Long, FundamentalObservation> after, long time) {
      double newPrice = sampler.getIntermediateValue(time, before.getValue().price,
          time - before.getKey(), after.getValue().price, after.getKey() - time);
      return new FundamentalObservation(newPrice);
    }

    private static final long serialVersionUID = 1;

  }

  private static class JumpRandomlyCount
      extends AbstractGaussianMeanReverting<JumpFundamentalObservation> {

    private final PositionalSeed seed;
    private final Random rand;
    private final Sampler sampler;
    private final double shockProb;

    private JumpRandomlyCount(double mean, Sampler sampler, double shockProb, Random rand) {
      super(new JumpFundamentalObservation(mean, 0));
      this.shockProb = shockProb;
      this.sampler = sampler;
      this.seed = PositionalSeed.with(rand.nextLong());
      this.rand = rand;
    }

    @Override
    protected JumpFundamentalObservation observeFuture(Entry<Long, JumpFundamentalObservation> last,
        long time) {
      rand.setSeed(seed.getSeed(time));
      long jumps = Binomial.with(time - last.getKey(), shockProb).sample(rand);
      double price = jumps == 0 ? last.getValue().price
          : sampler.getFutureValue(time, last.getValue().price, jumps);
      return new JumpFundamentalObservation(price, jumps);
    }

    @Override
    protected JumpFundamentalObservation observeIntermediate(
        Entry<Long, JumpFundamentalObservation> before,
        Entry<Long, JumpFundamentalObservation> after, long time) {
      rand.setSeed(seed.getSeed(time));
      int jumpsBefore = Hypergeometric.with(Ints.checkedCast(after.getKey() - before.getKey()),
          Ints.checkedCast(after.getValue().jumpsBefore), Ints.checkedCast(time - before.getKey()))
          .sample(rand);
      after.getValue().jumpsBefore -= jumpsBefore;

      double newPrice;
      if (jumpsBefore == 0) {
        newPrice = before.getValue().price;
      } else if (after.getValue().jumpsBefore == 0) {
        newPrice = after.getValue().price;
      } else {
        newPrice = sampler.getIntermediateValue(time, before.getValue().price, jumpsBefore,
            after.getValue().price, after.getValue().jumpsBefore);
      }

      return new JumpFundamentalObservation(newPrice, jumpsBefore);
    }

    private static final long serialVersionUID = 1;

  }

  private interface Sampler {

    double getFutureValue(long time, double lastPrice, long jumps);

    double getIntermediateValue(long time, double priceBefore, long jumpsBefore, double priceAfter,
        long jumpsAfter);

  }

  private static class RandomWalk implements Sampler, Serializable {

    private final PositionalSeed seed;
    private final Random rand;
    private final double shockVar;

    private RandomWalk(Random rand, double shockVar) {
      this.seed = PositionalSeed.with(rand.nextLong());
      this.shockVar = shockVar;
      this.rand = rand;
    }

    @Override
    public double getFutureValue(long time, double lastPrice, long jumps) {
      rand.setSeed(seed.getSeed(time));
      return Gaussian.withMeanVariance(lastPrice, shockVar * jumps).sample(rand);
    }

    @Override
    public double getIntermediateValue(long time, double priceBefore, long jumpsBefore,
        double priceAfter, long jumpsAfter) {
      rand.setSeed(seed.getSeed(time));
      return Gaussian
          .withMeanVariance(
              (priceBefore * jumpsAfter + priceAfter * jumpsBefore) / (jumpsBefore + jumpsAfter),
              jumpsBefore * jumpsAfter / (double) (jumpsBefore + jumpsAfter) * shockVar)
          .sample(rand);
    }

    private static final long serialVersionUID = 1;

  }

  private static class IidGaussian implements Sampler, Serializable {

    private final PositionalSeed seed;
    private final Random rand;
    private final Gaussian dist;

    private IidGaussian(Random rand, double mean, double shockVar) {
      this.seed = PositionalSeed.with(rand.nextLong());
      this.dist = Gaussian.withMeanVariance(mean, shockVar);
      this.rand = rand;
    }

    @Override
    public double getFutureValue(long time, double lastPrice, long jumps) {
      rand.setSeed(seed.getSeed(time));
      return dist.sample(rand);
    }

    @Override
    public double getIntermediateValue(long time, double priceBefore, long jumpsBefore,
        double priceAfter, long jumpsAfter) {
      rand.setSeed(seed.getSeed(time));
      return dist.sample(rand);
    }

    private static final long serialVersionUID = 1;

  }

  private static class MeanReverting implements Sampler, Serializable {

    private final PositionalSeed seed;
    private final Random rand;
    private final double shockVar;
    private final double mean;
    private final double kappac;

    private MeanReverting(Random rand, double mean, double shockVar, double meanReversion) {
      this.seed = PositionalSeed.with(rand.nextLong());
      this.mean = mean;
      this.shockVar = shockVar;
      this.kappac = 1 - meanReversion;
      this.rand = rand;
    }

    @Override
    public double getFutureValue(long time, double lastPrice, long jumps) {
      rand.setSeed(seed.getSeed(time));
      double kappacToPower = Math.pow(kappac, jumps);
      double stepMean = (1 - kappacToPower) * mean + kappacToPower * lastPrice;
      double stepVar = (1 - kappacToPower * kappacToPower) / (1 - kappac * kappac);
      return Gaussian.withMeanVariance(stepMean, shockVar * stepVar).sample(rand);
    }

    @Override
    public double getIntermediateValue(long time, double priceBefore, long jumpsBefore,
        double priceAfter, long jumpsAfter) {
      rand.setSeed(seed.getSeed(time));
      double kappacPowerBefore = Math.pow(kappac, jumpsBefore);
      double kappacPowerAfter = Math.pow(kappac, jumpsAfter);
      double stepMean = ((kappacPowerBefore - 1) * (kappacPowerAfter - 1)
          * (kappacPowerBefore * kappacPowerAfter - 1) * mean
          + kappacPowerBefore * (kappacPowerAfter * kappacPowerAfter - 1) * priceBefore
          + kappacPowerAfter * (kappacPowerBefore * kappacPowerBefore - 1) * priceAfter)
          / (kappacPowerBefore * kappacPowerBefore * kappacPowerAfter * kappacPowerAfter - 1);
      double stepVariance = (kappacPowerBefore * kappacPowerBefore - 1)
          * (kappacPowerAfter * kappacPowerAfter - 1) / ((kappac * kappac - 1)
              * (kappacPowerBefore * kappacPowerBefore * kappacPowerAfter * kappacPowerAfter - 1));
      return Gaussian.withMeanVariance(stepMean, stepVariance * shockVar).sample(rand);
    }

    private static final long serialVersionUID = 1;

  }

  private static class FundamentalObservation extends Number implements Serializable {

    final double price;

    private FundamentalObservation(double price) {
      this.price = price;
    }

    @Override
    public double doubleValue() {
      return price;
    }

    @Override
    public float floatValue() {
      return (float) price;
    }

    @Override
    public int intValue() {
      return (int) price;
    }

    @Override
    public long longValue() {
      return (long) price;
    }

    @Override
    public String toString() {
      return Double.toString(price);
    }

    private static final long serialVersionUID = 1;


  }

  private static class JumpFundamentalObservation extends FundamentalObservation {

    long jumpsBefore;

    private JumpFundamentalObservation(double price, long jumps) {
      super(price);
      this.jumpsBefore = jumps;
    }

    @Override
    public String toString() {
      return "(" + price + ", " + jumpsBefore + ")";
    }

    private static final long serialVersionUID = 1;

  }

  private static final long serialVersionUID = 1;

}
