package edu.umich.srg.marketsim.fundamental;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

import com.google.common.math.DoubleMath;

import edu.umich.srg.marketsim.TimeStamp;
import edu.umich.srg.util.TestDoubles;
import edu.umich.srg.util.TestLongs;

@RunWith(Theories.class)
public class GaussianMeanRevertingTest {
	
	private static final Random rand = new Random();
	private static final int n = 1000000,
			mean = 1000;

	@Theory
	public void fundTest(
			@TestDoubles({0, 0.3, 1}) double kappa,
			@TestDoubles({100, 1000000}) double shockVar,
			@TestDoubles({0, 0.2, 0.8, 1}) double shockProb,
			@TestLongs({1, 2, 5}) long dt1,
			@TestLongs({1, 2, 7}) long dt2
	) {
		TimeStamp t1 = TimeStamp.of(dt1), t2 = TimeStamp.of(dt1 + dt2);

		varianceTest(mean, n, () -> {
			Fundamental f = GaussianMeanReverting.create(rand, mean, kappa, shockVar, shockProb);
			double[] results = new double[2];
			results[0] = f.getValueAt(t1).doubleValue();
			results[1] = f.getValueAt(t2).doubleValue();
			return results;
		}, () -> {
			Fundamental f = GaussianMeanReverting.create(rand, mean, kappa, shockVar, shockProb);
			double[] results = new double[2];
			results[1] = f.getValueAt(t2).doubleValue();
			results[0] = f.getValueAt(t1).doubleValue();
			return results;
		});
	}
		
	static void varianceTest(double mean, int n, Supplier<double[]> fx, Supplier<double[]> fy) {
		List<double[]> x = IntStream.range(0, n).mapToObj(i -> fx.get()).collect(Collectors.toList()),
				y = IntStream.range(0, n).mapToObj(i -> fy.get()).collect(Collectors.toList());
		double[] x1 = x.stream().mapToDouble(r -> r[0]).toArray(), x2 = x.stream().mapToDouble(r -> r[1]).toArray(),
				y1 = y.stream().mapToDouble(r -> r[0]).toArray(), y2 = y.stream().mapToDouble(r -> r[1]).toArray();
		double mx1 = DoubleMath.mean(x1), mx2 = DoubleMath.mean(x2), my1 = DoubleMath.mean(y1),
				my2 = DoubleMath.mean(y2), sx1 = variance(x1), sx2 = variance(x2), sy1 = variance(y1), sy2 = variance(y2),
				e1 = Math.sqrt(DoubleMath.mean(sx1, sy1) / n), e2 = Math.sqrt(DoubleMath.mean(sx2, sy2) / n);

		assertEquals("First Mean Differs", mx1, my1, 100*e1);
		assertEquals("Middle Variances Differ", sx1, sy1, 0.02 * sx1);
		assertEquals("End Mean Differs", mx2, my2, 100*e2);
		assertEquals("End Variances Differ", sx2, sy2, 0.02 * sx2);
	}

	static double variance(double[] xs) {
		double m = Arrays.stream(xs).average().getAsDouble();
		return Arrays.stream(xs).map(x -> (x - m) * (x - m)).average().getAsDouble();
	}
	
}
