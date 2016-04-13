package edu.umich.srg.marketsim.market;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import edu.umich.srg.collect.Sparse;
import edu.umich.srg.collect.SparseArrayList;
import edu.umich.srg.fourheap.FourHeap;
import edu.umich.srg.fourheap.MatchedOrders;
import edu.umich.srg.fourheap.Order;
import edu.umich.srg.fourheap.Order.OrderType;
import edu.umich.srg.marketsim.Price;
import edu.umich.srg.marketsim.Sim;
import edu.umich.srg.marketsim.TimeStamp;
import edu.umich.srg.marketsim.agent.Agent;
import edu.umich.srg.marketsim.fundamental.Fundamental;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;

/**
 * Base class for all markets. This class provides almost all market functionality that one should
 * need for creating a market. The only thing an abstract market needs is a pricing rule. That is,
 * given a set of matched orders, what price is assigned to each transaction. By default the only
 * methods that schedules more activities is a clear and a quoteUpdate.
 */
abstract class AbstractMarket implements Market, Serializable {

  final Sim sim;
  private final FourHeap<Price> orderbook;
  private final PricingRule pricing;

  // Bookkeeping
  private final Collection<AbstractMarketView> views;
  private final Map<Order<Price>, AbstractMarketView> orderOwners;
  private final Sparse<Number> prices;

  AbstractMarket(Sim sim, PricingRule pricing) {
    this.sim = sim;
    this.orderbook = new FourHeap<>();
    this.pricing = pricing;

    this.views = new ArrayList<>();
    this.orderOwners = new HashMap<>();
    this.prices = SparseArrayList.empty();
  }

  Order<Price> submitOrder(AbstractMarketView submitter, OrderType buyOrSell, Price price,
      int quantity) {
    Order<Price> order = orderbook.submit(buyOrSell, price, quantity);
    orderOwners.put(order, submitter);
    return order;
  }

  void withdrawOrder(Order<Price> order, int quantity) {
    orderbook.withdraw(order, quantity);
    if (order.getQuantity() == 0) {
      orderOwners.remove(order);
    }
  }

  void clear() {
    Collection<MatchedOrders<Price>> matches = orderbook.clear();
    for (Entry<MatchedOrders<Price>, Price> pricedTrade : pricing.apply(matches)) {

      MatchedOrders<Price> matched = pricedTrade.getKey();
      Price price = pricedTrade.getValue();

      // Notify buyer
      Order<Price> buy = matched.getBuy();
      orderOwners.get(buy).transacted(buy, price, matched.getQuantity());


      // Notify seller
      Order<Price> sell = matched.getSell();
      orderOwners.get(sell).transacted(sell, price, matched.getQuantity());

      // Notify all agents of transaction
      for (AbstractMarketView view : views) {
        view.transaction(price, matched.getQuantity());
      }

      prices.add(sim.getCurrentTime().get(), price);
    }
  }

