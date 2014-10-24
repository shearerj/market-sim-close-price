package entity.agent;

import static fourheap.Order.OrderType.BUY;
import static fourheap.Order.OrderType.SELL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static utils.Tests.checkSingleTransaction;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Random;

import logger.Log;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import systemmanager.Keys.LaLatency;
import systemmanager.MockSim;
import data.Props;
import entity.market.Market;
import entity.market.Market.MarketView;
import entity.market.Price;
import event.TimeStamp;
import fourheap.Order.OrderType;

public class LAAgentTest {
	
	private static final Random rand = new Random();
	
	private MockSim sim;
	private Collection<Market> markets;
	private MarketView one, two;
	private Agent mockAgent;

	@Before
	public void setup() throws IOException {
		sim = MockSim.createCDA(getClass(), Log.Level.NO_LOGGING, 2);
		
		markets = sim.getMarkets();
		Iterator<Market> marketIter = markets.iterator();
		Market actualMarket = marketIter.next();
		one = actualMarket.getPrimaryView();
		two = marketIter.next().getPrimaryView();
		assertFalse(marketIter.hasNext());
		
		mockAgent = mockAgent();
	}
	
	@After
	public void cleanup() {
		sim.flushLog();
	}
	
	// FIXME Test that quote of a market accurately reflected when agent notified. Put tests in HFTAgentTest
	
	// FIXME Test that orderSubmitted happens at the correct time. E.g. before strategy Also in HFT Agent?
	
	// FIXME Assert that tied hfts act in random order
		
	/*
	 * Bug in LA that occurred in very particular circumstances. Imagine market1
	 * has BUY @ 30 and BUY @ 50. A SELL @ 10 arrives in market2. The LA submits
	 * a SELL @ 30 -> market1 and schedules a BUY @ 30 for market2. After the
	 * SELL clears, market1 has a BUY @ 30 left. There is still an arbitrage opp,
	 * and the LA acts again for before its second order goes into market2. So
	 * it submits a SELL @ 20 -> market1, and schedules a BUY @ 20 for market2.
	 * This first SELL clears, and results in no arbitrage opportunities, so
	 * then the first BUY @ 30 makes it market2 where it transacts. Finally the
	 * BUY @ 20 makes it to market2, but there are no more orders, and so this
	 * order sits while the LA holds a position.
	 */
	@Test
	public void oneSidedArbitrageTest() {
		LAAgent la = laAgent();
		submitOrder(one, BUY, Price.of(5));
		submitOrder(one, BUY, Price.of(7));
		submitOrder(two, SELL, Price.of(1));

		assertEquals(0, la.positionBalance);
		assertTrue(la.profit > 0);
	}
	
	@Test
	public void laProfitTest() {
		LAAgent la = laAgent();
		submitOrder(one, BUY, Price.of(5));
		submitOrder(two, SELL, Price.of(1));
		// LA Strategy gets called implicitly 
		
		assertEquals(0, la.positionBalance);
		assertEquals(4, la.profit);
		assertTrue(la.activeOrders.isEmpty());
	}
	
	@Test
	public void multiQuantityLaProfitTest() {
		LAAgent la = laAgent();
		submitOrder(one, BUY, Price.of(5), 3);
		submitOrder(two, SELL, Price.of(1), 2);
		// LA Strategy gets called implicitly 
		
		assertEquals(0, la.positionBalance);
		assertEquals(8, la.profit);
		assertTrue(la.activeOrders.isEmpty());
	}
	
	@Test
	/**
	 * This test verifies that an LA with latency doesn't submit new orders until it's sure it's old orders are reflected in its quote.
	 */
	public void laLatencyNoRepeatOrdersTest() {
		LAAgent la = laAgent(Props.fromPairs(LaLatency.class, TimeStamp.of(10)));
		
		submitOrder(one, BUY, Price.of(5));
		submitOrder(two, SELL, Price.of(1));
		sim.executeUntil(TimeStamp.of(5));
		submitOrder(one, BUY, Price.of(3)); // Used to cause LA to submit extra orders
		sim.executeUntil(TimeStamp.of(10)); // LA has submitted orders
		
		assertEquals(0, la.positionBalance);
		assertEquals(0, la.profit);
		assertEquals(2, la.activeOrders.size());
		
		sim.executeUntil(TimeStamp.of(15)); // LA acts on second "arbitrage"
		
		assertEquals(0, la.positionBalance);
		assertEquals(0, la.profit);
		assertEquals(2, la.activeOrders.size());
		
		sim.executeUntil(TimeStamp.of(20)); // Orders reach market
		checkSingleTransaction(one.getTransactions(), Price.of(5), TimeStamp.of(20), 1);
		checkSingleTransaction(two.getTransactions(), Price.of(1), TimeStamp.of(20), 1);
		
		sim.executeUntil(TimeStamp.of(30)); // Takes this long for the LA to find out about it
		assertEquals(0, la.positionBalance);
		assertEquals(4, la.profit);
		assertTrue(la.activeOrders.isEmpty());
	}
	
