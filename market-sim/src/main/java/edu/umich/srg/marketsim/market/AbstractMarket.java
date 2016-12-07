package edu.umich.srg.marketsim.market;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.BiMap;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import edu.umich.srg.fourheap.FourHeap;
import edu.umich.srg.fourheap.MatchedOrders;
import edu.umich.srg.fourheap.Order;
import edu.umich.srg.fourheap.OrderType;
import edu.umich.srg.fourheap.Selector;
import edu.umich.srg.marketsim.Price;
import edu.umich.srg.marketsim.Sim;
import edu.umich.srg.marketsim.TimeStamp;
import edu.umich.srg.marketsim.agent.Agent;
import edu.umich.srg.marketsim.fundamental.Fundamental;
import edu.umich.srg.marketsim.fundamental.Fundamental.FundamentalView;
import edu.umich.srg.util.SummStats;

import java.io.Serializable;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.function.Function;

/**
 * Base class for all markets. This class provides almost all market functionality that one should
 * need for creating a market. The only thing an abstract market needs is a pricing rule. That is,
 * given a set of matched orders, what price is assigned to each transaction. By default the only
 * methods that schedules more activities is a clear and a quoteUpdate.
 */
abstract class AbstractMarket implements Market, Serializable {

  final Sim sim;
  private final FourHeap<Price, Long, AbstractMarketOrder> orderbook;
  private final PricingRule pricing;
  private long marketTime;

  // Bookkeeping
  private final FundamentalView fundView;
  private final Collection<AbstractMarketView> views;
  private final List<Entry<TimeStamp, Price>> prices;
  private final SummStats rmsd;
  private double maxDiff;
  private final SummStats transPrice;

  AbstractMarket(Sim sim, Fundamental fundamental, PricingRule pricing,
      Selector<AbstractMarketOrder> selector) {
    this.sim = sim;
    this.orderbook = FourHeap.create(selector);
    this.pricing = pricing;
    this.marketTime = 0;

    this.fundView = fundamental.getView(sim);
    this.views = new ArrayList<>();
    this.prices = new ArrayList<>();
    this.rmsd = SummStats.empty();
    this.maxDiff = 0;
    this.transPrice = SummStats.empty();
  }

  AbstractMarketOrder submitOrder(AbstractMarketView submitter, OrderType buyOrSell, Price price,
      int quantity) {
    AbstractMarketOrder order =
        new AbstractMarketOrder(submitter, buyOrSell, price, marketTime, sim.getCurrentTime());
    orderbook.submit(order, quantity);
    return order;
  }

  void withdrawOrder(AbstractMarketOrder order, int quantity) {
    orderbook.withdraw(order, quantity);
  }

  void clear() {
    Collection<MatchedOrders<Price, Long, AbstractMarketOrder>> matches = orderbook.clear();
    for (Entry<MatchedOrders<Price, Long, AbstractMarketOrder>, Price> pricedTrade : pricing
        .apply(matches)) {

      MatchedOrders<Price, Long, AbstractMarketOrder> matched = pricedTrade.getKey();
      Price price = pricedTrade.getValue();

      // Notify buyer
      AbstractMarketOrder buy = matched.getBuy();
      buy.submitter.transacted(buy, price, matched.getQuantity());


      // Notify seller
      AbstractMarketOrder sell = matched.getSell();
      sell.submitter.transacted(sell, price, matched.getQuantity());

      // Notify all agents of transaction
      for (AbstractMarketView view : views) {
        view.transaction(price, matched.getQuantity());
      }

      // Bookkeeping
      if (!prices.isEmpty() && Iterables.getLast(prices).getKey().equals(sim.getCurrentTime())) {
        prices.remove(prices.size() - 1);
      }
      prices.add(new AbstractMap.SimpleImmutableEntry<>(sim.getCurrentTime(), price));
      double diff = price.doubleValue() - fundView.getEstimatedFinalFundamental();
      rmsd.acceptNTimes(diff * diff, matched.getQuantity());
      maxDiff = Double.max(maxDiff, Math.abs(diff));
      transPrice.acceptNTimes(price, matched.getQuantity());
    }
  }

