package data;

import event.*;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.TreeSet;

import org.apache.commons.lang3.ArrayUtils;

import systemmanager.Consts;

/**
 * Storage for time series objects.
 * 
 * Keeps track of the data points that are added to the time series
 * and the time they're added. Also expands it into a full list of values
 * (with an element for each time).
 * 
 * When filling in values, each new point is when the time series changes
 * (it is a step function).	
 * 
 * @author ewah
 *
 */
public class TimeSeries {

	private List<TimeStamp> times;
	private List<Double> points;
	private List<Double> values;
	private TimeStamp prevTime;
	
	public TimeSeries() {
		times = new ArrayList<TimeStamp>();
		points = new ArrayList<Double>();
		values = new ArrayList<Double>();
		
		prevTime = null;
	}
	
	public List<TimeStamp> getTimes() {
		return times;
	}
	
	public List<Double> getPoints() {
		return points;
	}
	
	public List<Double> getValues() {
		return values;
	}
	
	public double[] getArray() {
		return ArrayUtils.toPrimitive(values.toArray(new Double[values.size()]));
	}
	
	/**
	 * Returns subarray of values from 0 to maxTime, inclusive.
	 * @param maxTime
	 * @return
	 */
	public double[] getSubArray(long maxTime) {
		if (maxTime >= lastTime().longValue()) {
			return getArray();
		} else {
			int time = (int) maxTime;
			return ArrayUtils.toPrimitive(values.subList(0, time+1).
					toArray(new Double[time+1]));
		}
	}
	
	/**
	 * Sample values according to specified window size & return array.
	 * @param window
	 * @param maxTime
	 * @return
	 */
	public double[] getSampleArray(int window, long maxTime) {
		if (window <= 0) {
			return getSubArray(maxTime);
		}
		long endIndex = maxTime; 
		if (maxTime > lastTime().longValue()) {
			endIndex = lastTime().longValue();
		}
		// add 1 to endIndex because start at time 0
		int newSize = (int) Math.floor((endIndex + 1) / window);
		double[] array = new double[newSize];
		
		// sample at end of window, not at beginning
		for (int i = 0; i < newSize; i++) {
			array[i] = values.get((i+1) * window - 1); 
		}
		return array;
	}
	
	
	/**
	 * Add a data point (TimeStamp, double) to containers.
	 * @param ts
	 * @param point
	 */
	public void add(TimeStamp ts, double point) {
		TimeStamp startTime = prevTime;
		Double val = null;
		if (startTime == null) {
			// this is first element in list, so initialize startTime
			// fill in with NaNs 
			startTime = new TimeStamp(0);
			val = Consts.DOUBLE_NAN;
		} else {
			// otherwise fill in with last element seen so far
			val = points.get(size()-1);
		}
		// fill up to but not including ts
		for (long i = startTime.longValue(); i < ts.longValue(); i++) {
			values.add(val);
		}
		prevTime = ts;		// update prevTime with next time

		times.add(ts);
		points.add(point);
		values.add(point);
	}
	
	public int size() {
		return times.size();
	}
	
	public TimeStamp lastTime() {
		return times.get(size()-1);
	}
	
	
	// TODO how to output the time series so can do unit-testing/verification?
	
	
//	/**
//	 * Same as extractTimeSeries, but will remove any undefined values from the
//	 * beginning of the array and cut it off at maxTime.
//	 * 
//	 * @param map
//	 * @param maxTime
//	 * @return
//	 */
//	public double[] truncateTimeSeries(HashMap<TimeStamp, Double> map,
//			long maxTime) {
//
//		// Have to sort the TimeStamps since not necessarily sorted in HashMap
//		TreeSet<TimeStamp> times = new TreeSet<TimeStamp>();
//		ArrayList<TimeStamp> keys = new ArrayList<TimeStamp>(map.keySet());
//		for (Iterator<TimeStamp> i = keys.iterator(); i.hasNext();) {
//			TimeStamp t = i.next();
//			if (t != null)
//				times.add(t);
//		}
//
//		int cnt = 0;
//		TimeStamp prevTime = null;
//		double[] vals = new double[(int) maxTime];
//		for (Iterator<TimeStamp> it = times.iterator(); it.hasNext();) {
//			if (prevTime == null) {
//				// if prevTime has not been defined yet, set as the first time
//				// where value measured
//				prevTime = it.next();
//			} else {
//				// next Time is the next time at which to extend the time series
//				TimeStamp nextTime = it.next();
//				// fill in the vals array, but only for segments where it is not
//				// undefined
//				if (map.get(prevTime).intValue() != Consts.INF_PRICE) {
//					for (int i = (int) prevTime.longValue(); i < nextTime
//							.longValue() && i < maxTime; i++) {
//						vals[cnt] = map.get(prevTime); // fill in with prior
//														// value, up to maxTime
//						cnt++;
//					}
//				}
//				prevTime = nextTime;
//			}
//		}
//		// fill in to end of array
//		if (!prevTime.after(new TimeStamp(maxTime))) {
//			if (map.get(prevTime).intValue() != Consts.INF_PRICE) {
//				for (int i = (int) prevTime.longValue(); i < maxTime; i++) {
//					// get last inserted value and insert
//					vals[cnt] = map.get(prevTime);
//					cnt++;
//				}
//			}
//		}
//		// Must resize vals
//		double[] valsMod = new double[cnt];
//		for (int i = 0; i < valsMod.length; i++) {
//			valsMod[i] = vals[i];
//		}
//		return valsMod;
//	}

	
}
