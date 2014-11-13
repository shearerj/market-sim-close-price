package utils;

import static java.math.RoundingMode.HALF_EVEN;

import java.util.Iterator;

import org.apache.commons.math3.stat.descriptive.rank.Median;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.UnmodifiableIterator;
import com.google.common.math.DoubleMath;
import com.google.common.primitives.Doubles;

public abstract class Maths {
	
	private static final Median median = new Median();

	// XXX This ignores points that have a NaN rmsd. This may not be desired.
	public static double rmsd(Iterator<? extends Number> iter_a, Iterator<? extends Number> iter_b) {
		SummStats mean = SummStats.on();
		while (iter_a.hasNext() && iter_b.hasNext()) {
			double a = iter_a.next().doubleValue();
			double b = iter_b.next().doubleValue();
			if (!Double.isNaN(a) && !Double.isNaN(b))
				mean.add((a - b) * (a - b));
		}
		return Math.sqrt(mean.mean());
	}
	
	public static double rmsd(Iterable<? extends Number> iter_a, Iterable<? extends Number> iter_b) {
		return rmsd(iter_a.iterator(), iter_b.iterator());
	}
	
	public static Iterator<Double> logRatio(final Iterator<? extends Number> values) {
		if (!values.hasNext())
			return ImmutableList.<Double> of().iterator();
		else
			return new UnmodifiableIterator<Double>() {
				double previous = Math.log(values.next().doubleValue());
			
				@Override
				public boolean hasNext() {
					return values.hasNext();
				}

				@Override
				public Double next() {
					double next = Math.log(values.next().doubleValue());
					double logr = next - previous;
					previous = next;
					return logr;
				}			
		};
	}
	
	public static Iterable<Double> logRatio(final Iterable<? extends Number> values) {
		return new Iterable<Double> () {
			@Override public Iterator<Double> iterator() {
				return logRatio(values.iterator());
			}
		};
	}
	
	public static Iterable<Double> logRatio(double... values) {
		return logRatio(Doubles.asList(values));
	}
	
	public static double stddev(Iterable<? extends Number> values) {
		return SummStats.on(values).stddev();
	}
	
	public static double stddev(double... values) {
		return stddev(Doubles.asList(values));
	}

	/** Quantize "n" in increments of "quanta" */
	public static int quantize(int n, int quanta) {
		return quanta * DoubleMath.roundToInt(n / (double) quanta, HALF_EVEN);
	}

	public static double quantize(double n, double quanta) {
		// Floor instead of round to prevent the case in round which converts
		// NaN and Inf to 0
		return quanta * DoubleMath.roundToInt(n / quanta, HALF_EVEN);
	}

	public static int bound(int num, int lower, int upper) {
		return Math.max(Math.min(num, upper), lower);
	}

	public static double bound(double num, double lower, double upper) {
		return Math.max(Math.min(num, upper), lower);
	}
	
	public static double median(double... values) {
		return median.evaluate(values);
	}
	
	public static double median(Iterable<? extends Number> values) {
		return median(Doubles.toArray(ImmutableList.copyOf(values)));
	}
}
