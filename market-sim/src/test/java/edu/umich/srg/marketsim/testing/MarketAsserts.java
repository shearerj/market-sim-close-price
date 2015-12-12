package edu.umich.srg.marketsim.testing;

import static org.junit.Assert.assertEquals;

import com.google.common.base.Optional;

import edu.umich.srg.marketsim.Price;
import edu.umich.srg.marketsim.market.Quote;

public interface MarketAsserts {

  /** Prices are null for absent prices (for convenience) */
  static void assertQuote(Quote quote, Price bid, Price ask) {
    assertEquals("Incorrect ASK", Optional.fromNullable(ask), quote.getAskPrice());
    assertEquals("Incorrect BID", Optional.fromNullable(bid), quote.getBidPrice());
  }

}
