package edu.umich.srg.marketsim.agent;

import com.google.common.collect.ImmutableList;

import edu.umich.srg.distributions.Distribution.LongDistribution;
import edu.umich.srg.distributions.Geometric;
import edu.umich.srg.egtaonline.spec.Spec;
import edu.umich.srg.fourheap.Order.OrderType;
import edu.umich.srg.marketsim.Keys.ArrivalRate;
import edu.umich.srg.marketsim.Keys.FundamentalMean;
import edu.umich.srg.marketsim.Keys.FundamentalMeanReversion;
import edu.umich.srg.marketsim.Keys.FundamentalObservationVariance;
import edu.umich.srg.marketsim.Keys.FundamentalShockVar;
import edu.umich.srg.marketsim.Keys.NumRungs;
import edu.umich.srg.marketsim.Keys.PriceVarEst;
import edu.umich.srg.marketsim.Keys.RungSep;
import edu.umich.srg.marketsim.Keys.RungThickness;
import edu.umich.srg.marketsim.Keys.SimLength;
import edu.umich.srg.marketsim.Keys.Spread;
import edu.umich.srg.marketsim.Price;
import edu.umich.srg.marketsim.Sim;
import edu.umich.srg.marketsim.TimeStamp;
import edu.umich.srg.marketsim.fundamental.Fundamental;
import edu.umich.srg.marketsim.fundamental.FundamentalView;
import edu.umich.srg.marketsim.fundamental.NoisyFundamentalEstimator;
import edu.umich.srg.marketsim.market.Market;
import edu.umich.srg.marketsim.market.Market.MarketView;
import edu.umich.srg.marketsim.market.OrderRecord;
import edu.umich.srg.marketsim.strategy.MarketMakerLadder;

import java.util.Collection;
import java.util.Random;

public class FundamentalMarketMaker implements Agent {
  // TODO Add truncation

  private final Random rand;
  private final Sim sim;
  private final MarketView market;
  private final LongDistribution arrivalDistribution;

  private final FundamentalView fundamental;
  private final NoisyFundamentalEstimator estimator;

  private final double halfSpread;
  private final int rungThickness;
  private final MarketMakerLadder strategy;

  /** Basic constructor for a simple market maker. */
  public FundamentalMarketMaker(Sim sim, Market market, Fundamental fundamental, Spec spec,
      Random rand) {
    this.sim = sim;
    this.market = market.getView(this, TimeStamp.ZERO);
    this.arrivalDistribution = Geometric.withSuccessProbability(spec.get(ArrivalRate.class));

    this.fundamental = FundamentalView.create(sim, fundamental, TimeStamp.ZERO,
        spec.get(FundamentalObservationVariance.class), rand);
    this.estimator =
        NoisyFundamentalEstimator.create(spec.get(SimLength.class), spec.get(FundamentalMean.class),
            spec.get(FundamentalMeanReversion.class), spec.get(FundamentalShockVar.class),
            spec.get(FundamentalObservationVariance.class), spec.get(PriceVarEst.class));

    this.halfSpread = spec.get(Spread.class) / 2;
    this.rungThickness = spec.get(RungThickness.class);
    this.strategy =
        new MarketMakerLadder(spec.get(RungSep.class), spec.get(NumRungs.class), false, false);
    this.rand = rand;
  }

  public static FundamentalMarketMaker createFromSpec(Sim sim, Fundamental fundamental,
      Collection<Market> markets, Market market, Spec spec, Random rand) {
    return new FundamentalMarketMaker(sim, market, fundamental, spec, rand);
  }

  private void scheduleNextArrival() {
    sim.scheduleIn(TimeStamp.of(1 + arrivalDistribution.sample(rand)), this::strategy);
  }

  private void strategy() {
    for (OrderRecord o : ImmutableList.copyOf(market.getActiveOrders())) {
      market.withdrawOrder(o);
    }

    estimator.addFundamentalObservation(sim.getCurrentTime(), fundamental.getFundamental());
    double fundamentalPrice = estimator.estimate();

    strategy.createLadder(Price.of(fundamentalPrice - halfSpread),
        Price.of(fundamentalPrice + halfSpread)).forEach(order -> {
          market.submitOrder(order.getType(), order.getPrice(), rungThickness);
        });

    scheduleNextArrival();
  }

  @Override
  public void initilaize() {
    scheduleNextArrival();
  }

  @Override
  public double payoffForExchange(int position, OrderType type) {
    // No Private Value
    return 0;
  }

  @Override
  public String toString() {
    return "FMM " + Integer.toString(System.identityHashCode(this), 36).toUpperCase();
  }

}
