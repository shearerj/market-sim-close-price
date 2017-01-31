package edu.umich.srg.marketsim.agent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import edu.umich.srg.egtaonline.spec.Spec;
import edu.umich.srg.marketsim.Keys;
import edu.umich.srg.marketsim.Keys.ArrivalRate;
import edu.umich.srg.marketsim.Keys.FinalFrac;
import edu.umich.srg.marketsim.Keys.FundamentalObservationVariance;
import edu.umich.srg.marketsim.Keys.InitialFrac;
import edu.umich.srg.marketsim.Keys.MaxPosition;
import edu.umich.srg.marketsim.Keys.PriceVarEst;
import edu.umich.srg.marketsim.Keys.PrivateValueVar;
import edu.umich.srg.marketsim.Keys.Rmax;
import edu.umich.srg.marketsim.Keys.Rmin;
import edu.umich.srg.marketsim.Keys.SimLength;
import edu.umich.srg.marketsim.Keys.Thresh;
import edu.umich.srg.marketsim.fundamental.ConstantFundamental;
import edu.umich.srg.marketsim.fundamental.Fundamental;
import edu.umich.srg.marketsim.market.CallMarket;
import edu.umich.srg.marketsim.market.Market;
import edu.umich.srg.marketsim.testing.MockSim;
import edu.umich.srg.testing.Repeat;
import edu.umich.srg.testing.RepeatRule;

import org.junit.Rule;
import org.junit.Test;

import java.util.Random;

public class CallAgentTest {

  @Rule
  public RepeatRule repeatRule = new RepeatRule();

  private static final double eps = 1e-6;
  private static final Random rand = new Random();
  private static final Spec base = Spec.builder() //
      .putAll(Keys.DEFAULT_KEYS) //
      .put(ArrivalRate.class, 0.5) //
      .put(FundamentalObservationVariance.class, 100d) //
      .put(MaxPosition.class, 1).put(Thresh.class, 1d) //
      .put(PrivateValueVar.class, 100d) //
      .put(Rmin.class, 20) //
      .put(Rmax.class, 120) //
      .put(PriceVarEst.class, Double.POSITIVE_INFINITY) //
      .put(SimLength.class, 120l) //
      .build();

  @Test
  public void deterministicTest() {
    Spec spec = Spec.fromDefaultPairs(base, InitialFrac.class, 0d, FinalFrac.class, 0d);
    MockSim sim = new MockSim();
    Fundamental fund = ConstantFundamental.create(1e9, base.get(SimLength.class));
    Market market = CallMarket.create(sim, fund, 25, rand); // 25 clearing interval
    CallAgent agent = new CallAgent(sim, market, fund, spec, rand);

    sim.setTime(0);
    assertEquals(120, agent.getDesiredSurplus(), 1e-6);
    sim.setTime(5);
    assertEquals(100, agent.getDesiredSurplus(), 1e-6);
    sim.setTime(10);
    assertEquals(80, agent.getDesiredSurplus(), 1e-6);
    sim.setTime(15);
    assertEquals(60, agent.getDesiredSurplus(), 1e-6);
    sim.setTime(20);
    assertEquals(40, agent.getDesiredSurplus(), 1e-6);
    sim.setTime(25); // Cycle back
    assertEquals(120, agent.getDesiredSurplus(), 1e-6);

    sim.setTime(110); // Short final clearing interval
    assertEquals(70, agent.getDesiredSurplus(), 1e-6);

    // At final time, it should be no shading
    sim.setTime(120);
    assertEquals(20, agent.getDesiredSurplus(), 1e-6);
  }

  @Test
  /**
   * There was a singularity in the math when the call interval aligns with the final time, and an
   * agent arrives then.
   */
  public void alignedTest() {
    Spec spec = Spec.fromDefaultPairs(base, InitialFrac.class, 0d, FinalFrac.class, 0d);
    MockSim sim = new MockSim();
    Fundamental fund = ConstantFundamental.create(1e9, base.get(SimLength.class));
    Market market = CallMarket.create(sim, fund, 20, rand); // 25 clearing interval
    CallAgent agent = new CallAgent(sim, market, fund, spec, rand);

    // At final time, it should be no shading
    sim.setTime(120);
    assertEquals(20, agent.getDesiredSurplus(), 1e-6);
  }

  @Repeat(100)
  @Test
  public void randomTest() {
    double initialFrac = rand.nextDouble();
    double finalFrac = rand.nextDouble();
    int clearInterval = 60;
    Spec spec =
        Spec.fromDefaultPairs(base, InitialFrac.class, initialFrac, FinalFrac.class, finalFrac);
    MockSim sim = new MockSim();
    Fundamental fund = ConstantFundamental.create(1e9, base.get(SimLength.class));
    Market market = CallMarket.create(sim, fund, clearInterval, rand); // 25 clearing interval
    CallAgent agent = new CallAgent(sim, market, fund, spec, rand);

    double rmax = spec.get(Rmax.class);
    double rmin = spec.get(Rmin.class);
    double delta = rmax - rmin;

    double upperBound = rmax;
    double lowerBound = rmax - initialFrac * delta;
    double upperDelta = (1 - finalFrac) * delta / clearInterval;
    double lowerDelta = (1 - initialFrac) * delta / clearInterval;

    for (int t = 0; t < clearInterval; ++t) {
      sim.setTime(t);
      double utility = agent.getDesiredSurplus();
      assertTrue(lowerBound - eps <= utility && utility <= upperBound + eps);
      upperBound -= upperDelta;
      lowerBound -= lowerDelta;
    }
  }

}
