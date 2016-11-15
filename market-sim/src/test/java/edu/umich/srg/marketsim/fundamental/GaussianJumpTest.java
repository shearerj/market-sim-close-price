package edu.umich.srg.marketsim.fundamental;

import static org.junit.Assert.assertEquals;

import com.google.common.collect.Iterables;

import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

import edu.umich.srg.marketsim.Price;
import edu.umich.srg.marketsim.TimeStamp;
import edu.umich.srg.testing.Repeat;
import edu.umich.srg.testing.RepeatRule;
import edu.umich.srg.testing.TestDoubles;

import java.util.Random;

@RunWith(Theories.class)
public class GaussianJumpTest {

  @Rule
  public RepeatRule repeatRule = new RepeatRule();

  private static final Random rand = new Random();
  private static final int mean = 1000;

  @Repeat(1000)
  @Theory
  public void fundTest(@TestDoubles({100, 1000000}) double shockVar,
      @TestDoubles({0, 0.2, 0.8, 1}) double shockProb) {
    TimeStamp t1 = TimeStamp.of(rand.nextInt(100) + 1);
    TimeStamp t2 = TimeStamp.of(t1.get() + rand.nextInt(100) + 1);
    TimeStamp finalTime = TimeStamp.of(t2.get() + 1);
    long seed = rand.nextLong();

    Fundamental f1 = GaussianJump.create(new Random(seed), finalTime, mean, shockVar, shockProb);
    Price p11 = f1.getValueAt(t1);
    Price p12 = f1.getValueAt(t2);


    Fundamental f2 = GaussianJump.create(new Random(seed), finalTime, mean, shockVar, shockProb);
    Price p22 = f2.getValueAt(t2);
    Price p21 = f2.getValueAt(t1);

    assertEquals("First prices were not equal", p11, p21);
    assertEquals("Second prices were not equal", p12, p22);
  }

  /**
   * Tests that it can generate a large fundamental value reasonble quickly. This only really works
   * if there are always jumps, as hypergeometrics are hard to sample from.
   */
  @Test
  public void longFundamentalTest() {
    // FIXME Hypergeometric isn't very efficient, making it hard to sample backwards
    TimeStamp time = TimeStamp.of(1000000000000L);
    Fundamental fundamental = GaussianJump.create(rand, time, mean, 100, 1e-3);
    assertEquals(467, Iterables.size(fundamental.getFundamentalValues()));
  }

}
