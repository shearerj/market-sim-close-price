package edu.umich.srg.marketsim;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import edu.umich.srg.marketsim.market.Market;
import edu.umich.srg.util.SummStats;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

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
    
    // Summary features
    for (Entry<String, SummStats> entry : summaryFeatures.entrySet()) {
      String key = entry.getKey().toLowerCase().replace(' ', '_');
      features.addProperty(key + "_c", entry.getValue().getCount());
      features.addProperty(key + "_a", entry.getValue().getAverage());
    }
    
    // Market features
    for (Market market : simulator.getMarkets()) {
      merge(market.getFeatures(), features, (market + "_").toLowerCase().replace(' ', '_'));
    }
    
    // Fundamental features
    merge(simulator.getFundamental().getFeatures(), features, "");

    return features;
  }
  
  private static void merge(JsonObject copied, JsonObject modified, String prefix) {
    for (Entry<String, JsonElement> e : copied.entrySet()) {
      modified.add(prefix + e.getKey(), e.getValue());
    }
  }

}
