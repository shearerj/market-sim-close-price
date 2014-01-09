package data;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

/**
 * This class represents the observation of a player over several simulations.
 * 
 * @author erik
 * 
 */
public class MultiSimPlayerObservation {

	public final String role;
	public final String strategy;
	public final SummaryStatistics payoff;
	
	public MultiSimPlayerObservation(String role, String strategy) {
		this.role = role;
		this.strategy = strategy;
		this.payoff = new SummaryStatistics();
	}
	
	public MultiSimPlayerObservation(String role, String strategy, double firstValue) {
		this(role, strategy);
		payoff.addValue(firstValue);
	}
	
}
