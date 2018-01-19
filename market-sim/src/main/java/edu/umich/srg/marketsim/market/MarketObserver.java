package edu.umich.srg.marketsim.market;

import edu.umich.srg.marketsim.Price;

public interface MarketObserver {

  interface TransactionObserver {

    void notifyTransaction(Price price, int quantity);

  }

  interface QuoteObserver {

    void notifyQuote(Quote quote);

  }

}
