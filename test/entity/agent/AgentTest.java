package entity.agent;

import static org.junit.Assert.*;

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
import systemmanager.Consts.OrderType;
import data.DummyFundamental;
import data.FundamentalValue;
import entity.infoproc.SIP;
import entity.market.Market;
import entity.market.MockMarket;
import entity.market.Order;
import entity.market.Price;
import event.TimeStamp;

public class AgentTest {

	private FundamentalValue fundamental = new DummyFundamental(100000);
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
		market.submitOrder(agent, OrderType.BUY, new Price(100), 1, time);
		market.submitOrder(agent, OrderType.SELL, new Price(50), 2, time.plus(new TimeStamp(1)));
		Collection<Order> orders = agent.activeOrders;
		Order order = Iterables.getFirst(orders, null);
		// Note: nondeterministic which order is "first" so need to check
		boolean buyRemoved = true;
		if (order.getSubmitTime().equals(time)) {
			assertEquals(OrderType.BUY, order.getOrderType());
			assertEquals(new Price(100), order.getPrice());
			assertEquals(1, order.getQuantity());
		} else if (order.getSubmitTime().equals(new TimeStamp(1))) {
			assertEquals(OrderType.SELL, order.getOrderType());
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
			assertEquals(OrderType.SELL, order.getOrderType());
			assertEquals(new Price(50), order.getPrice());
			assertEquals(2, order.getQuantity());
		} else {
			assertEquals(OrderType.BUY, order.getOrderType());
			assertEquals(new Price(100), order.getPrice());
			assertEquals(1, order.getQuantity());
		}
	}
	
	@Test
	public void withdrawNewestOrder() {
		TimeStamp time0 = TimeStamp.ZERO;
		TimeStamp time1 = new TimeStamp(1);
		
		EventManager em = new EventManager(new Random());
		em.addActivity(new SubmitOrder(agent, market, OrderType.BUY, new Price(100), 1, time0));
		em.addActivity(new SubmitOrder(agent, market, OrderType.SELL, new Price(50), 1, time1));
		em.executeUntil(time1.plus(time1));
		
		// Verify orders added correctly
		Collection<Order> orders = agent.activeOrders;
		assertEquals(2, orders.size());
		Order orderToWithdraw = null;
		for (Order o : orders) {
			if (o.getOrderType() == OrderType.BUY) {
				assertEquals(new Price(100), o.getPrice());
				assertEquals(time0, o.getSubmitTime());
			}
			if (o.getOrderType() == OrderType.SELL) {
				orderToWithdraw = o;
				assertEquals(new Price(50), o.getPrice());
				assertEquals(time1, o.getSubmitTime());
			}
		}
		assertNotNull(orderToWithdraw);
		
		// Withdraw newest order (sell)
		Iterable<? extends Activity> acts = agent.withdrawNewestOrder();
		for (Activity a : acts)	em.addActivity(a);
		em.executeUntil(time1.plus(time1));
		
		orders = agent.activeOrders;
		assertEquals(1, orders.size());
		Order order = Iterables.getFirst(orders, null);
		assertEquals(OrderType.BUY, order.getOrderType());
		assertEquals(new Price(100), order.getPrice());
		assertEquals(1, order.getQuantity());
		assertTrue("Order was not withdrawn", !agent.activeOrders.contains(orderToWithdraw));		
	}
	
	@Test
	public void withdrawOldestOrder() {
		TimeStamp time0 = TimeStamp.ZERO;
		TimeStamp time1 = new TimeStamp(1);
		
		EventManager em = new EventManager(new Random());
		em.addActivity(new SubmitOrder(agent, market, OrderType.BUY, new Price(100), 1, time0));
		em.addActivity(new SubmitOrder(agent, market, OrderType.SELL, new Price(50), 1, time1));
		em.executeUntil(time1.plus(time1));
		
		// Verify orders added correctly
		Collection<Order> orders = agent.activeOrders;
		assertEquals(2, orders.size());
		Order orderToWithdraw = null;
		for (Order o : orders) {
			if (o.getOrderType() == OrderType.BUY) {
				orderToWithdraw = o;
				assertEquals(new Price(100), o.getPrice());
				assertEquals(time0, o.getSubmitTime());
			}
			if (o.getOrderType() == OrderType.SELL) {
				assertEquals(new Price(50), o.getPrice());
				assertEquals(time1, o.getSubmitTime());
			}
		}
		assertNotNull(orderToWithdraw);
		
		// Withdraw oldest order (buy)
		Iterable<? extends Activity> acts = agent.withdrawOldestOrder();
		for (Activity a : acts)	em.addActivity(a);
		em.executeUntil(time1.plus(time1));
		
		orders = agent.activeOrders;
		assertEquals(1, orders.size());
		Order order = Iterables.getFirst(orders, null);
		assertEquals(OrderType.SELL, order.getOrderType());
		assertEquals(new Price(50), order.getPrice());
		assertEquals(1, order.getQuantity());
		assertTrue("Order was not withdrawn", !agent.activeOrders.contains(orderToWithdraw));		
	}
	
	@Test
	public void withdrawAllOrders() {
		TimeStamp time0 = TimeStamp.ZERO;
		TimeStamp time1 = new TimeStamp(1);
		
		EventManager em = new EventManager(new Random());
		em.addActivity(new SubmitOrder(agent, market, OrderType.BUY, new Price(100), 1, time0));
		em.addActivity(new SubmitOrder(agent, market, OrderType.SELL, new Price(50), 1, time1));
		em.executeUntil(time1.plus(time1));
		
		// Verify orders added correctly
		Collection<Order> orders = agent.activeOrders;
		assertEquals(2, orders.size());
		for (Order o : orders) {
			if (o.getOrderType() == OrderType.BUY) {
				assertEquals(new Price(100), o.getPrice());
				assertEquals(time0, o.getSubmitTime());
			}
			if (o.getOrderType() == OrderType.SELL) {
				assertEquals(new Price(50), o.getPrice());
				assertEquals(time1, o.getSubmitTime());
			}
		}
		// Withdraw all orders
		Iterable<? extends Activity> acts = agent.withdrawAllOrders();
		for (Activity a : acts)	em.addActivity(a);
		em.executeUntil(time1.plus(time1));

		orders = agent.activeOrders;
		assertEquals(0, orders.size());	
	}
	
	public void addTransactionTest() {
		// TODO
	}
	
	public void surplusTest() {
		// TODO verify computation, plus discount factor
	}

	public void liquidation() {
		// TODO
	}
	
	@Test
	public void extraTest() {
		for (int i = 0; i < 100; i++) {
			setup();
			basicOrdersTest();
		}
	}
}
