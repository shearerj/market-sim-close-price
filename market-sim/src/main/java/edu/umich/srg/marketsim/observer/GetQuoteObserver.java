package edu.umich.srg.marketsim.observer;


import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import edu.umich.srg.marketsim.market.Market;
import edu.umich.srg.marketsim.market.MarketObserver.QuoteObserver;
import edu.umich.srg.marketsim.market.Quote;

public class GetQuoteObserver implements QuoteObserver {

  private static LoadingCache<Market, GetQuoteObserver> singletons =
      CacheBuilder.newBuilder().weakKeys().build(CacheLoader.from(GetQuoteObserver::new));

  private Quote quote;

  private GetQuoteObserver() {
    this.quote = Quote.empty();
  }

  @Override
  public void notifyQuote(Quote quote) {
    this.quote = quote;
  }

  public Quote getQuote() {
    return quote;
  }

  /** Create a GetQuoteObserver. */
  public static GetQuoteObserver create(Market market) {
    return singletons.getUnchecked(market);
  }

}
