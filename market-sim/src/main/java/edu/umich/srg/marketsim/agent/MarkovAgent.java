package edu.umich.srg.marketsim.agent;

import edu.umich.srg.distributions.Uniform;
import edu.umich.srg.distributions.Uniform.IntUniform;
import edu.umich.srg.egtaonline.spec.Spec;
import edu.umich.srg.marketsim.Keys.FundamentalObservationVariance;
import edu.umich.srg.marketsim.Keys.PriceVarEst;
import edu.umich.srg.marketsim.Keys.Rmax;
import edu.umich.srg.marketsim.Keys.Rmin;
import edu.umich.srg.marketsim.Keys.ShareEstimates;
import edu.umich.srg.marketsim.Sim;
import edu.umich.srg.marketsim.fundamental.Fundamental;
import edu.umich.srg.marketsim.fundamental.GaussianFundamentalView;
import edu.umich.srg.marketsim.fundamental.GaussianFundamentalView.GaussableView;
import edu.umich.srg.marketsim.market.Market;
import edu.umich.srg.marketsim.observer.MarkovObserver;
import edu.umich.srg.marketsim.strategy.SharedGaussianView;

import java.util.Collection;
import java.util.Random;

/**
 * This agent gets noisy observations of the fundamental and uses markov assumptions of the price
 * series to get more refined estimates of the final fundamental.
 */
public class MarkovAgent extends ASimpleAgent {

  private final GaussianFundamentalView fundamental;
  private final IntUniform shadingDistribution;

  /** Standard constructor for the Markov agent. */
  public MarkovAgent(Sim sim, Market market, Fundamental fundamental, Spec spec, Random rand) {
    super(sim, market, fundamental, spec, rand);
    if (spec.get(ShareEstimates.class)) {
      this.fundamental = SharedGaussianView.create(sim, fundamental, rand,
          spec.get(FundamentalObservationVariance.class));
    } else {
      this.fundamental = ((GaussableView) fundamental.getView(sim)).addNoise(rand,
          spec.get(FundamentalObservationVariance.class));
    }
    this.shadingDistribution = Uniform.closed(spec.get(Rmin.class), spec.get(Rmax.class));
    double priceVarEst = spec.get(PriceVarEst.class);
    if (Double.isFinite(priceVarEst)) {
      market.addTransactionObserver(MarkovObserver.create(this.fundamental, priceVarEst));
    }
  }

  public static MarkovAgent createFromSpec(Sim sim, Fundamental fundamental,
      Collection<Market> markets, Market market, Spec spec, Random rand) {
    return new MarkovAgent(sim, market, fundamental, spec, rand);
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
    return "Markov";
  }

}
