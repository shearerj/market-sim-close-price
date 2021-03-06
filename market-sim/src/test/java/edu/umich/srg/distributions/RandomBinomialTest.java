package edu.umich.srg.distributions;

import static org.junit.Assert.assertEquals;

import edu.umich.srg.testing.TestDoubles;
import edu.umich.srg.testing.TestInts;

import org.junit.Ignore;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Random;

@Ignore
@RunWith(Theories.class)
public class RandomBinomialTest {

  private static final Random rand = new Random();

  /**
   * This test compares the expected pmf with the actual pmf. The expected pmf is calculated in log
   * space for stability. The numbers 15, 37, and 100 were chosen because 15 is in the Bernoulli
   * range, 37 is the cutoff to the beta approximation method, and 100 is way above the beta
   * approximation bound. Finally, a chi squared test is not used because the distribtion is on;y
   * approximated, so it will always fail. Instead, this just verifies that they are "close". Since
   * this test is random, it is disabled by default.
   */
  @Theory
  public void pmfTest(@TestDoubles({0.2, 0.5, 0.8}) double prob, @TestInts({15, 37, 100}) int draws,
      @TestInts({100000}) int samples) {
    int[] observed = new int[draws + 1];
    double[] logExpected = new double[draws + 1];
    logExpected[0] = draws * Math.log(1 - prob);
    for (int k = 1; k <= draws; ++k) {
      logExpected[k] = logExpected[k - 1] + Math.log(draws - k + 1) + Math.log(prob) - Math.log(k)
          - Math.log(1 - prob);
    }
    assertEquals(1, Arrays.stream(logExpected).map(Math::exp).sum(), 1e-6);

    Binomial bin = Binomial.with(draws, prob);
    for (int i = 0; i < samples; ++i) {
      observed[(int) bin.sample(rand)]++;
    }

    for (int i = 0; i <= draws; ++i) {
      assertEquals("Element " + i + " wrong", Math.exp(logExpected[i]),
          observed[i] / (double) samples, 5e-2);
    }
  }

}
