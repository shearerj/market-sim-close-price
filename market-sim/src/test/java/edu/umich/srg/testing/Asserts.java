package edu.umich.srg.testing;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.IntStream;

public interface Asserts {

  public static final double[] chiSquared =
      {6.64, 9.21, 11.35, 13.28, 15.09, 16.81, 18.48, 20.09, 21.67, 23.21};

  public static <T> void assertSetEquals(Set<? extends T> expected, Set<? extends T> actual) {
    assertSetEquals(expected, actual, "Sets not equal");
  }

  public static <T> void assertSetEquals(Set<? extends T> expected, Set<? extends T> actual,
      String message) {
    if (checkNotNull(expected).equals(checkNotNull(actual)))
      return;

    SetView<? extends T> extra = Sets.difference(actual, expected),
        missing = Sets.difference(expected, actual);

    if (extra.isEmpty()) {
      throw new AssertionError(String.format("%s - missing: %s", message, missing));
    } else if (missing.isEmpty()) {
      throw new AssertionError(String.format("%s - extra: %s", message, extra));
    } else {
      throw new AssertionError(
          String.format("%s - missing: %s - extra: %s", message, missing, extra));
    }
  }

  public static void assertChiSquared(double[] expected, int[] observed) {
    int total = Arrays.stream(observed).sum();
    double testStatistic =
        IntStream.range(0, expected.length).mapToDouble(i -> (observed[i] - expected[i] * total)
            * (observed[i] / (double) total - expected[i]) / expected[i]).sum();
    assertTrue(testStatistic < chiSquared[expected.length - 2],
        "Assuming expected, observed would be seen less than 1%% of the time (%s instead of %s)",
        Arrays.toString(Arrays.stream(observed).mapToDouble(i -> i / (double) total).toArray()),
        Arrays.toString(expected));
  }

  public static void assertTrue(boolean assertion, String formatString, Object... objects) {
    if (!assertion) {
      throw new AssertionError(String.format(formatString, objects));
    }
  }

}

