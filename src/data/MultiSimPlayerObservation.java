package data;

import java.util.Map;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import com.google.common.collect.Maps;

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
	public final Map<String, SummaryStatistics> features;
	
	public MultiSimPlayerObservation(String role, String strategy) {
		this.role = role;
		this.strategy = strategy;
		this.payoff = new SummaryStatistics();
		this.features = Maps.newHashMap();
	}
	
	public MultiSimPlayerObservation(String role, String strategy, double firstValue,
			Map<String, Double> features) {
		this(role, strategy);
		payoff.addValue(firstValue);
		for (String key : features.keySet()) {
			SummaryStatistics summ = new SummaryStatistics();
			summ.addValue(features.get(key).doubleValue());
			this.features.put(key, summ);
		}
	}
	
}
