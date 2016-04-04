package edu.umich.srg.marketsim;

import static org.junit.Assert.assertEquals;

import com.google.gson.JsonObject;

import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

import edu.umich.srg.egtaonline.Log;
import edu.umich.srg.egtaonline.Log.Level;
import edu.umich.srg.egtaonline.spec.Spec;
import edu.umich.srg.marketsim.Keys.ArrivalRate;
import edu.umich.srg.marketsim.Keys.FundamentalMean;
import edu.umich.srg.marketsim.Keys.FundamentalMeanReversion;
import edu.umich.srg.marketsim.Keys.FundamentalShockProb;
import edu.umich.srg.marketsim.Keys.FundamentalShockVar;
import edu.umich.srg.marketsim.Keys.MaxPosition;
import edu.umich.srg.marketsim.Keys.PrivateValueVar;
import edu.umich.srg.marketsim.Keys.Rmax;
import edu.umich.srg.marketsim.Keys.Rmin;
import edu.umich.srg.marketsim.Keys.SimLength;
import edu.umich.srg.marketsim.Keys.Thresh;
import edu.umich.srg.marketsim.agent.NoiseAgent;
import edu.umich.srg.marketsim.agent.ZirAgent;
import edu.umich.srg.marketsim.fundamental.ConstantFundamental;
import edu.umich.srg.marketsim.fundamental.Fundamental;
import edu.umich.srg.marketsim.fundamental.GaussianMeanReverting;
import edu.umich.srg.marketsim.market.CdaMarket;
import edu.umich.srg.marketsim.market.Market;
import edu.umich.srg.marketsim.privatevalue.PrivateValue;
import edu.umich.srg.marketsim.privatevalue.PrivateValues;
import edu.umich.srg.marketsim.testing.MockAgent;
import edu.umich.srg.marketsim.testing.NullWriter;
import edu.umich.srg.testing.Asserts;
import edu.umich.srg.testing.Repeat;
import edu.umich.srg.testing.RepeatRule;
import edu.umich.srg.testing.TestBools;
import edu.umich.srg.testing.TestInts;

import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@RunWith(Theories.class)
public class FeaturesTest {

  private static final Random rand = new Random();
  private static final double tol = 1e-6;
  private static final Log log = Log.create(Level.DEBUG, NullWriter.get(), l -> "");
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

  @Repeat(2)
  @Theory
  public void simpleRandomTest(@TestInts({10}) int numAgents,
      @TestBools({false, true}) boolean intermediate) {
    Fundamental fundamental = GaussianMeanReverting.create(rand, spec.get(FundamentalMean.class),
        spec.get(FundamentalMeanReversion.class), spec.get(FundamentalShockVar.class),
        spec.get(FundamentalShockProb.class));
    MarketSimulator sim = MarketSimulator.create(fundamental, log, rand);
    Market cda = sim.addMarket(CdaMarket.create(sim));
    for (int i = 0; i < numAgents; ++i) {
      sim.addAgent(new ZirAgent(sim, cda, fundamental, spec, rand));
    }
    if (intermediate) {
      sim.addAgent(new NoiseAgent(sim, cda, spec, rand));
    }
    sim.initialize();
    sim.executeUntil(TimeStamp.of(spec.get(SimLength.class)));
    JsonObject features = sim.computeFeatures();

    double maxSurplus = features.get("max_surplus").getAsDouble();
    double totalSurplus =
        features.get("total_surplus").getAsDouble() + features.get("im_surplus_loss").getAsDouble()
            + features.get("em_surplus_loss").getAsDouble();
    assertEquals(maxSurplus, totalSurplus, tol);
  }

  /** Test that intermediary improves surplus. */
  @Theory
  public void intermediateBenefitTest(@TestInts({2}) int numAgents) {
    // Private Value of agent that wants to buy
    PrivateValue pv = PrivateValues.fromMarginalBuys(new double[] {10, 3});

    // First create simulation with only "background" agents
    MarketSimulator sim = MarketSimulator.create(ConstantFundamental.create(0), log, rand);
    for (int i = 0; i < numAgents; ++i) {
      sim.addAgent(MockAgent.builder().privateValue(pv).build());
    }

    // Verify that surplus is 0 because they won't trade
    sim.initialize();
    double maxSurplus = sim.computeFeatures().get("max_surplus").getAsDouble();
    assertEquals(0, maxSurplus, tol);

    // Now add an intermediary (no private value)
    sim = MarketSimulator.create(ConstantFundamental.create(0), log, rand);
    for (int i = 0; i < numAgents; ++i) {
      sim.addAgent(MockAgent.builder().privateValue(pv).build());
    }
    sim.addAgent(MockAgent.create());

    // Assert that each agent trades with intermediary for 3 surplus
    sim.initialize();
    maxSurplus = sim.computeFeatures().get("max_surplus").getAsDouble();
    assertEquals(3 * numAgents, maxSurplus, tol);
  }

  /**
   * Test that in situation where agents could infinitely trade, max surplus still terminates due to
   * diminishing returns in private values.
   */
  @Test
  public void infiniteTradeTest() throws ExecutionException, InterruptedException {
    Asserts.assertCompletesIn(() -> {
      MarketSimulator sim = MarketSimulator.create(ConstantFundamental.create(0), log, rand);
      // Buyer
      sim.addAgent(MockAgent.builder()
          .privateValue(PrivateValues.fromMarginalBuys(new double[] {1, 1})).build());
      // Seller
      sim.addAgent(MockAgent.builder()
          .privateValue(PrivateValues.fromMarginalBuys(new double[] {-1, -1})).build());

      // Verify correct surplus is calculated
      sim.initialize();
      double maxSurplus = sim.computeFeatures().get("max_surplus").getAsDouble();
      assertEquals(2, maxSurplus, tol);
    } , 5, TimeUnit.SECONDS);
  }

}
