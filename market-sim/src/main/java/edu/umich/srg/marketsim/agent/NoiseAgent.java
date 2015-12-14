package edu.umich.srg.marketsim.agent;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonObject;

import edu.umich.srg.distributions.Distribution;
import edu.umich.srg.distributions.Geometric;
import edu.umich.srg.distributions.Multinomial;
import edu.umich.srg.distributions.Multinomial.IntMultinomial;
import edu.umich.srg.distributions.Uniform;
import edu.umich.srg.distributions.Uniform.IntUniform;
import edu.umich.srg.egtaonline.spec.Spec;
import edu.umich.srg.fourheap.Order.OrderType;
import edu.umich.srg.marketsim.Keys.ArrivalRate;
import edu.umich.srg.marketsim.Price;
import edu.umich.srg.marketsim.Sim;
import edu.umich.srg.marketsim.TimeStamp;
import edu.umich.srg.marketsim.fundamental.Fundamental;
import edu.umich.srg.marketsim.market.Market;
import edu.umich.srg.marketsim.market.Market.MarketView;
import edu.umich.srg.marketsim.market.OrderRecord;

import java.util.Collection;
import java.util.Random;

/**
 * This agent is mainly for testing as it submits entirely random orders.
 */
public class NoiseAgent implements Agent {

  private static final Distribution<OrderType> orderTypeDistribution =
      Uniform.over(OrderType.values());
  private static final IntUniform orderPriceDistribution = Uniform.closed(1000, 2000);
  private static final IntMultinomial quantityDistribution = Multinomial.withWeights(0.5, 0.3, 0.2);

  private final Sim sim;
  private final MarketView market;
  private final Random rand;
  private final Geometric arrivalDistribution;

  // Features
  private int numTransactions;

  public NoiseAgent(Sim sim, Market market, Spec spec, Random rand) {
    this.sim = sim;
    this.market = market.getView(this, TimeStamp.ZERO);
    this.arrivalDistribution = Geometric.withSuccessProbability(spec.get(ArrivalRate.class));
    this.rand = rand;

    this.numTransactions = 0;
  }

  public static NoiseAgent createFromSpec(Sim sim, Fundamental fundamental,
      Collection<Market> markets, Market market, Spec spec, Random rand) {
    return new NoiseAgent(sim, market, spec, rand);
  }

  private void scheduleNextArrival() {
    sim.scheduleIn(TimeStamp.of(1 + arrivalDistribution.sample(rand)), this::strategy);
  }

  private void strategy() {
    for (OrderRecord order : ImmutableList.copyOf(market.getActiveOrders()))
      market.withdrawOrder(order);

    market.submitOrder(orderTypeDistribution.sample(rand),
        Price.of(orderPriceDistribution.sample(rand)), quantityDistribution.sample(rand) + 1);
    sim.addFeature("orders", 1);
    scheduleNextArrival();
  }

  @Override
  public void initilaize() {
    scheduleNextArrival();
  }

  @Override
  public double payoffForPosition(int position) {
    return 0;
  }

  @Override
  public JsonObject getFeatures() {
    JsonObject features = new JsonObject();
    features.addProperty("transactions", numTransactions);
    return features;
  }

  @Override
  public String toString() {
    return "NoiseAgent " + Integer.toString(System.identityHashCode(this), 36).toUpperCase();
  }

  // Notifications

  @Override
  public void notifyOrderSubmitted(OrderRecord order) {}

  @Override
  public void notifyOrderWithrawn(OrderRecord order, int quantity) {}

  @Override
  public void notifyOrderTransacted(OrderRecord order, Price price, int quantity) {
    this.numTransactions += 1;
  }

  @Override
  public void notifyQuoteUpdated(MarketView market) {}

  @Override
  public void notifyTransaction(MarketView market, Price price, int quantity) {}

}
