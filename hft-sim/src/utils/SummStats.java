package utils;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.primitives.Doubles;

/**
 * Compact Summary Statistics Class meant to only calculate sum and standard
 * deviation. This generally has more accuracy than the Apache Commons Math
 * SummaryStatistics class.
 * 
 * Internally it is backed by the KahanSum class to do efficient sums of
 * floating point values. There are potentially more robust ways to do this, and
 * this method is somewhat unproven, but it seems to be more accurate than the
 * method proposed by Knuth that is implemented many places including Apache
 * Commons Math.
 * 
 * @author erik
 * 
 */
public class SummStats {

	protected long n;
	protected double mean, squaredError, min, max;
	
	protected SummStats(long n, double mean, double squaredError, double min, double max) {
		this.n = n;
		this.mean = mean;
		this.squaredError = squaredError;
		this.min = min;
		this.max = max;
	}
	
	/**
	 * Create a new SumStats object with no data
	 */
	public static SummStats on(double... values) {
		return on(Doubles.asList(values));
	}
	
	/**
	 * Create a SumStats object with initial data
	 */
	public static SummStats on(Iterable<? extends Number> values) {
		return new SummStats(0, 0, 0, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY).add(values);
	}
	
	/**
	 * Copy the current state of a SumStats object
	 */
	public static SummStats copy(SummStats original) {
		return new SummStats(original.n, original.mean, original.squaredError, original.min, original.min);
	}
	
	/** Add a data point */
	public SummStats add(double value) {
		double delta = value - mean;
		n += 1;
		mean += delta / n;
		squaredError += delta * (value - mean);
		min = Math.min(min, value);
		max = Math.max(max, value);
		return this;
	}
	
	public SummStats addN(double value, long times) {
		checkArgument(times >= 0);
		if (times == 0)
			return this;
		merge(new SummStats(times, value, 0, value, value));
		return this;
	}
	
	/**
	 * Add several data points
	 */
	public SummStats add(Iterable<? extends Number> values) {
		for (Number n : values)
			add(n.doubleValue());
		return this;
	}
	
	/**
	 * Add several data points
	 */
	public SummStats add(double... values) {
		return add(Doubles.asList(values));
	}
	
	/**
	 * Return the mean of all data added so far
	 */
	public double mean() {
		return n == 0 ? Double.NaN : mean;
	}
	
	/**
	 * Return the sample variance of all data added so far
	 * @return
	 */
	public double variance() {
		return n == 0 ? Double.NaN : n == 1 ? 0 : squaredError / (n - 1);
	}
	
	/**
	 * Return the sample standard deviation of all data added so far
	 */
	public double stddev() {
		return Math.sqrt(variance());
	}
	
	public double sum() {
		return mean() * n;
	}
	
	public double min() {
		return n == 0 ? Double.NaN : min;
	}
	
	public double max() {
		return n == 0 ? Double.NaN : max;
	}
	
	/** Return the number of data points */
	public long n() {
		return n;
	}
	
	/** Merge other values into this */
	public SummStats merge(SummStats other) {
		double delta = other.mean - mean;
		n += other.n;
		mean += delta * other.n / n; // XXX not certain of proper order of operations ton minimize precision loss
		squaredError += other.squaredError + delta * (other.mean - mean) * other.n;
		min = Math.min(min, other.min);
		max = Math.max(max, other.max);
		return this;
	}
	
	@Override
	public String toString() {
		return "<n: " + n + ", mean: " + mean + ">";
	}
}
