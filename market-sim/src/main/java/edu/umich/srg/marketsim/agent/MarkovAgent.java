package edu.umich.srg.marketsim.agent;

import edu.umich.srg.egtaonline.spec.Spec;
import edu.umich.srg.marketsim.Keys.FundamentalMean;
import edu.umich.srg.marketsim.Keys.FundamentalMeanReversion;
import edu.umich.srg.marketsim.Keys.FundamentalObservationVariance;
import edu.umich.srg.marketsim.Keys.FundamentalShockVar;
import edu.umich.srg.marketsim.Keys.PriceVarEst;
import edu.umich.srg.marketsim.Keys.SimLength;
import edu.umich.srg.marketsim.Price;
import edu.umich.srg.marketsim.Sim;
import edu.umich.srg.marketsim.fundamental.Fundamental;
import edu.umich.srg.marketsim.fundamental.NoisyGaussianMeanRevertingView;
import edu.umich.srg.marketsim.market.Market;
import edu.umich.srg.marketsim.market.Market.MarketView;

import java.util.Collection;
import java.util.Random;

/**
 * This agent gets noisy observations of the fundamental and uses markov assumptions of the price
 * series to get more refined estimates of the final fundamental.
 */
public class MarkovAgent extends StandardMarketAgent {

  private final double transactionVariance;
  private final NoisyGaussianMeanRevertingView fundamental;

  /** Standard constructor for the Markov agent. */
  public MarkovAgent(Sim sim, Market market, Fundamental fundamental, Spec spec, Random rand) {
    super(sim, market, spec, rand);
    this.transactionVariance = spec.get(PriceVarEst.class);
    this.fundamental =
        NoisyGaussianMeanRevertingView.create(sim, fundamental, rand, spec.get(SimLength.class),
            spec.get(FundamentalMean.class), spec.get(FundamentalMeanReversion.class),
            spec.get(FundamentalShockVar.class), spec.get(FundamentalObservationVariance.class));
  }

  public static MarkovAgent createFromSpec(Sim sim, Fundamental fundamental,
      Collection<Market> markets, Market market, Spec spec, Random rand) {
    return new MarkovAgent(sim, market, fundamental, spec, rand);
  }

  @Override
  public void notifyTransaction(MarketView market, Price price, int quantity) {
    fundamental.addObservation(price.doubleValue(), quantity, transactionVariance);
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
