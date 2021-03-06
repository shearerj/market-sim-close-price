package edu.umich.srg.marketsim;

import static edu.umich.srg.fourheap.OrderType.BUY;
import static edu.umich.srg.fourheap.OrderType.SELL;
import static edu.umich.srg.testing.Asserts.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import edu.umich.srg.egtaonline.spec.Spec;
import edu.umich.srg.fourheap.OrderType;
import edu.umich.srg.marketsim.Keys.ArrivalRate;
import edu.umich.srg.marketsim.Keys.FundamentalMean;
import edu.umich.srg.marketsim.Keys.FundamentalMeanReversion;
import edu.umich.srg.marketsim.Keys.FundamentalShockVar;
import edu.umich.srg.marketsim.Keys.MaxPosition;
import edu.umich.srg.marketsim.Keys.PrivateValueVar;
import edu.umich.srg.marketsim.Keys.Rmax;
import edu.umich.srg.marketsim.Keys.Rmin;
import edu.umich.srg.marketsim.Keys.SimLength;
import edu.umich.srg.marketsim.Keys.Thresh;
import edu.umich.srg.marketsim.agent.Agent;
import edu.umich.srg.marketsim.agent.NoiseAgent;
import edu.umich.srg.marketsim.agent.ZiAgent;
import edu.umich.srg.marketsim.fundamental.ConstantFundamental;
import edu.umich.srg.marketsim.fundamental.Fundamental;
import edu.umich.srg.marketsim.fundamental.GaussianMeanReverting;
import edu.umich.srg.marketsim.market.CdaMarket;
import edu.umich.srg.marketsim.market.Market;
import edu.umich.srg.marketsim.market.Market.AgentInfo;
import edu.umich.srg.marketsim.market.Market.MarketView;
import edu.umich.srg.marketsim.privatevalue.PrivateValue;
import edu.umich.srg.marketsim.privatevalue.PrivateValues;
import edu.umich.srg.marketsim.testing.MockAgent;
import edu.umich.srg.testing.Repeat;
import edu.umich.srg.testing.RepeatRule;
import edu.umich.srg.testing.TestBools;
import edu.umich.srg.testing.TestInts;

import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@RunWith(Theories.class)
public class FeaturesTest {

  // FIXME Test that making agents submit true values for a single clear produces measured social
  // wellfare

  private static final Random rand = new Random();
  private static final double tol = 1e-5;
  private static final Spec spec = Spec.builder().putAll(Keys.DEFAULT_KEYS) //
      .put(ArrivalRate.class, 0.5) //
      .put(MaxPosition.class, 10) //
      .put(Thresh.class, 1.0) //
      .put(PrivateValueVar.class, 1000.0) //
      .put(Rmin.class, 0) //
      .put(Rmax.class, 100) //
      .put(SimLength.class, 20L) //
      .put(FundamentalMean.class, (double) Integer.MAX_VALUE / 2) //
      .put(FundamentalMeanReversion.class, 0.1) //
      .put(FundamentalShockVar.class, 100.0) //
      .build();

  @Rule
  public final RepeatRule repeatRule = new RepeatRule();

  @Repeat(10)
  @Theory
  public void simpleRandomTest(@TestInts({10}) int numAgents,
      @TestBools({false, true}) boolean intermediate) {
    Fundamental fundamental = GaussianMeanReverting.create(rand, spec.get(SimLength.class),
        spec.get(FundamentalMean.class), spec.get(FundamentalMeanReversion.class),
        spec.get(FundamentalShockVar.class));
    MarketSimulator sim = MarketSimulator.create(fundamental, rand);
    Market cda = sim.addMarket(CdaMarket.create(sim, fundamental));
    for (int i = 0; i < numAgents; ++i) {
      sim.addAgent(new ZiAgent(sim, cda, fundamental, spec, rand));
    }
    if (intermediate) {
      sim.addAgent(new NoiseAgent(sim, cda, spec, rand));
    }
    sim.initialize();
    sim.executeUntil(TimeStamp.of(spec.get(SimLength.class)));
    JsonObject features = sim.getFeatures();

    double surplus = features.get("total_surplus").getAsDouble();
    double maxSurplus = features.get("max_surplus").getAsDouble();
    double imSurplusLoss = features.get("im_surplus_loss").getAsDouble();
    double emSurplusLoss = features.get("em_surplus_loss").getAsDouble();

    // All of these should already be tested by asserts, but are included for completeness / in case
    // someone doesn't turn on asserts
    // Assert that surplus is nonnegative
    assertTrue(maxSurplus >= 0, "max surplus negative %f", maxSurplus);

    // Assert that max surpluses are greater than realized surplus
    assertTrue(surplus <= maxSurplus);
    // Assert that max surplus - losses = surplus
    assertEquals(surplus, maxSurplus - imSurplusLoss - emSurplusLoss, tol);
  }

