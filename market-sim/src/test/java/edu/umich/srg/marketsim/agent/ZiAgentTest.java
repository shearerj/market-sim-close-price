package edu.umich.srg.marketsim.agent;

import static org.junit.Assert.assertEquals;

import com.google.common.collect.ImmutableSet;

import edu.umich.srg.egtaonline.spec.Spec;
import edu.umich.srg.marketsim.Keys;
import edu.umich.srg.marketsim.Keys.ArrivalRate;
import edu.umich.srg.marketsim.Keys.FundamentalMeanReversion;
import edu.umich.srg.marketsim.Keys.FundamentalObservationVariance;
import edu.umich.srg.marketsim.Keys.MaxPosition;
import edu.umich.srg.marketsim.Keys.PrivateValueVar;
import edu.umich.srg.marketsim.Keys.Rmax;
import edu.umich.srg.marketsim.Keys.Rmin;
import edu.umich.srg.marketsim.Keys.Sides;
import edu.umich.srg.marketsim.Keys.SimLength;
import edu.umich.srg.marketsim.Keys.SubmitDepth;
import edu.umich.srg.marketsim.Keys.Thresh;
import edu.umich.srg.marketsim.MarketSimulator;
import edu.umich.srg.marketsim.Sim;
import edu.umich.srg.marketsim.TimeStamp;
import edu.umich.srg.marketsim.agent.ZiAgent.OrderStyle;
import edu.umich.srg.marketsim.fundamental.ConstantFundamental;
import edu.umich.srg.marketsim.fundamental.Fundamental;
import edu.umich.srg.marketsim.fundamental.GaussianJump;
import edu.umich.srg.marketsim.fundamental.GaussianMeanReverting;
import edu.umich.srg.marketsim.market.CdaMarket;
import edu.umich.srg.marketsim.market.Market;
import edu.umich.srg.marketsim.market.Market.AgentInfo;
import edu.umich.srg.marketsim.market.Market.MarketView;
import edu.umich.srg.marketsim.market.Quote;
import edu.umich.srg.marketsim.testing.MockSim;

import org.junit.Test;

import java.util.Map;
import java.util.Random;

public class ZiAgentTest {

  private static final Random rand = new Random();
  private static final double fundamentalMean = 1e9;
  private static final Spec base =
      Spec.builder().putAll(Keys.DEFAULT_KEYS).put(ArrivalRate.class, 0.5).put(Rmin.class, 100)
          .put(Rmax.class, 500).put(MaxPosition.class, 5).put(PrivateValueVar.class, 1e3)
          .put(SimLength.class, 10L).put(FundamentalMeanReversion.class, 0d).build();

  // FIXME Test that agents created with same seed have same private value

  /** Test that both sides submits an order to both sides. */
  @Test
  public void bothSidesTest() {
    Spec spec = Spec.fromDefaultPairs(base, Sides.class, OrderStyle.BOTH);
    Fundamental fundamental =
        ConstantFundamental.create(fundamentalMean, spec.get(SimLength.class));
    MarketSimulator sim = MarketSimulator.create(fundamental, rand);
    Market cda = sim.addMarket(CdaMarket.create(sim, fundamental));

    ZiAgent agent = new ZiAgent(sim, cda, fundamental, spec, rand);
    MarketView view = cda.getView(agent);
    Quote quote;

    // Assert quote empty before
    quote = view.getQuote();
    assertEquals(0, quote.getAskDepth());
    assertEquals(0, quote.getBidDepth());

    agent.strategy();

    // Assert buy and sell afterwards
    quote = view.getQuote();
    assertEquals(1, quote.getAskDepth());
    assertEquals(1, quote.getBidDepth());
  }

  /** Test that setting submit depth works. */
  @Test
  public void multiOrderTest() {
    Spec spec = Spec.fromDefaultPairs(base, SubmitDepth.class, 2);
    Fundamental fundamental =
        ConstantFundamental.create(fundamentalMean, spec.get(SimLength.class));
    MarketSimulator sim = MarketSimulator.create(fundamental, rand);
    Market cda = sim.addMarket(CdaMarket.create(sim, fundamental));

    ZiAgent agent = new ZiAgent(sim, cda, fundamental, spec, rand);
    MarketView view = cda.getView(agent);
    Quote quote;

    // Assert quote empty before
    quote = view.getQuote();
    assertEquals(0, quote.getAskDepth());
    assertEquals(0, quote.getBidDepth());

    agent.strategy();

    // Assert 2 buys or 2 sells
    quote = view.getQuote();
    assertEquals(ImmutableSet.of(0, 2), ImmutableSet.of(quote.getAskDepth(), quote.getBidDepth()));
  }

