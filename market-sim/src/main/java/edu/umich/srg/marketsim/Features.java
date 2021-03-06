package edu.umich.srg.marketsim;

import static edu.umich.srg.fourheap.OrderType.BUY;
import static edu.umich.srg.fourheap.OrderType.SELL;

import com.google.common.collect.Multiset;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import edu.umich.srg.fourheap.OrderType;
import edu.umich.srg.marketsim.agent.Agent;
import edu.umich.srg.marketsim.fundamental.Fundamental;
import edu.umich.srg.marketsim.market.Market;
import edu.umich.srg.marketsim.market.Market.AgentInfo;

import java.util.AbstractMap;
import java.util.Comparator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.stream.Collectors;

class Features {

  /*
   * FIXME Add Information metic, that somehow captures amount of information about the underlying
   * fundamental. We can probably do something like rmsd, but instead of transaction prices do
   * expected rmsd over time. I'm imagining something like a Gaussian process, where there's more
   * penalty away from known observations as there's uncertainty. This could potentially be tied to
   * generic uncertainty about the fundamental.
   */

  static JsonObject computeFeatures(MarketSimulator simulator) {
    JsonObject features = new JsonObject();

    // Fundamental features
    Fundamental fundamental = simulator.getFundamental();
    JsonArray jfund = new JsonArray();
    long time = 0;
    for (Multiset.Entry<Double> ent : fundamental.getFundamentalValues()) {
      JsonArray point = new JsonArray();
      point.add(time);
      point.add(ent.getElement());
      jfund.add(point);
      time += ent.getCount();
    }
    features.add("fundamental", jfund);

    // Market features
    JsonArray marketFeatures = new JsonArray();
    for (Market market : simulator.getMarkets()) {
      marketFeatures.add(market.getFeatures());
    }
    features.add("markets", marketFeatures);

    // Surplus Features
    surplusFeatures(simulator.getAgentPayoffs(), features);

    return features;
  }

  private static void surplusFeatures(Map<Agent, ? extends AgentInfo> results,
      JsonObject features) {
    double surplus = results.values().stream().mapToDouble(AgentInfo::getProfit).sum();
    // For completeness, we also compute it with submission limits
    CompEqResults compEq = calcCompetitiveEquilibrium(results);

    // Assert results are consistent
    assert Math.abs(
        compEq.maxSurplus - compEq.imSurplusLoss - compEq.emSurplusLoss - surplus) < 1e-5 : String
            .format("surplus losses didn't sum properly (%f)",
                compEq.maxSurplus - compEq.imSurplusLoss - compEq.emSurplusLoss - surplus);

    // Record features
    features.addProperty("total_surplus", surplus);

    features.addProperty("max_surplus", compEq.maxSurplus);
    features.addProperty("ce_price", compEq.cePrice);
    features.addProperty("ce_volume", compEq.ceVolume);
    features.addProperty("im_surplus_loss", compEq.imSurplusLoss);
    features.addProperty("em_surplus_loss", compEq.emSurplusLoss);
  }

  // FIXME This produces different results with the same random seed, but for reasons I don't
  // understand...

