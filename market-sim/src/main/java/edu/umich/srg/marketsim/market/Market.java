package edu.umich.srg.marketsim.market;

import com.google.common.collect.Multiset;
import com.google.gson.JsonObject;

import edu.umich.srg.fourheap.OrderType;
import edu.umich.srg.marketsim.Price;
import edu.umich.srg.marketsim.TimeStamp;
import edu.umich.srg.marketsim.agent.Agent;
import edu.umich.srg.marketsim.market.MarketObserver.QuoteObserver;
import edu.umich.srg.marketsim.market.MarketObserver.TransactionObserver;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

public interface Market {

  MarketView getView(Agent agent, TimeStamp latency);

  /** Get zero latency view. */
  default MarketView getView(Agent agent) {
    return getView(agent, TimeStamp.ZERO);
  }

  void clear();

  <T extends TransactionObserver> T addTransactionObserver(T obs);

  <Q extends QuoteObserver> Q addQuoteObserver(Q obs);

  Iterable<Entry<Agent, AgentInfo>> getAgentInfo();

  JsonObject getFeatures();
  
  double getBenchmark();

  // FIXME Remove market view, and instead make latency an agent feature that it can add
  interface MarketView {
	  
	TimeStamp getLatency();

    OrderRecord submitOrder(OrderType buyOrSell, Price price, int quantity);

    void withdrawOrder(OrderRecord record, int quantity);

    default void withdrawOrder(OrderRecord record) {
      withdrawOrder(record, getQuantity(record));
    }

    default void withdrawOrder(Multiset.Entry<OrderRecord> entry) {
      withdrawOrder(entry.getElement(), entry.getCount());
    }

    Multiset<OrderRecord> getActiveOrders();

    int getQuantity(OrderRecord record);

    double getProfit();

    int getHoldings();
    
    double getCurrentBenchmark();
    
    int getCurrentNumTransactions();
    
    ArrayList<Price> getBidVector();
    
    ArrayList<Price> getAskVector();

    // TODO Add last transaction price, maybe notification

  }

  interface AgentInfo {

    /** The total agent profit. */
    double getProfit();

    /** The agents net holdings at the end of simulation. */
    int getHoldings();

    /**
     * The number of times an agent submitted an order. Useful for calculating the maximum possible
     * surplus constrained by how many times an agent acted in the market.
     */
    int getSubmissions();

    /**
     * Gets the total number of orders traded. Useful in conjunction with holdings to look at trader
     * behavior. An HFT might have high volume relative to absolute position, while a background
     * trader should have them close to equal.
     */
    int getVolumeTraded();
  }

  class ImmutableAgentInfo implements AgentInfo, Serializable {

    private static final ImmutableAgentInfo empty = new ImmutableAgentInfo(0, 0, 0, 0);

    private final double profit;
    private final int holdings;
    private final int submissions;
    private final int volume;

    private ImmutableAgentInfo(double profit, int holdings, int submissions, int volume) {
      this.profit = profit;
      this.holdings = holdings;
      this.submissions = submissions;
      this.volume = volume;
    }

    public static ImmutableAgentInfo empty() {
      return empty;
    }

    public static ImmutableAgentInfo of(double profit, int holdings, int submissions, int volume) {
      return new ImmutableAgentInfo(profit, holdings, submissions, volume);
    }

    @Override
    public double getProfit() {
      return profit;
    }

    @Override
    public int getHoldings() {
      return holdings;
    }

    @Override
    public int getSubmissions() {
      return submissions;
    }

    @Override
    public int getVolumeTraded() {
      return volume;
    }

    @Override
    public String toString() {
      return String.format("<prof: %f, hold: %d, subs: %d, vol: %d>", profit, holdings, submissions,
          volume);
    }


    private static final long serialVersionUID = 1L;

  }

}
