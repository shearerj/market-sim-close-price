package edu.umich.srg.marketsim.fundamental;

import edu.umich.srg.distributions.Gaussian;
import edu.umich.srg.marketsim.Sim;
import edu.umich.srg.marketsim.fundamental.Fundamental.FundamentalView;

import java.util.Random;

public abstract class NoisyGaussianMeanRevertingView implements FundamentalView {

  /**
   * Create a noisy markov fundamental estimator.
   *
   * @param observationVariance The actual observation variance of the fundamental.
   */
  public static NoisyGaussianMeanRevertingView create(Sim sim, Fundamental fundamental, Random rand,
      long simLength, double mean, double meanReversion, double shockVariance,
      double observationVariance) {
    if (shockVariance == 0) {
      return new ConstantEstimator(mean);
    } else if (meanReversion == 0) {
      return new RandomWalk(sim, fundamental, rand, mean, shockVariance, observationVariance);
    } else {
      return new MeanReverting(sim, fundamental, rand, simLength, mean, meanReversion,
          shockVariance, observationVariance);
    }
  }

  public abstract void addObservation(double observation, int quantity, double variance);

  private static class ConstantEstimator extends NoisyGaussianMeanRevertingView {

    private final double estimate;

    private ConstantEstimator(double estimate) {
      this.estimate = estimate;
    }

    @Override
    public double getEstimatedFinalFundamental() {
      return estimate;
    }

    @Override
    public void addObservation(double observation, int quantity, double variance) {}

  }

  private abstract static class VarianceEstimator extends NoisyGaussianMeanRevertingView {

    private final Sim sim;
    private final Fundamental fundamental;
    private final double observationVariance;
    private final Gaussian dist;
    private final Random rand;

    long lastUpdate;
    double posteriorMean;
    double posteriorVariance;

    private VarianceEstimator(Sim sim, Fundamental fundamental, Random rand, double initialMean,
        double observationVariance) {
      this.sim = sim;
      this.fundamental = fundamental;
      this.observationVariance = observationVariance;
      this.dist = Gaussian.withMeanVariance(0, observationVariance);
      this.rand = rand;

      this.posteriorMean = initialMean;
      this.lastUpdate = 0;
      this.posteriorVariance = 0;
    }

    @Override
    public double getEstimatedFinalFundamental() {
      long time = sim.getCurrentTime().get();
      if (Double.isFinite(observationVariance)) {
        double obs = fundamental.getValueAt(time) + dist.sample(rand);
        addObservation(obs, 1, observationVariance);
      } else {
        if (time > lastUpdate) {
          updateTime(time - lastUpdate);
        }
        lastUpdate = time;
      }
      return posteriorMean;
    }

    @Override
    public void addObservation(double observation, int quantity, double variance) {
      long time = sim.getCurrentTime().get();
      if (time > lastUpdate) {
        updateTime(time - lastUpdate);
      }
      observationUpdate(observation, variance, quantity);
      lastUpdate = time;
    }

    private void observationUpdate(double observation, double variance, int times) {
      if (variance == 0) {
        posteriorMean = observation;
        posteriorVariance = 0;
      } else if (variance != Double.POSITIVE_INFINITY) {
        // No update for infinite variance
        double varToPower = Math.pow(variance, times - 1);
        posteriorMean = (posteriorMean * variance * varToPower
            + times * observation * varToPower * posteriorVariance)
            / (varToPower * variance + times * varToPower * posteriorVariance);
        posteriorVariance = posteriorVariance * varToPower * variance
            / (varToPower * variance + times * varToPower * posteriorVariance);
      }
    }

    abstract void updateTime(long advance);

  }

  private static class RandomWalk extends VarianceEstimator {

    private final double shockVariance;

    private RandomWalk(Sim sim, Fundamental fundamental, Random rand, double mean,
        double shockVariance, double observationVariance) {
      super(sim, fundamental, rand, mean, observationVariance);
      this.shockVariance = shockVariance;
    }

    @Override
    void updateTime(long advance) {
      posteriorVariance += advance * shockVariance;
    }

  }

  private static class MeanReverting extends VarianceEstimator {

    private final long simLength;
    private final double fundamentalMean;
    private final double kappac;
    private final double shockVariance;

    private MeanReverting(Sim sim, Fundamental fundamental, Random rand, long simLength,
        double mean, double meanReversion, double shockVariance, double observationVariance) {
      super(sim, fundamental, rand, mean, observationVariance);
      this.simLength = simLength;
      this.fundamentalMean = mean;
      this.kappac = 1 - meanReversion;
      this.shockVariance = shockVariance;
    }

    @Override
    public double getEstimatedFinalFundamental() {
      super.getEstimatedFinalFundamental();
      double kappacToPower = Math.pow(kappac, simLength - lastUpdate);
      return (1 - kappacToPower) * fundamentalMean + kappacToPower * posteriorMean;
    }

    @Override
    void updateTime(long advance) {
      double kappacToPower = Math.pow(kappac, advance);
      posteriorMean = (1 - kappacToPower) * fundamentalMean + kappacToPower * posteriorMean;
      posteriorVariance = kappacToPower * kappacToPower * posteriorVariance
          + (1 - kappacToPower * kappacToPower) / (1 - kappac * kappac) * shockVariance;
    }

  }

}
