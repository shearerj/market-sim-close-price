package edu.umich.srg.marketsim.agent;

import com.google.gson.JsonObject;

import edu.umich.srg.fourheap.OrderType;
import edu.umich.srg.marketsim.Price;
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

  // FIXME How to handle this
  /** Called when one of an agent's existing orders transacts. */
  default void notifyOrderTransacted(OrderRecord order, Price price, int quantity) {}

}
