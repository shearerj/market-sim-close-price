package edu.umich.srg.marketsim.fundamental;

import static org.junit.Assert.assertEquals;

import org.junit.Rule;
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
public class GaussianMeanRevertingTest {

  @Rule
  public RepeatRule repeatRule = new RepeatRule();

  private static final Random rand = new Random();
  private static final int mean = 1000;

  @Repeat(1000)
  @Theory
  public void fundTest(@TestDoubles({0, 0.3, 1}) double kappa,
      @TestDoubles({100, 1000000}) double shockVar,
      @TestDoubles({0, 0.2, 0.8, 1}) double shockProb) {
    TimeStamp t1 = TimeStamp.of(rand.nextInt(100) + 1),
        t2 = TimeStamp.of(t1.get() + rand.nextInt(100) + 1);
    long seed = rand.nextLong();

    Fundamental f1 =
        GaussianMeanReverting.create(new Random(seed), mean, kappa, shockVar, shockProb);
    Price p11 = f1.getValueAt(t1);
    Price p12 = f1.getValueAt(t2);


    Fundamental f2 =
        GaussianMeanReverting.create(new Random(seed), mean, kappa, shockVar, shockProb);
    Price p22 = f2.getValueAt(t2);
    Price p21 = f2.getValueAt(t1);

    assertEquals("First prices were not equal", p11, p21);
    assertEquals("Second prices were not equal", p12, p22);
  }

}
