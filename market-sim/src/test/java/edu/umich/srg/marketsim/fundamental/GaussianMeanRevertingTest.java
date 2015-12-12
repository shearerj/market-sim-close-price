package edu.umich.srg.marketsim.fundamental;

import static org.junit.Assert.assertEquals;

import com.google.common.collect.ImmutableSet;
import com.google.common.math.DoubleMath;

import org.junit.Ignore;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

import edu.umich.srg.marketsim.TimeStamp;
import edu.umich.srg.testing.TestDoubles;
import edu.umich.srg.testing.TestLongs;
import edu.umich.srg.util.SummStats;

import java.util.Random;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Stream;

/*
 * These tests aren't guaranteed to pass, and they take a long time to run. It's recommended that
 * these tests be uncommented and run if changing the fundamental code.
 */
@Ignore
@RunWith(Theories.class)
public class GaussianMeanRevertingTest {

  private static final Random rand = new Random();
  private static final int n = 1000000, mean = 1000;

  @Theory
  public void fundTest(@TestDoubles({0, 0.3, 1}) double kappa,
      @TestDoubles({100, 1000000}) double shockVar, @TestDoubles({0, 0.2, 0.8, 1}) double shockProb,
      @TestLongs({1, 2, 5}) long dt1, @TestLongs({1, 2, 7}) long dt2) {
    TimeStamp t1 = TimeStamp.of(dt1), t2 = TimeStamp.of(dt1 + dt2);

    varianceTest(mean, n, () -> {
      Fundamental f = GaussianMeanReverting.create(rand, mean, kappa, shockVar, shockProb);
      double[] results = new double[2];
      results[0] = f.getValueAt(t1).doubleValue();
      results[1] = f.getValueAt(t2).doubleValue();
      return results;
    } , () -> {
      Fundamental f = GaussianMeanReverting.create(rand, mean, kappa, shockVar, shockProb);
      double[] results = new double[2];
      results[1] = f.getValueAt(t2).doubleValue();
      results[0] = f.getValueAt(t1).doubleValue();
      return results;
    });
  }

  private static void varianceTest(double mean, int n, Supplier<double[]> fx,
      Supplier<double[]> fy) {
    SummStats[] x = Stream.generate(fx).limit(n).collect(new InfoCollector());
    SummStats[] y = Stream.generate(fy).limit(n).collect(new InfoCollector());

    double sx1 = x[0].getVariance(), sx2 = x[1].getVariance(), sy1 = y[0].getVariance(),
        sy2 = y[1].getVariance(), e1 = Math.sqrt(DoubleMath.mean(sx1, sy1) / n),
        e2 = Math.sqrt(DoubleMath.mean(sx2, sy2) / n);

    assertEquals("First Mean Differs", x[0].getAverage(), y[0].getAverage(), 100 * e1);
    assertEquals("Middle Variances Differ", sx1, sy1, 0.02 * sx1);
    assertEquals("End Mean Differs", x[1].getAverage(), y[1].getAverage(), 100 * e2);
    assertEquals("End Variances Differ", sx2, sy2, 0.02 * sx2);
  }

  private static class InfoCollector implements Collector<double[], SummStats[], SummStats[]> {

    @Override
    public BiConsumer<SummStats[], double[]> accumulator() {
      return (a, v) -> {
        a[0].accept(v[0]);
        a[1].accept(v[1]);
      };
    }

    @Override
    public Set<Characteristics> characteristics() {
      return ImmutableSet.of(Characteristics.UNORDERED, Characteristics.IDENTITY_FINISH);
    }

    @Override
    public BinaryOperator<SummStats[]> combiner() {
      return (a, b) -> {
        a[0].combine(b[0]);
        a[1].combine(b[1]);
        return a;
      };
    }

    @Override
    public Function<SummStats[], SummStats[]> finisher() {
      return Function.identity();
    }

    @Override
    public Supplier<SummStats[]> supplier() {
      return () -> Stream.generate(SummStats::empty).limit(2).toArray(SummStats[]::new);
    }

  }

}
