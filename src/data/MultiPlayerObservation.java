package data;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

public class MultiPlayerObservation {

	public final String role;
	public final String strategy;
	public final SummaryStatistics payoff;
	
	public MultiPlayerObservation(String role, String strategy) {
		this.role = role;
		this.strategy = strategy;
		this.payoff = new SummaryStatistics();
	}
	
	public MultiPlayerObservation(String role, String strategy, double firstValue) {
		this(role, strategy);
		payoff.addValue(firstValue);
	}
	
}
