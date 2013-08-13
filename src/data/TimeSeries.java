package data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;

/**
 * Storage for time series objects.
 * 
 * Keeps track of the data points that are added to the time series and the time
 * they're added. Also expands it into a full list of values (with an element
 * for each time).
 * 
 * When filling in values, a new element is added to series if the time for the
 * new data point is different from the most recent time recorded.
 * 
 * @author ewah
 * 
 */
public class TimeSeries implements Serializable {
	
	private static final long serialVersionUID = 7835744389750549565L;

	protected static double DEFAULT = Double.NaN;
	
	protected final List<Integer> times;
	protected final List<Double> points; // recorded data points parallel with times

	public TimeSeries() {
		times = new ArrayList<Integer>(Collections.singleton(0));
		points = new ArrayList<Double>(Collections.singleton(DEFAULT));
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append('[');
		Iterator<Integer> itt = times.iterator();
		Iterator<Double> itd = points.iterator();
		while(itt.hasNext() && itd.hasNext())
			sb.append('(').append(itt.next()).append(", ").append(itd.next()).append("), ");
		sb.delete(sb.length() - 2, sb.length()).append(']');
		return sb.toString();
	}

	/**
	 * Add a data point (int, double) to container
	 */
	public void add(int ts, double point) {
		int lastTime = times.get(times.size() - 1);

		if (ts < lastTime) {
			throw new IllegalArgumentException("Can't add time before last time");
		} else if (ts == lastTime) {
			points.set(points.size() - 1, point);
			times.set(times.size() - 1, ts);
		} else {
			times.add(ts);
			points.add(point);
		}
	}

	/**
	 * Sample values according to specified period & return a DescriptiveStatistics object for the
	 * sampled values. Returns value at the END of each period. Example: For sampling interval of
	 * 100, the first item in the sampled array would be the 100th element (index 99).
	 * 
	 * Will also fill in values up to (not including) maxTime, if the last time stored is before
	 * maxTime.
	 * 
	 * If period == 1, then will include every time stamp.
	 */
	public DSPlus getSampledStats(int period, int maxTime) {
		return new DSPlus(sample(period, maxTime));
	}

	/**
	 * Same as getSampledStats, but removes all NaNs
	 */
	public DSPlus getSampledStatsSansNaNs(int period, int maxTime) {
		return new DSPlus(sansNans(sample(period, maxTime)));
	}
	
	/**
	 * Same as getSampledStatsSansNaNs, but this returns a descriptive statistics object seeded with
	 * the log ratio of adjacent sampled values. NaNs are removed
	 */
	public DSPlus getSampledLogRatioStatsSansNaNs(int period, int maxTime) {
		return new DSPlus(sansNans(logRatio(sample(period, maxTime))));
	}
	
	/**
	 * Sample values according to specified period & return array. Returns value at the END of each
	 * period. Example: For sampling interval of 100, the first item in the sampled array would be
	 * the 100th element (index 99).
	 * 
	 * Will also fill in values up to (not including) maxTime, if the last time stored is before
	 * maxTime.
	 * 
	 * If period == 1, then will include every time stamp.
	 */
	protected double[] sample(int period, int maxTime) {
		if (period <= 0)
			throw new IllegalArgumentException("Period must be positive");
		
		List<Double> sampled = new ArrayList<Double>(points.size());
		
		Iterator<Integer> itt = times.iterator();
		Iterator<Double> itd = points.iterator();
		int time = period - 1; // Sample at end of period
		int nextTime;
		double point = Double.NaN;
		while(itt.hasNext() && itd.hasNext() && time < maxTime) {
			nextTime = itt.next();
			while (time < nextTime && time < maxTime) {
				sampled.add(point);
				time += period;
			}
			point = itd.next();
		}
		while (time < maxTime) {
			sampled.add(point);
			time += period;
		}
		return ArrayUtils.toPrimitive(sampled.toArray(new Double[sampled.size()]));
	}
	
	/**
	 * Removes NaNs from an array
	 * 
	 * [4, 6, NaN, 10] -> [4, 6, 10]
	 */
	protected final static double[] sansNans(double[] array) {
		int nonNans = 0;
		for (double d : array)
			if (!Double.isNaN(d)) nonNans++;
		double[] sansNans = new double[nonNans];
		int i = 0;
		for (double d : array)
			if (!Double.isNaN(d)) sansNans[i++] = d;
		return sansNans;
	}

	/**
	 * Returns a new array with one less element, where each element is the logRatio
	 * between the two adjacent elements
	 */
	protected final static double[] logRatio(double[] array) {
		double[] logr = new double[array.length - 1];
		for (int i = 0; i < logr.length; i++)
			logr[i] = Math.log(array[i + 1]) - Math.log(array[i]);
		return logr;
	}

}
