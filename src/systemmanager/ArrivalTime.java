package systemmanager;

import event.*;

import java.util.Random;

/**
 * Class to store and compute agent arrival times.
 * 
 * @author ewah
 */
public class ArrivalTime {

	private TimeStamp time;
	private double lambda;
	private Random rand;
	
	public ArrivalTime() {
		time = new TimeStamp(0);
		lambda = 0.1;
		rand = new Random();
	}
	
	public ArrivalTime(TimeStamp ts, double lambda) {
		time = ts;
		this.lambda = lambda;
		rand = new Random();
	}
	
	public TimeStamp next() {
		double tmp = 100 * getExponentialRV(lambda);
		TimeStamp waitTime = new TimeStamp((int) Math.ceil(tmp));
		time = time.sum(waitTime);
		return time;
	}
	
	// generate exponential random variate with rate parameter
	private double getExponentialRV(double rateParam) {
		double r = rand.nextDouble();
		return -Math.log(r) / rateParam;
	}
}
