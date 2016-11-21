package edu.umich.srg.marketsim.market;

import static edu.umich.srg.fourheap.OrderType.BUY;
import static edu.umich.srg.fourheap.OrderType.SELL;
import static edu.umich.srg.marketsim.testing.MarketAsserts.ABSENT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import edu.umich.srg.marketsim.Price;
import edu.umich.srg.marketsim.TimeStamp;
import edu.umich.srg.marketsim.fundamental.ConstantFundamental;
import edu.umich.srg.marketsim.fundamental.Fundamental;
import edu.umich.srg.marketsim.market.Market.MarketView;
import edu.umich.srg.marketsim.testing.MarketAsserts;
import edu.umich.srg.marketsim.testing.MockAgent;
import edu.umich.srg.marketsim.testing.MockSim;

public class CDAMarketTest {
  private Fundamental fund;
  private MockSim sim;
  private CdaMarket market;
  private MockAgent agent;
  private MarketView view;

  @Before
  public void setup() {
    sim = new MockSim();
    fund = ConstantFundamental.create(0);
    market = CdaMarket.create(sim, fund);
    agent = new MockAgent();
    view = market.getView(agent, TimeStamp.ZERO);
  }

  @Test
  public void addBid() {
    view.submitOrder(BUY, Price.of(1), 1);
    MarketAsserts.assertQuote(view.getQuote(), Price.of(1), null);
  }

  @Test
  public void addAsk() {
    view.submitOrder(SELL, Price.of(1), 1);
    MarketAsserts.assertQuote(view.getQuote(), null, Price.of(1));
  }

  @Test
  public void withdrawTest() {
    OrderRecord order = view.submitOrder(SELL, Price.of(100), 1);
    MarketAsserts.assertQuote(view.getQuote(), null, Price.of(100));

    view.withdrawOrder(order, view.getQuantity(order));
    MarketAsserts.assertQuote(view.getQuote(), null, null);
  }

  @Test
  public void bidFirst() {
    OrderRecord buy = view.submitOrder(BUY, Price.of(100), 1);
    OrderRecord sell = view.submitOrder(SELL, Price.of(50), 1);

    MarketAsserts.assertQuote(view.getQuote(), ABSENT, ABSENT);
    assertEquals(Price.of(100), agent.lastTransactionPrice);
    assertTrue(view.getActiveOrders().isEmpty());
    assertEquals(0, view.getQuantity(buy));
    assertEquals(0, view.getQuantity(sell));
  }

  @Test
  public void askFirst() {
    OrderRecord sell = view.submitOrder(SELL, Price.of(50), 1);
    OrderRecord buy = view.submitOrder(BUY, Price.of(100), 1);

    MarketAsserts.assertQuote(view.getQuote(), ABSENT, ABSENT);
    assertEquals(Price.of(50), agent.lastTransactionPrice);
    assertTrue(view.getActiveOrders().isEmpty());
    assertEquals(0, view.getQuantity(buy));
    assertEquals(0, view.getQuantity(sell));
  }

  /**
   * Test clearing when there are ties in price. Should match at uniform price. Also checks
   * tie-breaking by time.
   */
  @Test
  public void priceTimeTest() {
    OrderRecord order1 = view.submitOrder(SELL, Price.of(100), 1);
    OrderRecord order2 = view.submitOrder(SELL, Price.of(100), 1);
    OrderRecord order3 = view.submitOrder(BUY, Price.of(150), 1);

    market.clear();

    // Check that earlier order (order1) is trading with order3
    // Testing the market for the correct transactions
    assertEquals(2, agent.transactions);
    assertEquals(0, view.getQuantity(order1));
    assertEquals(0, view.getQuantity(order3));

    // Existing order2 S1@100
    view.submitOrder(SELL, Price.of(100), 1);
    view.submitOrder(SELL, Price.of(100), 1);
    view.submitOrder(SELL, Price.of(100), 1);

    market.clear();

    OrderRecord order5 = view.submitOrder(BUY, Price.of(130), 1);

    market.clear();

    // Check that the first submitted order2 S1@100 transacts
    assertEquals(4, agent.transactions);
    assertEquals(0, view.getQuantity(order2));
    assertEquals(0, view.getQuantity(order5));

    // Let's try populating the market with random orders
    // 3 S1@100 remain
    order5 = view.submitOrder(SELL, Price.of(90), 1);
    view.submitOrder(SELL, Price.of(100), 1);
    view.submitOrder(SELL, Price.of(110), 1);
    view.submitOrder(SELL, Price.of(120), 1);
    view.submitOrder(BUY, Price.of(80), 1);
    view.submitOrder(BUY, Price.of(70), 1);
    view.submitOrder(BUY, Price.of(60), 1);

    market.clear();

    assertEquals(4, agent.transactions); // no change

    // Check basic overlap - between order5 (@90) and next order (order2)
    order2 = view.submitOrder(BUY, Price.of(130), 1);

    market.clear();

    assertEquals(6, agent.transactions);
    assertEquals(0, view.getQuantity(order2));
    assertEquals(0, view.getQuantity(order5));

    // Check additional overlapping orders
    view.submitOrder(BUY, Price.of(110), 4);

    market.clear();

    assertEquals(14, agent.transactions);
  }

  /** Verify that earlier orders transact */
  @Test
  public void timePriorityBuy() {
    OrderRecord first = view.submitOrder(BUY, Price.of(100), 1);
    OrderRecord second = view.submitOrder(BUY, Price.of(100), 1);
    view.submitOrder(SELL, Price.of(100), 1);

    market.clear();

    assertEquals(0, view.getQuantity(first));
    assertEquals(1, view.getQuantity(second));
  }

  /** Verify that earlier orders transact */
  @Test
  public void timePrioritySell() {
    OrderRecord first = view.submitOrder(SELL, Price.of(100), 1);
    OrderRecord second = view.submitOrder(SELL, Price.of(100), 1);
    view.submitOrder(BUY, Price.of(100), 1);

    market.clear();

    assertEquals(0, view.getQuantity(first));
    assertEquals(1, view.getQuantity(second));
  }

}
