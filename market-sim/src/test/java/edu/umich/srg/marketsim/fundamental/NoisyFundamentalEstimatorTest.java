package edu.umich.srg.marketsim.fundamental;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

import edu.umich.srg.distributions.Gaussian;
import edu.umich.srg.distributions.Uniform;
import edu.umich.srg.distributions.Uniform.IntUniform;
import edu.umich.srg.marketsim.testing.MockSim;
import edu.umich.srg.testing.Repeat;
import edu.umich.srg.testing.RepeatRule;
import edu.umich.srg.testing.TestDoubles;
import edu.umich.srg.testing.TestInts;
import edu.umich.srg.util.SummStats;

import java.util.Random;

/*
 * This class has a few tests that take a long time to run, so they shouldn't be run normally. If
 * you run this test, make sure to remove the ignores.
 */
@RunWith(Theories.class)
public class NoisyFundamentalEstimatorTest {

  @Rule
  public RepeatRule repeatRule = new RepeatRule();

  private static final Random rand = new Random();
  private static final int times = 1000000;
  private static final double mean = 5e8, shockVariance = 1000, eps = 0.5;

  @Ignore
  @Theory
  public void fundamentalAccuracyTest(@TestInts({1, 4}) int finalTime,
      @TestDoubles({0, 0.5, 1}) double meanReversion,
      @TestDoubles({0, 100}) double observationVariance) {
    SummStats naiveBias = SummStats.empty(), naiveError = SummStats.empty(),
        hmmBias = SummStats.empty(), hmmError = SummStats.empty();

    for (int i = 0; i < times; ++i) {
      MockSim sim = new MockSim();
      Fundamental fund =
          GaussianMeanReverting.create(rand, finalTime, mean, meanReversion, shockVariance);
      NoisyGaussianMeanRevertingView estimator = NoisyGaussianMeanRevertingView.create(sim, fund,
          rand, finalTime, mean, meanReversion, shockVariance, observationVariance);

      sim.setTime(finalTime);
      double estimate = estimator.getEstimatedFinalFundamental();
      double actual = fund.getValueAt(finalTime);

      naiveBias.accept(mean - actual);
      hmmBias.accept(estimate - actual);

      naiveError.accept((mean - actual) * (mean - actual));
      hmmError.accept((estimate - actual) * (estimate - actual));
    }

    assertEquals(0, naiveBias.getAverage(), eps);
    assertEquals(0, hmmBias.getAverage(), eps);
    assertTrue(naiveError.getAverage() > hmmError.getAverage() - eps);
  }

  @Ignore
  @Theory
  public void priceAccuracyTest(@TestInts({1, 4}) int finalTime,
      @TestDoubles({0, 0.5, 1}) double meanReversion,
      @TestDoubles({0, 100}) double transactionVariance) {
    Gaussian noise = Gaussian.withMeanVariance(0, transactionVariance);

    SummStats naiveBias = SummStats.empty(), naiveError = SummStats.empty(),
        hmmBias = SummStats.empty(), hmmError = SummStats.empty();
    OptimalLinearError optimal = new OptimalLinearError();

    for (int i = 0; i < times; ++i) {
      MockSim sim = new MockSim();
      Fundamental fund =
          GaussianMeanReverting.create(rand, finalTime, mean, meanReversion, shockVariance);
      NoisyGaussianMeanRevertingView estimator = NoisyGaussianMeanRevertingView.create(sim, fund,
          rand, finalTime, mean, meanReversion, shockVariance, Double.POSITIVE_INFINITY);

      sim.setTime(finalTime);
      double actual = fund.getValueAt(finalTime);
      double observation = actual + noise.sample(rand);
      estimator.addObservation(observation, 1, transactionVariance);
      double estimate = estimator.getEstimatedFinalFundamental();

      naiveBias.accept(mean - actual);
      hmmBias.accept(estimate - actual);

      naiveError.accept((mean - actual) * (mean - actual));
      hmmError.accept((estimate - actual) * (estimate - actual));

      optimal.addObservation(actual, observation);
    }

    assertEquals(0, naiveBias.getAverage(), eps);
    assertEquals(0, hmmBias.getAverage(), eps);
    assertTrue(naiveError.getAverage() > hmmError.getAverage() - eps);
    assertTrue(hmmError.getAverage() > optimal.getError() - eps);
  }

  @Ignore
  @Theory
  public void multiSampleTest(@TestInts({1, 2}) int tstep, @TestInts({2, 5}) int numSteps,
      @TestDoubles({0, 0.5, 1}) double meanReversion,
      @TestDoubles({0, 100}) double observationVariance) {
    SummStats naiveBias = SummStats.empty(), naiveError = SummStats.empty(),
        hmmBias = SummStats.empty(), hmmError = SummStats.empty();

    for (int i = 0; i < times; ++i) {
      MockSim sim = new MockSim();
      Fundamental fund =
          GaussianMeanReverting.create(rand, tstep * numSteps, mean, meanReversion, shockVariance);
      NoisyGaussianMeanRevertingView estimator = NoisyGaussianMeanRevertingView.create(sim, fund,
          rand, tstep * numSteps, mean, meanReversion, shockVariance, observationVariance);

      double actual = 0;
      double estimate = 0;
      int time = 0;
      for (int j = 0; j < numSteps; ++j) {
        time += tstep;
        sim.setTime(time);
        actual = fund.getValueAt(time);
        estimate = estimator.getEstimatedFinalFundamental();
      }

      naiveBias.accept(mean - actual);
      hmmBias.accept(estimate - actual);

      naiveError.accept((mean - actual) * (mean - actual));
      hmmError.accept((estimate - actual) * (estimate - actual));
    }

    assertEquals(0, naiveBias.getAverage(), eps);
    assertEquals(0, hmmBias.getAverage(), eps);
    assertTrue(naiveError.getAverage() > hmmError.getAverage() - eps);
  }

