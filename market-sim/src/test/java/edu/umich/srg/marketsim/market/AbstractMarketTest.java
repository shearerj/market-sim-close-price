package edu.umich.srg.marketsim.market;

import static edu.umich.srg.fourheap.OrderType.BUY;
import static edu.umich.srg.fourheap.OrderType.SELL;
import static edu.umich.srg.marketsim.testing.MarketAsserts.ABSENT;
import static org.junit.Assert.assertEquals;

import com.google.common.collect.Maps;

import org.junit.Before;
import org.junit.Test;

import edu.umich.srg.fourheap.MatchedOrders;
import edu.umich.srg.fourheap.PrioritySelector;
import edu.umich.srg.marketsim.Price;
import edu.umich.srg.marketsim.Sim;
import edu.umich.srg.marketsim.TimeStamp;
import edu.umich.srg.marketsim.fundamental.ConstantFundamental;
import edu.umich.srg.marketsim.market.Market.MarketView;
import edu.umich.srg.marketsim.testing.MarketAsserts;
import edu.umich.srg.marketsim.testing.MockAgent;
import edu.umich.srg.marketsim.testing.MockSim;
import edu.umich.srg.testing.Repeat;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map.Entry;

// TODO Switch to long sentinel version of assertQuote
public class AbstractMarketTest {
  private MockSim sim;
  private MockMarket market;
  private MockAgent agent;
  private MarketView view;

  @Before
  public void setup() {
    sim = new MockSim();
    market = new MockMarket(sim);
    agent = new MockAgent();
    view = market.getView(agent, TimeStamp.ZERO);
  }

  @Test
  public void addBid() {
    view.submitOrder(BUY, Price.of(1), 1);
    market.updateQuote();
    MarketAsserts.assertQuote(view.getQuote(), 1, ABSENT);
  }

  @Test
  public void addAsk() {
    view.submitOrder(SELL, Price.of(1), 1);
    market.updateQuote();
    MarketAsserts.assertQuote(view.getQuote(), null, Price.of(1));
  }

  @Test
  public void basicEqualClear() {
    OrderRecord buy = view.submitOrder(BUY, Price.of(100), 1);
    OrderRecord sell = view.submitOrder(SELL, Price.of(100), 1);

    market.clear();
    market.updateQuote();

    assertEquals(2, agent.transactions);
    MarketAsserts.assertQuote(view.getQuote(), null, null);
    assertEquals(0, view.getQuantity(buy));
    assertEquals(0, view.getQuantity(sell));
  }

  @Test
  public void basicOverlapClear() {
    OrderRecord buy = view.submitOrder(BUY, Price.of(200), 1);
    OrderRecord sell = view.submitOrder(SELL, Price.of(50), 1);

    market.clear();
    market.updateQuote();

    assertEquals(2, agent.transactions);
    MarketAsserts.assertQuote(view.getQuote(), null, null);
    assertEquals(0, view.getQuantity(buy));
    assertEquals(0, view.getQuantity(sell));
  }

  /** Several bids with one clear have proper transaction */
  @Test
  public void multiBidSingleClear() {
    OrderRecord buyTrans = view.submitOrder(BUY, Price.of(150), 1);
    OrderRecord buy = view.submitOrder(BUY, Price.of(100), 1);
    OrderRecord sell = view.submitOrder(SELL, Price.of(180), 1);
    OrderRecord sellTrans = view.submitOrder(SELL, Price.of(120), 1);

    market.clear();
    market.updateQuote();

    // Testing the market for the correct transactions
    assertEquals(2, agent.transactions);
    MarketAsserts.assertQuote(view.getQuote(), 100, 180);
    assertEquals(0, view.getQuantity(buyTrans));
    assertEquals(0, view.getQuantity(sellTrans));
    assertEquals(1, view.getQuantity(buy));
    assertEquals(1, view.getQuantity(sell));
  }

