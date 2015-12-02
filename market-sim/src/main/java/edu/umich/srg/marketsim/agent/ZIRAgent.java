package edu.umich.srg.marketsim.agent;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonObject;

import edu.umich.srg.distributions.Distribution;
import edu.umich.srg.distributions.Geometric;
import edu.umich.srg.distributions.Uniform;
import edu.umich.srg.distributions.Uniform.IntUniform;
import edu.umich.srg.egtaonline.spec.Spec;
import edu.umich.srg.fourheap.Order.OrderType;
import edu.umich.srg.marketsim.Keys.ArrivalRate;
import edu.umich.srg.marketsim.Keys.FundamentalMean;
import edu.umich.srg.marketsim.Keys.FundamentalMeanReversion;
import edu.umich.srg.marketsim.Keys.MaxPosition;
import edu.umich.srg.marketsim.Keys.PrivateValueVar;
import edu.umich.srg.marketsim.Keys.Rmax;
import edu.umich.srg.marketsim.Keys.Rmin;
import edu.umich.srg.marketsim.Keys.SimLength;
import edu.umich.srg.marketsim.Keys.Thresh;
import edu.umich.srg.marketsim.Price;
import edu.umich.srg.marketsim.Sim;
import edu.umich.srg.marketsim.TimeStamp;
import edu.umich.srg.marketsim.fundamental.Fundamental;
import edu.umich.srg.marketsim.fundamental.FundamentalView;
import edu.umich.srg.marketsim.fundamental.GaussianMeanReverting;
import edu.umich.srg.marketsim.market.Market;
import edu.umich.srg.marketsim.market.Market.MarketView;
import edu.umich.srg.marketsim.market.OrderRecord;
import edu.umich.srg.marketsim.privatevalue.GaussianPrivateValue;
import edu.umich.srg.marketsim.privatevalue.PrivateValue;
import edu.umich.srg.marketsim.strategy.GaussianFundamentalEstimator;
import edu.umich.srg.marketsim.strategy.SurplusThreshold;

import java.util.Collection;
import java.util.Random;

public class ZIRAgent implements Agent {

  private static final Distribution<OrderType> orderTypeDistribution =
      Uniform.over(OrderType.values());

  private final Sim sim;
  private final MarketView market;
  private final FundamentalView fundamental;
  private final GaussianFundamentalEstimator estimator;
  private final int maxPosition;
  private final SurplusThreshold threshold;
  private final PrivateValue privateValue;
  private final Random rand;
  private final Geometric arrivalDistribution;
  private final IntUniform shadingDistribution;

  public ZIRAgent(Sim sim, Market market, Fundamental fundamental, Spec spec, Random rand) {
    checkArgument(fundamental instanceof GaussianMeanReverting);

    this.sim = sim;
    this.market = market.getView(this, TimeStamp.ZERO);
    this.fundamental = FundamentalView.create(sim, fundamental);
    this.estimator = GaussianFundamentalEstimator.create(spec.get(SimLength.class),
        spec.get(FundamentalMean.class), spec.get(FundamentalMeanReversion.class));
    this.maxPosition = spec.get(MaxPosition.class);
    this.threshold = SurplusThreshold.create(spec.get(Thresh.class));
    this.privateValue = GaussianPrivateValue.generate(rand, spec.get(MaxPosition.class),
        spec.get(PrivateValueVar.class));
    this.arrivalDistribution = Geometric.withSuccessProbability(spec.get(ArrivalRate.class));
    this.shadingDistribution = Uniform.closed(spec.get(Rmin.class), spec.get(Rmax.class));
    this.rand = rand;
  }

  public static ZIRAgent createFromSpec(Sim sim, Fundamental fundamental,
      Collection<Market> markets, Market market, Spec spec, Random rand) {
    return new ZIRAgent(sim, market, fundamental, spec, rand);
  }

  private void scheduleNextArrival() {
    sim.scheduleIn(TimeStamp.of(1 + arrivalDistribution.sample(rand)), this::strategy);
  }

  private void strategy() {
    for (OrderRecord order : ImmutableList.copyOf(market.getActiveOrders()))
      market.withdrawOrder(order);

    OrderType type = orderTypeDistribution.sample(rand);
    if (Math.abs(market.getHoldings() + type.sign()) <= maxPosition) {

      double finalEstimate = estimator.estimate(sim.getCurrentTime(), fundamental.getFundamental()),
          privateBenefit = type.sign() * privateValue.valueForExchange(market.getHoldings(), type),
          demandedSurplus = shadingDistribution.sample(rand);

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
    return "ZIR " + Integer.toString(System.identityHashCode(this), 36).toUpperCase();
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