  @Ignore
  @Theory
  public void multipleFundamentalAccuracyTest(@TestInts({1, 4}) int finalTime,
      @TestDoubles({0, 0.5, 1}) double meanReversion,
      @TestDoubles({0, 100}) double observationVariance, @TestInts({2}) int numSamples) {
    SummStats naiveBias = SummStats.empty(), naiveError = SummStats.empty(),
        hmmBias = SummStats.empty(), hmmError = SummStats.empty(), baselineBias = SummStats.empty(),
        baselineError = SummStats.empty();

    for (int i = 0; i < times; ++i) {
      MockSim sim = new MockSim();
      Fundamental fund =
          GaussianMeanReverting.create(rand, finalTime, mean, meanReversion, shockVariance);
      NoisyGaussianMeanRevertingView estimator =
          NoisyGaussianMeanRevertingView.create(sim, fund, rand, finalTime, mean, meanReversion,
              shockVariance, observationVariance),
          baseline = NoisyGaussianMeanRevertingView.create(sim, fund, rand, finalTime, mean,
              meanReversion, shockVariance, observationVariance);

      sim.setTime(finalTime);
      double actual = fund.getValueAt(finalTime);
      double estimate = 0;
      for (int j = 0; j < numSamples; ++j) {
        estimate = estimator.getEstimatedFinalFundamental();
      }
      double baselineEstimate = baseline.getEstimatedFinalFundamental();

      naiveBias.accept(mean - actual);
      hmmBias.accept(estimate - actual);
      baselineBias.accept(baselineEstimate - actual);

      naiveError.accept((mean - actual) * (mean - actual));
      hmmError.accept((estimate - actual) * (estimate - actual));
      baselineError.accept((baselineEstimate - actual) * (baselineEstimate - actual));
    }

    assertEquals(0, naiveBias.getAverage(), eps);
    assertEquals(0, hmmBias.getAverage(), eps);
    assertEquals(0, baselineBias.getAverage(), eps);
    assertTrue(naiveError.getAverage() > hmmError.getAverage() - eps);
    assertTrue(baselineError.getAverage() > hmmError.getAverage() - eps);
  }

  @Repeat(100)
  @Theory
  public void infiniteObservationVarianceTest(@TestDoubles({0, 0.2, 1}) double meanReversion,
      @TestDoubles({0, 100}) double shockVariance) {
    MockSim sim = new MockSim();
    Fundamental fund = ConstantFundamental.create(mean);
    NoisyGaussianMeanRevertingView estimator = NoisyGaussianMeanRevertingView.create(sim, fund,
        rand, 100, mean, meanReversion, shockVariance, Double.POSITIVE_INFINITY);
    double estimate = estimator.getEstimatedFinalFundamental();
    assertEquals(mean, estimate, 0);
  }

  @Repeat(100)
  @Theory
  public void zeroObservationVarianceTest(@TestDoubles({0, 0.2, 1}) double meanReversion,
      @TestDoubles({0, 100}) double shockVariance) {
    long simLength = 100;

    MockSim sim = new MockSim();
    Fundamental fund =
        GaussianMeanReverting.create(rand, simLength, mean, meanReversion, shockVariance);
    NoisyGaussianMeanRevertingView estimator = NoisyGaussianMeanRevertingView.create(sim, fund,
        rand, simLength, mean, meanReversion, shockVariance, 0);
    GaussianMeanRevertingView groundTruth =
        GaussianMeanRevertingView.create(sim, fund, simLength, mean, meanReversion);

    IntUniform nextTime = Uniform.closed(1, 3);
    int time = 0;
    while (time < simLength) {
      sim.setTime(time);
      assertEquals(groundTruth.getEstimatedFinalFundamental(),
          estimator.getEstimatedFinalFundamental(), eps);
      time += nextTime.sample(rand);
    }
  }

  private static class OptimalLinearError {
    private int count = 0;
    private double featureMean = 0, targetMean = 0, featureError = 0, targetError = 0,
        featureTargetError = 0;

    public void addObservation(double target, double feature) {
      ++count;
      double featureDelta = feature - featureMean, targetDelta = target - targetMean;
      featureMean += featureDelta / count;
      featureError += featureDelta * (feature - featureMean);
      featureTargetError += (feature - featureMean) * targetDelta;
      targetMean += targetDelta / count;
      targetError += targetDelta * (target - targetMean);
    }

    public double getError() {
      double slope = featureTargetError / featureError,
          intercept = targetMean - slope * featureMean;
      return slope * slope * featureError / count + intercept * intercept + targetError / count
          + 2 * slope * intercept * featureMean - 2 * intercept * targetMean
          - 2 * slope * featureTargetError / count;
    }

  }

}
