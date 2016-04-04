package edu.umich.srg.marketsim;

import static edu.umich.srg.fourheap.Order.OrderType.BUY;
import static edu.umich.srg.fourheap.Order.OrderType.SELL;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import edu.umich.srg.collect.Sparse;
import edu.umich.srg.fourheap.Order.OrderType;
import edu.umich.srg.marketsim.MarketSimulator.AgentResult;
import edu.umich.srg.marketsim.agent.Agent;
import edu.umich.srg.marketsim.fundamental.Fundamental;
import edu.umich.srg.marketsim.market.Market;
import edu.umich.srg.util.SummStats;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.stream.Collectors;

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

    // Surplus Features
    surplusFeatures(simulator.getAgentPayoffs().entrySet(), features);

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

  private static void surplusFeatures(Collection<Entry<Agent, AgentResult>> results,
      JsonObject features) {
    // XXX This uses payoffForPosition which scales linearly. The linear number should be small
    // enough that it doesn't really matter, but it is a source of inefficiency.

    Map<Agent, Integer> cePositions =
        results.stream().collect(Collectors.toMap(e -> e.getKey(), e -> 0));
    Comparator<Entry<Agent, Double>> comp =
        Comparator.<Entry<Agent, Double>, Double>comparing(Entry::getValue).reversed();
    PriorityQueue<Entry<Agent, Double>> buyers = new PriorityQueue<>(comp);
    PriorityQueue<Entry<Agent, Double>> sellers = new PriorityQueue<>(comp);

    for (Entry<Agent, Integer> entry : cePositions.entrySet()) {
      Agent agent = entry.getKey();
      buyers.add(new AbstractMap.SimpleEntry<>(agent, agent.payoffForPosition(1)));
      sellers.add(new AbstractMap.SimpleEntry<>(agent, agent.payoffForPosition(-1)));
    }

    double cePrice = 0;
    double maxSurplus = 0;
    while (buyers.peek().getValue() + sellers.peek().getValue() > 0) {
      // Extract agents
      Entry<Agent, Double> buyerEnt = buyers.poll();
      Agent buyer = buyerEnt.getKey();
      double buyerVal = buyerEnt.getValue();

      Entry<Agent, Double> sellerEnt = sellers.poll();
      Agent seller = sellerEnt.getKey();
      double sellerVal = sellerEnt.getValue();

      // Update values
      cePrice = (buyerVal - sellerVal) / 2;
      maxSurplus += buyerVal + sellerVal;

      // Update positions
      int buyerPos = cePositions.compute(buyer, (key, val) -> val + 1);
      int sellerPos = cePositions.compute(seller, (key, val) -> val - 1);

      // Put back in priority queue with updated values
      buyerEnt.setValue(buyer.payoffForPosition(buyerPos + 1) - buyer.payoffForPosition(buyerPos));
      buyers.add(buyerEnt);
      sellerEnt
          .setValue(seller.payoffForPosition(sellerPos - 1) - seller.payoffForPosition(sellerPos));
      sellers.add(sellerEnt);

      // Make sure queues are up to date
      double desiredBenefit;
      while ((desiredBenefit =
          calcDesiredBenefit(BUY, buyers.peek().getKey(), cePositions)) != buyers.peek()
              .getValue()) {
        buyerEnt = buyers.poll();
        buyerEnt.setValue(desiredBenefit);
        buyers.add(buyerEnt);
      }
      while ((desiredBenefit =
          calcDesiredBenefit(SELL, sellers.peek().getKey(), cePositions)) != sellers.peek()
              .getValue()) {
        sellerEnt = sellers.poll();
        sellerEnt.setValue(desiredBenefit);
        sellers.add(sellerEnt);
      }
    }

    features.addProperty("max_surplus", maxSurplus);
    features.addProperty("ce_price", cePrice);

    double surplus = 0;
    double imSurplusLoss = 0;
    double emSurplusLoss = 0;
    for (Entry<Agent, AgentResult> result : results) {
      Agent agent = result.getKey();
      int cePosition = cePositions.get(agent);
      int actualPosition = result.getValue().getHoldings();
      surplus += result.getValue().getPayoff();

      double valLoss =
          agent.payoffForPosition(cePosition) - agent.payoffForPosition(actualPosition);
      int posDiff = actualPosition - cePosition;
      if (posDiff * cePosition > 0) {
        emSurplusLoss += valLoss + posDiff * cePrice;
      } else {
        imSurplusLoss += valLoss + posDiff * cePrice;
      }
    }

    features.addProperty("total_surplus", surplus);
    features.addProperty("im_surplus_loss", imSurplusLoss);
    features.addProperty("em_surplus_loss", emSurplusLoss);
  }

  private static double calcDesiredBenefit(OrderType type, Agent agent,
      Map<Agent, Integer> positionMap) {
    int position = positionMap.get(agent);
    return agent.payoffForPosition(position + type.sign()) - agent.payoffForPosition(position);
  }

}
