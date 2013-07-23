package data;

import event.*;

import java.util.ArrayList;

import utils.RandPlus;

/**
 * Class to store and compute agent arrival times.
 * 
 * @author ewah
 */
// TODO Remove and replace with Generator<TimeStamp>
public class ArrivalTime {

	private TimeStamp time;
	private double lambda;
	private RandPlus rand;
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
		rand = new RandPlus();
		intervals = new ArrayList<TimeStamp>();
		arrivalTimes = new ArrayList<TimeStamp>();
	}
	
	/**
	 * Constructor using given random seed.
	 * @param ts TimeStamp with which to start arrivals
	 * @param lambda arrival rate
	 * @param rand
	 */
	public ArrivalTime(TimeStamp ts, double lambda, RandPlus rand) {
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
		double tmp = rand.nextExponential(lambda);
		TimeStamp interval = new TimeStamp((int) Math.ceil(tmp));
		time = time.plus(interval);
		intervals.add(interval);
		arrivalTimes.add(time);
		return time;
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
