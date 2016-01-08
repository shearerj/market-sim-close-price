package edu.umich.srg.marketsim;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import edu.umich.srg.collect.SparseList;
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
    long finalTime = simulator.getCurrentTime().get();
    // This filters out fundamental values above last time;
    Fundamental fundamental = simulator.getFundamental();
    // Iterable<SparseList.Entry<Number>> fundamental = () -> StreamSupport
    // .stream(simulator.getFundamental().getInfo().getFundamentalValues().spliterator(), false)
    // .filter(e -> e.getIndex() <= finalTime).iterator();

    addSparseData(features, fundamental.getFundamentalValues(finalTime), "fundamental");

    // Market features
    for (Market market : simulator.getMarkets()) {
      String marketTag = market.toString().toLowerCase().replace(' ', '_');
      Iterable<? extends SparseList.Entry<? extends Number>> prices =
          market.getMarketInfo().getPrices();
      addSparseData(features, prices, marketTag);
      features.addProperty(marketTag + "_rmsd", fundamental.rmsd(prices.iterator(), finalTime));
    }

    return features;
  }

  private static void addSparseData(JsonObject root,
      Iterable<? extends SparseList.Entry<? extends Number>> data, String prefix) {
    JsonArray prices = new JsonArray();
    JsonArray indices = new JsonArray();
    for (SparseList.Entry<? extends Number> obs : data) {
      prices.add(new JsonPrimitive(obs.getElement().doubleValue()));
      indices.add(new JsonPrimitive(obs.getIndex()));
    }

    root.add(prefix + "_prices", prices);
    root.add(prefix + "_indices", indices);
  }

}
