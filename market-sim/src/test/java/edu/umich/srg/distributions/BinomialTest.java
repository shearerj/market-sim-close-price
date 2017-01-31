package edu.umich.srg.distributions;

import static edu.umich.srg.testing.Asserts.assertTrue;

import edu.umich.srg.testing.TestDoubles;
import edu.umich.srg.testing.TestInts;

import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

import java.util.Random;

@RunWith(Theories.class)
public class BinomialTest {

  private static final Random rand = new Random();

  @Theory
  public void boundTest(@TestDoubles({0, 0.2, 0.5, 0.8, 1}) double prob,
      @TestInts({0, 1, 15, 50, 100}) int samples) {
    Binomial bin = Binomial.with(samples, prob);
    for (int i = 0; i < 10000; ++i) {
      long draw = bin.sample(rand);
      assertTrue(0 <= draw, "Drew a negative number of samples");
      assertTrue(draw <= samples, "Got more samples than it should have been able to");
    }
  }

}
