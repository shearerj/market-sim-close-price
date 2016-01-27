package edu.umich.srg.marketsim;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import edu.umich.srg.collect.Sparse;
import edu.umich.srg.marketsim.fundamental.Fundamental;
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

    // Fundamental features
    Fundamental fundamental = simulator.getFundamental();
    features.add("fundamental",
        convertSparseData(fundamental.getFundamentalValues(simulator.getCurrentTime())));

    // Market features
    for (Market market : simulator.getMarkets()) {
      features.add(market.toString().toLowerCase().replace(' ', '_'),
          market.getFeatures(fundamental));
    }

    return features;
  }

  private static JsonArray convertSparseData(
      Iterable<? extends Sparse.Entry<? extends Number>> data) {
    JsonArray json = new JsonArray();
    for (Sparse.Entry<? extends Number> obs : data) {
      JsonArray point = new JsonArray();
      point.add(new JsonPrimitive(obs.getIndex()));
      point.add(new JsonPrimitive(obs.getElement().doubleValue()));
      json.add(point);
    }
    return json;
  }

}
