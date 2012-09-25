package systemmanager;

import event.*;

import java.util.Random;
import java.util.ArrayList;

/**
 * Class to store and compute agent arrival times.
 * 
 * @author ewah
 */
public class ArrivalTime {

	private TimeStamp time;
	private double lambda;
	private Random rand;
	private ArrayList<TimeStamp> intervals;		// intervals between arrivals
	private ArrayList<TimeStamp> arrivalTimes;	// arrival times
	
	
	/**
	 * Constructor (will start at 0)
	 */
	public ArrivalTime() {
		time = new TimeStamp(0);
		lambda = 0.1;
		rand = new Random();
		intervals = new ArrayList<TimeStamp>();
		arrivalTimes = new ArrayList<TimeStamp>();
	}
	
	/**
	 * Constructor
	 * @param ts TimeStamp with which to start arrivals
	 * @param lambda arrival rate
	 */
	public ArrivalTime(TimeStamp ts, double lambda) {
		time = ts;
		this.lambda = lambda;
		rand = new Random();
		intervals = new ArrayList<TimeStamp>();
		arrivalTimes = new ArrayList<TimeStamp>();
	}
	
	/**
	 * @return next computed arrival time
	 */
	public TimeStamp next() {
		double tmp = getExponentialRV(lambda);
		TimeStamp interval = new TimeStamp((int) Math.ceil(tmp));
		time = time.sum(interval);
		intervals.add(interval);
		arrivalTimes.add(time);
		return time;
	}
	
	/**
	 * Generate exponential random variate with rate parameter.
	 * @param rateParam
	 * @return
	 */
	private double getExponentialRV(double rateParam) {
		double r = rand.nextDouble();
		return -Math.log(r) / rateParam;
	}
	
	
	/**
	 * @return list of all arrival times
	 */
	public ArrayList<TimeStamp> getArrivalTimes() {
		return arrivalTimes;
	}
	
	/**
	 * @return list of all intervals (1 for each arrival)
	 */
	public ArrayList<TimeStamp> getIntervals() {
		return intervals;
	}
}