  // This will utterly fail if private valuations aren't diminishing marginal
  // This uses payoffForPosition which scales linearly. The linear number should be small
  // enough that it doesn't really matter, but it is a source of inefficiency / slowness.
  private static CompEqResults calcCompetitiveEquilibrium(Map<Agent, ? extends AgentInfo> results) {
    // Editable Map holding positions in ce
    Map<Agent, Integer> cePositions =
        results.keySet().stream().collect(Collectors.toMap(e -> e, e -> 0));

    // Priority queues of buyers and sellers, ordered by the highest gain from a marginal trade
    Comparator<Entry<Agent, Double>> comp =
        Comparator.<Entry<Agent, Double>, Double>comparing(Entry::getValue).reversed();
    PriorityQueue<Entry<Agent, Double>> buyers = new PriorityQueue<>(comp);
    PriorityQueue<Entry<Agent, Double>> sellers = new PriorityQueue<>(comp);

    // These are necessary to get a clearing price when no agent transacts
    for (Entry<Agent, Integer> entry : cePositions.entrySet()) {
      Agent agent = entry.getKey();
      // Only include agents that can trade
      if (results.get(agent).getSubmissions() > 0) {
        buyers.add(new AbstractMap.SimpleEntry<>(agent, agent.payoffForExchange(0, BUY)));
        sellers.add(new AbstractMap.SimpleEntry<>(agent, agent.payoffForExchange(0, SELL)));
      }
    }

    double cePrice = 0; // Any price is valid if no one trades
    double maxSurplus = 0;
    while (!buyers.isEmpty() && !sellers.isEmpty()
        && !buyers.peek().getKey().equals(sellers.peek().getKey())
        && buyers.peek().getValue() + sellers.peek().getValue() > 0) {

      // Extract agents
      Entry<Agent, Double> buyerEnt = buyers.poll();
      Agent buyer = buyerEnt.getKey();
      double buyerVal = buyerEnt.getValue();

      Entry<Agent, Double> sellerEnt = sellers.poll();
      Agent seller = sellerEnt.getKey();
      double sellerVal = sellerEnt.getValue();

      assert buyer != seller;

      // Update values
      cePrice = (buyerVal - sellerVal) / 2;
      maxSurplus += buyerVal + sellerVal;

      // Update positions
      int buyerPos = cePositions.compute(buyer, (key, val) -> val + 1);
      int sellerPos = cePositions.compute(seller, (key, val) -> val - 1);

      // Put back in priority queue with updated values if they can still trade
      if (buyerPos < results.get(buyer).getSubmissions()) {
        buyerEnt.setValue(buyer.payoffForExchange(buyerPos, BUY));
        buyers.add(buyerEnt);
      }
      if (-sellerPos < results.get(seller).getSubmissions()) {
        sellerEnt.setValue(seller.payoffForExchange(sellerPos, SELL));
        sellers.add(sellerEnt);
      }

      // Make sure queues are up to date
      double desiredBenefit;
      while (!buyers.isEmpty() && (desiredBenefit = buyers.peek().getKey()
          .payoffForExchange(cePositions.get(buyers.peek().getKey()), BUY)) != buyers.peek()
              .getValue()) {
        buyerEnt = buyers.poll();
        buyerEnt.setValue(desiredBenefit);
        buyers.add(buyerEnt);
      }
      while (!sellers.isEmpty() && (desiredBenefit = sellers.peek().getKey()
          .payoffForExchange(cePositions.get(sellers.peek().getKey()), SELL)) != sellers.peek()
              .getValue()) {
        sellerEnt = sellers.poll();
        sellerEnt.setValue(desiredBenefit);
        sellers.add(sellerEnt);
      }
    }

    // Calculate surplus loss based off of trade differences
    double imSurplusLoss = 0;
    double emSurplusLoss = 0;
    for (Entry<Agent, ? extends AgentInfo> result : results.entrySet()) {
      Agent agent = result.getKey();
      int cePosition = cePositions.get(agent);
      int actualPosition = result.getValue().getHoldings();

      if (cePosition == actualPosition) { // No loss
        continue;
      }

      int posDiff = actualPosition - cePosition;
      double surplusLoss = surplusDifference(agent, actualPosition, cePosition) + posDiff * cePrice;
      if (cePosition * posDiff >= 0) { // EM Trader
        emSurplusLoss += surplusLoss;
      } else { // IM Trader
        imSurplusLoss += surplusLoss;
      }
    }

    int ceVolume =
        cePositions.entrySet().stream().mapToInt(Entry::getValue).filter(p -> p > 0).sum();

    return new CompEqResults(ceVolume, cePrice, maxSurplus, imSurplusLoss, emSurplusLoss);
  }

  private static double surplusDifference(Agent agent, int start, int end) {
    double surplus = 0;
    OrderType direction = end > start ? BUY : SELL;
    for (int pos = start; pos != end; pos += direction.sign()) {
      surplus += agent.payoffForExchange(pos, direction);
    }
    return surplus;
  }

  private static final class CompEqResults {
    private final int ceVolume;
    private final double cePrice;
    private final double maxSurplus;
    private final double imSurplusLoss;
    private final double emSurplusLoss;

    private CompEqResults(int ceVolume, double cePrice, double maxSurplus, double imSurplusLoss,
        double emSurplusLoss) {
      assert ceVolume >= 0 : "ce volume was negative";
      assert maxSurplus >= 0 : "max surplus was negative";
      assert !Double.isNaN(maxSurplus) : "max surplus was nan";
      assert !Double.isNaN(imSurplusLoss) : "im surplus loss was nan";
      assert !Double.isNaN(emSurplusLoss) : "em surplus loss was nan";

      this.ceVolume = ceVolume;
      this.cePrice = cePrice;
      this.maxSurplus = maxSurplus;
      this.imSurplusLoss = imSurplusLoss;
      this.emSurplusLoss = emSurplusLoss;
    }
  }

  // Unconstructable
  private Features() {}

}
