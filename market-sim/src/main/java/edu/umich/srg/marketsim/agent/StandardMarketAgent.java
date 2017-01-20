package edu.umich.srg.marketsim.agent;

import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;

import edu.umich.srg.distributions.Distribution;
import edu.umich.srg.distributions.Geometric;
import edu.umich.srg.distributions.Uniform;
import edu.umich.srg.egtaonline.spec.Spec;
import edu.umich.srg.fourheap.OrderType;
import edu.umich.srg.marketsim.Keys.ArrivalRate;
import edu.umich.srg.marketsim.Keys.MaxPosition;
import edu.umich.srg.marketsim.Keys.PrivateValueVar;
import edu.umich.srg.marketsim.Keys.Sides;
import edu.umich.srg.marketsim.Keys.SubmitDepth;
import edu.umich.srg.marketsim.Keys.Thresh;
import edu.umich.srg.marketsim.Price;
import edu.umich.srg.marketsim.Sim;
import edu.umich.srg.marketsim.TimeStamp;
import edu.umich.srg.marketsim.market.Market;
import edu.umich.srg.marketsim.market.Market.MarketView;
import edu.umich.srg.marketsim.privatevalue.PrivateValue;
import edu.umich.srg.marketsim.privatevalue.PrivateValues;
import edu.umich.srg.marketsim.strategy.SurplusThreshold;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Random;
import java.util.Set;

public abstract class StandardMarketAgent implements Agent {

  public static enum OrderStyle {
    RANDOM, BOTH
  }

  private static final Distribution<OrderType> randomOrder = Uniform.over(OrderType.values());
  private static final Set<OrderType> allOrders = EnumSet.allOf(OrderType.class);

  protected final Sim sim;
  protected final Random rand;
  private final int id;
  private final MarketView market;
  private final int maxPosition;
  private final SurplusThreshold threshold;
  private final PrivateValue privateValue;
  private final Geometric arrivalDistribution;
  private final Supplier<Set<OrderType>> side;
  private final int ordersPerSide;

  /** Standard constructor for ZIR agent. */
  public StandardMarketAgent(Sim sim, Market market, Spec spec, Random rand) {
    this.sim = sim;
    this.id = rand.nextInt();
    this.market = market.getView(this, TimeStamp.ZERO);
    this.maxPosition = spec.get(MaxPosition.class);
    this.threshold = SurplusThreshold.create(spec.get(Thresh.class));
    this.privateValue = PrivateValues.gaussianPrivateValue(rand, spec.get(MaxPosition.class),
        spec.get(PrivateValueVar.class));
    this.arrivalDistribution = Geometric.withSuccessProbability(spec.get(ArrivalRate.class));
    switch (spec.get(Sides.class)) {
      case RANDOM:
        this.side = () -> Collections.singleton(randomOrder.sample(rand));
        break;
      case BOTH:
        this.side = () -> allOrders;
        break;
      default:
        throw new IllegalArgumentException("Sides was null");
    }
    this.ordersPerSide = spec.get(SubmitDepth.class);
    this.rand = rand;
  }

  protected abstract double getDesiredSurplus();

  protected abstract double getFinalFundamentalEstiamte();

  protected abstract String name();

  private void scheduleNextArrival() {
    sim.scheduleIn(TimeStamp.of(1 + arrivalDistribution.sample(rand)), this::strategy);
  }

  protected void strategy() {
    ImmutableList.copyOf(market.getActiveOrders().entrySet()).forEach(market::withdrawOrder);

    Set<OrderType> sides = side.get();
    double finalEstimate = getFinalFundamentalEstiamte();
    double demandedSurplus = getDesiredSurplus();

    for (OrderType type : sides) {
      for (int num = 0; num < ordersPerSide; num++) {
        if (Math.abs(market.getHoldings() + (num + 1) * type.sign()) <= maxPosition) {

          double privateBenefit = type.sign()
              * privateValue.valueForExchange(market.getHoldings() + num * type.sign(), type);

          Price toSubmit = threshold.shadePrice(type, market.getQuote(),
              finalEstimate + privateBenefit, demandedSurplus);

          if (toSubmit.longValue() > 0) {
            market.submitOrder(type, toSubmit, 1);
          }
        }
      }
    }

    scheduleNextArrival();
  }

  @Override
  public void initilaize() {
    scheduleNextArrival();
  }

  @Override
  public double payoffForExchange(int position, OrderType type) {
    return privateValue.valueForExchange(position, type);
  }

  @Override
  public int getId() {
    return id;
  }

  @Override
  public String toString() {
    return name() + " " + Integer.toUnsignedString(id, 36).toUpperCase();
  }

}
