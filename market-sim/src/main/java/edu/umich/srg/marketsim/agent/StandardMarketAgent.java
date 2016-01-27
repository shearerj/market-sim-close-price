package edu.umich.srg.marketsim.agent;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonObject;

import edu.umich.srg.distributions.Distribution;
import edu.umich.srg.distributions.Geometric;
import edu.umich.srg.distributions.Uniform;
import edu.umich.srg.distributions.Uniform.IntUniform;
import edu.umich.srg.egtaonline.spec.Spec;
import edu.umich.srg.fourheap.Order.OrderType;
import edu.umich.srg.marketsim.Keys.ArrivalRate;
import edu.umich.srg.marketsim.Keys.MaxPosition;
import edu.umich.srg.marketsim.Keys.PrivateValueVar;
import edu.umich.srg.marketsim.Keys.Rmax;
import edu.umich.srg.marketsim.Keys.Rmin;
import edu.umich.srg.marketsim.Keys.Thresh;
import edu.umich.srg.marketsim.Price;
import edu.umich.srg.marketsim.Sim;
import edu.umich.srg.marketsim.TimeStamp;
import edu.umich.srg.marketsim.market.Market;
import edu.umich.srg.marketsim.market.Market.MarketView;
import edu.umich.srg.marketsim.market.OrderRecord;
import edu.umich.srg.marketsim.privatevalue.GaussianPrivateValue;
import edu.umich.srg.marketsim.privatevalue.PrivateValue;
import edu.umich.srg.marketsim.strategy.SurplusThreshold;

import java.util.Random;

abstract class StandardMarketAgent implements Agent {

  private static final Distribution<OrderType> orderTypeDistribution =
      Uniform.over(OrderType.values());

  protected final Sim sim;
  private final MarketView market;
  private final int maxPosition;
  private final SurplusThreshold threshold;
  private final PrivateValue privateValue;
  private final Random rand;
  private final Geometric arrivalDistribution;
  private final IntUniform shadingDistribution;

  /** Standard constructor for ZIR agent. */
  public StandardMarketAgent(Sim sim, Market market, Spec spec, Random rand) {
    this.sim = sim;
    this.market = market.getView(this, TimeStamp.ZERO);
    this.maxPosition = spec.get(MaxPosition.class);
    this.threshold = SurplusThreshold.create(spec.get(Thresh.class));
    this.privateValue = GaussianPrivateValue.generate(rand, spec.get(MaxPosition.class),
        spec.get(PrivateValueVar.class));
    this.arrivalDistribution = Geometric.withSuccessProbability(spec.get(ArrivalRate.class));
    this.shadingDistribution = Uniform.closed(spec.get(Rmin.class), spec.get(Rmax.class));
    this.rand = rand;
  }

  protected abstract double getFinalFundamentalEstiamte();

  protected abstract String name();

  private void scheduleNextArrival() {
    sim.scheduleIn(TimeStamp.of(1 + arrivalDistribution.sample(rand)), this::strategy);
  }

  protected void strategy() {
    ImmutableList.copyOf(market.getActiveOrders()).forEach(market::withdrawOrder);

    OrderType type = orderTypeDistribution.sample(rand);
    if (Math.abs(market.getHoldings() + type.sign()) <= maxPosition) {

      double finalEstimate = getFinalFundamentalEstiamte();
      double privateBenefit =
          type.sign() * privateValue.valueForExchange(market.getHoldings(), type);
      double demandedSurplus = shadingDistribution.sample(rand);

      Price toSubmit = threshold.shadePrice(type, market.getQuote(), finalEstimate + privateBenefit,
          demandedSurplus);

      if (toSubmit.longValue() > 0) {
        market.submitOrder(type, toSubmit, 1);
      }
    }

    scheduleNextArrival();
  }

  @Override
  public void initilaize() {
    scheduleNextArrival();
  }

  @Override
  public double payoffForPosition(int position) {
    return privateValue.valueAtPosition(position);
  }

  @Override
  public JsonObject getFeatures() {
    return new JsonObject();
  }

  @Override
  public String toString() {
    return name() + " " + Integer.toString(System.identityHashCode(this), 36).toUpperCase();
  }

  // Notifications

  @Override
  public void notifyOrderSubmitted(OrderRecord order) {}

  @Override
  public void notifyOrderWithrawn(OrderRecord order, int quantity) {}

  @Override
  public void notifyOrderTransacted(OrderRecord order, Price price, int quantity) {}

  @Override
  public void notifyQuoteUpdated(MarketView market) {}

  @Override
  public void notifyTransaction(MarketView market, Price price, int quantity) {}

}