  void updateQuote() {
    Quote quote = new Quote(orderbook.bidQuote(), orderbook.getBidDepth(), orderbook.askQuote(),
        orderbook.getAskDepth());
    for (AbstractMarketView view : views) {
      view.setQuote(quote);
    }
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
  public JsonObject getFeatures(Fundamental fundamental) {
    JsonObject features = new JsonObject();

    features.add("prices", convertSparseData(prices));
    features.addProperty("rmsd", fundamental.rmsd(prices, sim.getCurrentTime()));

    return features;
  }

  private static JsonArray convertSparseData(
      Iterable<? extends Sparse.Entry<? extends Number>> data) {
    JsonArray json = new JsonArray();
    for (Sparse.Entry<? extends Number> obs : data) {
      JsonArray point = new JsonArray();
      point.add(new JsonPrimitive(obs.getIndex()));
      point.add(new JsonPrimitive(obs.getElement().doubleValue()));
      json.add(point);
    }
    return json;
  }

  @Override
  public String toString() {
    return "CDA " + Integer.toString(System.identityHashCode(this), 36).toUpperCase();
  }

  abstract class AbstractMarketView implements MarketView, Serializable {

    abstract void setQuote(Quote quote);

    abstract void transacted(Order<Price> order, Price price, int quantity);

    abstract void transaction(Price price, int quantity);

    abstract Agent getAgent();

    abstract double getTrueProfit();

    abstract int getTrueHoldings();

    private static final long serialVersionUID = 1730831537473428295L;

  }

  class AbstractLatentMarketView extends AbstractMarketView {
    private final TimeStamp latency;
    private Quote quote;
    private final Agent agent;
    private double profit;
    private double observedProfit;
    private int holdings;
    private int submissions;
    private int observedHoldings;
    private Set<OrderRecord> observedOrders;
    private final BiMap<OrderRecord, Order<Price>> recordMap;

    AbstractLatentMarketView(Agent agent, TimeStamp latency) {
      this.latency = latency;
      this.quote = Quote.empty();
      this.agent = agent;
      this.profit = 0;
      this.observedProfit = 0;
      this.holdings = 0;
      this.submissions = 0;
      this.observedHoldings = 0;
      this.observedOrders = new HashSet<>();
      this.recordMap = HashBiMap.create();
    }

    @Override
    void setQuote(Quote quote) {
      AbstractMarket.this.sim.scheduleIn(latency, () -> {
        this.quote = quote;
        agent.notifyQuoteUpdated(this);
      });
    }

    @Override
    void transaction(Price price, int quantity) {
      AbstractMarket.this.sim.scheduleIn(latency,
          () -> agent.notifyTransaction(this, price, quantity));
    }

    @Override
    public TimeStamp getLatency() {
      return latency;
    }

    @Override
    public OrderRecord submitOrder(OrderType buyOrSell, Price price, int quantity) {
      OrderRecord record = new OrderRecord(this, buyOrSell, price, quantity);
      observedOrders.add(record);

      AbstractMarket.this.sim.scheduleIn(latency, () -> {
        Order<Price> order = AbstractMarket.this.submitOrder(this, buyOrSell, price, quantity);
        submissions += quantity;
        recordMap.put(record, order);

        AbstractMarket.this.sim.scheduleIn(latency, () -> agent.notifyOrderSubmitted(record));
      });

      return record;
    }

    @Override
    public void withdrawOrder(OrderRecord record, int quantity) {
      record.quantity -= quantity;

      AbstractMarket.this.sim.scheduleIn(latency, () -> {
        Order<Price> order = recordMap.get(record);
        if (order == null) {
          return; // This will happen if the order transacted, but hasn't reached the agent yet
        }

        // Min because some of the order may have transacted already, in which case we want to
        // withdraw the rest
        AbstractMarket.this.withdrawOrder(order, Math.min(quantity, order.getQuantity()));

        if (order.getQuantity() == 0) {
          recordMap.remove(record);
        }

        AbstractMarket.this.sim.scheduleIn(latency,
            () -> agent.notifyOrderWithrawn(record, quantity));
      });

      if (record.quantity == 0) {
        observedOrders.remove(record);
      }

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
    double getTrueProfit() {
      return profit;
    }

    @Override
    public int getHoldings() {
      return observedHoldings;
    }

    @Override
    int getTrueHoldings() {
      return holdings;
    }

    @Override
    public int getSubmissions() {
      return submissions;
    }

    @Override
    public Set<OrderRecord> getActiveOrders() {
      return Collections.unmodifiableSet(observedOrders);
    }

    @Override
    void transacted(Order<Price> order, Price price, int quantity) {
      OrderRecord record = recordMap.inverse().get(order);
      double profitChange = -order.getOrderType().sign() * price.doubleValue() * quantity;
      int holdingsChange = order.getOrderType().sign() * quantity;

      profit += profitChange;
      holdings += holdingsChange;

      AbstractMarket.this.sim.scheduleIn(latency, () -> {
        record.quantity -= quantity;
        observedProfit += profitChange;
        observedHoldings += holdingsChange;

        if (record.quantity == 0) {
          observedOrders.remove(record);
        }

        agent.notifyOrderTransacted(record, price, quantity);
      });

      if (order.getQuantity() == 0) {
        recordMap.remove(record);
      }
    }

    @Override
    Agent getAgent() {
      return agent;
    }

    @Override
    public String toString() {
      return AbstractMarket.this + "*";
    }

    private static final long serialVersionUID = 7349920586918545462L;

  }

  /** A market view when there is no latency between market access. */
  class AbstractImmediateMarketView extends AbstractMarketView {
    private Quote quote;
    private final Agent agent;
    private int holdings;
    private int submissions;
    private double profit;
    private final BiMap<OrderRecord, Order<Price>> recordMap;

    private OrderRecord submittedOrder;

    AbstractImmediateMarketView(Agent agent) {
      this.quote = Quote.empty();
      this.agent = agent;
      this.holdings = 0;
      this.submissions = 0;
      this.profit = 0;
      this.recordMap = HashBiMap.create();
      this.submittedOrder = null;
    }

    @Override
    void setQuote(Quote quote) {
      this.quote = quote;
      agent.notifyQuoteUpdated(this);
    }

    @Override
    void transaction(Price price, int quantity) {
      this.agent.notifyTransaction(this, price, quantity);
    }

    @Override
    public TimeStamp getLatency() {
      return TimeStamp.ZERO;
    }

    @Override
    public OrderRecord submitOrder(OrderType buyOrSell, Price price, int quantity) {
      OrderRecord record = new OrderRecord(this, buyOrSell, price, quantity);

      submittedOrder = record; // In case we transact before we get the order
      agent.notifyOrderSubmitted(record);
      Order<Price> order = AbstractMarket.this.submitOrder(this, buyOrSell, price, quantity);
      submittedOrder = null;

      submissions += quantity;
      if (order.getQuantity() != 0) {
        recordMap.put(record, order);
      }

      return record;
    }

    @Override
    public void withdrawOrder(OrderRecord record, int quantity) {
      Order<Price> order = recordMap.get(record);
      if (order == null) {
        return; // order already removed
      }

      AbstractMarket.this.withdrawOrder(order, quantity);
      record.quantity = order.getQuantity();

      if (record.quantity == 0) {
        recordMap.remove(record);
      }

      agent.notifyOrderWithrawn(record, quantity);
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
    double getTrueProfit() {
      return profit;
    }

    @Override
    public int getHoldings() {
      return holdings;
    }

    @Override
    int getTrueHoldings() {
      return holdings;
    }

    @Override
    public int getSubmissions() {
      return submissions;
    }

    @Override
    public Set<OrderRecord> getActiveOrders() {
      return Collections.unmodifiableSet(recordMap.keySet());
    }

    @Override
    void transacted(Order<Price> order, Price price, int quantity) {
      OrderRecord record = recordMap.inverse().getOrDefault(order, submittedOrder);
      record.quantity -= quantity;
      profit -= order.getOrderType().sign() * price.doubleValue() * quantity;
      holdings += order.getOrderType().sign() * quantity;

      if (record.quantity == 0) {
        recordMap.remove(record);
      }

      agent.notifyOrderTransacted(record, price, quantity);
    }

    @Override
    Agent getAgent() {
      return agent;
    }

    @Override
    public String toString() {
      return AbstractMarket.this + "*";
    }

    private static final long serialVersionUID = -1206172976823244457L;

  }

  public interface PricingRule extends
      Function<Collection<MatchedOrders<Price>>, Iterable<Entry<MatchedOrders<Price>, Price>>> {
  }

  private static final long serialVersionUID = 8806298743451593261L;

}