  /** Two sets of bids overlap before clear */
  @Test
  public void multiOverlapClear() {
    OrderRecord buy1 = view.submitOrder(BUY, Price.of(150), 1);
    OrderRecord sell1 = view.submitOrder(SELL, Price.of(100), 1);
    OrderRecord buy2 = view.submitOrder(BUY, Price.of(200), 1);
    OrderRecord sell2 = view.submitOrder(SELL, Price.of(130), 1);

    market.clear();
    market.updateQuote();

    assertEquals(4, agent.transactions);
    MarketAsserts.assertQuote(view.getQuote(), null, null);
    assertEquals(0, view.getQuantity(buy1));
    assertEquals(0, view.getQuantity(sell1));
    assertEquals(0, view.getQuantity(buy2));
    assertEquals(0, view.getQuantity(sell2));

  }

  /** Scenario with two possible matches, but only one pair transacts at the match. */
  @Test
  @Repeat(100)
  public void partialOverlapClear() {
    OrderRecord buyTrans = view.submitOrder(BUY, Price.of(200), 1);
    OrderRecord buy = view.submitOrder(SELL, Price.of(130), 1);
    OrderRecord sell = view.submitOrder(BUY, Price.of(110), 1);
    OrderRecord sellTrans = view.submitOrder(SELL, Price.of(100), 1);

    market.clear();
    market.updateQuote();

    assertEquals(2, agent.transactions);
    MarketAsserts.assertQuote(view.getQuote(), Price.of(110), Price.of(130));
    assertEquals(0, view.getQuantity(buyTrans));
    assertEquals(0, view.getQuantity(sellTrans));
    assertEquals(1, view.getQuantity(buy));
    assertEquals(1, view.getQuantity(sell));
  }

  @Test
  public void partialQuantity() {
    OrderRecord sell = view.submitOrder(SELL, Price.of(100), 2);
    OrderRecord buy = view.submitOrder(BUY, Price.of(150), 5);

    market.clear();
    market.updateQuote();

    // Check that two units transact and that post-trade BID is correct (3 buy units at 150)
    assertEquals(2, agent.transactions);
    assertEquals(4, agent.transactedUnits);
    MarketAsserts.assertQuote(view.getQuote(), Price.of(150), null);
    assertEquals(0, view.getQuantity(sell));
    assertEquals(3, view.getQuantity(buy));
  }

  @Test
  public void multiQuantity() {
    OrderRecord sell1 = view.submitOrder(SELL, Price.of(150), 1);
    OrderRecord sell2 = view.submitOrder(SELL, Price.of(140), 1);

    market.clear();

    assertEquals(0, agent.transactions);

    // Both sell orders should transact
    OrderRecord buy = view.submitOrder(BUY, Price.of(160), 2);

    market.clear();

    assertEquals(4, agent.transactions);
    MarketAsserts.assertQuote(view.getQuote(), null, null);
    assertEquals(0, view.getQuantity(sell1));
    assertEquals(0, view.getQuantity(sell2));
    assertEquals(0, view.getQuantity(buy));
  }

  @Test
  public void basicWithdraw() {
    OrderRecord order = view.submitOrder(SELL, Price.of(100), 1);

    market.clear();
    market.updateQuote();

    // Check that quotes are correct (no bid, ask @100)
    MarketAsserts.assertQuote(view.getQuote(), null, Price.of(100));

    // Withdraw order
    view.withdrawOrder(order, view.getQuantity(order));

    market.updateQuote();

    // Check that quotes are correct (no bid, no ask)
    MarketAsserts.assertQuote(view.getQuote(), null, null);

    // Check that no transaction, because order withdrawn
    order = view.submitOrder(BUY, Price.of(125), 1);

    assertEquals(0, agent.transactions);

    view.submitOrder(BUY, Price.of(115), 1);

    view.withdrawOrder(order);

    // Check that it transacts with order (@115) that was not withdrawn
    view.submitOrder(SELL, Price.of(105), 1);

    market.clear();
    market.updateQuote();

    assertEquals(2, agent.transactions);
    MarketAsserts.assertQuote(view.getQuote(), null, null);
  }

