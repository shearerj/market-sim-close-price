package edu.umich.srg.marketsim.agent;

import org.junit.Test;

import edu.umich.srg.egtaonline.spec.Spec;
import edu.umich.srg.marketsim.Keys;
import edu.umich.srg.marketsim.Keys.ArrivalRate;
import edu.umich.srg.marketsim.Keys.FundamentalObservationVariance;
import edu.umich.srg.marketsim.Keys.MaxPosition;
import edu.umich.srg.marketsim.Keys.PriceVarEst;
import edu.umich.srg.marketsim.Keys.PrivateValueVar;
import edu.umich.srg.marketsim.Keys.Rmax;
import edu.umich.srg.marketsim.Keys.Rmin;
import edu.umich.srg.marketsim.Keys.Thresh;
import edu.umich.srg.marketsim.Sim;
import edu.umich.srg.marketsim.fundamental.ConstantFundamental;
import edu.umich.srg.marketsim.fundamental.Fundamental;
import edu.umich.srg.marketsim.fundamental.GaussianJump;
import edu.umich.srg.marketsim.fundamental.GaussianMeanReverting;
import edu.umich.srg.marketsim.market.CdaMarket;
import edu.umich.srg.marketsim.market.Market;
import edu.umich.srg.marketsim.testing.MockSim;

import java.util.Random;

public class MarkovAgentTest {

  private static final Random rand = new Random();
  private static final Spec spec = Spec.builder() //
      .putAll(Keys.DEFAULT_KEYS) //
      .put(ArrivalRate.class, 0.5) //
      .put(FundamentalObservationVariance.class, 100d) //
      .put(MaxPosition.class, 1).put(Thresh.class, 1d) //
      .put(PrivateValueVar.class, 100d) //
      .put(Rmin.class, 0) //
      .put(Rmax.class, 0) //
      .put(PriceVarEst.class, Double.POSITIVE_INFINITY) //
      .build();

  @Test
  public void fundamentalConstructionTest() {
    Sim sim = new MockSim();
    Market market = CdaMarket.create(sim, ConstantFundamental.create(0, 100));

    // Constant
    new MarkovAgent(sim, market, ConstantFundamental.create(0, 100), spec, rand);
    new MarkovAgent(sim, market, GaussianMeanReverting.create(rand, 1000, 0, 0, 0), spec, rand);
    new MarkovAgent(sim, market, GaussianJump.create(rand, 100, 0, 0, .5), spec, rand);
    new MarkovAgent(sim, market, GaussianJump.create(rand, 100, 0, 100, 0), spec, rand);

    // Random Walk
    new MarkovAgent(sim, market, GaussianMeanReverting.create(rand, 1000, 0, 0, 10), spec, rand);

    // IID Gaussian
    new MarkovAgent(sim, market, GaussianMeanReverting.create(rand, 1000, 0, 1, 10), spec, rand);

    // Mean Revering
    new MarkovAgent(sim, market, GaussianMeanReverting.create(rand, 1000, 0, 0.001, 10), spec,
        rand);
  }

  @Test(expected = ClassCastException.class)
  /** Test that invalid class fails. */
  public void fundamentalConstructionErrorTest() {
    Sim sim = new MockSim();
    Fundamental fundamental = GaussianJump.create(rand, 100, 0, 100, .5);
    Market market = CdaMarket.create(sim, fundamental);
    new MarkovAgent(sim, market, fundamental, spec, rand);
  }
}
