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
		return new SummStats(0, 0, 0, Double.NaN, Double.NaN).add(values);
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
		if (Double.isNaN(min) || value < min)
			min = value;
		if (Double.isNaN(max) || value > max)
			max = value;
		return this;
	}
	
	public SummStats addNTimes(double value, long times) {
		checkArgument(times > 0);
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
		return mean;
	}
	
	/**
	 * Return the sample variance of all data added so far
	 * @return
	 */
	public double variance() {
		return squaredError / (n - 1);
	}
	
	/**
	 * Return the sample standard deviation of all data added so far
	 */
	public double stddev() {
		return Math.sqrt(variance());
	}
	
	public double sum() {
		return mean * n;
	}
	
	public double min() {
		return min;
	}
	
	public double max() {
		return max;
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
		if (Double.isNaN(min) || other.min < min)
			min = other.min;
		if (Double.isNaN(max) || other.max > max)
			max = other.max;
		return this;
	}
}
