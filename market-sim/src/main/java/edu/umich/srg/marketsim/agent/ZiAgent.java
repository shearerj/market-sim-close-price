package edu.umich.srg.marketsim.agent;

import static edu.umich.srg.fourheap.OrderType.BUY;
import static java.math.RoundingMode.CEILING;
import static java.math.RoundingMode.FLOOR;

import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;
import com.google.common.math.DoubleMath;
import com.google.gson.JsonObject;

import edu.umich.srg.distributions.Distribution;
import edu.umich.srg.distributions.Geometric;
import edu.umich.srg.distributions.Uniform;
import edu.umich.srg.distributions.Uniform.IntUniform;
import edu.umich.srg.egtaonline.spec.Spec;
import edu.umich.srg.fourheap.OrderType;
import edu.umich.srg.marketsim.Keys.ArrivalRate;
import edu.umich.srg.marketsim.Keys.FundamentalObservationVariance;
import edu.umich.srg.marketsim.Keys.MaxPosition;
import edu.umich.srg.marketsim.Keys.PrivateValueVar;
import edu.umich.srg.marketsim.Keys.Rmax;
import edu.umich.srg.marketsim.Keys.Rmin;
import edu.umich.srg.marketsim.Keys.Sides;
import edu.umich.srg.marketsim.Keys.SubmitDepth;
import edu.umich.srg.marketsim.Keys.Thresh;
import edu.umich.srg.marketsim.Price;
import edu.umich.srg.marketsim.Sim;
import edu.umich.srg.marketsim.TimeStamp;
import edu.umich.srg.marketsim.fundamental.Fundamental;
import edu.umich.srg.marketsim.fundamental.Fundamental.FundamentalView;
import edu.umich.srg.marketsim.fundamental.GaussianFundamentalView.GaussableView;
import edu.umich.srg.marketsim.market.Market;
import edu.umich.srg.marketsim.market.Market.MarketView;
import edu.umich.srg.marketsim.observer.GetQuoteObserver;
import edu.umich.srg.marketsim.privatevalue.PrivateValue;
import edu.umich.srg.marketsim.privatevalue.PrivateValues;
import edu.umich.srg.marketsim.strategy.SurplusThreshold;
import edu.umich.srg.util.SummStats;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Random;
import java.util.Set;

public class ZiAgent implements Agent {

  public enum OrderStyle {
    RANDOM, BOTH
  }

  private static final Distribution<OrderType> randomOrder = Uniform.over(OrderType.values());
  private static final Set<OrderType> allOrders = EnumSet.allOf(OrderType.class);

  protected final Sim sim;
  protected final Random rand;
  private final int id;
  private final MarketView market;
  private final GetQuoteObserver quoteInfo;
  private final int maxPosition;
  private final SurplusThreshold threshold;
  private final PrivateValue privateValue;
  private final Geometric arrivalDistribution;
  private final Supplier<Set<OrderType>> side;
  private final int ordersPerSide;
  private final IntUniform shadingDistribution;
  private final FundamentalView fundamental;

  // Bookkeeping
  private final SummStats shadingStats;

  /** Standard constructor for ZIR agent. */
  public ZiAgent(Sim sim, Market market, Fundamental fundamental, Spec spec, Random rand) {
    this.sim = sim;
    this.id = rand.nextInt();
    this.market = market.getView(this, TimeStamp.ZERO);
    this.quoteInfo = market.addQuoteObserver(GetQuoteObserver.create(market));
    this.maxPosition = spec.get(MaxPosition.class);
    this.threshold = SurplusThreshold.create(spec.get(Thresh.class));
    this.privateValue = PrivateValues.gaussianPrivateValue(rand, spec.get(MaxPosition.class),
        spec.get(PrivateValueVar.class));
    this.arrivalDistribution = Geometric.withSuccessProbability(spec.get(ArrivalRate.class));
    this.shadingDistribution = Uniform.closed(spec.get(Rmin.class), spec.get(Rmax.class));
    this.fundamental =
        spec.get(FundamentalObservationVariance.class).isInfinite() ? fundamental.getView(sim)
            : ((GaussableView) fundamental.getView(sim)).addNoise(rand,
                spec.get(FundamentalObservationVariance.class));

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

    this.shadingStats = SummStats.empty();
  }

  public static ZiAgent createFromSpec(Sim sim, Fundamental fundamental, Collection<Market> markets,
      Market market, Spec spec, Random rand) {
    return new ZiAgent(sim, market, fundamental, spec, rand);
  }

  private void scheduleNextArrival() {
    sim.scheduleIn(TimeStamp.of(1 + arrivalDistribution.sample(rand)), this::strategy);
  }

  protected void strategy() {
    ImmutableList.copyOf(market.getActiveOrders().entrySet()).forEach(market::withdrawOrder);

    Set<OrderType> sides = side.get();
    double finalEstimate = fundamental.getEstimatedFinalFundamental();
    double demandedSurplus = shadingDistribution.sample(rand);

    for (OrderType type : sides) {
      for (int num = 0; num < ordersPerSide; num++) {
        if (Math.abs(market.getHoldings() + (num + 1) * type.sign()) <= maxPosition) {

          double privateBenefit = type.sign()
              * privateValue.valueForExchange(market.getHoldings() + num * type.sign(), type);
          double estimatedValue = finalEstimate + privateBenefit;

          double toSubmit =
              threshold.shadePrice(type, quoteInfo.getQuote(), estimatedValue, demandedSurplus);
          long rounded = DoubleMath.roundToLong(toSubmit, type == BUY ? FLOOR : CEILING);
          shadingStats.accept(Math.abs(estimatedValue - toSubmit));

          if (rounded > 0) {
            market.submitOrder(type, Price.of(rounded), 1);
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
  public JsonObject getFeatures() {
    JsonObject feats = Agent.super.getFeatures();
    feats.addProperty("count_shading", shadingStats.getCount());
    feats.addProperty("mean_shading", shadingStats.getAverage().orElse(0.0));
    return feats;
  }
  
  @Override
  public int getBenchmarkDir() {
	return 0;
  }
  
  @Override
  public double getContractHoldings() {
	  return 0;
  }

  @Override
  public int getId() {
    return id;
  }

  @Override
  public String toString() {
    return "ZI " + Integer.toUnsignedString(id, 36).toUpperCase();
  }
}
