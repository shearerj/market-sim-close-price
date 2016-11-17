package edu.umich.srg.marketsim.agent;

import edu.umich.srg.egtaonline.spec.Spec;
import edu.umich.srg.marketsim.Keys.FundamentalObservationVariance;
import edu.umich.srg.marketsim.Keys.PriceVarEst;
import edu.umich.srg.marketsim.Price;
import edu.umich.srg.marketsim.Sim;
import edu.umich.srg.marketsim.fundamental.Fundamental;
import edu.umich.srg.marketsim.fundamental.GaussianFundamentalView;
import edu.umich.srg.marketsim.fundamental.GaussianFundamentalView.GaussableView;
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
  private final GaussianFundamentalView fundamental;

  /** Standard constructor for the Markov agent. */
  public MarkovAgent(Sim sim, Market market, Fundamental fundamental, Spec spec, Random rand) {
    super(sim, market, spec, rand);
    this.transactionVariance = spec.get(PriceVarEst.class);
    this.fundamental = ((GaussableView) fundamental.getView(sim)).addNoise(rand,
        spec.get(FundamentalObservationVariance.class));
  }

  public static MarkovAgent createFromSpec(Sim sim, Fundamental fundamental,
      Collection<Market> markets, Market market, Spec spec, Random rand) {
    return new MarkovAgent(sim, market, fundamental, spec, rand);
  }

  @Override
  public void notifyTransaction(MarketView market, Price price, int quantity) {
    fundamental.addObservation(price.doubleValue(), transactionVariance, quantity);
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
