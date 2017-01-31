package edu.umich.srg.marketsim.fundamental;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.Multiset.Entry;

import edu.umich.srg.distributions.Uniform;
import edu.umich.srg.distributions.Uniform.IntUniform;
import edu.umich.srg.marketsim.fundamental.GaussianFundamentalView.GaussableView;
import edu.umich.srg.marketsim.testing.MockSim;
import edu.umich.srg.testing.Repeat;
import edu.umich.srg.testing.RepeatRule;
import edu.umich.srg.testing.TestDoubles;
import edu.umich.srg.testing.TestInts;

import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

import java.util.Random;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

@RunWith(Theories.class)
public class GaussianMeanRevertingTest {

  @Rule
  public RepeatRule repeatRule = new RepeatRule();

  private static final Random rand = new Random();
  private static final int mean = 1000;
  private static final double eps = 0.5;

  @Repeat(1000)
  @Theory
  public void fundTest(@TestDoubles({0, 0.3, 1}) double kappa,
      @TestDoubles({100, 1000000}) double shockVar) {
    long t1 = rand.nextInt(100) + 1;
    long t2 = t1 + rand.nextInt(100) + 1;
    long finalTime = t2 + 1;
    long seed = rand.nextLong();

    Fundamental f1 =
        GaussianMeanReverting.create(new Random(seed), finalTime, mean, kappa, shockVar);
    double p11 = f1.getValueAt(t1);
    double p12 = f1.getValueAt(t2);

    Fundamental f2 =
        GaussianMeanReverting.create(new Random(seed), finalTime, mean, kappa, shockVar);
    double p22 = f2.getValueAt(t2);
    double p21 = f2.getValueAt(t1);

    assertEquals("First prices were not equal", p11, p21, 0);
    assertEquals("Second prices were not equal", p12, p22, 0);
  }

  @Repeat(10)
  @Theory
  public void fundLengthTest(@TestDoubles({100}) double shockVar,
      @TestDoubles({0, 0.3, 1}) double kappa, @TestInts({100, 1000}) int finalTime) {

    Fundamental fund = GaussianMeanReverting.create(rand, finalTime, mean, kappa, shockVar);
    // Sample fundamental at random points
    IntStream.generate(() -> rand.nextInt(finalTime)).limit(10)
        .forEach(time -> fund.getValueAt(time));
    long length = StreamSupport.stream(fund.getFundamentalValues().spliterator(), true)
        .mapToLong(Entry::getCount).sum();
    assertEquals(finalTime + 1, length);
  }

  /**
   * Tests that it can generate a large fundamental value reasonble quickly. This only really works
   * if there are always jumps, as hypergeometrics are hard to sample from.
   */
  @Test
  public void longFundamentalTest() {
    long time = 1000000000000L;
    long start = System.currentTimeMillis();
    Fundamental fundamental = GaussianMeanReverting.create(rand, time, mean, 0.5, 100);
    fundamental.getValueAt(time - 1);
    assertTrue(System.currentTimeMillis() < start + 1000);
  }


  @Repeat(100)
  @Theory
  public void infiniteObservationVarianceTest(@TestDoubles({0, 0.2, 1}) double meanReversion,
      @TestDoubles({0, 100}) double shockVariance) {
    MockSim sim = new MockSim();
    Fundamental fund = ConstantFundamental.create(mean, 100);
    GaussianFundamentalView estimator = ((GaussableView) fund.getView(sim)).addNoise(rand, 0);
    double estimate = estimator.getEstimatedFinalFundamental();
    assertEquals(mean, estimate, 0);
  }

  @Repeat(100)
  @Theory
  public void zeroObservationVarianceTest(@TestDoubles({/* 0, */ 0.2, 1}) double meanReversion,
      @TestDoubles({/* 0, */ 100}) double shockVariance) {
    long simLength = 100;

    MockSim sim = new MockSim();
    Fundamental fund =
        GaussianMeanReverting.create(rand, simLength, mean, meanReversion, shockVariance);
    GaussableView groundTruth = (GaussableView) fund.getView(sim);
    GaussianFundamentalView estimator = groundTruth.addNoise(rand, 0);

    IntUniform nextTime = Uniform.closed(1, 3);
    int time = 0;
    while (time < simLength) {
      sim.setTime(time);
      assertEquals(groundTruth.getEstimatedFinalFundamental(),
          estimator.getEstimatedFinalFundamental(), eps);
      time += nextTime.sample(rand);
    }
  }

}
