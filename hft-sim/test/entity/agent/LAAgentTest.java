package entity.agent;

import static fourheap.Order.OrderType.BUY;
import static fourheap.Order.OrderType.SELL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static utils.Tests.assertSingleTransaction;
import logger.Log;

import org.junit.Before;
import org.junit.Test;

import systemmanager.Keys.LaLatency;
import utils.Mock;
import utils.Rand;

import com.google.common.collect.ImmutableList;

import data.Props;
import entity.market.CDAMarket;
import entity.market.Market;
import entity.market.Market.MarketView;
import entity.market.Price;
import event.EventQueue;
import event.TimeStamp;
import fourheap.Order.OrderType;

public class LAAgentTest {
	
	private static final Rand rand = Rand.create();
	
	private EventQueue timeline;
	private Market nyse, nasdaq;
	private MarketView nyseView, nasdaqView;
	private Agent mockAgent;

	@Before
	public void setup() {
		timeline = EventQueue.create(Log.nullLogger(), rand);
		nyse = CDAMarket.create(0, Mock.stats, timeline, Log.nullLogger(), rand, Mock.sip, Props.fromPairs());
		nyseView = nyse.getPrimaryView();		
		nasdaq = CDAMarket.create(1, Mock.stats, timeline, Log.nullLogger(), rand, Mock.sip, Props.fromPairs());
		nasdaqView = nasdaq.getPrimaryView();
		mockAgent = Mock.agent();
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
		submitOrder(nyseView, BUY, Price.of(5));
		submitOrder(nyseView, BUY, Price.of(7));
		submitOrder(nasdaqView, SELL, Price.of(1));
		timeline.executeUntil(TimeStamp.ZERO);

		assertEquals(0, la.getPosition());
		assertTrue(la.getProfit() > 0);
	}
	
	@Test
	public void laProfitTest() {
		LAAgent la = laAgent();
		submitOrder(nyseView, BUY, Price.of(5));
		submitOrder(nasdaqView, SELL, Price.of(1));
		timeline.executeUntil(TimeStamp.ZERO);
		// LA Strategy gets called implicitly 
		
		assertEquals(0, la.getPosition());
		assertEquals(4, la.getProfit(), 1e-6);
		assertTrue(la.getActiveOrders().isEmpty());
	}
	
	@Test
	public void multiQuantityLaProfitTest() {
		LAAgent la = laAgent();
		submitOrder(nyseView, BUY, Price.of(5), 3);
		submitOrder(nasdaqView, SELL, Price.of(1), 2);
		timeline.executeUntil(TimeStamp.ZERO);
		// LA Strategy gets called implicitly 
		
		assertEquals(0, la.getPosition());
		assertEquals(8, la.getProfit(), 1e-6);
		assertTrue(la.getActiveOrders().isEmpty());
	}
	
	@Test
	/**
	 * This test verifies that an LA with latency doesn't submit new orders until it's sure it's old orders are reflected in its quote.
	 */
	public void laLatencyNoRepeatOrdersTest() {
		LAAgent la = laAgent(Props.fromPairs(LaLatency.class, TimeStamp.of(10)));
		
		submitOrder(nyseView, BUY, Price.of(5));
		submitOrder(nasdaqView, SELL, Price.of(1));
		timeline.executeUntil(TimeStamp.of(5));
		submitOrder(nyseView, BUY, Price.of(3)); // Used to cause LA to submit extra orders
		timeline.executeUntil(TimeStamp.of(10)); // LA has submitted orders
		
		assertEquals(0, la.getPosition());
		assertEquals(0, la.getProfit(), 1e-6);
		assertEquals(2, la.getActiveOrders().size());
		
		timeline.executeUntil(TimeStamp.of(15)); // LA acts on second "arbitrage"
		
		assertEquals(0, la.getPosition());
		assertEquals(0, la.getProfit(), 1e-6);
		assertEquals(2, la.getActiveOrders().size());
		
		timeline.executeUntil(TimeStamp.of(20)); // Orders reach market
		assertSingleTransaction(nyseView.getTransactions(), Price.of(5), TimeStamp.of(20), 1);
		assertSingleTransaction(nasdaqView.getTransactions(), Price.of(1), TimeStamp.of(20), 1);
		
		timeline.executeUntil(TimeStamp.of(30)); // Takes this long for the LA to find out about it
		assertEquals(0, la.getPosition());
		assertEquals(4, la.getProfit(), 1e-6);
		assertTrue(la.getActiveOrders().isEmpty());
	}
	
