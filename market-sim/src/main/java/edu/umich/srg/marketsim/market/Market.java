package edu.umich.srg.marketsim.market;

import edu.umich.srg.collect.SparseList;
import edu.umich.srg.fourheap.Order.OrderType;
import edu.umich.srg.marketsim.Price;
import edu.umich.srg.marketsim.TimeStamp;
import edu.umich.srg.marketsim.agent.Agent;

import java.io.Serializable;
import java.util.Map.Entry;
import java.util.Set;

public interface Market {

  MarketView getView(Agent agent, TimeStamp latency);

  Iterable<Entry<Agent, AgentInfo>> getAgentInfo();

  MarketInfo getMarketInfo();

  public interface MarketView {

    TimeStamp getLatency();

    OrderRecord submitOrder(OrderType buyOrSell, Price price, int quantity);

    void withdrawOrder(OrderRecord record, int quantity);

    void withdrawOrder(OrderRecord record);

    Quote getQuote();

    Set<OrderRecord> getActiveOrders();

    double getProfit();

    int getHoldings();

    // TODO Add last transaction price, maybe notification

  }

  abstract static class SimpleMarketView implements MarketView {

    public void withdrawOrder(OrderRecord record) {
      withdrawOrder(record, record.getQuantity());
    }

  }

  static interface MarketInfo {

    Iterable<? extends SparseList.Entry<? extends Number>> getPrices();

  }

  static interface AgentInfo {

    double getProfit();

    int getHoldings();

  }

  static class ImmutableAgentInfo implements AgentInfo, Serializable {

    private static final ImmutableAgentInfo empty = new ImmutableAgentInfo(0, 0);

    private final double profit;
    private final int holdings;

    private ImmutableAgentInfo(double profit, int holdings) {
      this.profit = profit;
      this.holdings = holdings;
    }

    public static ImmutableAgentInfo empty() {
      return empty;
    }

    public static ImmutableAgentInfo of(double profit, int holdings) {
      return new ImmutableAgentInfo(profit, holdings);
    }

    @Override
    public double getProfit() {
      return profit;
    }

    @Override
    public int getHoldings() {
      return holdings;
    }

    private static final long serialVersionUID = 1;

  }

}
