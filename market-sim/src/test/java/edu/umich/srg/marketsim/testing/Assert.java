package edu.umich.srg.marketsim.testing;

import static org.junit.Assert.assertEquals;

import edu.umich.srg.marketsim.Price;
import edu.umich.srg.marketsim.market.Quote;

import java.util.Optional;

public class Assert {

  /** Prices are null for absent prices (for convenience) */
  public static void assertQuote(Quote quote, Price bid, Price ask) {
    assertEquals("Incorrect ASK", Optional.ofNullable(ask), quote.getAskPrice());
    assertEquals("Incorrect BID", Optional.ofNullable(bid), quote.getBidPrice());
  }
}

