package edu.umich.srg.learning;

import static edu.umich.srg.testing.Asserts.assertTrue;

import com.google.common.primitives.Doubles;
import com.google.common.primitives.Ints;

import edu.umich.srg.learning.ExponentialWeights.NumericExponentialWeights;
import edu.umich.srg.util.Linear;
import edu.umich.srg.util.SummStats;

import org.junit.Test;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * This test case contains a lot of random tests that aren't guaranteed to pass, so they're ignored
 */
public class ExponentialWeightsTest {

  private static final Random rand = new Random();

  @Test
  public void learningBoundTestFlipFlop() {
    int maxTime = 100, n = 10000;
    List<SummStats> averageReward =
        Stream.generate(SummStats::empty).limit(maxTime).collect(Collectors.toList());
    for (int i = 0; i < n; ++i) {
      NumericExponentialWeights<Integer> ew =
          ExponentialWeights.createNumeric(1, Ints.asList(0, 1));
      double reward = 0;
      for (int t = 0; t < maxTime; ++t) {
        reward += (ew.sample(rand) + t) % 2;
        ew.update(Ints.asList(t % 2, (t + 1) % 2));
        averageReward.get(t).accept(reward);
      }
    }
    double rSquared =
        Linear.pearsonCoefficient(IntStream.range(0, maxTime).asDoubleStream().iterator(),
            averageReward.stream()
                .mapToDouble(d -> d.getAverage().getAsDouble() * d.getAverage().getAsDouble())
                .iterator());
    assertTrue(rSquared > 0.9, "Regret wasn't close to square root t (r2 only %f)", rSquared);
  }

  @Test
  public void learningBoundTestRandom() {
    int maxTime = 100, n = 10000;
    List<SummStats> averageReward =
        Stream.generate(SummStats::empty).limit(maxTime).collect(Collectors.toList());
    for (int i = 0; i < n; ++i) {
      NumericExponentialWeights<Integer> ew =
          ExponentialWeights.createNumeric(1, Ints.asList(0, 1));
      double reward = 0;
      for (int t = 0; t < maxTime; ++t) {
        List<Double> gains = Doubles.asList(rand.nextDouble(), rand.nextDouble());
        reward += gains.get(ew.sample(rand));
        ew.update(gains);
        averageReward.get(t).accept(reward);
      }
    }
    double rSquared =
        Linear.pearsonCoefficient(IntStream.range(0, maxTime).asDoubleStream().iterator(),
            averageReward.stream()
                .mapToDouble(d -> d.getAverage().getAsDouble() * d.getAverage().getAsDouble())
                .iterator());
    assertTrue(rSquared > 0.90, "Regret wasn't close to square root t (r2 only %f)", rSquared);
  }

}
