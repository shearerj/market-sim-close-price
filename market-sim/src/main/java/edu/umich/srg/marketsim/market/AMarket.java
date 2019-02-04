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
import com.google.common.primitives.Ints;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import edu.umich.srg.fourheap.FourHeap;
import edu.umich.srg.fourheap.IOrder;
import edu.umich.srg.fourheap.MatchedOrders;
import edu.umich.srg.fourheap.OrderType;
import edu.umich.srg.fourheap.Selector;
import edu.umich.srg.marketsim.Price;
import edu.umich.srg.marketsim.Sim;
import edu.umich.srg.marketsim.TimeStamp;
import edu.umich.srg.marketsim.agent.Agent;
import edu.umich.srg.marketsim.fundamental.Fundamental;
import edu.umich.srg.marketsim.fundamental.Fundamental.FundamentalView;
import edu.umich.srg.marketsim.market.MarketObserver.QuoteObserver;
import edu.umich.srg.marketsim.market.MarketObserver.TransactionObserver;
import edu.umich.srg.marketsim.market.Benchmark;
import edu.umich.srg.util.SummStats;

import java.io.Serializable;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.function.Function;

/**
 * Base class for all markets. This class provides almost all market functionality that one should
 * need for creating a market. The only thing an abstract market needs is a pricing rule. That is,
 * given a set of matched orders, what price is assigned to each transaction. By default the only
 * methods that schedules more activities is a clear and a quoteUpdate.
 */
abstract class AMarket implements Market, Serializable {

  final Sim sim;
  private final FourHeap<Price, AOrder> orderbook;
  private final PricingRule pricing;
  private long sequenceNum;

  // Bookkeeping
  private final FundamentalView fundView;
  private final Collection<AMarketView> views;
  private final Set<QuoteObserver> quoteObservers;
  private final Set<TransactionObserver> transactionObservers;
  private final List<Entry<TimeStamp, Price>> prices;

  // Features
  private final SummStats rmsd;
  private double maxDiff;
  private final SummStats transPrice;
  private long lastSpreadUpdate;
  private double lastSpread;
  private final Multiset<Double> spreads;
  private final SummStats executionTimes;
  private long volume;
  private final SummStats priceDiff;
  private final SummStats bidDepth;
  private final SummStats askDepth;
  
  // Benchmark
  private final Benchmark benchType;
  private double benchmark;

  AMarket(Sim sim, Fundamental fundamental, PricingRule pricing, Selector<AOrder> selector, String benchmarkType) {
    this.sim = sim;
    this.orderbook = FourHeap.create(selector);
    this.pricing = pricing;
    this.sequenceNum = Long.MIN_VALUE;

    this.fundView = fundamental.getView(sim);
    this.views = new ArrayList<>();
    this.quoteObservers = new LinkedHashSet<>();
    this.transactionObservers = new LinkedHashSet<>();
    this.prices = new ArrayList<>();

    this.rmsd = SummStats.empty();
    this.maxDiff = 0;
    this.transPrice = SummStats.empty();
    this.lastSpreadUpdate = 0;
    this.lastSpread = Double.POSITIVE_INFINITY;
    this.spreads = HashMultiset.create();
    this.executionTimes = SummStats.empty();
    this.volume = 0;
    this.priceDiff = SummStats.empty();
    this.bidDepth = SummStats.empty();
    this.askDepth = SummStats.empty();
    this.benchType = Benchmark.create(benchmarkType);
    this.benchmark = 0;
  }

  AOrder submitOrder(AMarketView submitter, OrderType buyOrSell, Price price, int quantity) {
    AOrder order = new AOrder(submitter, buyOrSell, price, sim.getCurrentTime(), sequenceNum++);
    orderbook.add(order, quantity);
    return order;
  }

  void withdrawOrder(AOrder order, int quantity) {
    orderbook.remove(order, quantity);
  }

