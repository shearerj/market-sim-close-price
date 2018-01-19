package edu.umich.srg.marketsim.observer;

import edu.umich.srg.marketsim.market.Market;
import edu.umich.srg.marketsim.market.MarketObserver.QuoteObserver;
import edu.umich.srg.marketsim.market.Quote;
import edu.umich.srg.util.LruCache;

import java.util.Collections;
import java.util.Map;

public class GetQuoteObserver implements QuoteObserver {

  private static Map<Market, GetQuoteObserver> singletons =
      Collections.synchronizedMap(new LruCache<>(128));
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

  public static GetQuoteObserver create(Market market) {
    return singletons.computeIfAbsent(market, m -> new GetQuoteObserver());
  }

}