  /** Test that intermediary improves surplus. */
  @Theory
  public void intermediateBenefitTest(@TestInts({2}) int numAgents) {
    // Private Value of agent that wants to buy
    PrivateValue pv = PrivateValues.fromMarginalBuys(new double[] {10, 3});

    // First create simulation with only "background" agents
    Fundamental fundamental = ConstantFundamental.create(0, 100);
    MarketSimulator sim = MarketSimulator.create(fundamental, rand);
    CdaMarket market = CdaMarket.create(sim, fundamental);
    sim.addMarket(market);
    for (int i = 0; i < numAgents; ++i) {
      Agent agent = MockAgent.builder().privateValue(pv).build();
      sim.addAgent(agent);
      market.getView(agent).submitOrder(BUY, Price.of(0), 2);
    }

    // Verify that surplus is 0 because they won't trade
    sim.initialize();
    double maxSurplus = sim.getFeatures().get("max_surplus").getAsDouble();
    assertEquals(0, maxSurplus, tol);

    // Now add an intermediary (no private value)
    sim = MarketSimulator.create(ConstantFundamental.create(0, 100), rand);
    market = CdaMarket.create(sim, fundamental);
    sim.addMarket(market);
    for (int i = 0; i < numAgents; ++i) {
      Agent agent = MockAgent.builder().privateValue(pv).build();
      sim.addAgent(agent);
      market.getView(agent).submitOrder(BUY, Price.of(0), 2);
    }
    Agent intermediary = MockAgent.create();
    sim.addAgent(intermediary);
    market.getView(intermediary).submitOrder(BUY, Price.of(0), 2 * numAgents);

    // Assert that each agent trades with intermediary for 3 surplus
    sim.initialize();
    maxSurplus = sim.getFeatures().get("max_surplus").getAsDouble();
    assertEquals(3 * numAgents, maxSurplus, tol);
  }

  /**
   * Test that in situation where agents could infinitely trade, max surplus still terminates due to
   * diminishing returns in private values. Because surplus is calcualtes relative to the number of
   * submissions, they need to submit at least two orders to verify that only one counts.
   */
  @Test
  public void multipleTradeTest() throws ExecutionException, InterruptedException {
    Fundamental fundamental = ConstantFundamental.create(0, 100);
    MarketSimulator sim = MarketSimulator.create(fundamental, rand);
    CdaMarket market = CdaMarket.create(sim, fundamental);
    sim.addMarket(market);
    // Buyer
    Agent buyer = MockAgent.builder()
        .privateValue(PrivateValues.fromMarginalBuys(new double[] {1, 1})).build();
    sim.addAgent(buyer);
    market.getView(buyer).submitOrder(BUY, Price.of(0), 2);
    // Seller
    Agent seller = MockAgent.builder()
        .privateValue(PrivateValues.fromMarginalBuys(new double[] {-1, -1})).build();
    sim.addAgent(seller);
    market.getView(seller).submitOrder(BUY, Price.of(0), 2);

    // Verify correct surplus is calculated
    sim.initialize();
    double maxSurplus = sim.getFeatures().get("max_surplus").getAsDouble();
    assertEquals(2, maxSurplus, tol);
  }

