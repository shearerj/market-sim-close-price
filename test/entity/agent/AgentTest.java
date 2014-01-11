package entity.agent;

import static org.junit.Assert.*;
import static fourheap.Order.OrderType.*;

import java.io.File;
import java.util.Collection;
import java.util.Random;

import logger.Logger;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import activity.Activity;
import activity.SubmitOrder;

import com.google.common.collect.Iterables;

import systemmanager.Consts;
import systemmanager.EventManager;
import systemmanager.Executer;
import data.MockFundamental;
import data.FundamentalValue;
import entity.infoproc.SIP;
import entity.market.Market;
import entity.market.MockMarket;
import entity.market.Order;
import entity.market.Price;
import entity.market.Quote;
import entity.market.Transaction;
import event.TimeStamp;

public class AgentTest {

	private FundamentalValue fundamental = new MockFundamental(100000);
	private Market market;
	private Agent agent;
	private SIP sip;

	@BeforeClass
	public static void setupClass() {
		Logger.setup(3, new File(Consts.TEST_OUTPUT_DIR + "AgentTest.log"));
	}

	@Before
	public void setup() {
		sip = new SIP(TimeStamp.IMMEDIATE);
		market = new MockMarket(sip);
		agent = new MockAgent(fundamental, sip, market);
	}

	@Test
	public void basicOrdersTest() {
		TimeStamp time = TimeStamp.ZERO;
		
		// test adding and removing orders
		market.submitOrder(agent, BUY, new Price(100), 1, time);
		market.submitOrder(agent, SELL, new Price(50), 2, time.plus(new TimeStamp(1)));
		Collection<Order> orders = agent.activeOrders;
		Order order = Iterables.getFirst(orders, null);
		// Note: nondeterministic which order is "first" so need to check
		boolean buyRemoved = true;
		if (order.getSubmitTime().equals(time)) {
			assertEquals(BUY, order.getOrderType());
			assertEquals(new Price(100), order.getPrice());
			assertEquals(1, order.getQuantity());
		} else if (order.getSubmitTime().equals(new TimeStamp(1))) {
			assertEquals(SELL, order.getOrderType());
			assertEquals(new Price(50), order.getPrice());
			assertEquals(2, order.getQuantity());
			buyRemoved = false;
		} else {
			fail("Should never get here");
		}
		assertEquals(2, orders.size());
		assertTrue("Agent does not know about buy order", agent.activeOrders.contains(order));
		
		// Test that remove works correctly
		market.withdrawOrder(order, new TimeStamp(1));
		orders = agent.activeOrders;
		assertEquals(1, orders.size());
		assertTrue("Order was not removed", !agent.activeOrders.contains(order));
		order = Iterables.getFirst(orders, null);
		if (buyRemoved) {
			assertEquals(SELL, order.getOrderType());
			assertEquals(new Price(50), order.getPrice());
			assertEquals(2, order.getQuantity());
		} else {
			assertEquals(BUY, order.getOrderType());
			assertEquals(new Price(100), order.getPrice());
			assertEquals(1, order.getQuantity());
		}
	}
	
	@Test
	public void withdrawOrder() {
		TimeStamp time0 = TimeStamp.ZERO;
		TimeStamp time1 = new TimeStamp(1);
		
		EventManager em = new EventManager(new Random());
		em.addActivity(new SubmitOrder(agent, market, BUY, new Price(100), 1, time0));
		em.executeUntil(time1.plus(time1));
		
		// Verify orders added correctly
		Collection<Order> orders = agent.activeOrders;
		assertEquals(1, orders.size());
		Order orderToWithdraw = null;
		for (Order o : orders) {
			orderToWithdraw = o;
			assertEquals(new Price(100), o.getPrice());
			assertEquals(time0, o.getSubmitTime());
		}
		assertNotNull(orderToWithdraw);
		
		Quote q = market.getQuoteProcessor().getQuote();
		assertEquals("Incorrect ASK", null,  q.getAskPrice() );
		assertEquals("Incorrect BID", new Price(100),  q.getBidPrice() );
		assertEquals("Incorrect ASK quantity",  0,  q.getAskQuantity() );
		assertEquals("Incorrect BID quantity",  1,  q.getBidQuantity() );
		
		// Withdraw order
		Iterable<? extends Activity> acts = agent.withdrawOrder(orderToWithdraw, time1);
		assertEquals(0, Iterables.size(acts));
		
		orders = agent.activeOrders;
		assertEquals("Order was not withdrawn", 0, orders.size());
		q = market.getQuoteProcessor().getQuote();
		assertEquals("Incorrect ASK", null,  q.getAskPrice() );
		assertEquals("Incorrect BID", null,  q.getBidPrice() );
		assertEquals("Incorrect ASK quantity",  0,  q.getAskQuantity() );
		assertEquals("Incorrect BID quantity",  0,  q.getBidQuantity() );
	}
	
