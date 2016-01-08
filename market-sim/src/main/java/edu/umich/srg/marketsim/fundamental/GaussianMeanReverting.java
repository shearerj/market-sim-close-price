package edu.umich.srg.marketsim.fundamental;

import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;
import com.google.common.primitives.Ints;

import edu.umich.srg.collect.SparseList;
import edu.umich.srg.distributions.Binomial;
import edu.umich.srg.distributions.Gaussian;
import edu.umich.srg.distributions.Hypergeometric;
import edu.umich.srg.marketsim.Price;
import edu.umich.srg.marketsim.TimeStamp;
import edu.umich.srg.util.PositionalSeed;
import edu.umich.srg.util.SummStats;

import java.io.Serializable;
import java.util.AbstractMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Random;
import java.util.TreeMap;

/**
 * This class models a Guassian mean reverting process that doesn't necessarily make a jump at every
 * time step. It is implemented such that independent of the TimeStamp query order, values are
 * generated lazily on demand, but two processes given the random seed will produce the exact same
 * values. The process is defined more precisly below.
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
 * Due to the decision to make the fundamental consistent, the general growth and query time is log
 * instead of constant.
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
 */

public abstract class GaussianMeanReverting implements Fundamental, Serializable {

  public static GaussianMeanReverting create(Random rand, double mean, double meanReversion,
      double shockVar, double shockProb) {
    if (shockProb == 0)
      return ConstantFundamental.create(Price.of(mean));
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

  private static abstract class AbstractGaussianMeanReverting<F extends FundamentalObservation>
      extends GaussianMeanReverting {

    private final NavigableMap<Long, F> fundamental;

    private AbstractGaussianMeanReverting(F initial) {
      // Put in zero and one, so doubling works
      this.fundamental = new TreeMap<>();
      fundamental.put(0l, initial);
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
    public Iterable<SparseList.Entry<Number>> getFundamentalValues(long finalTime) {
      // TODO Replace filter with takeWhile
      return () -> fundamental.entrySet().stream().filter(e -> e.getKey() <= finalTime)
          .map(SparseList::<Number>asSparseEntry).iterator();
    }

    protected Iterable<Entry<Long, F>> getEntriesIterable() {
      return fundamental.entrySet();
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

    @Override
    public double rmsd(Iterator<? extends SparseList.Entry<? extends Number>> prices,
        long finalTime) {
      if (!prices.hasNext())
        return Double.NaN;

      getValueAt(TimeStamp.of(finalTime)); // Make sure there's a final time node

      PeekingIterator<Entry<Long, ? extends Number>> pfundamental =
          Iterators.peekingIterator(getEntriesIterable().iterator());
      PeekingIterator<SparseList.Entry<? extends Number>> pprices =
          Iterators.peekingIterator(prices);

      SummStats rmsd = SummStats.empty();
      Entry<Long, ? extends Number> lastFundamental = pfundamental.next();
      SparseList.Entry<? extends Number> lastPrice = pprices.next();
      long lastIndex = Math.max(lastFundamental.getKey(), lastPrice.getIndex());

      // Guaranteed to have fundamentals up to this point, don't care about prices
      while (lastIndex < finalTime) {
        Entry<Long, ? extends Number> nextFundamental = pfundamental.peek();
        long nextIndex = Math.min(nextFundamental.getKey(),
            pprices.hasNext() ? pprices.peek().getIndex() : Long.MAX_VALUE);
        long count = nextIndex - lastIndex;

        if (count > 0) {
          double diff =
              lastFundamental.getValue().doubleValue() - lastPrice.getElement().doubleValue();
          rmsd.accept(diff * diff);
        }
        if (count > 1) {
          double expectedRmsd = sampler.expectedAverageIntermediateRmsd(
              lastFundamental.getValue().doubleValue(), nextFundamental.getValue().doubleValue(),
              lastPrice.getElement().doubleValue(), count, count);
          rmsd.acceptNTimes(expectedRmsd, count - 1);
        }

        lastIndex = nextIndex;
        if (pfundamental.peek().getKey() == nextIndex)
          lastFundamental = pfundamental.next();
        if (pprices.hasNext() && pprices.peek().getIndex() == nextIndex)
          lastPrice = pprices.next();
      }
      double diff = lastFundamental.getValue().doubleValue() - lastPrice.getElement().doubleValue();
      rmsd.accept(diff * diff);

      return Math.sqrt(rmsd.getAverage());
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
      if (jumpsBefore == 0)
        newPrice = before.getValue().price;
      else if (after.getValue().jumpsBefore == 0)
        newPrice = after.getValue().price;
      else
        newPrice = sampler.getIntermediateValue(time, before.getValue().price, jumpsBefore,
            after.getValue().price, after.getValue().jumpsBefore);

      return new JumpFundamentalObservation(newPrice, jumpsBefore);
    }

    @Override
    public double rmsd(
        Iterator<? extends edu.umich.srg.collect.SparseList.Entry<? extends Number>> prices,
        long finalTime) {
      // FIXME This is unimplemented, and doesn't seem super trivial to accomplish
      return Double.NaN;
    }

    private static final long serialVersionUID = 1;

  }

  private interface Sampler {

    double getFutureValue(long time, double lastPrice, long jumps);

    double getIntermediateValue(long time, double priceBefore, long jumpsBefore, double priceAfter,
        long jumpsAfter);

    /**
     * Returns the expected average rmsd between fundamentalA and B where price was price, that
     * cooccurred deltat apart in time with jumpsBetween jumps.
     */
    double expectedAverageIntermediateRmsd(double fundamentalA, double fundamentalB, double price,
        long jumpsBetween, long deltat);

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

    // Calculated using mathematica
    @Override
    public double expectedAverageIntermediateRmsd(double fundamentalA, double fundamentalB,
        double price, long jumpsBetween, long deltat) {
      return ((-1 + deltat) * ((-1 + 2 * deltat) * Math.pow(fundamentalA, 2)
          + (-1 + 2 * deltat) * Math.pow(fundamentalB, 2) - 6 * deltat * fundamentalB * price
          + 2 * fundamentalA * (fundamentalB + deltat * fundamentalB - 3 * deltat * price)
          + deltat * (6 * Math.pow(price, 2) + shockVar + deltat * shockVar)))
          / (6 * deltat * (deltat - 1));
    }

    private static final long serialVersionUID = 1;

  }

  private static class IIDGaussian implements Sampler, Serializable {

    private final PositionalSeed seed;
    private final Random rand;
    private final Gaussian dist;

    private IIDGaussian(Random rand, double mean, double shockVar) {
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

    @Override
    public double expectedAverageIntermediateRmsd(double fundamentalA, double fundamentalB,
        double price, long jumpsBetween, long deltat) {
      return dist.getVariance() + dist.getMean() * dist.getMean() - 2 * dist.getMean() * price
          + price * price;
    }

    private static final long serialVersionUID = 1;

  }

  private static class MeanReverting implements Sampler, Serializable {

    private final PositionalSeed seed;
    private final Random rand;
    private final double shockVar, mean, kappac;

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
      double kappacToPower = Math.pow(kappac, jumps),
          stepMean = (1 - kappacToPower) * mean + kappacToPower * lastPrice,
          stepVar = (1 - kappacToPower * kappacToPower) / (1 - kappac * kappac);
      return Gaussian.withMeanVariance(stepMean, shockVar * stepVar).sample(rand);
    }

    @Override
    public double getIntermediateValue(long time, double priceBefore, long jumpsBefore,
        double priceAfter, long jumpsAfter) {
      rand.setSeed(seed.getSeed(time));
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

    // Calculated using mathematica.
    @Override
    public double expectedAverageIntermediateRmsd(double fundamentalA, double fundamentalB,
        double price, long jumpsBetween, long deltat) {
      double temp1 = 2 * deltat, temp2 = Math.pow(kappac, temp1), temp3 = -1 + temp2,
          temp4 = Math.pow(kappac, 2), temp5 = -1 + temp4, temp6 = Math.pow(kappac, deltat),
          temp7 = Math.pow(kappac, 4 * deltat) - temp4 - (-1 + temp1) * temp2 * temp5,
          temp8 = Math.pow(-1 + temp6, 2),
          temp9 = -kappac + Math.pow(kappac, 1 + temp1) - deltat * temp5 * temp6,
          temp10 = -(temp3 * (1 + temp4)) + deltat * (1 + temp2) * temp5, temp11 = -1 + deltat;
      return (shockVar * temp3 * temp10
          + temp5 * (Math.pow(fundamentalA, 2) * temp7 + Math.pow(fundamentalB, 2) * temp7
              + Math.pow(mean, 2) * (-((1 + kappac * (4 + kappac)) * temp3)
                  + deltat * temp5 * (1 + temp6 * (4 + temp6))) * temp8
          + 2 * fundamentalB * mean * temp8 * temp9
          + 2 * fundamentalA * (mean * temp8 * temp9 + fundamentalB * temp6 * temp10))
          + Math
              .pow(price,
                  2)
              * Math.pow(temp3, 2) * Math.pow(temp5, 2) * temp11
          - 2 * (-1 + kappac) * Math.pow(1 + kappac, 2) * price * (1 + temp6) * temp8
              * (mean - deltat * mean - (fundamentalA + fundamentalB) * kappac
                  + (1 + deltat) * mean * kappac
                  + temp6 * (fundamentalA + fundamentalB + mean * (-1 - deltat + kappac * temp11))))
          / (Math.pow(temp3, 2) * Math.pow(temp5, 2) * (deltat - 1));
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