  /** Test that submissions is accurately counted. */
  @Test
  public void agentInfoTest() {
    Fundamental fundamental = ConstantFundamental.create(0, 100);
    MarketSimulator sim = MarketSimulator.create(fundamental, rand);
    Market cda = sim.addMarket(CdaMarket.create(sim, fundamental));

    // Buy 3 @ 100 then Sell 1 @ 120
    sim.addAgent(new MockAgent() {
      final MarketView view = cda.getView(this, TimeStamp.ZERO);

      @Override
      public void initilaize() {
        sim.scheduleIn(TimeStamp.of(1), () -> {
          view.submitOrder(BUY, Price.of(100), 3);
        });
        sim.scheduleIn(TimeStamp.of(2), () -> {
          view.submitOrder(SELL, Price.of(120), 1);
        });
      }
    });

    // Sell 2 @ 100 then Buy 1 @ 120
    sim.addAgent(new MockAgent() {
      final MarketView view = cda.getView(this, TimeStamp.ZERO);

      @Override
      public void initilaize() {
        sim.scheduleIn(TimeStamp.of(1), () -> {
          view.submitOrder(SELL, Price.of(100), 2);
        });
        sim.scheduleIn(TimeStamp.of(2), () -> {
          view.submitOrder(BUY, Price.of(120), 1);
        });
      }
    });

    sim.initialize();
    sim.executeUntil(TimeStamp.of(2));

    Map<Agent, ? extends AgentInfo> payoffs = sim.getAgentPayoffs();
    assertEquals(2, payoffs.size());
    assertEquals(0, payoffs.values().stream().mapToDouble(AgentInfo::getProfit).sum(), tol);

    Set<Long> payoffSet =
        payoffs.values().stream().map(i -> Math.round(i.getProfit())).collect(Collectors.toSet());
    assertEquals(ImmutableSet.of(-80L, 80L), payoffSet);

    Set<Integer> holdings =
        payoffs.values().stream().map(AgentInfo::getHoldings).collect(Collectors.toSet());
    assertEquals(ImmutableSet.of(-1, 1), holdings);

    Set<Integer> submissions =
        payoffs.values().stream().map(AgentInfo::getSubmissions).collect(Collectors.toSet());
    assertEquals(ImmutableSet.of(3, 4), submissions);

    Set<Integer> volumes =
        payoffs.values().stream().map(AgentInfo::getVolumeTraded).collect(Collectors.toSet());
    assertEquals(ImmutableSet.of(3), volumes);
  }

  @Test
  public void noTradeTest() {
    Fundamental fundamental = ConstantFundamental.create(0, 100);
    MarketSimulator sim = MarketSimulator.create(fundamental, rand);
    Market cda = sim.addMarket(CdaMarket.create(sim, fundamental));

    // Buyer willing to buy at 5
    sim.addAgent(new MockAgent() {
      final MarketView view = cda.getView(this, TimeStamp.ZERO);
      final PrivateValue pv = PrivateValues.fromMarginalBuys(new double[] {100, 5});

      @Override
      public void initilaize() {
        sim.scheduleIn(TimeStamp.of(1), () -> {
          view.submitOrder(BUY, Price.of(200), 2);
        });
      }

      @Override
      public double payoffForExchange(int position, OrderType type) {
        return pv.valueForExchange(position, type);
      }
    });

    // Seller willing to sell at 10
    sim.addAgent(new MockAgent() {
      final MarketView view = cda.getView(this, TimeStamp.ZERO);
      final PrivateValue pv = PrivateValues.fromMarginalBuys(new double[] {10, -100});

      @Override
      public void initilaize() {
        sim.scheduleIn(TimeStamp.of(1), () -> {
          view.submitOrder(SELL, Price.of(100), 1);
        });
      }

      @Override
      public double payoffForExchange(int position, OrderType type) {
        return pv.valueForExchange(position, type);
      }
    });

    sim.initialize();
    sim.executeUntil(TimeStamp.of(2));

    JsonObject features = sim.getFeatures();
    double surplus = features.get("total_surplus").getAsDouble();
    double maxSurplus = features.get("max_surplus").getAsDouble();
    double imSurplusLoss = features.get("im_surplus_loss").getAsDouble();
    double emSurplusLoss = features.get("em_surplus_loss").getAsDouble();

    // Verify that max surplus is 0
    assertEquals(0, maxSurplus, tol);
    assertEquals(surplus, maxSurplus - imSurplusLoss - emSurplusLoss, tol);
  }


  @Test
  public void fundmanetalTest() {
    Fundamental fundamental = GaussianMeanReverting.create(rand, 100, 100, 0.01, 2);
    MarketSimulator sim = MarketSimulator.create(fundamental, rand);
    Market cda = sim.addMarket(CdaMarket.create(sim, fundamental));
    Agent agent = new MockAgent();
    sim.addAgent(agent);
    MarketView view = cda.getView(agent, TimeStamp.ZERO);
    sim.initialize();
    sim.executeUntil(TimeStamp.of(1));

    view.submitOrder(BUY, Price.of(200), 2);
    view.submitOrder(SELL, Price.of(100), 1);

    sim.executeUntil(TimeStamp.of(2));

    JsonArray fund = sim.getFeatures().get("fundamental").getAsJsonArray();
    assertTrue(fund.size() > 0);
  }

}
