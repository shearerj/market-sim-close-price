package edu.umich.srg.marketsim.agent;

import edu.umich.srg.distributions.Uniform;
import edu.umich.srg.distributions.Uniform.IntUniform;
import edu.umich.srg.egtaonline.spec.Spec;
import edu.umich.srg.marketsim.Keys.Rmax;
import edu.umich.srg.marketsim.Keys.Rmin;
import edu.umich.srg.marketsim.Sim;
import edu.umich.srg.marketsim.fundamental.Fundamental;
import edu.umich.srg.marketsim.fundamental.Fundamental.FundamentalView;
import edu.umich.srg.marketsim.market.Market;

import java.util.Collection;
import java.util.Random;

public class ZirAgent extends StandardMarketAgent {

  private final FundamentalView fundamental;
  private final IntUniform shadingDistribution;

  /** Standard constructor for ZIR agent. */
  public ZirAgent(Sim sim, Market market, Fundamental fundamental, Spec spec, Random rand) {
    super(sim, market, spec, rand);
    this.fundamental = fundamental.getView(sim);
    this.shadingDistribution = Uniform.closed(spec.get(Rmin.class), spec.get(Rmax.class));
  }

  public static ZirAgent createFromSpec(Sim sim, Fundamental fundamental,
      Collection<Market> markets, Market market, Spec spec, Random rand) {
    return new ZirAgent(sim, market, fundamental, spec, rand);
  }

  @Override
  protected double getDesiredSurplus() {
    return shadingDistribution.sample(rand);
  }

  @Override
  protected double getFinalFundamentalEstiamte() {
    return fundamental.getEstimatedFinalFundamental();
  }

  @Override
  protected String name() {
    return "ZIR";
  }

}
