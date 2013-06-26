package data;

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
	 * Constructor using given random seed.
	 * @param ts TimeStamp with which to start arrivals
	 * @param lambda arrival rate
	 * @param rand
	 */
	public ArrivalTime(TimeStamp ts, double lambda, Random rand) {
		time = ts;
		this.lambda = lambda;
		this.rand = rand;
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
	 * @param idx
	 * @return arrival time at index idx
	 */
	public TimeStamp getArrivalTime(int idx) {
		return arrivalTimes.get(idx);
	}
	
	/**
	 * @return list of all intervals (1 for each arrival)
	 */
	public ArrayList<TimeStamp> getIntervals() {
		return intervals;
	}
}
