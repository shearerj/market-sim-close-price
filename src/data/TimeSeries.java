package data;

import event.*;
import systemmanager.Consts;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.ArrayList;

import org.apache.commons.lang3.ArrayUtils;


/**
 * Storage for time series objects.
 * 
 * Keeps track of the data points that are added to the time series
 * and the time they're added. Also expands it into a full list of values
 * (with an element for each time).
 * 
 * When filling in values, a new element is added to series if the time
 * for the new data point is different from the most recent time recorded.
 * 
 * @author ewah
 *
 */
public class TimeSeries {

	private List<TimeStamp> times;
	private List<Double> points;	// recorded data points
	private List<Double> series;	// fully expanded
	private TimeStamp prevTime;
	
	public TimeSeries() {
		times = new ArrayList<TimeStamp>();
		points = new ArrayList<Double>();
		series = new ArrayList<Double>();
		
		prevTime = null;
	}
	
	public List<TimeStamp> getTimes() {
		return times;
	}
	
	public List<Double> getPoints() {
		return points;
	}
	
	public List<Double> getValues() {
		return series;
	}
	
	public double[] getArray() {
		return ArrayUtils.toPrimitive(series.toArray(new Double[series.size()]));
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return series.toString();
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
			return ArrayUtils.toPrimitive(series.subList(0, time+1).
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
			array[i] = series.get((i+1) * window - 1); 
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
		// determine value to fill from prevTime to ts
		if (startTime == null) {
			// this is first element in list, so initialize startTime
			// fill in with NaNs 
			startTime = new TimeStamp(0);
			val = Consts.DOUBLE_NAN;
		} else {
			// fill in with last element seen so far
			val = points.get(indexofLastPoint());
		}
		// fill up to but not including ts
		for (long i = startTime.longValue(); i < ts.longValue(); i++) {
			series.add(val);
		}
		if (!prevTime.equals(ts)) {
			// update prevTime with next time if point at a new time
			prevTime = ts;
			times.add(ts);
			points.add(point);
			series.add(point);
		} else {
			// updated the last added point, leave times the same
			points.set(indexofLastPoint(), point);
			series.set(series.size()-1, point);
		}
	}
	
	private int indexofLastPoint() {
		return times.size()-1;
	}
	
	public TimeStamp lastTime() {
		return times.get(indexofLastPoint());
	}
	
	
	/**
	 * Writes entire time series to file. First element is time 0.
	 * 
	 * @param filename
	 */
	public void writeSeriesToFile(String filename) {
		try {
			File f = new File(filename);
			FileOutputStream os = new FileOutputStream(f);
			OutputStreamWriter osw = new OutputStreamWriter(os, "UTF-8");
			BufferedWriter bw = new BufferedWriter(osw);
			for (Double d : series) {
				bw.write(d.toString());
				bw.newLine();
			}
			bw.close();
			osw.close();
			os.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Writes times & points to CSV.
	 * 
	 * @param filename
	 */
	public void writePointsToFile(String filename) {
		try {
			File f = new File(filename);
			FileOutputStream os = new FileOutputStream(f);
			OutputStreamWriter osw = new OutputStreamWriter(os, "UTF-8");
			BufferedWriter bw = new BufferedWriter(osw);
			// insert column headers
			bw.write("time,point");
			bw.newLine();
			for (int i = 0; i < times.size(); i++) {
				String s = times.get(i) + "," + points.size();
				bw.write(s);
				bw.newLine();
			}
			bw.close();
			osw.close();
			os.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	
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