  @Override
  public void clear() {
    Collection<MatchedOrders<Price, AOrder>> matches = orderbook.marketClear();
    for (Entry<MatchedOrders<Price, AOrder>, Price> pricedTrade : pricing.apply(matches)) {

      MatchedOrders<Price, AOrder> matched = pricedTrade.getKey();
      Price price = pricedTrade.getValue();

      // Notify buyer
      AOrder buy = matched.getBuy();
      buy.submitter.transacted(buy, price, matched.getQuantity());


      // Notify seller
      AOrder sell = matched.getSell();
      sell.submitter.transacted(sell, price, matched.getQuantity());

      // Notify all agents of transaction
      for (TransactionObserver obs : transactionObservers) {
        obs.notifyTransaction(price, matched.getQuantity());
      }

      // Bookkeeping
      prices.add(new AbstractMap.SimpleImmutableEntry<>(sim.getCurrentTime(), price));
      double diff = price.doubleValue() - fundView.getEstimatedFinalFundamental();
      rmsd.acceptNTimes(diff * diff, matched.getQuantity());
      maxDiff = Double.max(maxDiff, Math.abs(diff));
      transPrice.acceptNTimes(price, matched.getQuantity());
      long currentTime = sim.getCurrentTime().get();
      executionTimes.accept(currentTime - buy.getSubmitTime().get());
      executionTimes.accept(currentTime - sell.getSubmitTime().get());
      volume += matched.getQuantity();
      priceDiff.accept(diff);
    }
    benchmark = benchType.calcBenchmark(prices);
  }

  void updateQuote() {
    final Quote quote = new Quote(orderbook.getBidQuote(), orderbook.getBidDepth(),
        orderbook.getAskQuote(), orderbook.getAskDepth());

    long currentTime = sim.getCurrentTime().get();
    spreads.remove(lastSpread);
    spreads.add(lastSpread, Ints.checkedCast(currentTime - lastSpreadUpdate));
    lastSpreadUpdate = currentTime;
    lastSpread = quote.getSpread();
    spreads.add(lastSpread);
    bidDepth.accept(quote.getBidDepth());
    askDepth.accept(quote.getAskDepth());

    for (QuoteObserver obs : quoteObservers) {
      obs.notifyQuote(quote);
    }
  }

  @Override
  public Iterable<Entry<Agent, AgentInfo>> getAgentInfo() {
    return Iterables.transform(views,
        v -> Maps.<Agent, AgentInfo>immutableEntry(v.getAgent(), ImmutableAgentInfo
            .of(v.getProfit(), v.getHoldings(), v.getTrueSubmissions(), v.getTrueVolume())));
  }

  @Override
  public <T extends TransactionObserver> T addTransactionObserver(T obs) {
    this.transactionObservers.add(obs);
    return obs;
  }

  @Override
  public <Q extends QuoteObserver> Q addQuoteObserver(Q obs) {
    this.quoteObservers.add(obs);
    return obs;
  }

  @Override
  public MarketView getView(Agent agent, TimeStamp latency) {
    AMarketView view = latency.equals(TimeStamp.ZERO) ? new AImmediateMarketView(agent)
        : new ALatentMarketView(agent, latency);
    views.add(view);
    return view;
  }

  @Override
  public JsonObject getFeatures() {
    JsonObject features = new JsonObject();

    features.addProperty("rmsd", Math.sqrt(rmsd.getAverage().orElse(Double.NaN)));
    features.addProperty("max_diff", maxDiff);
    features.addProperty("trans_vol", transPrice.getStandardDeviation().orElse(Double.NaN));
    features.addProperty("median_spread", SummStats.median(spreads).orElse(Double.NaN));
    features.addProperty("mean_exec_time", executionTimes.getAverage().orElse(Double.NaN));
    features.addProperty("volume", volume);
    features.addProperty("price_var", priceDiff.getVariance().orElse(Double.NaN));
    features.addProperty("bid_depth", bidDepth.getAverage().orElse(Double.NaN));
    features.addProperty("ask_depth", askDepth.getAverage().orElse(Double.NaN));
    features.addProperty("benchmark", benchmark);

    JsonArray jprices = new JsonArray();
    for (Entry<TimeStamp, Price> obs : prices) {
      JsonArray point = new JsonArray();
      point.add(obs.getKey().get());
      point.add(obs.getValue().doubleValue());
      jprices.add(point);
    }
    features.add("prices", jprices);

    return features;
  }
  
  @Override
  public double getBenchmark() {
  	return benchmark;
  }

  @Override
  public String toString() {
    return Integer.toUnsignedString(System.identityHashCode(this), 36).toUpperCase();
  }

  interface AMarketView extends MarketView {

    void transacted(AOrder order, Price price, int quantity);

    Agent getAgent();

    double getTrueProfit();

    int getTrueHoldings();

    int getTrueSubmissions();

    int getTrueVolume();

  }

  class ALatentMarketView implements AMarketView {
    private final TimeStamp latency;
    private final Agent agent;
    private double profit;
    private double observedProfit;
    private int holdings;
    private int submissions;
    private int volume;
    private int observedHoldings;
    private Multiset<OrderRecord> orders;
    private final BiMap<OrderRecord, AOrder> recordMap;

