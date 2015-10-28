package edu.umich.srg.util;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;

public class StatisticalAssertions {

  private static final double[] criticalValues =
      {0, 3.8415, 5.9915, 7.8147, 9.4877, 11.0705, 12.5916, 14.0671, 15.5073, 16.9190};

  public static void assertKnownChiSquared(int[] observed, double[] expected) {
    long sum = Arrays.stream(observed).asLongStream().sum();
    double chiSquared = 0;
    for (int i = 0; i < observed.length; ++i)
      chiSquared += observed[i] == 0 && expected[i] == 0 ? 0
          : (observed[i] - sum * expected[i]) * (observed[i] - sum * expected[i])
              / (sum * expected[i]);
    assertTrue(
        Arrays.toString(observed) + " "
            + Arrays.toString(Arrays.stream(expected).mapToInt(e -> (int) (sum * e)).toArray()),
        chiSquared < criticalValues[observed.length]);
  }

}
