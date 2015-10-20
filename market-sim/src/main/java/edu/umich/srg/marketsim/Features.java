package edu.umich.srg.marketsim;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.google.gson.JsonObject;

import edu.umich.srg.util.SummStats;

class Features {

	private final Map<String, SummStats> summaryFeatures;
	
	Features() {
		this.summaryFeatures = new HashMap<>();
	}
	
	void accept(String name, double value) {
		SummStats stats = summaryFeatures.get(name);
		if (stats == null) {
			stats = SummStats.empty();
			summaryFeatures.put(name, stats);
		}
		stats.accept(value);
	}
	
	JsonObject computeFeatures(MarketSimulator simulator) {
		JsonObject features = new JsonObject();
		for (Entry<String, SummStats> entry : summaryFeatures.entrySet()) {
			features.addProperty(entry.getKey() + "_c", entry.getValue().getCount());
			features.addProperty(entry.getKey() + "_a", entry.getValue().getAverage());
		}
		
		// FIXME Put other computed features here
		
		return features;
	}
	
}
