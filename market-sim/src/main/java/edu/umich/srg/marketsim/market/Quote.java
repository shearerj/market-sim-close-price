package edu.umich.srg.marketsim.market;

import static com.google.common.base.Preconditions.checkNotNull;

import edu.umich.srg.marketsim.Price;
import edu.umich.srg.util.Optionals;

import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;

/** Container for Quote data. */
public class Quote implements Serializable {

  private static final Quote empty = new Quote(Optional.empty(), 0, Optional.empty(), 0);

  private final Optional<Price> ask;
  private final Optional<Price> bid;
  private final int bidDepth;
  private final int askDepth;

  Quote(Optional<Price> bid, int bidDepth, Optional<Price> ask, int askDepth) {
    this.ask = checkNotNull(ask);
    this.bid = checkNotNull(bid);
    this.bidDepth = bidDepth;
    this.askDepth = askDepth;
  }

  static Quote empty() {
    return empty;
  }

  public Optional<Price> getAskPrice() {
    return ask;
  }

  public Optional<Price> getBidPrice() {
    return bid;
  }

  public int getBidDepth() {
    return bidDepth;
  }

  public int getAskDepth() {
    return askDepth;
  }

  /** True if the quote is defined (has an ask and a bid price). */
  public boolean isDefined() {
    return ask.isPresent() && bid.isPresent();
  }

  /** bid-ask spread of the quote. */
  public double getSpread() {
    return Optionals.apply((aq, bq) -> aq.doubleValue() - bq.doubleValue(), ask, bid)
        .orElse(Double.POSITIVE_INFINITY);
  }

  /** Return the midquote. */
  public double getMidquote() {
    return Optionals.apply((aq, bq) -> (aq.doubleValue() + bq.doubleValue()) / 2, ask, bid)
        .orElse(Double.NaN);
  }

  @Override
  public int hashCode() {
    return Objects.hash(ask, bid);
  }

  @Override
  public boolean equals(Object other) {
    if (other == null || !(other instanceof Quote)) {
      return false;
    } else {
      Quote that = (Quote) other;
      return Objects.equals(ask, that.ask) && Objects.equals(bid, that.bid);
    }
  }

  @Override
  public String toString() {
    return "(Bid: " + (bid.isPresent() ? bid.get() : "- ") + ", Ask: "
        + (ask.isPresent() ? ask.get() : "- ") + ')';
  }

  private static final long serialVersionUID = 1;

}