  @Test
  public void multiQuantityWithdraw() {
    view.submitOrder(SELL, Price.of(150), 1);
    OrderRecord order = view.submitOrder(SELL, Price.of(140), 2);

    market.clear();

    view.withdrawOrder(order, 1);

    // Both agents' sell orders should transact b/c partial quantity withdrawn
    view.submitOrder(BUY, Price.of(160), 1);
    view.submitOrder(BUY, Price.of(160), 2);

    market.clear();
    market.updateQuote();

    assertEquals(4, agent.transactions);
    MarketAsserts.assertQuote(view.getQuote(), Price.of(160), null);
  }

  // /** Information propagates at proper times */
  // @Test
  // public void latencyTest() {
  // EventQueue timeline = latencySetup(TimeStamp.of(100));
  //
  // submitOrder(SELL, Price.of(100), 1);
  //
  // // In market
  // timeline.executeUntil(TimeStamp.of(99));
  // assertQuote(fast.getQuote(), null, 0, Price.of(100), 1);
  // assertQuote(view.getQuote(), null, 0, null, 0);
  //
  // // Reached primary view
  // timeline.executeUntil(TimeStamp.of(100));
  // assertQuote(fast.getQuote(), null, 0, Price.of(100), 1);
  // }
  //
  // @Test
  // public void updateQuoteLatency() {
  // EventQueue timeline = latencySetup(TimeStamp.of(100));
  //
  // submitOrder(SELL, Price.of(100), 1);
  //
  // timeline.executeUntil(TimeStamp.of(99));
  // assertQuote(view.getQuote(), null, 0, null, 0);
  //
  // // Update QP
  // timeline.executeUntil(TimeStamp.of(100));
  // assertQuote(view.getQuote(), null, 0, Price.of(100), 1);
  //
  // // Add new quote
  // submitOrder(BUY, Price.of(80), 1);
  //
  // // Update QP
  // timeline.executeUntil(TimeStamp.of(200));
  // assertQuote(view.getQuote(), Price.of(80), 1, Price.of(100), 1);
  // }
  //
  // @Test
  // public void updateTransactionsLatency() {
  // EventQueue timeline = latencySetup(TimeStamp.of(100));
  //
  // OrderRecord buy = submitOrder(BUY, Price.of(150), 1);
  // OrderRecord sell = submitOrder(SELL, Price.of(140), 1);
  // market.clear();
  //
  // // Verify that transactions have not updated yet (for primary view)
  // timeline.executeUntil(TimeStamp.of(99));
  // assertTrue(view.getTransactions().isEmpty());
  // assertSingleTransaction(fast.getTransactions(), Price.of(145), TimeStamp.ZERO, 1);
  // assertEquals(1, buy.getQuantity());
  // assertEquals(1, sell.getQuantity());
  //
  // // Test that after 100 new transaction did get updated
  // timeline.executeUntil(TimeStamp.of(100));
  // assertSingleTransaction(view.getTransactions(), Price.of(145), TimeStamp.ZERO, 1);
  // }
  //
  // @Test
  // public void basicTransactionTest() {
  // assertTrue("Incorrect initial transaction list", view.getTransactions().isEmpty());
  //
  // addTransaction(Price.of(150), 2);
  // assertSingleTransaction(view.getTransactions(), Price.of(150), TimeStamp.ZERO, 2);
  //
  // addTransaction(Price.of(170), 1);
  // assertTransaction(Iterables.getFirst(view.getTransactions(), null), Price.of(170),
  // TimeStamp.ZERO, 1);
  // }
  //
  // @Test
  // public void basicDelayProcessTransaction() {
  // EventQueue timeline = latencySetup(TimeStamp.of(100));
  //
  // assertTrue("Incorrect initial transaction list", view.getTransactions().isEmpty());
  //
  // // Transaction in market, but not seen via slow view
  // addTransaction(Price.of(150), 2);
  //
  // timeline.executeUntil(TimeStamp.of(99));
  // assertTrue("Primary view updated too early", view.getTransactions().isEmpty());
  // assertSingleTransaction(fast.getTransactions(), Price.of(150), TimeStamp.ZERO, 2);
  //
  // timeline.executeUntil(TimeStamp.of(100));
  // assertSingleTransaction(view.getTransactions(), Price.of(150), TimeStamp.ZERO, 2);
  // }
  //
  // /** Test handling of stale quotes when better order happened later */
  // @Test
  // @Repeat(100)
  // public void staleQuotesFirst() {
  // EventQueue timeline = latencySetup(TimeStamp.of(100));
  //
  // submitOrder(BUY, Price.of(50), 1);
  // submitOrder(BUY, Price.of(60), 1);
  //
  // timeline.executeUntil(TimeStamp.of(100));
  // assertQuote(view.getQuote(), Price.of(60), 1, null, 0);
  //
  // submitOrder(SELL, Price.of(110), 1);
  // submitOrder(SELL, Price.of(100), 1);
  //
  // timeline.executeUntil(TimeStamp.of(200));
  // assertQuote(view.getQuote(), Price.of(60), 1, Price.of(100), 1);
  // }
  //
  // /** Test handling of stale quotes when better order happened earlier */
  // @Test
  // @Repeat(100)
  // public void staleQuotesLast() {
  // EventQueue timeline = latencySetup(TimeStamp.of(100));
  //
  // submitOrder(BUY, Price.of(60), 1);
  // submitOrder(BUY, Price.of(50), 1);
  //
  // timeline.executeUntil(TimeStamp.of(100));
  // assertQuote(view.getQuote(), Price.of(60), 1, null, 0);
  //
  // submitOrder(SELL, Price.of(100), 1);
  // submitOrder(SELL, Price.of(110), 1);
  //
  // timeline.executeUntil(TimeStamp.of(200));
  // assertQuote(view.getQuote(), Price.of(60), 1, Price.of(100), 1);
  // }
  //
  // /** Test that routes if transacts in other market */
  // @Test
  // public void nbboRoutingBuy() {
  // nbboSetup(Price.of(80), Price.of(100));
  // submitNMSOrder(BUY, Price.of(100), 1);
  //
  // // Verify it got routed due to empty quote
  // assertQuote(view.getQuote(), null, 0, null, 0);
  // }
  //
  // /** Test that routes when other is better even if it would transact locally */
  // @Test
  // public void nbboBetterRoutingBuy() {
  // nbboSetup(Price.of(80), Price.of(100));
  // submitOrder(SELL, Price.of(120), 1);
  //
  // submitNMSOrder(BUY, Price.of(120), 1);
  //
  // // Verify it got routed due to better price
  // assertQuote(view.getQuote(), null, 0, Price.of(120), 1);
  // }
  //
  // /** Test that doesn't route when it won't transact in other market */
  // @Test
  // public void nbboNoRoutingBuy() {
  // nbboSetup(Price.of(80), Price.of(100));
  //
  // submitNMSOrder(BUY, Price.of(90), 1);
  //
  // // Verify it did route because it wouldn't transact
  // assertQuote(view.getQuote(), Price.of(90), 1, null, 0);
  // }
  //
  // /** Test that doesn't route when the price locally is better */
  // @Test
  // public void nbboNoRoutingBetterBuy() {
  // nbboSetup(Price.of(80), Price.of(100));
  // submitOrder(SELL, Price.of(90), 1);
  //
  // submitNMSOrder(BUY, Price.of(100), 1);
  // market.clear(); // Orders would transact but market doesn't automatically clear
  //
  // // Verify it didn't route because price is better locally
  // assertQuote(view.getQuote(), null, 0, null, 0);
  // }
  //
  // @Test
  // public void nbboRoutingSell() {
  // nbboSetup(Price.of(80), Price.of(100));
  // submitNMSOrder(SELL, Price.of(80), 1);
  //
  // // Verify it got routed due to empty quote
  // assertQuote(view.getQuote(), null, 0, null, 0);
  // }
  //
  // /** Test that routes when other is better even if it would transact locally */
  // @Test
  // public void nbboBetterRoutingSell() {
  // nbboSetup(Price.of(80), Price.of(100));
  // submitOrder(BUY, Price.of(60), 1);
  //
  // submitNMSOrder(SELL, Price.of(80), 1);
  //
  // // Verify it got routed due to better price
  // assertQuote(view.getQuote(), Price.of(60), 1, null, 0);
  // }
  //
  // /** Test that doesn't route when it won't transact in other market */
  // @Test
  // public void nbboNoRoutingSell() {
  // nbboSetup(Price.of(80), Price.of(100));
  //
  // submitNMSOrder(SELL, Price.of(90), 1);
  //
  // // Verify it did route because it wouldn't transact
  // assertQuote(view.getQuote(), null, 0, Price.of(90), 1);
  // }
  //
  // /** Test that doesn't route when the price locally is better */
  // @Test
  // public void nbboNoRoutingBetterSell() {
  // nbboSetup(Price.of(80), Price.of(100));
  // submitOrder(BUY, Price.of(90), 1);
  //
  // submitNMSOrder(SELL, Price.of(80), 1);
  // market.clear(); // Orders would transact but market doesn't automatically clear
  //
  // // Verify it didn't route because price is better locally
  // assertQuote(view.getQuote(), null, 0, null, 0);
  // }
  //
  // @Test
  // public void spreadsPostTest() {
  // EventQueue timeline = EventQueue.create(Log.nullLogger(), rand);
  // Stats stats = Stats.create();
  // market = mockMarket(0, stats, timeline, Mock.sip, Props.fromPairs());
  // view = market.getPrimaryView();
  // TimeSeries expected = TimeSeries.create();
  // expected.add(TimeStamp.of(0), Double.POSITIVE_INFINITY);
  //
  // timeline.executeUntil(TimeStamp.of(2));
  // submitOrder(SELL, Price.of(100), 1);
  // submitOrder(BUY, Price.of(50), 1);
  // expected.add(TimeStamp.of(2), 50);
  //
  // timeline.executeUntil(TimeStamp.of(100));
  // submitOrder(SELL, Price.of(80), 1);
  // submitOrder(BUY, Price.of(60), 1);
  // expected.add(TimeStamp.of(100), 20);
  //
  // assertEquals(expected, stats.getTimeStats().get(Stats.SPREAD + 0));
  // }
  //
  // @Test
  // public void midpointPostTest() {
  // EventQueue timeline = EventQueue.create(Log.nullLogger(), rand);
  // Stats stats = Stats.create();
  // market = mockMarket(0, stats, timeline, Mock.sip, Props.fromPairs());
  // view = market.getPrimaryView();
  // TimeSeries expected = TimeSeries.create();
  // expected.add(TimeStamp.of(0), Double.NaN);
  //
  // timeline.executeUntil(TimeStamp.of(2));
  // submitOrder(SELL, Price.of(100), 1);
  // submitOrder(BUY, Price.of(50), 1);
  // expected.add(TimeStamp.of(2), 75);
  //
  // timeline.executeUntil(TimeStamp.of(100));
  // submitOrder(SELL, Price.of(80), 1);
  // submitOrder(BUY, Price.of(60), 1);
  // expected.add(TimeStamp.of(100), 70);
  //
  // assertEquals(expected, stats.getTimeStats().get(Stats.MIDQUOTE + 0));
  // }
  //
  // @Test
  // public void transactionPricePostTest() {
  // EventQueue timeline = EventQueue.create(Log.nullLogger(), rand);
  // Stats stats = Stats.create();
  // market = mockMarket(0, stats, timeline, Mock.sip, Props.fromPairs());
  // view = market.getPrimaryView();
  // Market other = mockMarket(1, stats, timeline, Mock.sip, Props.fromPairs());
  //
  // addTransaction(Price.of(100), 1);
  //
  // timeline.executeUntil(TimeStamp.of(50));
  // addTransaction(other, Price.of(50), 1);
  //
  // timeline.executeUntil(TimeStamp.of(100));
  // addTransaction(Price.of(200), 2);
  // addTransaction(Price.of(150), 1);
  //
  // TimeSeries truth = TimeSeries.create();
  // truth.add(TimeStamp.of(0), 100);
  // truth.add(TimeStamp.of(50), 50);
  // truth.add(TimeStamp.of(100), 150);
  //
  // assertEquals(truth, stats.getTimeStats().get(Stats.TRANSACTION_PRICE));
  //
  // assertEquals(125, stats.getSummaryStats().get(Stats.PRICE).mean(), eps);
  // assertEquals(4, stats.getSummaryStats().get(Stats.PRICE).n());
  // }
  //
  // private EventQueue latencySetup(TimeStamp latency) {
  // EventQueue timeline = EventQueue.create(Log.nullLogger(), rand);
  // this.timeline = timeline;
  // market = mockMarket(0, Mock.stats, timeline, Mock.sip, Props.fromPairs(MarketLatency.class,
  // latency));
  // view = market.getPrimaryView();
  // fast = market.getView(TimeStamp.ZERO);
  // return timeline;
  // }
  //
  // private MarketInfo nbboSetup(Price bid, Price ask) {
  // SIP sip = SIP.create(Mock.stats, Mock.timeline, Log.nullLogger(), rand, TimeStamp.ZERO);
  // market = mockMarket(0, Mock.stats, Mock.timeline, sip, Props.fromPairs());
  // MarketView other = mockMarket(1, Mock.stats, Mock.timeline, sip,
  // Props.fromPairs()).getPrimaryView();
  // view = market.getPrimaryView();
  // fast = market.getView(TimeStamp.ZERO);
  // other.submitOrder(agent, OrderRecord.create(other, TimeStamp.ZERO, BUY, bid, 1));
  // other.submitOrder(agent, OrderRecord.create(other, TimeStamp.ZERO, SELL, ask, 1));
  // return sip;
  // }
  //
  // private void addTransaction(Price price, int quantity) {
  // addTransaction(market, price, quantity);
  // }
  //
  // private void addTransaction(Market market, Price price, int quantity) {
  // submitOrder(market, BUY, price, quantity);
  // submitOrder(market, SELL, price, quantity);
  // market.clear();
  // }
  //
  // private OrderRecord submitNMSOrder(OrderType buyOrSell, Price price, int quantity) {
  // OrderRecord order = OrderRecord.create(market.getPrimaryView(), TimeStamp.ZERO, buyOrSell,
  // price, quantity);
  // market.submitNMSOrder(market.getPrimaryView(), agent,
  // agent.getView(market.getPrimaryView().getLatency()), order);
  // return order;
  // }

  private static class MockMarket extends AbstractMarket {

    private MockMarket(Sim sim) {
      super(sim, ConstantFundamental.create(0, 100), MockMarket::mockPricing,
          PrioritySelector.create());
    }

    private static Iterable<Entry<MatchedOrders<Price, Long, AbstractMarketOrder>, Price>> mockPricing(
        Collection<MatchedOrders<Price, Long, AbstractMarketOrder>> matches) {
      Iterator<MatchedOrders<Price, Long, AbstractMarketOrder>> it = matches.iterator();
      return () -> new Iterator<Entry<MatchedOrders<Price, Long, AbstractMarketOrder>, Price>>() {

        @Override
        public boolean hasNext() {
          return it.hasNext();
        }

        @Override
        public Entry<MatchedOrders<Price, Long, AbstractMarketOrder>, Price> next() {
          return Maps.immutableEntry(it.next(), Price.ZERO);
        }

      };
    }

    private static final long serialVersionUID = 971056535454290161L;

  }

}
