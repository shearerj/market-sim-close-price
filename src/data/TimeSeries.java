package data;

import event.*;

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
		points.add(Double.NaN);
		series.add(Double.NaN);
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
	
	/********************************************
	 * Extracting arrays
	 *******************************************/
	
	/**
	 * Returns subarray of values from 0 to maxTime, inclusive.
	 * Fills in the end with the last available value, so the 
	 * returned array is always of length maxTime.
	 * 
	 * @param maxTime (inclusive)
	 * @return
	 */
	public double[] getArrayUpTo(long maxTime) {
		if (maxTime <= lastTime().longValue()) {
			// truncate at maxTime
			int maxSize = (int) maxTime;
			return ArrayUtils.toPrimitive(series.subList(0, maxSize+1).
					toArray(new Double[maxSize+1]));
		} else {
			// add 1 to lastIndex because start at time 0
			int lastIndex = (int) lastTime().longValue() + 1;
			int maxSize = (int) maxTime + 1;
			double[] array = new double[maxSize];	// since first time is 0
			for (int i = 0; i < lastIndex; i++) {
				array[i] = series.get(i);
			}
			for (int i = lastIndex; i < maxSize; i++) {
				array[i] = series.get(lastIndex-1);
			}
			return array;
		}
	}
	
	/**
	 * Sample values according to specified period & return array.
	 * Returns value at the END of each period. Example: For sampling
	 * interval of 100, the first item in the sampled array would be the
	 * 100th element.
	 * 
	 * Will also fill in values up to maxTime, if the last time stored
	 * is before maxTime.
	 * 
	 * If period == 0, then will include every time stamp.
	 * 
	 * @param period
	 * @param maxTime (inclusive)
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
			array[i] = series.get((i+1)*period - 1); 
		}
		
		// if lastIndex is before maxTime, fill in up to maxTime
		// only fill in if size > 0, i.e. if a value to fill across exists
		if (lastIndex < maxTime && size > 0) {
			for (int i = size; i < maxSize; i++) {
				array[i] = series.get(size-1);
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
		if (period <= 0) freq = 1;
		long lastIndex = maxTime; 
		if (maxTime > lastTime().longValue()) {
			lastIndex = lastTime().longValue();
		}
		// add 1 to lastIndex because start at time 0
		int size = (int) Math.round((lastIndex + 1) / freq);
		int maxSize = (int) Math.round((maxTime + 1) / freq);
		
		List<Double> arr = new ArrayList<Double>();
		// sample at end of window, not at beginning
		for (int i = freq-1; i <= (int) lastIndex; i += freq) {
			double x = series.get(i);
			if (!Double.isNaN(x)) arr.add(x);
		}
		
		// if lastIndex is before maxTime, fill in up to maxTime
		if (lastIndex < maxTime && size > 0) {
			for (int i = size; i < maxSize; i++) {
				double x = series.get(size - 1);
				if (!Double.isNaN(x)) arr.add(x);
			}
		}
		return ArrayUtils.toPrimitive(arr.toArray(new Double[arr.size()]));
	}
	
	
	
	/********************************************
	 * Writing to output files
	 *******************************************/
	
	/**
	 * Writes entire time series to file. First element is time 0.
	 * 
	 * @param filename
	 */
	public void writeSeriesToFile(File file) {
		try {
			if (!file.isFile()) file.createNewFile();
			FileOutputStream os = new FileOutputStream(file);
			OutputStreamWriter osw = new OutputStreamWriter(os, "UTF-8");
			BufferedWriter bw = new BufferedWriter(osw);
			for (double d : series) {
				bw.write(Double.toString(d));
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
			if (!f.isFile()) f.createNewFile();
			FileOutputStream os = new FileOutputStream(f);
			OutputStreamWriter osw = new OutputStreamWriter(os, "UTF-8");
			BufferedWriter bw = new BufferedWriter(osw);
			double[] subarray = this.getSampledArray(period, maxTime);
			for (int i = 0; i < subarray.length; i++) {
				bw.write(Double.toString(subarray[i]));
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
			if (!f.isFile()) f.createNewFile();
			FileOutputStream os = new FileOutputStream(f);
			OutputStreamWriter osw = new OutputStreamWriter(os, "UTF-8");
			BufferedWriter bw = new BufferedWriter(osw);
			// insert column headers
			bw.write("time,point");
			bw.newLine();
			for (int i = 0; i < times.size(); i++) {
				String s = times.get(i) + "," + points.get(i);
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