	@Test
	public void withdrawOrderDelayed() {
		// withdraw order when quotes are delayed
		
		TimeStamp time0 = TimeStamp.ZERO;
		TimeStamp time1 = new TimeStamp(10);
		
		EventManager em = new EventManager(new Random());
		
		MockMarket market2 = new MockMarket(sip, new TimeStamp(10));
		em.addActivity(new SubmitOrder(agent, market2, BUY, new Price(100), 1, time0));
		em.executeUntil(new TimeStamp(1));
		
		// Verify orders added correctly
		Collection<Order> orders = agent.activeOrders;
		assertEquals(1, orders.size());
		Order orderToWithdraw = null;
		for (Order o : orders) {
			orderToWithdraw = o;
			assertEquals(new Price(100), o.getPrice());
			assertEquals(time0, o.getSubmitTime());
		}
		assertNotNull(orderToWithdraw);
		
		Quote q = market2.getQuoteProcessor().getQuote();
		assertEquals("Incorrect ASK", null,  q.getAskPrice() );
		assertEquals("Incorrect BID", null,  q.getBidPrice() );
		assertEquals("Incorrect ASK quantity",  0,  q.getAskQuantity() );
		assertEquals("Incorrect BID quantity",  0,  q.getBidQuantity() );
		
		// After quotes have updated
		em.executeUntil(new TimeStamp(11));
		q = market2.getQuoteProcessor().getQuote();
		assertEquals("Incorrect ASK", null,  q.getAskPrice() );
		assertEquals("Incorrect BID", new Price(100),  q.getBidPrice() );
		assertEquals("Incorrect ASK quantity",  0,  q.getAskQuantity() );
		assertEquals("Incorrect BID quantity",  1,  q.getBidQuantity() );
		
		// Withdraw order
		Iterable<? extends Activity> acts = agent.withdrawOrder(orderToWithdraw, time1);
		assertEquals(1, Iterables.size(acts));	// ProcessQuote for QP since delayed
		em.addActivity(Iterables.getOnlyElement(acts));
		
		orders = agent.activeOrders;
		assertEquals("Order was not withdrawn", 0, orders.size());
		assertTrue("Order was not removed", !agent.activeOrders.contains(orderToWithdraw));
		// Verify that quote is now stale
		q = market2.getQuoteProcessor().getQuote();
		assertEquals("Incorrect ASK", null,  q.getAskPrice() );
		assertEquals("Incorrect BID", new Price(100),  q.getBidPrice() );
		assertEquals("Incorrect ASK quantity",  0,  q.getAskQuantity() );
		assertEquals("Incorrect BID quantity",  1,  q.getBidQuantity() );
		
		// After quotes have updated
		em.executeUntil(new TimeStamp(21));
		q = market2.getQuoteProcessor().getQuote();
		assertEquals("Incorrect ASK", null,  q.getAskPrice() );
		assertEquals("Incorrect BID", null,  q.getBidPrice() );
		assertEquals("Incorrect ASK quantity",  0,  q.getAskQuantity() );
		assertEquals("Incorrect BID quantity",  0,  q.getBidQuantity() );
	}
	
