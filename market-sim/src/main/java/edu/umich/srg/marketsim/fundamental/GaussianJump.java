package edu.umich.srg.marketsim.fundamental;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.primitives.Ints;

import edu.umich.srg.collect.Sparse;
import edu.umich.srg.distributions.Binomial;
import edu.umich.srg.distributions.Gaussian;
import edu.umich.srg.distributions.Hypergeometric;
import edu.umich.srg.marketsim.Price;
import edu.umich.srg.util.PositionalSeed;

import java.io.Serializable;
import java.util.AbstractMap;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Random;
import java.util.TreeMap;

/**
 * This class models a Guassian jump process that doesn't necessarily make a jump at every time
 * step. It has two important implementation features. First, fundamental values are computed lazily
 * and on demand, so that even if you ask for the fundamental value at time 10000000, it should
 * return reasonable fast, without having to generate all 1000000 values. It is also randomly
 * stable, that is, two fundamentals with the same random generator will produce the same value at
 * every point independent of query order. This costs a logarithmic factor to do, but the stability
 * is generally worth it, and the log factor is tiny in terms of actual time costs. More detail on
 * the math for sampling from the fundamental is in the docs folder.
 */

public class GaussianJump implements Fundamental, Serializable {

  /** Create a standard gaussian mean reverting fundamental stochastic process. */
  public static Fundamental create(Random rand, long finalTime, double mean, double shockVar,
      double shockProb) {
    if (shockProb == 0) {
      return ConstantFundamental.create(Price.of(mean));
    } else if (shockProb == 1) {
      return GaussianMeanReverting.create(rand, finalTime, mean, 0, shockVar);
    } else {
      return new GaussianJump(finalTime, mean, shockVar, shockProb, rand);
    }
  }

  private final NavigableMap<Long, FundObs> fundamental;
  private final PositionalSeed seed;
  private final Random rand;
  private final double shockVar;

  private GaussianJump(long finalTime, double mean, double shockVar, double shockProb,
      Random rand) {
    this.fundamental = new TreeMap<>();
    this.shockVar = shockVar;
    this.seed = PositionalSeed.with(rand.nextLong());
    this.rand = rand;

    fundamental.put(0L, new FundObs(mean, 0));
    long time = 0;
    double price = mean;
    while (time < finalTime) {
      int step = Ints.saturatedCast(finalTime - time);
      int jumps = (int) Binomial.with(step, shockProb).sample(rand);
      time += step;
      price = jumps == 0 ? price : Gaussian.withMeanVariance(price, shockVar * jumps).sample(rand);
      fundamental.put(time, new FundObs(price, jumps));
    }
  }

  @Override
  public double getValueAt(long time) {
    checkArgument(time <= fundamental.lastEntry().getKey(), "Can't ask for time beyond final time");

    Entry<Long, FundObs> before = fundamental.floorEntry(time);
    Entry<Long, FundObs> after = fundamental.ceilingEntry(time);

    while (before.getKey() != time && after.getKey() != time) {
      long midTime = (before.getKey() + after.getKey()) / 2;
      FundObs observation = observeIntermediate(before, after, midTime);
      fundamental.put(midTime, observation);
      Entry<Long, FundObs> entry = new AbstractMap.SimpleImmutableEntry<>(midTime, observation);
      if (midTime > time) {
        after = entry;
      } else {
        before = entry;
      }
    }

    if (before.getKey() == time) {
      return before.getValue().price;
    } else {
      return after.getValue().price;
    }
  }

  @Override
  public Iterable<Sparse.Entry<Double>> getFundamentalValues() {
    return () -> fundamental.entrySet().stream()
        .map(e -> Sparse.immutableEntry(e.getKey(), e.getValue().price)).iterator();
  }

  private FundObs observeIntermediate(Entry<Long, FundObs> before, Entry<Long, FundObs> after,
      long time) {
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
      rand.setSeed(seed.getSeed(time));
      long jumpsAfter = after.getValue().jumpsBefore;
      newPrice = Gaussian
          .withMeanVariance(
              (before.getValue().price * jumpsAfter + after.getValue().price * jumpsBefore)
                  / (jumpsBefore + jumpsAfter),
              jumpsBefore * jumpsAfter / (double) (jumpsBefore + jumpsAfter) * shockVar)
          .sample(rand);
    }
    return new FundObs(newPrice, jumpsBefore);
  }

  private static class FundObs implements Serializable {

    final double price;
    int jumpsBefore;

    private FundObs(double price, int jumps) {
      this.price = price;
      this.jumpsBefore = jumps;
    }

    private static final long serialVersionUID = 1;

  }

  private static final long serialVersionUID = 1;

}
