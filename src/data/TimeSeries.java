package data;

import event.*;
import systemmanager.Consts;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
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
	
	public TimeSeries() {
		times = new ArrayList<TimeStamp>();
		points = new ArrayList<Double>();
		series = new ArrayList<Double>();
		
		// initialized
		times.add(new TimeStamp(0));
		points.add(Consts.DOUBLE_NAN);
		series.add(Consts.DOUBLE_NAN);
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
	public double[] getArrayUpTo(long maxTime) {
		if (maxTime >= lastTime().longValue()) {
			return getArray();
		} else {
			int time = (int) maxTime;
			return ArrayUtils.toPrimitive(series.subList(0, time+1).
					toArray(new Double[time+1]));
		}
	}
	
	/**
	 * Sample values according to specified period & return array.
	 * @param period
	 * @param maxTime
	 * @return
	 */
	public double[] getSampledArray(int period, long maxTime) {
		if (period <= 0) {
			return getArrayUpTo(maxTime);
		}
		long endIndex = maxTime; 
		if (maxTime > lastTime().longValue()) {
			endIndex = lastTime().longValue();
		}
		// add 1 to endIndex because start at time 0
		int newSize = (int) Math.floor((endIndex + 1) / period);
		double[] array = new double[newSize];
		
		// sample at end of window, not at beginning
		for (int i = 0; i < newSize; i++) {
			array[i] = series.get((i+1) * period - 1); 
		}
		return array;
	}
	
	
	/**
	 * Add a data point (TimeStamp, Double) to containers.
	 * 
	 * @param ts
	 * @param point
	 */
	public void add(TimeStamp ts, double point) {

		// most recent time with associated point
		TimeStamp startTime = times.get(indexOfLastPoint());
		
		if (startTime.equals(ts)) {
			points.set(indexOfLastPoint(), point);
			times.set(indexOfLastPoint(), ts);
			series.set(series.size()-1, point);
			
		} else {
			// fill in with last element seen so far
			for (long i = startTime.longValue()+1; i < ts.longValue(); i++) {
				series.add(points.get(indexOfLastPoint()));
			}
			times.add(ts);
			points.add(point);
			series.add(point);
		}
	}
	
	/**
	 * @return
	 */
	private int indexOfLastPoint() {
		return times.size()-1;
	}
	
	/**
	 * @return
	 */
	public TimeStamp lastTime() {
		return times.get(indexOfLastPoint());
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
	 * Write a sampled version of the time series to a file.
	 * @param period
	 * @param maxTime
	 * @param filename
	 */
	public void writeSampledSeriesToFile(int period, long maxTime, String filename) {
		try {
			File f = new File(filename);
			FileOutputStream os = new FileOutputStream(f);
			OutputStreamWriter osw = new OutputStreamWriter(os, "UTF-8");
			BufferedWriter bw = new BufferedWriter(osw);
			double[] subarray = this.getSampledArray(period, maxTime);
			for (int i = 0; i < subarray.length; i++) {
				bw.write(new Double(subarray[i]).toString());
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
	public void writePointsToCSFile(String filename) {
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
	
}