	@Test
	public void withdrawNewestOrder() {
		TimeStamp time0 = TimeStamp.ZERO;
		TimeStamp time1 = new TimeStamp(1);
		
		EventManager em = new EventManager(new Random());
		em.addActivity(new SubmitOrder(agent, market, BUY, new Price(50), 1, time0));
		em.addActivity(new SubmitOrder(agent, market, SELL, new Price(100), 1, time1));
		em.executeUntil(time1.plus(time1));
		
		// Verify orders added correctly
		Collection<Order> orders = agent.activeOrders;
		assertEquals(2, orders.size());
		Order orderToWithdraw = null;
		for (Order o : orders) {
			if (o.getOrderType() == BUY) {
				assertEquals(new Price(50), o.getPrice());
				assertEquals(time0, o.getSubmitTime());
			}
			if (o.getOrderType() == SELL) {
				orderToWithdraw = o;
				assertEquals(new Price(100), o.getPrice());
				assertEquals(time1, o.getSubmitTime());
			}
		}
		assertNotNull(orderToWithdraw);
		Quote q = market.getQuoteProcessor().getQuote();
		assertEquals("Incorrect ASK", new Price(100),  q.getAskPrice() );
		assertEquals("Incorrect BID", new Price(50),  q.getBidPrice() );
		assertEquals("Incorrect ASK quantity",  1,  q.getAskQuantity() );
		assertEquals("Incorrect BID quantity",  1,  q.getBidQuantity() );
		
		// Withdraw newest order (sell)
		Iterable<? extends Activity> acts = agent.withdrawNewestOrder(time1);
		assertEquals(0, Iterables.size(acts));
		
		orders = agent.activeOrders;
		assertEquals(1, orders.size());
		Order order = Iterables.getFirst(orders, null);
		assertEquals(BUY, order.getOrderType());
		assertEquals(new Price(50), order.getPrice());
		assertEquals(1, order.getQuantity());
		assertTrue("Order was not withdrawn", !agent.activeOrders.contains(orderToWithdraw));
		q = market.getQuoteProcessor().getQuote();
		assertEquals("Incorrect ASK", null,  q.getAskPrice() );
		assertEquals("Incorrect BID", new Price(50),  q.getBidPrice() );
		assertEquals("Incorrect ASK quantity",  0,  q.getAskQuantity() );
		assertEquals("Incorrect BID quantity",  1,  q.getBidQuantity() );
	}
	
	@Test
	public void withdrawOldestOrder() {
		TimeStamp time0 = TimeStamp.ZERO;
		TimeStamp time1 = new TimeStamp(1);
		
		EventManager em = new EventManager(new Random());
		em.addActivity(new SubmitOrder(agent, market, BUY, new Price(50), 1, time0));
		em.addActivity(new SubmitOrder(agent, market, SELL, new Price(100), 1, time1));
		em.executeUntil(time1.plus(time1));
		
		// Verify orders added correctly
		Collection<Order> orders = agent.activeOrders;
		assertEquals(2, orders.size());
		Order orderToWithdraw = null;
		for (Order o : orders) {
			if (o.getOrderType() == BUY) {
				orderToWithdraw = o;
				assertEquals(new Price(50), o.getPrice());
				assertEquals(time0, o.getSubmitTime());
			}
			if (o.getOrderType() == SELL) {
				assertEquals(new Price(100), o.getPrice());
				assertEquals(time1, o.getSubmitTime());
			}
		}
		assertNotNull(orderToWithdraw);
		// Verify quote correct
		Quote q = market.getQuoteProcessor().getQuote();
		assertEquals("Incorrect ASK", new Price(100),  q.getAskPrice() );
		assertEquals("Incorrect BID", new Price(50),  q.getBidPrice() );
		assertEquals("Incorrect ASK quantity",  1,  q.getAskQuantity() );
		assertEquals("Incorrect BID quantity",  1,  q.getBidQuantity() );
		
		// Withdraw oldest order (buy)
		Iterable<? extends Activity> acts = agent.withdrawOldestOrder(time1);
		assertEquals(0, Iterables.size(acts));
		
		orders = agent.activeOrders;
		assertEquals(1, orders.size());
		Order order = Iterables.getFirst(orders, null);
		assertEquals(SELL, order.getOrderType());
		assertEquals(new Price(100), order.getPrice());
		assertEquals(1, order.getQuantity());
		assertTrue("Order was not withdrawn", !agent.activeOrders.contains(orderToWithdraw));
		// Verify quote correct
		q = market.getQuoteProcessor().getQuote();
		assertEquals("Incorrect ASK", new Price(100),  q.getAskPrice() );
		assertEquals("Incorrect BID", null,  q.getBidPrice() );
		assertEquals("Incorrect ASK quantity",  1,  q.getAskQuantity() );
		assertEquals("Incorrect BID quantity",  0,  q.getBidQuantity() );
	}
	
