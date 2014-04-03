package entity.agent;

import static org.junit.Assert.*;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.junit.Before;
import org.junit.Test;

import activity.Activity;
import activity.SubmitNMSOrder;
import systemmanager.Executor;
import systemmanager.Scheduler;
import data.FundamentalValue;
import data.MockFundamental;
import data.OrderDatum;
import entity.infoproc.MarketQuoteProcessor;
import entity.infoproc.SIP;
import entity.market.Market;
import entity.market.MockMarket;
import entity.market.Order;
import entity.market.Price;
import entity.market.Quote;
import event.TimeStamp;
import event.TimedActivity;
import fourheap.Order.OrderType;
import static fourheap.Order.OrderType.BUY;
import static fourheap.Order.OrderType.SELL;


public class MarketDataAgentTest {
	
	private Random rand;
	private FundamentalValue fundamental;
	private Executor exec;
	private SIP sip;
	private MockMarket market;
	

	@Before
	public void setupTest() {
		rand = new Random();
		fundamental = new MockFundamental(100000);
		exec = new Executor();
		sip = new SIP(exec, TimeStamp.create(0));
		market = new MockMarket(exec, sip);
	}

	@Test
	public void nyseSimpleTest() {
		MarketDataAgent agent = new MarketDataAgent(exec, fundamental,
				sip, market, rand, "dataTests/nyseSimpleTest.csv");		
		exec.executeUntil(TimeStamp.create(88));

		Collection<Order> orders = market.getOrders();
		assertTrue(orders.size() > 0);
		
		for (Order order : orders) {
			assertEquals(agent, order.getAgent());
			assertEquals(new Price(3249052), order.getPrice());
			assertTrue(5742346 == order.getQuantity());
			assertEquals(TimeStamp.create(88), order.getSubmitTime());
			assertEquals(BUY, order.getOrderType());
			assertEquals(market, order.getMarket());
		}
	}
	
	@Test
	public void nyseDeleteTest() {
		MarketDataAgent agent = new MarketDataAgent(exec, fundamental,
				sip, market, rand, "dataTests/nyseDeleteTest.csv");		
		exec.executeUntil(TimeStamp.create(88));
		
		Collection<Order> orders = market.getOrders();
		assertTrue(orders.size() > 0);
		
		for (Order order : orders) {
			assertEquals(agent, order.getAgent());
			assertEquals(new Price(5516081), order.getPrice());
			assertTrue(981477 == order.getQuantity());
			assertEquals(TimeStamp.create(0), order.getSubmitTime());
			assertEquals(BUY, order.getOrderType());
			assertEquals(market, order.getMarket());
		}
		
		exec.executeUntil(TimeStamp.create(2001));
		orders = market.getActiveOrders();
		// FIXME why isn't the order getting removed?
//		assertTrue(orders.size() == 0);		
		for (Order order : orders) {
			System.out.println(order.toString());
		}
	}
	
	@Test
	public void nyseTest() {
//		MarketDataAgent agent = new MarketDataAgent(scheduler, fundamental,
//				sip, market, rand, "test_files/nyseTest.csv");
//		Iterator<OrderDatum> orderDatumItr = agent.getOrderDatumList().iterator();
//				
//		assertTrue("Too few orders", orderDatumItr.hasNext());		
//		OrderDatum orderDatum = orderDatumItr.next();
//		OrderDatum correct1 = new OrderDatum('A', "1", "1", 'B', "SRG2",
//				TimeStamp.create(0), 'L', "AARCA", new Price(5516081), 981477, BUY);
//		compareOrderDatums(orderDatum, correct1);
//
//		assertTrue("Too few orders", orderDatumItr.hasNext());
//		orderDatum = orderDatumItr.next();
//		OrderDatum correct2 = new OrderDatum('A', "4", "4", 'P', "SRG3", 
//				TimeStamp.create(0), 'B', "AARCA", new Price(1177542), 1747834, SELL);
//		compareOrderDatums(orderDatum, correct2);
//
//
//		assertTrue("Too few orders", orderDatumItr.hasNext());
//		orderDatum = orderDatumItr.next();
//		OrderDatum correct3 = new OrderDatum('A', "0", "10", 'B', "SRG4", 
//				TimeStamp.create(192), 'O', "AARCA", new Price(5223572), 3921581, SELL);
//		compareOrderDatums(orderDatum, correct3);
		
	}
	
	@Test
	public void nasdaqAddTest() {
		MarketDataAgent agent = new MarketDataAgent(exec, fundamental,
				sip, market, rand, "dataTests/nasdaqAddTest.csv");		
		exec.executeUntil(TimeStamp.create(16));

		Collection<Order> orders = market.getOrders();
		assertTrue(orders.size() > 0);
		
		for (Order order : orders) {
			assertEquals(agent, order.getAgent());
			assertEquals(new Price(5630815), order.getPrice());
			assertTrue(3748742 == order.getQuantity());
			assertEquals(TimeStamp.create(16), order.getSubmitTime());
			assertEquals(BUY, order.getOrderType());
			assertEquals(market, order.getMarket());
		}
	}
}