  void updateQuote() {
    Quote quote = new Quote(orderbook.getBidQuote(), orderbook.getBidDepth(),
        orderbook.getAskQuote(), orderbook.getAskDepth());
    for (AbstractMarketView view : views) {
      view.setQuote(quote);
    }
  }

  protected void incrementMarketTime() {
    marketTime++;
  }

  @Override
  public Iterable<Entry<Agent, AgentInfo>> getAgentInfo() {
    return Iterables.transform(views, v -> Maps.<Agent, AgentInfo>immutableEntry(v.getAgent(),
        ImmutableAgentInfo.of(v.getProfit(), v.getHoldings(), v.getSubmissions())));
  }

  @Override
  public MarketView getView(Agent agent, TimeStamp latency) {
    AbstractMarketView view = latency.equals(TimeStamp.ZERO)
        ? new AbstractImmediateMarketView(agent) : new AbstractLatentMarketView(agent, latency);
    views.add(view);
    return view;
  }

  @Override
  public JsonObject getFeatures() {
    JsonObject features = new JsonObject();

    features.addProperty("rmsd", Math.sqrt(rmsd.getAverage()));
    features.addProperty("max_diff", maxDiff);
    features.addProperty("trans_vol", transPrice.getStandardDeviation());

    JsonArray jprices = new JsonArray();
    for (Entry<TimeStamp, Price> obs : prices) {
      JsonArray point = new JsonArray();
      point.add(obs.getKey().get());
      point.add(obs.getValue());
      jprices.add(point);
    }
    features.add("prices", jprices);

    return features;
  }

  @Override
  public String toString() {
    return Integer.toUnsignedString(System.identityHashCode(this), 36).toUpperCase();
  }

  interface AbstractMarketView extends MarketView {

    void setQuote(Quote quote);

    void transacted(AbstractMarketOrder order, Price price, int quantity);

    void transaction(Price price, int quantity);

    Agent getAgent();

    double getTrueProfit();

    int getTrueHoldings();

  }

  class AbstractLatentMarketView implements AbstractMarketView {
    private final TimeStamp latency;
    private Quote quote;
    private final Agent agent;
    private double profit;
    private double observedProfit;
    private int holdings;
    private int submissions;
    private int observedHoldings;
    private Multiset<OrderRecord> orders;
    private final BiMap<OrderRecord, AbstractMarketOrder> recordMap;

    AbstractLatentMarketView(Agent agent, TimeStamp latency) {
      this.latency = latency;
      this.quote = Quote.empty();
      this.agent = agent;
      this.profit = 0;
      this.observedProfit = 0;
      this.holdings = 0;
      this.submissions = 0;
      this.observedHoldings = 0;
      this.orders = HashMultiset.create();
      this.recordMap = HashBiMap.create();
    }

    @Override
    public void setQuote(Quote quote) {
      sim.scheduleIn(latency, () -> {
        this.quote = quote;
        agent.notifyQuoteUpdated(this);
      });
    }

    @Override
    public void transaction(Price price, int quantity) {
      sim.scheduleIn(latency, () -> agent.notifyTransaction(this, price, quantity));
    }

    @Override
    public TimeStamp getLatency() {
      return latency;
    }

    @Override
    public OrderRecord submitOrder(OrderType buyOrSell, Price price, int quantity) {
      OrderRecord record = new Rec(buyOrSell, price);
      orders.add(record, quantity);

      sim.scheduleIn(latency, () -> {
        AbstractMarketOrder order =
            AbstractMarket.this.submitOrder(this, buyOrSell, price, quantity);
        submissions += quantity;

        sim.scheduleIn(latency, () -> {
          recordMap.put(record, order);
          agent.notifyOrderSubmitted(record);
        });
      });

      return record;
    }

