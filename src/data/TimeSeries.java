package data;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;

import event.TimeStamp;

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
public class TimeSeries {
	// TODO - Implement as a Map. Doesn't need to be a map, but need to make
	// sure that time in variant is there. Down sample methods could be better
	// implemented instead of just taking from the series. Could also do
	// interpolation.
	private List<TimeStamp> times;
	private List<Double> points; // recorded data points parallel with times
	private List<Double> series; // fully expanded

	public TimeSeries() {
		times = new ArrayList<TimeStamp>();
		points = new ArrayList<Double>();
		series = new ArrayList<Double>();

		// initialized
		times.add(new TimeStamp(0));
		points.add(Double.NaN);
		series.add(Double.NaN);
	}
//
//	public boolean isEmpty() {
//		return times.isEmpty();
//	}

	@Override
	public String toString() {
		return series.toString();
	}

	/**
	 * Add a data point (TimeStamp, Double) to containers.
	 */
	public void add(TimeStamp ts, double point) {

		// most recent time with associated point
		TimeStamp lastTime = lastTime();

		if (lastTime.after(ts)) {
			throw new IllegalArgumentException("Can't add time before last time");
		} else if (lastTime.equals(ts)) {
			points.set(indexOfLastPoint(), point);
			times.set(indexOfLastPoint(), ts);
			series.set(series.size() - 1, point);

		} else {
			// fill in with last element seen so far
			for (long i = lastTime.longValue() + 1; i < ts.longValue(); i++) {
				series.add(points.get(indexOfLastPoint()));
			}
			times.add(ts);
			points.add(point);
			series.add(point);
		}
	}

	private int indexOfLastPoint() {
		return times.size() - 1;
	}

	public TimeStamp lastTime() {
		return times.get(indexOfLastPoint());
	}

	/********************************************
	 * Extracting arrays
	 *******************************************/

	/**
	 * Returns subarray of values from 0 to maxTime, inclusive. Fills in the end
	 * with the last available value, so the returned array is always of length
	 * maxTime.
	 * 
	 * @param maxTime
	 *            (inclusive)
	 * @return
	 */
	public double[] getArrayUpTo(long maxTime) {
		if (maxTime <= lastTime().longValue()) {
			// truncate at maxTime
			int maxSize = (int) maxTime;
			return ArrayUtils.toPrimitive(series.subList(0, maxSize + 1).toArray(
					new Double[maxSize + 1]));
		} else {
			// add 1 to lastIndex because start at time 0
			int lastIndex = (int) lastTime().longValue() + 1;
			int maxSize = (int) maxTime + 1;
			double[] array = new double[maxSize]; // since first time is 0
			for (int i = 0; i < lastIndex; i++) {
				array[i] = series.get(i);
			}
			for (int i = lastIndex; i < maxSize; i++) {
				array[i] = series.get(lastIndex - 1);
			}
			return array;
		}
	}

	/**
	 * Sample values according to specified period & return array. Returns value
	 * at the END of each period. Example: For sampling interval of 100, the
	 * first item in the sampled array would be the 100th element.
	 * 
	 * Will also fill in values up to maxTime, if the last time stored is before
	 * maxTime.
	 * 
	 * If period == 0, then will include every time stamp.
	 * 
	 * @param period
	 * @param maxTime
	 *            (inclusive)
	 * @return
	 */
	public double[] getSampledArray(int period, long maxTime) {
		if (period <= 0) {
			return getArrayUpTo(maxTime);
		}
		long lastIndex = maxTime;
		if (maxTime > lastTime().longValue()) {
			lastIndex = lastTime().longValue();
		}
		// add 1 to lastIndex because start at time 0
		int size = (int) Math.floor((lastIndex + 1) / period);
		int maxSize = (int) Math.floor((maxTime + 1) / period);
		double[] array = new double[maxSize];

		// sample at end of window, not at beginning
		for (int i = 0; i < size; i++) {
			array[i] = series.get((i + 1) * period - 1);
		}

		// if lastIndex is before maxTime, fill in up to maxTime
		// only fill in if size > 0, i.e. if a value to fill across exists
		if (lastIndex < maxTime && size > 0) {
			for (int i = size; i < maxSize; i++) {
				array[i] = series.get(size - 1);
			}
		}
		return array;
	}

	/**
	 * For use with DescriptiveStatistics objects, which cannot ignore NaNs.
	 * 
	 * @param period
	 * @param maxTime
	 * @return
	 */
	// FIXME necessary to have it return an array? Also, possible to move this
	// functionality into DSPlus class?
	public double[] getSampledArrayWithoutNaNs(int period, long maxTime) {
		int freq = period;
		if (period <= 0)
			freq = 1;
		long lastIndex = maxTime;
		if (maxTime > lastTime().longValue()) {
			lastIndex = lastTime().longValue();
		}
		// add 1 to lastIndex because start at time 0
		int size = (int) Math.round((lastIndex + 1) / freq);
		int maxSize = (int) Math.round((maxTime + 1) / freq);

		List<Double> arr = new ArrayList<Double>();
		// sample at end of window, not at beginning
		for (int i = freq - 1; i <= (int) lastIndex; i += freq) {
			double x = series.get(i);
			if (!Double.isNaN(x))
				arr.add(x);
		}

		// if lastIndex is before maxTime, fill in up to maxTime
		if (lastIndex < maxTime && size > 0) {
			for (int i = size; i < maxSize; i++) {
				double x = series.get(size - 1);
				if (!Double.isNaN(x))
					arr.add(x);
			}
		}
		return ArrayUtils.toPrimitive(arr.toArray(new Double[arr.size()]));
	}

}