  /** Test that both sides and submit depth submits multiple orders to both sides. */
  @Test
  public void bothSidesAndSubmitDepthTest() {
    Spec spec = Spec.fromDefaultPairs(base, Sides.class, OrderStyle.BOTH, SubmitDepth.class, 3);
    Fundamental fundamental =
        ConstantFundamental.create(fundamentalMean, spec.get(SimLength.class));
    MarketSimulator sim = MarketSimulator.create(fundamental, rand);
    Market cda = sim.addMarket(CdaMarket.create(sim, fundamental));

    ZiAgent agent = new ZiAgent(sim, cda, fundamental, spec, rand);
    MarketView view = cda.getView(agent);
    Quote quote;

    // Assert quote empty before
    quote = view.getQuote();
    assertEquals(0, quote.getAskDepth());
    assertEquals(0, quote.getBidDepth());

    agent.strategy();

    // Assert buy and sell afterwards
    quote = view.getQuote();
    assertEquals(3, quote.getAskDepth());
    assertEquals(3, quote.getBidDepth());
  }

  /** Test that setting submit depth works, but is clipped if max position would be exceeded. */
  @Test
  public void clippedMultiOrderTest() {
    Spec spec = Spec.fromDefaultPairs(base, SubmitDepth.class, 3, MaxPosition.class, 2);
    Fundamental fundamental =
        ConstantFundamental.create(fundamentalMean, spec.get(SimLength.class));
    MarketSimulator sim = MarketSimulator.create(fundamental, rand);
    Market cda = sim.addMarket(CdaMarket.create(sim, fundamental));

    ZiAgent agent = new ZiAgent(sim, cda, fundamental, spec, rand);
    MarketView view = cda.getView(agent);
    Quote quote;

    // Assert quote empty before
    quote = view.getQuote();
    assertEquals(0, quote.getAskDepth());
    assertEquals(0, quote.getBidDepth());

    agent.strategy();

    // Assert 2 buys or 2 sells
    quote = view.getQuote();
    assertEquals(ImmutableSet.of(0, 2), ImmutableSet.of(quote.getAskDepth(), quote.getBidDepth()));
  }

  @Test
  public void integrationTest() {
    int numAgents = 10;
    long simLength = 10;

    Fundamental fundamental = GaussianMeanReverting.create(new Random(), simLength, 1e9, 0, 0);
    MarketSimulator sim = MarketSimulator.create(fundamental, rand);
    Market cda = sim.addMarket(CdaMarket.create(sim, fundamental));
    for (int i = 0; i < numAgents; ++i) {
      sim.addAgent(new ZiAgent(sim, cda, fundamental, base, rand));
    }
    sim.initialize();
    sim.executeUntil(TimeStamp.of(simLength));

    Map<Agent, ? extends AgentInfo> payoffs = sim.getAgentPayoffs();
    assertEquals(numAgents, payoffs.size());
  }

  @Test
  public void fundamentalConstructionTest() {
    Sim sim = new MockSim();
    Market market = CdaMarket.create(sim, ConstantFundamental.create(0, 100));

    Spec spec = Spec.builder().putAll(base).put(FundamentalObservationVariance.class, 100d)
        .put(Thresh.class, 1d).put(PrivateValueVar.class, 100d).put(Rmin.class, 0)
        .put(Rmax.class, 0).build();

    // Constant
    new ZiAgent(sim, market, ConstantFundamental.create(0, 100), spec, rand);
    new ZiAgent(sim, market, GaussianMeanReverting.create(rand, 1000, 0, 0, 0), spec, rand);
    new ZiAgent(sim, market, GaussianJump.create(rand, 100, 0, 0, .5), spec, rand);
    new ZiAgent(sim, market, GaussianJump.create(rand, 100, 0, 100, 0), spec, rand);

    // Random Walk
    new ZiAgent(sim, market, GaussianMeanReverting.create(rand, 1000, 0, 0, 10), spec, rand);

    // IID Gaussian
    new ZiAgent(sim, market, GaussianMeanReverting.create(rand, 1000, 0, 1, 10), spec, rand);

    // Mean Revering
    new ZiAgent(sim, market, GaussianMeanReverting.create(rand, 1000, 0, 0.001, 10), spec, rand);
  }

  @Test(expected = ClassCastException.class)
  /** Test that invalid class fails. Can't have Gaussian Jump with Observation Variance */
  public void fundamentalConstructionErrorTest() {
    Sim sim = new MockSim();
    Fundamental fundamental = GaussianJump.create(rand, 100, 0, 100, .5);
    Market market = CdaMarket.create(sim, fundamental);
    Spec spec = Spec.builder().putAll(base).put(FundamentalObservationVariance.class, 100d).build();
    new ZiAgent(sim, market, fundamental, spec, rand);
  }

}
