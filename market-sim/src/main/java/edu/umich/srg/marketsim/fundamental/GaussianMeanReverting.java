package edu.umich.srg.marketsim.fundamental;

import static com.google.common.base.Preconditions.checkArgument;

import edu.umich.srg.collect.Sparse;
import edu.umich.srg.distributions.Gaussian;
import edu.umich.srg.marketsim.Sim;
import edu.umich.srg.util.PositionalSeed;

import java.io.Serializable;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Random;
import java.util.TreeMap;

/**
 * This class models a Guassian mean reverting process. It has two important implementation
 * features. First, fundamental values are computed lazily and on demand, so that even if you ask
 * for the fundamental value at time 10000000, it should return reasonably fast, without having to
 * generate all 1000000 values. It is also randomly stable, that is, two fundamentals with the same
 * random generator will produce the same value at every point independent of query order. This
 * costs a logarithmic factor to do, but the stability is generally worth it, and the log factor is
 * tiny in terms of actual time costs. More detail on the math for sampling from the fundamental is
 * in the docs folder.
 */

public abstract class GaussianMeanReverting implements Fundamental, Serializable {

  /** Create a standard Gaussian mean reverting fundamental stochastic process. */
  public static Fundamental create(Random rand, long finalTime, double mean, double meanReversion,
      double shockVar) {
    if (meanReversion == 0) {
      return new RandomWalk(rand, finalTime, mean, shockVar);
    } else if (meanReversion == 1) {
      return new IidGaussian(rand, finalTime, mean, shockVar);
    } else {
      return new MeanReverting(rand, finalTime, mean, shockVar, meanReversion);
    }
  }

  private final NavigableMap<Long, Double> fundamental;
  protected final long finalTime;

  private GaussianMeanReverting(long finalTime, double start, double end) {
    // Put in zero and one, so doubling works
    this.fundamental = new TreeMap<>();
    this.finalTime = finalTime;
    fundamental.put(0L, start);
    fundamental.put(finalTime, end);
  }

  @Override
  public double getValueAt(long time) {
    checkArgument(time <= fundamental.lastEntry().getKey(), "Can't ask for time beyond final time");

    Entry<Long, Double> before = fundamental.floorEntry(time);
    Entry<Long, Double> after = fundamental.ceilingEntry(time);

    // Binary search for value
    while (before.getKey() != time && after.getKey() != time) {
      long midTime = (before.getKey() + after.getKey()) / 2;
      double observation = getIntermediateValue(midTime, before.getValue(),
          midTime - before.getKey(), after.getValue(), after.getKey() - midTime);
      fundamental.put(midTime, observation);
      Entry<Long, Double> entry = new AbstractMap.SimpleImmutableEntry<>(midTime, observation);
      if (midTime > time) {
        after = entry;
      } else {
        before = entry;
      }
    }

    if (before.getKey() == time) {
      return before.getValue();
    } else {
      return after.getValue();
    }
  }

  @Override
  public Iterable<Sparse.Entry<Double>> getFundamentalValues() {
    return () -> fundamental.entrySet().stream()
        .map(e -> Sparse.immutableEntry(e.getKey(), e.getValue().doubleValue())).iterator();
  }

  protected abstract double getIntermediateValue(long time, double priceBefore, long timeBefore,
      double priceAfter, long timeAfter);

  private static class RandomWalk extends GaussianMeanReverting implements Serializable {

    private final PositionalSeed seed;
    private final Random rand;
    private final double shockVar;
    private final Map<Sim, FundamentalView> cachedViews;

    private RandomWalk(Random rand, long finalTime, double mean, double shockVar) {
      super(finalTime, mean, Gaussian.withMeanVariance(mean, shockVar * finalTime).sample(rand));
      this.seed = PositionalSeed.with(rand.nextLong());
      this.shockVar = shockVar;
      this.rand = rand;
      this.cachedViews = new HashMap<>();
    }