    ALatentMarketView(Agent agent, TimeStamp latency) {
      this.latency = latency;
      this.agent = agent;
      this.profit = 0;
      this.observedProfit = 0;
      this.holdings = 0;
      this.submissions = 0;
      this.volume = 0;
      this.observedHoldings = 0;
      this.orders = HashMultiset.create();
      this.recordMap = HashBiMap.create();
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
        AOrder order = AMarket.this.submitOrder(this, buyOrSell, price, quantity);
        submissions += quantity;

        sim.scheduleIn(latency, () -> {
          recordMap.put(record, order);
        });
      });

      return record;
    }

    @Override
    public void withdrawOrder(OrderRecord record, int quantity) {
      orders.remove(record, quantity);
      AOrder order = recordMap.get(record);
      if (order == null) {
        return; // This will happen if the order transacted, but that information hasn't reached the
                // agent yet
      }

      sim.scheduleIn(latency, () -> {
        AMarket.this.withdrawOrder(order, quantity);
        if (!orderbook.contains(order)) {
          recordMap.remove(record);
        }
      });

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
    public int getTrueSubmissions() {
      return submissions;
    }

    @Override
    public int getTrueVolume() {
      return volume;
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
    public void transacted(AOrder order, Price price, int quantity) {
      OrderRecord record = recordMap.inverse().get(order);
      double profitChange = -order.getType().sign() * price.doubleValue() * quantity;
      int holdingsChange = order.getType().sign() * quantity;

      profit += profitChange;
      holdings += holdingsChange;
      volume += quantity;

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
      return AMarket.this + "*";
    }

  }

  /** A market view when there is no latency between market access. */
  class AImmediateMarketView implements AMarketView {
    private final Agent agent;
    private int holdings;
    private int submissions;
    private int volume;
    private double profit;
    private final Multiset<AOrder> orders;

    private boolean inSubmission;
    private final Queue<QueuedTransaction> queuedTransactions;

    AImmediateMarketView(Agent agent) {
      this.agent = agent;
      this.holdings = 0;
      this.submissions = 0;
      this.volume = 0;
      this.profit = 0;
      this.orders = HashMultiset.create();

      this.inSubmission = false;
      this.queuedTransactions = new LinkedList<>();
    }

    @Override
    public TimeStamp getLatency() {
      return TimeStamp.ZERO;
    }

    @Override
    public OrderRecord submitOrder(OrderType buyOrSell, Price price, int quantity) {
      inSubmission = true;
      AOrder order = AMarket.this.submitOrder(this, buyOrSell, price, quantity);
      submissions += quantity;
      orders.add(order, quantity);
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
      AMarket.this.withdrawOrder((AOrder) order, quantity);
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
    public int getTrueSubmissions() {
      return submissions;
    }

    @Override
    public int getTrueVolume() {
      return volume;
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
    public void transacted(AOrder order, Price price, int quantity) {
      if (inSubmission) {
        queuedTransactions.offer(new QueuedTransaction(order, price, quantity));
      } else {
        profit -= order.getType().sign() * price.doubleValue() * quantity;
        holdings += order.getType().sign() * quantity;
        volume += quantity;
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
      return AMarket.this + "*";
    }

    private class QueuedTransaction {

      private final AOrder transactedOrder;
      private final Price transactedPrice;
      private final int transactedQuantity;

      private QueuedTransaction(AOrder transactedOrder, Price transactedPrice,
          int transactedQuantity) {
        this.transactedOrder = transactedOrder;
        this.transactedPrice = transactedPrice;
        this.transactedQuantity = transactedQuantity;
      }

    }

  }

  protected static class AOrder implements IOrder<Price>, OrderRecord, Comparable<AOrder> {

    protected final long sequence;
    private final Price price;
    private final TimeStamp submitTime;
    private final OrderType type;
    private final AMarketView submitter;

    AOrder(AMarketView submiter, OrderType type, Price price, TimeStamp submitTime, long sequence) {
      this.submitter = checkNotNull(submiter);
      this.price = checkNotNull(price);
      this.submitTime = checkNotNull(submitTime);
      this.type = checkNotNull(type);
      this.sequence = sequence;
    }

    @Override
    public Price getPrice() {
      return price;
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
    public int compareTo(AOrder that) {
      return ComparisonChain.start().compare(this.submitTime, that.submitTime)
          .compare(this.submitter.getAgent().getId(), that.submitter.getAgent().getId()).result();
    }

    @Override
    public String toString() {
      return String.format("(%s @ %s, args)", type, price);
    }

  }

  public interface PricingRule extends Function<Collection<MatchedOrders<Price, AOrder>>, //
      Iterable<Entry<MatchedOrders<Price, AOrder>, Price>>> {
  }

  private static final long serialVersionUID = 8806298743451593261L;

}
