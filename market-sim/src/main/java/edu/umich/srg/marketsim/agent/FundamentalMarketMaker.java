package edu.umich.srg.marketsim.agent;

import com.google.common.collect.ImmutableList;

import edu.umich.srg.distributions.Distribution.LongDistribution;
import edu.umich.srg.distributions.Geometric;
import edu.umich.srg.egtaonline.spec.Spec;
import edu.umich.srg.fourheap.OrderType;
import edu.umich.srg.marketsim.Keys.ArrivalRate;
import edu.umich.srg.marketsim.Keys.NumRungs;
import edu.umich.srg.marketsim.Keys.RungSep;
import edu.umich.srg.marketsim.Keys.RungThickness;
import edu.umich.srg.marketsim.Keys.Spread;
import edu.umich.srg.marketsim.Price;
import edu.umich.srg.marketsim.Sim;
import edu.umich.srg.marketsim.TimeStamp;
import edu.umich.srg.marketsim.fundamental.Fundamental;
import edu.umich.srg.marketsim.fundamental.Fundamental.FundamentalView;
import edu.umich.srg.marketsim.market.Market;
import edu.umich.srg.marketsim.market.Market.MarketView;
import edu.umich.srg.marketsim.market.OrderRecord;
import edu.umich.srg.marketsim.strategy.MarketMakerLadder;

import java.util.Collection;
import java.util.Random;

public class FundamentalMarketMaker implements Agent {
  // TODO Add truncation

  private final Random rand;
  private final int id;
  private final Sim sim;
  private final MarketView market;
  private final LongDistribution arrivalDistribution;

  private final FundamentalView fundamental;

  private final double halfSpread;
  private final int rungThickness;
  private final MarketMakerLadder strategy;

  /** Basic constructor for a simple market maker. */
  public FundamentalMarketMaker(Sim sim, Market market, Fundamental fundamental, Spec spec,
      Random rand) {
    this.sim = sim;
    this.id = rand.nextInt();
    this.market = market.getView(this, TimeStamp.ZERO);
    this.arrivalDistribution = Geometric.withSuccessProbability(spec.get(ArrivalRate.class));
    this.fundamental = fundamental.getView(sim);

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

    double fundamentalPrice = fundamental.getEstimatedFinalFundamental();
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
  public int getBenchmarkDir() {
	  return 0;
  }
  
  @Override
  public double getContractHoldings() {
	  return 0;
  }
  
  @Override
  public double getRunningPayoff() {
	return -1;
  }

  @Override
  public int getId() {
    return id;
  }

  @Override
  public String toString() {
    return "FMM " + Integer.toUnsignedString(id, 36).toUpperCase();
  }

}
