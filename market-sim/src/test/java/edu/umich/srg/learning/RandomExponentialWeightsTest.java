package edu.umich.srg.learning;

import static edu.umich.srg.testing.Asserts.assertChiSquared;

import com.google.common.primitives.Ints;

import org.junit.Ignore;
import org.junit.Test;

import edu.umich.srg.learning.ExponentialWeights.NumericExponentialWeights;

import java.util.Map.Entry;
import java.util.Random;
import java.util.stream.IntStream;

/**
 * This test case contains a lot of random tests that aren't guaranteed to pass, so they're ignored
 */
@Ignore
public class RandomExponentialWeightsTest {

  private static final Random rand = new Random();

  @Test
  public void unbiasedCountsTest() {
    int n = 10000;
    double[] expected = {0.2, 0.3, 0.5};
    NumericExponentialWeights<Integer> ew = ExponentialWeights.createNumeric(1,
        Ints.asList(IntStream.range(0, expected.length).toArray()));
    for (int i = 0; i < expected.length; ++i) {
      ew.weights.put(i, expected[i]);
    }

    for (int count : new int[] {1, 2, 3, 4, 5, 6}) {
      int[] observed = new int[expected.length];
      for (int i = 0; i < n; ++i) {
        for (Entry<Integer, Integer> e : ew.getCounts(count, rand).entrySet()) {
          observed[e.getKey()] += e.getValue();
        }
        assertChiSquared(expected, observed);
      }
    }
  }

}
