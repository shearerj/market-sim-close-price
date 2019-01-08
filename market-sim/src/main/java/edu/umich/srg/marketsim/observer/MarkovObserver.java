package edu.umich.srg.marketsim.observer;

import edu.umich.srg.marketsim.Price;
import edu.umich.srg.marketsim.fundamental.GaussianFundamentalView;
import edu.umich.srg.marketsim.market.MarketObserver.TransactionObserver;

public class MarkovObserver implements TransactionObserver {

  private final double transactionVariance;
  private final GaussianFundamentalView fundamental;

  private MarkovObserver(GaussianFundamentalView fundamental, double transactionVariance) {
    this.fundamental = fundamental;
    this.transactionVariance = transactionVariance;
  }

  @Override
  public void notifyTransaction(Price price, int quantity) {
    fundamental.addObservation(price.doubleValue(), transactionVariance, quantity);
  }

  public static MarkovObserver create(GaussianFundamentalView fundamental,
      double transactionVariance) {
    return new MarkovObserver(fundamental, transactionVariance);
  }

}