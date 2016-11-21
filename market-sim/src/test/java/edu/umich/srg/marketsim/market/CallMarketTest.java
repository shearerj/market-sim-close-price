package edu.umich.srg.marketsim.market;

import static edu.umich.srg.fourheap.OrderType.BUY;
import static edu.umich.srg.fourheap.OrderType.SELL;
import static edu.umich.srg.marketsim.testing.MarketAsserts.ABSENT;
import static edu.umich.srg.marketsim.testing.MarketAsserts.assertQuote;
import static org.junit.Assert.assertEquals;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import edu.umich.srg.marketsim.MarketSimulator;
import edu.umich.srg.marketsim.Price;
import edu.umich.srg.marketsim.TimeStamp;
import edu.umich.srg.marketsim.agent.Agent;
import edu.umich.srg.marketsim.fundamental.ConstantFundamental;
import edu.umich.srg.marketsim.fundamental.Fundamental;
import edu.umich.srg.marketsim.market.Market.MarketView;
import edu.umich.srg.marketsim.testing.MockAgent;
import edu.umich.srg.marketsim.testing.MockSim;
import edu.umich.srg.testing.Repeat;
import edu.umich.srg.testing.RepeatRule;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

public class CallMarketTest {

  @Rule
  public RepeatRule repeatRule = new RepeatRule();

  private static final Random rand = new Random();
  private static final Fundamental fund = ConstantFundamental.create(0);

  /** Test normal pricing policy */
  @Test
  public void normalPricingPolicyTest() {
    MockSim sim = new MockSim();
    CallMarket market = CallMarket.create(sim, fund, 100, rand);
    AtomicInteger transacted = new AtomicInteger(0);
    Agent agent = new MockAgent() {

      @Override
      public void notifyOrderTransacted(OrderRecord order, Price price, int quantity) {
        transacted.incrementAndGet();
        Assert.assertEquals(150, price.longValue());
      }

    };
    MarketView view = market.getView(agent, TimeStamp.ZERO);

    view.submitOrder(BUY, Price.of(200), 1);
    view.submitOrder(SELL, Price.of(100), 1);
    market.clear();

    assertQuote(view.getQuote(), null, null);
    Assert.assertEquals("Agent didn't transact appropriate number of times", 2, transacted.get());
  }

  /** Test abnormal pricing policy */
  @Test
  public void abnormalPricingPolicyTest() {
    MockSim sim = new MockSim();
    CallMarket market = CallMarket.create(sim, fund, 0.2, 100, rand);
    AtomicInteger transacted = new AtomicInteger(0);
    Agent agent = new MockAgent() {

      @Override
      public void notifyOrderTransacted(OrderRecord order, Price price, int quantity) {
        transacted.incrementAndGet();
        Assert.assertEquals(180, price.longValue());
      }

    };
    MarketView view = market.getView(agent);

    view.submitOrder(BUY, Price.of(200), 1);
    view.submitOrder(SELL, Price.of(100), 1);
    market.clear();

    assertQuote(view.getQuote(), null, null);
    Assert.assertEquals("Agent didn't transact appropriate number of times", 2, transacted.get());
  }

  /** Test that market clears at intervals */
  @Test
  public void clearActivityInsertion() {
    MarketSimulator sim = MarketSimulator.create(ConstantFundamental.create(1000), rand);
    CallMarket market = CallMarket.create(sim, fund, 100, rand);
    AtomicInteger numQuotes = new AtomicInteger(0);
    Agent agent = new MockAgent() {

      @Override
      public void notifyQuoteUpdated(MarketView market) {
        numQuotes.incrementAndGet();
      }

    };
    MarketView view = market.getView(agent);

    // Test that before time 100 quotes do not change
    sim.executeUntil(TimeStamp.of(50));
    Assert.assertEquals(0, numQuotes.get());
    assertQuote(view.getQuote(), ABSENT, ABSENT);

    // Quote still undefined before clear
    view.submitOrder(BUY, Price.of(100), 1);
    OrderRecord sell = view.submitOrder(SELL, Price.of(110), 1);
    sim.executeUntil(TimeStamp.of(99));
    Assert.assertEquals(0, numQuotes.get());
    assertQuote(view.getQuote(), ABSENT, ABSENT);

    // Now quote should be updated
    sim.executeUntil(TimeStamp.of(100));
    Assert.assertEquals(1, numQuotes.get());
    assertQuote(view.getQuote(), 100, 110);

    // Now assert that if nothing happens, no quote happens
    sim.executeUntil(TimeStamp.of(200));
    Assert.assertEquals(1, numQuotes.get());
    assertQuote(view.getQuote(), 100, 110);

    // Test that withdraw triggers a clear
    sim.executeUntil(TimeStamp.of(250));
    view.withdrawOrder(sell);
    sim.executeUntil(TimeStamp.of(300));
    Assert.assertEquals(2, numQuotes.get());
    assertQuote(view.getQuote(), 100, ABSENT);

    // Check that even if a quote wasn't scheduled for a normal quote tick, an order at that time
    // will bump it up to the next clear, i.e. no clear is scheduled for 400, if we advance to that
    // that time, and then submit an order, the next clear will be at 500, not 400.
    sim.executeUntil(TimeStamp.of(400));
    Assert.assertEquals(2, numQuotes.get());
    assertQuote(view.getQuote(), 100, ABSENT);

    view.submitOrder(SELL, Price.of(125), 1);
    sim.executeUntil(TimeStamp.of(400));
    Assert.assertEquals(2, numQuotes.get());
    assertQuote(view.getQuote(), 100, ABSENT);

    sim.executeUntil(TimeStamp.of(500));
    Assert.assertEquals(3, numQuotes.get());
    assertQuote(view.getQuote(), 100, 125);
  }

  @Repeat(100)
  @Test
  public void consistentRandomTest() {
    long seed = rand.nextLong();

    // Test that all have the same results
    assertEquals(1, IntStream.range(0, 3).map(i -> {
      MockSim sim = new MockSim();
      CallMarket market = CallMarket.create(sim, fund, 100, new Random(seed));
      MockAgent agent1 = MockAgent.builder().id(0).build();
      MockAgent agent2 = MockAgent.builder().id(1).build();
      MarketView view1 = market.getView(agent1, TimeStamp.ZERO);
      MarketView view2 = market.getView(agent2, TimeStamp.ZERO);

      view1.submitOrder(BUY, Price.of(200), 1);
      // Either of these could transact
      view1.submitOrder(SELL, Price.of(100), 1);
      view2.submitOrder(SELL, Price.of(100), 1);
      market.clear();

      // Will be 0 or 1, but with constant random seed it should always be the same
      return agent2.transactedUnits;
    }).distinct().count());
  }

  @Test
  public void inconsistentRandomTest() {
    // Test that at least two have different results
    assertEquals(2, IntStream.range(0, 100).parallel().map(i -> {
      MockSim sim = new MockSim();
      CallMarket market = CallMarket.create(sim, fund, 100, rand);
      MockAgent agent1 = MockAgent.builder().id(0).build();
      MockAgent agent2 = MockAgent.builder().id(1).build();
      MarketView view1 = market.getView(agent1, TimeStamp.ZERO);
      MarketView view2 = market.getView(agent2, TimeStamp.ZERO);

      view1.submitOrder(BUY, Price.of(200), 1);
      // Either of these could transact
      view1.submitOrder(SELL, Price.of(100), 1);
      view2.submitOrder(SELL, Price.of(100), 1);
      market.clear();

      // Will be 0 or 1, but with constant random seed it should always be the same
      return agent2.transactedUnits;
    }).distinct().count());
  }

}
