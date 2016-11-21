package edu.umich.srg.marketsim.agent;

import com.google.gson.JsonObject;

import edu.umich.srg.fourheap.OrderType;
import edu.umich.srg.marketsim.Price;
import edu.umich.srg.marketsim.market.Market.MarketView;
import edu.umich.srg.marketsim.market.OrderRecord;

public interface Agent {

  /** Anything the agent needs to set up initially e.g. a first arrival. */
  void initilaize();

  /**
   * The id of the agent, used for consistent random execution. This should not be based off of the
   * system id of the agent object.
   */
  int getId();

  /** The payoff for the agent for making an exchange at position. */
  double payoffForExchange(int position, OrderType type);

  /** A json object of any miscellaneous features an agent wishes to compute. */
  default JsonObject getFeatures() {
    return new JsonObject();
  }

  // Notifications

  /**
   * Called when an order submitted by MarketView::submitOrder actually reaches a market. Unless
   * there is latency in order information, this is fairly unnecessary.
   */
  default void notifyOrderSubmitted(OrderRecord order) {}

  /**
   * Similar to notifyOrderSubmitted, this is called when an order is actually withdrawn. If there
   * is no latency, this will be called as soon as MarketView::withdrawOrder is. If there is, it
   * might never be called, if the order transacts before it can be withdrawn.
   */
  default void notifyOrderWithrawn(OrderRecord order, int quantity) {}

  /** Called when one of an agent's existing orders transacts. */
  default void notifyOrderTransacted(OrderRecord order, Price price, int quantity) {}

  /** Called when the markets quote updates, it may still be the same as before. */
  default void notifyQuoteUpdated(MarketView market) {}

  /**
   * Called when a market processes a transaction. This is usually called at the same time as
   * quoteUpdated, but won't be called if an order setting the spread is cancelled.
   */
  default void notifyTransaction(MarketView market, Price price, int quantity) {}

}