	@Test
	public void severalArbitragesNoRepeatOrders() {
		LAAgent la = laAgent(Props.fromPairs(LaLatency.class, TimeStamp.of(10)));
		
		submitOrder(one, BUY, Price.of(5));
		submitOrder(two, SELL, Price.of(1));
		submitOrder(one, BUY, Price.of(3));
		
		sim.executeUntil(TimeStamp.of(10)); // LA has submitted orders
		
		assertEquals(0, la.positionBalance);
		assertEquals(0, la.profit);
		assertEquals(2, la.activeOrders.size());
		
		sim.executeUntil(TimeStamp.of(20)); // Orders reach market
		checkSingleTransaction(one.getTransactions(), Price.of(5), TimeStamp.of(20), 1);
		checkSingleTransaction(two.getTransactions(), Price.of(1), TimeStamp.of(20), 1);
		
		sim.executeUntil(TimeStamp.of(30)); // Takes this long for the LA to find out about it
		assertEquals(0, la.positionBalance);
		assertEquals(4, la.profit);
		assertTrue(la.activeOrders.isEmpty());
	}
	
	@Test
	public void severalArbitragesNoRepeatOrdersDifferentOrder() {
		LAAgent la = laAgent(Props.fromPairs(LaLatency.class, TimeStamp.of(10)));
		
		// Different Order
		submitOrder(one, BUY, Price.of(3));
		submitOrder(two, SELL, Price.of(1));
		submitOrder(one, BUY, Price.of(5));
		
		sim.executeUntil(TimeStamp.of(10)); // LA has submitted orders
		
		assertEquals(0, la.positionBalance);
		assertEquals(0, la.profit);
		assertEquals(2, la.activeOrders.size());
		
		sim.executeUntil(TimeStamp.of(20)); // Orders reach market
		checkSingleTransaction(one.getTransactions(), Price.of(5), TimeStamp.of(20), 1);
		checkSingleTransaction(two.getTransactions(), Price.of(1), TimeStamp.of(20), 1);
		
		sim.executeUntil(TimeStamp.of(30)); // Takes this long for the LA to find out about it
		assertEquals(0, la.positionBalance);
		assertEquals(4, la.profit);
		assertTrue(la.activeOrders.isEmpty());
	}
	
	@Test
	/**
	 * This test makes sure the fix for the previous test doesn't effect infinitely fast LAs.
	 */
	public void laNoLatencySeveralOrders() {
		LAAgent la = laAgent();
		
		submitOrder(one, BUY, Price.of(5));
		submitOrder(two, SELL, Price.of(1));
		// LA Strategy gets called implicitly
		
		assertEquals(0, la.positionBalance);
		assertEquals(4, la.profit);
		assertTrue(la.activeOrders.isEmpty());
		
		submitOrder(one, BUY, Price.of(10));
		submitOrder(two, SELL, Price.of(6));
		// LA Strategy gets called implicitly 

		assertEquals(0, la.positionBalance);
		assertEquals(8, la.profit);
		assertTrue(la.activeOrders.isEmpty());
	}
	
	@Test
	public void randomTests() throws IOException {
		for (int i = 0; i < 100; ++i) {
			setup();
			oneSidedArbitrageTest();
			setup();
			laLatencyNoRepeatOrdersTest();
			setup();
			severalArbitragesNoRepeatOrders();
			setup();
			severalArbitragesNoRepeatOrdersDifferentOrder();
		}
	}
	
	private OrderRecord submitOrder(MarketView view, OrderType buyOrSell, Price price) {
		return submitOrder(view, buyOrSell, price, 1);
	}

	private OrderRecord submitOrder(MarketView view, OrderType buyOrSell, Price price, int quantity) {
		OrderRecord order = mockAgent.submitOrder(view, buyOrSell, price, quantity);
		sim.executeImmediate();
		return order;
	}
	
	private LAAgent laAgent(Props parameters) {
		return LAAgent.create(sim, markets, rand, parameters);
	}
	
	private LAAgent laAgent() {
		return laAgent(Props.fromPairs());
	}
	
	private Agent mockAgent() {
		return new Agent(sim, TimeStamp.ZERO, rand, Props.fromPairs()) {
			private static final long serialVersionUID = 1L;
			@Override public void agentStrategy() { }
			@Override public String toString() { return "TestAgent " + id; }
		};
	}

}
