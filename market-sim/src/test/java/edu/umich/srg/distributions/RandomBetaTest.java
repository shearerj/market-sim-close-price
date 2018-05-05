package edu.umich.srg.distributions;

import static org.junit.Assert.assertEquals;

import edu.umich.srg.testing.TestDoubles;
import edu.umich.srg.util.SummStats;

import org.junit.Ignore;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

import java.util.Random;
import java.util.stream.DoubleStream;

@Ignore // This test is random and won't always be successful
@RunWith(Theories.class)
public class RandomBetaTest {

  private static Random rand = new Random();

  @Theory
  public void summaryStatisticsTest(@TestDoubles({1, 10, 16}) double alpha) {
    Beta dist = Beta.with(alpha, alpha);
    SummStats stats = SummStats.over(DoubleStream.generate(() -> dist.sample(rand)).limit(100000));
    assertEquals(0.5, stats.getAverage().getAsDouble(), 1e-2);
    assertEquals(1 / (8 * alpha + 4), stats.getVariance().getAsDouble(), 1e-2);
  }

}
