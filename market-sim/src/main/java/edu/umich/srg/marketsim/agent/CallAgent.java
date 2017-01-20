package edu.umich.srg.marketsim.agent;

import static com.google.common.base.Preconditions.checkArgument;

import edu.umich.srg.distributions.Uniform;
import edu.umich.srg.egtaonline.spec.Spec;
import edu.umich.srg.marketsim.Keys.FinalFrac;
import edu.umich.srg.marketsim.Keys.InitialFrac;
import edu.umich.srg.marketsim.Keys.Rmax;
import edu.umich.srg.marketsim.Keys.Rmin;
import edu.umich.srg.marketsim.Keys.SimLength;
import edu.umich.srg.marketsim.Sim;
import edu.umich.srg.marketsim.fundamental.Fundamental;
import edu.umich.srg.marketsim.fundamental.Fundamental.FundamentalView;
import edu.umich.srg.marketsim.market.CallMarket;
import edu.umich.srg.marketsim.market.Market;

import java.util.Collection;
import java.util.Random;

public class CallAgent extends StandardMarketAgent {

  private final FundamentalView fundamental;
  private final long clearInterval;
  private final long finalTime;
  private final double rmin;
  private final double rmax;
  private final double finit;
  private final double ffin;

  /** Standard constructor for Call agent. */
  public CallAgent(Sim sim, Market market, Fundamental fundamental, Spec spec, Random rand) {
    super(sim, market, spec, rand);
    this.fundamental = fundamental.getView(sim);
    this.clearInterval = ((CallMarket) market).getClearingInterval();
    this.finalTime = spec.get(SimLength.class);
    this.rmin = spec.get(Rmin.class);
    this.rmax = spec.get(Rmax.class);
    this.finit = 1 - spec.get(InitialFrac.class);
    this.ffin = 1 - spec.get(FinalFrac.class);
    checkArgument(0 <= finit && finit <= 1, "initial fraction must be in [0, 1] : %f", 1 - finit);
    checkArgument(0 <= ffin && ffin <= 1, "final fraction must be in [0, 1] : %f", 1 - ffin);
  }

  public static CallAgent createFromSpec(Sim sim, Fundamental fundamental,
      Collection<Market> markets, Market market, Spec spec, Random rand) {
    return new CallAgent(sim, market, fundamental, spec, rand);
  }

  @Override
  protected double getDesiredSurplus() {
    /*
     * Being on a clearing interval counts as being at the start of the next one, so agents will
     * shade accordingly. Also, rminEff and rmaxEff are compressed, but they are correct and are
     * tested.
     */
    long current = sim.getCurrentTime().get();
    long next = (current / clearInterval + 1) * clearInterval;
    long delta = Math.min(next, finalTime) - next + clearInterval;
    double frac = (current - next + clearInterval) / (double) delta;
    double rminEff = finit * (frac - 1) * (rmin - rmax) + rmin;
    double rmaxEff = ffin * frac * (rmin - rmax) + rmax;
    return Uniform.closedOpen(rminEff, rmaxEff).sample(rand);
  }

  @Override
  protected double getFinalFundamentalEstiamte() {
    return fundamental.getEstimatedFinalFundamental();
  }

  @Override
  protected String name() {
    return "CA";
  }

}