    @Override
    public void withdrawOrder(OrderRecord record, int quantity) {
      orders.remove(record, quantity);
      AbstractMarketOrder order = recordMap.get(record);
      if (order == null) {
        return; // This will happen if the order transacted, but that information hasn't reached the
                // agent yet
      }

      sim.scheduleIn(latency, () -> {
        AbstractMarket.this.withdrawOrder(order, quantity);
        if (!orderbook.contains(order)) {
          recordMap.remove(record);
        }

        sim.scheduleIn(latency, () -> {
          agent.notifyOrderWithrawn(record, quantity);
        });
      });

    }

    @Override
    public Quote getQuote() {
      return quote;
    }

    @Override
    public double getProfit() {
      return observedProfit;
    }

    @Override
    public double getTrueProfit() {
      return profit;
    }

    @Override
    public int getHoldings() {
      return observedHoldings;
    }

    @Override
    public int getTrueHoldings() {
      return holdings;
    }

    @Override
    public int getSubmissions() {
      return submissions;
    }

    @Override
    public Multiset<OrderRecord> getActiveOrders() {
      return Multisets.unmodifiableMultiset(orders);
    }


    @Override
    public int getQuantity(OrderRecord record) {
      return orders.count(record);
    }

    @Override
    public void transacted(AbstractMarketOrder order, Price price, int quantity) {
      OrderRecord record = recordMap.inverse().get(order);
      double profitChange = -order.getType().sign() * price.doubleValue() * quantity;
      int holdingsChange = order.getType().sign() * quantity;

      profit += profitChange;
      holdings += holdingsChange;

      if (!orderbook.contains(order)) {
        recordMap.remove(record);
      }

      sim.scheduleIn(latency, () -> {
        observedProfit += profitChange;
        observedHoldings += holdingsChange;
        orders.remove(record, quantity);

        agent.notifyOrderTransacted(record, price, quantity);
      });
    }

    @Override
    public Agent getAgent() {
      return agent;
    }

    private class Rec implements OrderRecord {

      private final Price price;
      private final OrderType type;

      private Rec(OrderType type, Price price) {
        this.type = type;
        this.price = price;
      }

      @Override
      public OrderType getType() {
        return type;
      }

      @Override
      public Price getPrice() {
        return price;
      }

    }

    @Override
    public String toString() {
      return AbstractMarket.this + "*";
    }

  }

  /** A market view when there is no latency between market access. */
  class AbstractImmediateMarketView implements AbstractMarketView {
    private Quote quote;
    private final Agent agent;
    private int holdings;
    private int submissions;
    private double profit;
    private final Multiset<AbstractMarketOrder> orders;

    private boolean inSubmission;
    private final Queue<QueuedTransaction> queuedTransactions;

    AbstractImmediateMarketView(Agent agent) {
      this.quote = Quote.empty();
      this.agent = agent;
      this.holdings = 0;
      this.submissions = 0;
      this.profit = 0;
      this.orders = HashMultiset.create();

      this.inSubmission = false;
      this.queuedTransactions = new LinkedList<>();
    }

    @Override
    public void setQuote(Quote quote) {
      this.quote = quote;
      agent.notifyQuoteUpdated(this);
    }

    @Override
    public void transaction(Price price, int quantity) {
      this.agent.notifyTransaction(this, price, quantity);
    }

    @Override
    public TimeStamp getLatency() {
      return TimeStamp.ZERO;
    }

    @Override
    public OrderRecord submitOrder(OrderType buyOrSell, Price price, int quantity) {
      inSubmission = true;
      AbstractMarketOrder order = AbstractMarket.this.submitOrder(this, buyOrSell, price, quantity);
      submissions += quantity;
      orders.add(order, quantity);
      agent.notifyOrderSubmitted(order);
      inSubmission = false;

      while (!queuedTransactions.isEmpty()) {
        QueuedTransaction trans = queuedTransactions.poll();
        transacted(trans.transactedOrder, trans.transactedPrice, trans.transactedQuantity);
      }

      return order;
    }

