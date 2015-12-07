package edu.umich.srg.marketsim.testing;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.junit.Assert.assertEquals;

import com.google.common.base.Optional;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;

import edu.umich.srg.marketsim.Price;
import edu.umich.srg.marketsim.market.Quote;

import java.util.Set;

public class Asserts {

  /** Prices are null for absent prices (for convenience) */
  public static void assertQuote(Quote quote, Price bid, Price ask) {
    assertEquals("Incorrect ASK", Optional.fromNullable(ask), quote.getAskPrice());
    assertEquals("Incorrect BID", Optional.fromNullable(bid), quote.getBidPrice());
  }

  public static <T> void assertSetEquals(Set<? extends T> expected, Set<? extends T> actual) {
    assertSetEquals(expected, actual, "Sets not equal");
  }

  public static <T> void assertSetEquals(Set<? extends T> expected, Set<? extends T> actual,
      String message) {
    if (checkNotNull(expected).equals(checkNotNull(actual)))
      return;

    SetView<? extends T> extra = Sets.difference(actual, expected),
        missing = Sets.difference(expected, actual);

    if (extra.isEmpty()) {
      throw new AssertionError(String.format("%s - missing: %s", message, missing));
    } else if (missing.isEmpty()) {
      throw new AssertionError(String.format("%s - extra: %s", message, extra));
    } else {
      throw new AssertionError(
          String.format("%s - missing: %s - extra: %s", message, missing, extra));
    }
  }

}