	@Test
	public void withdrawAllOrders() {
		TimeStamp time0 = TimeStamp.ZERO;
		TimeStamp time1 = new TimeStamp(1);
		
		EventManager em = new EventManager(new Random());
		em.addActivity(new SubmitOrder(agent, market, BUY, new Price(50), 1, time0));
		em.addActivity(new SubmitOrder(agent, market, SELL, new Price(100), 1, time1));
		em.executeUntil(time1.plus(time1));
		
		// Verify orders added correctly
		Collection<Order> orders = agent.activeOrders;
		assertEquals(2, orders.size());
		for (Order o : orders) {
			if (o.getOrderType() == BUY) {
				assertEquals(new Price(50), o.getPrice());
				assertEquals(time0, o.getSubmitTime());
			}
			if (o.getOrderType() == SELL) {
				assertEquals(new Price(100), o.getPrice());
				assertEquals(time1, o.getSubmitTime());
			}
		}
		// Verify quote correct
		Quote q = market.getQuoteProcessor().getQuote();
		assertEquals("Incorrect ASK", new Price(100),  q.getAskPrice() );
		assertEquals("Incorrect BID", new Price(50),  q.getBidPrice() );
		assertEquals("Incorrect ASK quantity",  1,  q.getAskQuantity() );
		assertEquals("Incorrect BID quantity",  1,  q.getBidQuantity() );
		
		// Withdraw all orders
		Iterable<? extends Activity> acts = agent.withdrawAllOrders(time1);
		assertEquals(0, Iterables.size(acts));
		
		assertEquals(0, agent.activeOrders.size());	
		// Verify quote correct
		q = market.getQuoteProcessor().getQuote();
		assertEquals("Incorrect ASK", null,  q.getAskPrice() );
		assertEquals("Incorrect BID", null,  q.getBidPrice() );
		assertEquals("Incorrect ASK quantity",  0,  q.getAskQuantity() );
		assertEquals("Incorrect BID quantity",  0,  q.getBidQuantity() );
	}
	
	@Test
	public void addTransactionTest() {
		TimeStamp time = TimeStamp.ZERO;
		MockAgent agent2 = new MockAgent(fundamental, sip, market);
		
		assertEquals( 0, agent.transactions.size());
		
		// Creating and adding bids
		market.submitOrder(agent, BUY, new Price(110), 1, time);
		market.submitOrder(agent2, SELL, new Price(100), 1, time);
		assertEquals(0, market.getTransactions().size());
		
		// Testing the market for the correct transactions
		Executer.execute(market.clear(time));	// will call agent's addTransaction
		assertEquals( 1, market.getTransactions().size() );
		Transaction tr = market.getTransactions().get(0);
		assertEquals( 1, agent.transactions.size());
		assertEquals(tr, agent.transactions.get(0));
	}
	
	@Test
	public void liquidation() {
		TimeStamp time = new TimeStamp(100);
		agent.profit = 5000;
		
		// Check that no change if position 0
		agent.positionBalance = 0;
		agent.liquidateAtPrice(new Price(100000), time);
		assertEquals(5000, agent.getPostLiquidationProfit());
		
		// Check liquidation when position > 0 (sell 1 unit)
		agent.profit = 5000;
		agent.positionBalance = 1;
		agent.liquidateAtPrice(new Price(100000), time);
		assertEquals(105000, agent.getPostLiquidationProfit());
		
		// Check liquidation when position < 0 (buy 2 units)
		agent.profit = 5000;
		agent.positionBalance = -2;
		agent.liquidateAtPrice(new Price(100000), time);
		assertEquals(-195000, agent.getPostLiquidationProfit());
		
		// Check liquidation at fundamental
		agent.profit = 5000;
		agent.positionBalance = 1;
		agent.liquidateAtFundamental(time);
		assertEquals(fundamental.getValueAt(time).longValue() + 5000, agent.getPostLiquidationProfit());
	}
	
	@Test
	public void extraTest() {
		for (int i = 0; i < 100; i++) {
			setup();
			basicOrdersTest();
		}
	}
}