	@Test
	public void severalArbitragesNoRepeatOrders() {
		LAAgent la = laAgent(Props.fromPairs(LaLatency.class, TimeStamp.of(10)));
		
		submitOrder(nyseView, BUY, Price.of(5));
		submitOrder(nasdaqView, SELL, Price.of(1));
		submitOrder(nyseView, BUY, Price.of(3));
		
		timeline.executeUntil(TimeStamp.of(10)); // LA has submitted orders
		
		assertEquals(0, la.getPosition());
		assertEquals(0, la.getProfit(), 1e-6);
		assertEquals(2, la.getActiveOrders().size());
		
		timeline.executeUntil(TimeStamp.of(20)); // Orders reach market
		assertSingleTransaction(nyseView.getTransactions(), Price.of(5), TimeStamp.of(20), 1);
		assertSingleTransaction(nasdaqView.getTransactions(), Price.of(1), TimeStamp.of(20), 1);
		
		timeline.executeUntil(TimeStamp.of(30)); // Takes this long for the LA to find out about it
		assertEquals(0, la.getPosition());
		assertEquals(4, la.getProfit(), 1e-6);
		assertTrue(la.getActiveOrders().isEmpty());
	}
	
	@Test
	public void severalArbitragesNoRepeatOrdersDifferentOrder() {
		LAAgent la = laAgent(Props.fromPairs(LaLatency.class, TimeStamp.of(10)));
		
		// Different Order
		submitOrder(nyseView, BUY, Price.of(3));
		submitOrder(nasdaqView, SELL, Price.of(1));
		submitOrder(nyseView, BUY, Price.of(5));
		
		timeline.executeUntil(TimeStamp.of(10)); // LA has submitted orders
		
		assertEquals(0, la.getPosition());
		assertEquals(0, la.getProfit(), 1e-6);
		assertEquals(2, la.getActiveOrders().size());
		
		timeline.executeUntil(TimeStamp.of(20)); // Orders reach market
		assertSingleTransaction(nyseView.getTransactions(), Price.of(5), TimeStamp.of(20), 1);
		assertSingleTransaction(nasdaqView.getTransactions(), Price.of(1), TimeStamp.of(20), 1);
		
		timeline.executeUntil(TimeStamp.of(30)); // Takes this long for the LA to find out about it
		assertEquals(0, la.getPosition());
		assertEquals(4, la.getProfit(), 1e-6);
		assertTrue(la.getActiveOrders().isEmpty());
	}
	
	@Test
	/**
	 * This test makes sure the fix for the previous test doesn't effect infinitely fast LAs.
	 */
	public void laNoLatencySeveralOrders() {
		LAAgent la = laAgent();
		
		submitOrder(nyseView, BUY, Price.of(5));
		submitOrder(nasdaqView, SELL, Price.of(1));
		timeline.executeUntil(TimeStamp.ZERO);
		// LA Strategy gets called implicitly
		
		assertEquals(0, la.getPosition());
		assertEquals(4, la.getProfit(), 1e-6);
		assertTrue(la.getActiveOrders().isEmpty());
		
		submitOrder(nyseView, BUY, Price.of(10));
		submitOrder(nasdaqView, SELL, Price.of(6));
		timeline.executeUntil(TimeStamp.ZERO);
		// LA Strategy gets called implicitly 

		assertEquals(0, la.getPosition());
		assertEquals(8, la.getProfit(), 1e-6);
		assertTrue(la.getActiveOrders().isEmpty());
	}
	
	@Test
	public void randomTests() {
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
		return mockAgent.submitOrder(view, buyOrSell, price, quantity);
	}
	
	private LAAgent laAgent(Props parameters) {
		return LAAgent.create(0, Mock.stats, timeline, Log.nullLogger(), rand, Mock.sip, Mock.fundamental,
				ImmutableList.of(nyse, nasdaq), parameters);
	}
	
	private LAAgent laAgent() {
		return laAgent(Props.fromPairs());
	}

}
