package edu.umich.srg.marketsim.strategy;

import static com.google.common.base.Preconditions.*;

import edu.umich.srg.marketsim.Price;
import edu.umich.srg.marketsim.TimeStamp;

// FIXME Test

public abstract class GaussianHMMFundamentalEstimator {
	
	long lastUpdate;
	private final double observationVariance, transactionVariance;
	double posteriorMean, posteriorVariance;

	private GaussianHMMFundamentalEstimator(double initialMean, double observationVariance, double transactionVariance) {
		this.observationVariance = observationVariance;
		this.transactionVariance = transactionVariance;
		this.posteriorMean = initialMean;
		this.lastUpdate = 0;
		this.posteriorVariance = 0;
	}
	
	public static GaussianHMMFundamentalEstimator create(long simLength, double mean, double meanReversion,
			double shockVariance, double observationVariance, double transactionVariance) {
		checkArgument(shockVariance > 0);
		checkArgument(observationVariance > 0);
		checkArgument(transactionVariance > 0);
		if (meanReversion == 0)
			return new RandomWalk(mean, shockVariance, observationVariance, transactionVariance);
		else
			return new MeanReverting(simLength, mean, meanReversion, shockVariance, observationVariance, transactionVariance);
	}
	
	public void addFundamentalObservation(TimeStamp currentTime, Price observation) {
		updateTime(currentTime.get());
		observationUpdate(observation.doubleValue(), observationVariance, 1);
	}
	
	public void addTransactionObservation(TimeStamp currentTime, Price observation, int quantity) {
		updateTime(currentTime.get());
		observationUpdate(observation.doubleValue(), transactionVariance, quantity);
	}
	
	abstract void updateTime(long currentTime);
	
	private void observationUpdate(double observation, double variance, int times) {
		double varToPower = Math.pow(variance, times - 1);
		posteriorMean = (posteriorMean * variance * varToPower + times * observation * varToPower * posteriorVariance) / (varToPower * variance + times * varToPower * posteriorVariance);
		posteriorVariance = posteriorVariance * varToPower * variance / (varToPower * variance + times * varToPower * posteriorVariance);
	}
	
	public abstract double estimate();
	
	private static class RandomWalk extends GaussianHMMFundamentalEstimator {

		private final double shockVariance;
		
		private RandomWalk(double mean, double shockVariance, double observationVariance, double transactionVariance) {
			super(mean, observationVariance, transactionVariance);
			this.shockVariance = shockVariance;
		}
		
		@Override
		void updateTime(long currentTime) {
			if (currentTime > lastUpdate) {
				long deltat = currentTime - lastUpdate;
				posteriorVariance += deltat * shockVariance;
				lastUpdate = currentTime;
			}
		}

		@Override
		public double estimate() {
			return posteriorMean;
		}
		
	}
	
	private static class MeanReverting extends GaussianHMMFundamentalEstimator {

		private final long simLength;
		private final double fundamentalMean, kappac, shockVariance;

		private MeanReverting(long simLength, double mean, double meanReversion, double shockVariance,
				double observationVariance, double transactionVariance) {
			super(mean, observationVariance, transactionVariance);
			this.simLength = simLength;
			this.fundamentalMean = mean;
			this.kappac = 1 - meanReversion;
			this.shockVariance = shockVariance;
		}
		
		@Override
		void updateTime(long currentTime) {
			if (currentTime > lastUpdate) {
				long deltat = currentTime - lastUpdate;
				double kappacToPower = Math.pow(kappac, deltat);
				posteriorMean = (1 - kappacToPower) * fundamentalMean + kappacToPower * posteriorMean;
				posteriorVariance = kappacToPower * kappacToPower * posteriorVariance
						+ (1 - kappacToPower * kappacToPower) / (1 - kappac * kappac) * shockVariance;
				lastUpdate = currentTime;
			}
		}

		@Override
		public double estimate() {
			double kappacToPower = Math.pow(kappac, simLength - lastUpdate);
			return (1 - kappacToPower) * fundamentalMean + kappacToPower * posteriorMean;
		}
		
	}

}