    @Override
    public void withdrawOrder(OrderRecord order, int quantity) {
      orders.remove(order, quantity);
      AbstractMarket.this.withdrawOrder((AbstractMarketOrder) order, quantity);
      agent.notifyOrderWithrawn(order, quantity);
    }

    @Override
    public Quote getQuote() {
      return quote;
    }

    @Override
    public double getProfit() {
      return profit;
    }

    @Override
    public double getTrueProfit() {
      return profit;
    }

    @Override
    public int getHoldings() {
      return holdings;
    }

    @Override
    public int getTrueHoldings() {
      return holdings;
    }

    @Override
    public int getSubmissions() {
      return submissions;
    }

    @Override
    public Multiset<OrderRecord> getActiveOrders() {
      return Multisets.unmodifiableMultiset(orders);
    }

    @Override
    public int getQuantity(OrderRecord record) {
      return orders.count(record);
    }

    @Override
    public void transacted(AbstractMarketOrder order, Price price, int quantity) {
      if (inSubmission) {
        queuedTransactions.offer(new QueuedTransaction(order, price, quantity));
      } else {
        profit -= order.getType().sign() * price.doubleValue() * quantity;
        holdings += order.getType().sign() * quantity;
        orders.remove(order, quantity);
        agent.notifyOrderTransacted(order, price, quantity);
      }
    }

    @Override
    public Agent getAgent() {
      return agent;
    }

    @Override
    public String toString() {
      return AbstractMarket.this + "*";
    }

    private class QueuedTransaction {

      private final AbstractMarketOrder transactedOrder;
      private final Price transactedPrice;
      private final int transactedQuantity;

      private QueuedTransaction(AbstractMarketOrder transactedOrder, Price transactedPrice,
          int transactedQuantity) {
        this.transactedOrder = transactedOrder;
        this.transactedPrice = transactedPrice;
        this.transactedQuantity = transactedQuantity;
      }

    }

  }

  protected static class AbstractMarketOrder
      implements Order<Price, Long>, OrderRecord, Comparable<AbstractMarketOrder> {

    private final Price price;
    private final long time;
    private final TimeStamp submitTime;
    private final OrderType type;
    private final AbstractMarketView submitter;

    AbstractMarketOrder(AbstractMarketView submiter, OrderType type, Price price, long time,
        TimeStamp submitTime) {
      this.submitter = checkNotNull(submiter);
      this.price = checkNotNull(price);
      this.time = time;
      this.submitTime = checkNotNull(submitTime);
      this.type = checkNotNull(type);
    }

    @Override
    public Price getPrice() {
      return price;
    }

    @Override
    public Long getTime() {
      return time;
    }

    @Override
    public OrderType getType() {
      return type;
    }

    public TimeStamp getSubmitTime() {
      return submitTime;
    }

    /*
     * XXX This is necessary for consistent random selection of orders that tie, but since agent ids
     * are not guaranteed to be unique, this is not completely deterministic, but for it to fail,
     * two orders need to be submitted at the same: price, market time, simulation time, side, and
     * from agents that had an id collision, so it's exceedingly unlikely. We could make ids unique,
     * but we would probably loose some stability.
     */
    @Override
    public int compareTo(AbstractMarketOrder that) {
      return ComparisonChain.start().compare(this.submitTime, that.submitTime)
          .compare(this.submitter.getAgent().getId(), that.submitter.getAgent().getId()).result();
    }

    @Override
    public String toString() {
      return "(" + type + " @ " + price + ")";
    }

  }

  public interface PricingRule
      extends Function<Collection<MatchedOrders<Price, Long, AbstractMarketOrder>>, //
          Iterable<Entry<MatchedOrders<Price, Long, AbstractMarketOrder>, Price>>> {
  }

  private static final long serialVersionUID = 8806298743451593261L;

}