    @Override
    public double getIntermediateValue(long time, double priceBefore, long timeBefore,
        double priceAfter, long timeAfter) {
      rand.setSeed(seed.getSeed(time));
      return Gaussian.withMeanVariance(
          (priceBefore * timeAfter + priceAfter * timeBefore) / (timeBefore + timeAfter),
          timeBefore * timeAfter / (double) (timeBefore + timeAfter) * shockVar).sample(rand);
    }


    @Override
    public FundamentalView getView(Sim sim) {
      return cachedViews.computeIfAbsent(sim, RandomWalkView::new);
    }

    private class RandomWalkView implements FundamentalView {

      private final Sim sim;

      private RandomWalkView(Sim sim) {
        this.sim = sim;
      }

      @Override
      public double getEstimatedFinalFundamental() {
        return getValueAt(sim.getCurrentTime().get());
      }

    }

    private static final long serialVersionUID = 1;

  }


  private static class IidGaussian extends GaussianMeanReverting implements Serializable {

    private final PositionalSeed seed;
    private final Random rand;
    private final Gaussian dist;
    private final Map<Sim, FundamentalView> cachedViews;

    private IidGaussian(Random rand, long finalTime, double mean, double shockVar) {
      super(finalTime, mean, Gaussian.withMeanVariance(mean, shockVar).sample(rand));
      this.seed = PositionalSeed.with(rand.nextLong());
      this.dist = Gaussian.withMeanVariance(mean, shockVar);
      this.rand = rand;
      this.cachedViews = new HashMap<>();
    }

    @Override
    public double getIntermediateValue(long time, double priceBefore, long jumpsBefore,
        double priceAfter, long jumpsAfter) {
      rand.setSeed(seed.getSeed(time));
      return dist.sample(rand);
    }

    @Override
    public FundamentalView getView(Sim sim) {
      return cachedViews.computeIfAbsent(sim, IidGaussianView::new);
    }

    private class IidGaussianView implements FundamentalView {

      private final Sim sim;

      private IidGaussianView(Sim sim) {
        this.sim = sim;
      }

      @Override
      public double getEstimatedFinalFundamental() {
        if (sim.getCurrentTime().get() == finalTime) {
          return getValueAt(finalTime);
        } else {
          return dist.getMean();
        }
      }

    }

    private static final long serialVersionUID = 1;

  }


  private static class MeanReverting extends GaussianMeanReverting implements Serializable {

    private final PositionalSeed seed;
    private final Random rand;
    private final double shockVar;
    private final double mean;
    private final double kappac;
    private final Map<Sim, FundamentalView> cachedViews;

    private MeanReverting(Random rand, long finalTime, double mean, double shockVar,
        double meanReversion) {
      super(finalTime, mean, getFinalValue(rand, mean, finalTime, 1 - meanReversion, shockVar));
      this.seed = PositionalSeed.with(rand.nextLong());
      this.mean = mean;
      this.shockVar = shockVar;
      this.kappac = 1 - meanReversion;
      this.rand = rand;
      this.cachedViews = new HashMap<>();
    }

    private static double getFinalValue(Random rand, double mean, long finalTime, double kappac,
        double shockVar) {
      double kappacToPower = Math.pow(kappac, finalTime);
      double stepMean = (1 - kappacToPower) * mean + kappacToPower * mean;
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

    @Override
    public FundamentalView getView(Sim sim) {
      return cachedViews.computeIfAbsent(sim, MeanRevertingView::new);
    }

    private class MeanRevertingView implements FundamentalView {

      private final Sim sim;

      private MeanRevertingView(Sim sim) {
        this.sim = sim;
      }

      @Override
      public double getEstimatedFinalFundamental() {
        long time = sim.getCurrentTime().get();
        double kappacToPower = Math.pow(kappac, finalTime - time);
        return (1 - kappacToPower) * mean + kappacToPower * getValueAt(time);
      }

    }

    private static final long serialVersionUID = 1;

  }

  private static final long serialVersionUID = 1;

}
