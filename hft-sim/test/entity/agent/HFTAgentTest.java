package entity.agent;

import static fourheap.Order.OrderType.BUY;
import static fourheap.Order.OrderType.SELL;
import static org.junit.Assert.*;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import logger.Log;

import org.junit.Before;
import org.junit.Test;

import utils.Mock;
import utils.Rand;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;

import data.Props;
import data.Stats;
import entity.market.CDAMarket;
import entity.market.Market;
import entity.market.Market.MarketView;
import entity.market.Price;
import entity.market.Quote;
import event.EventQueue;
import event.TimeStamp;
import fourheap.Order.OrderType;

public class HFTAgentTest {

	private static final Rand rand = Rand.create();
	
	private EventQueue timeline;
	private Market market;
	private MarketView view;
	private Agent mockAgent;

	@Before
	public void setup() {
		timeline = EventQueue.create(Log.nullLogger(), rand);
		market = CDAMarket.create(0, Mock.stats, timeline, Log.nullLogger(), rand, Mock.sip, Props.fromPairs());
		view = market.getPrimaryView();
		mockAgent = Mock.agent();
	}
	
	/** Test if quote is updated in quote view by the time hft strategy is called */
	@Test
	public void testQuoteBuyNoLatencyUpdate() {
		final AtomicReference<Quote> quote = new AtomicReference<Quote>();
		new HFTAgent(0, Stats.create(), timeline, Log.nullLogger(), rand, Mock.sip, Mock.fundamental, TimeStamp.ZERO,
				ImmutableMap.of(market, TimeStamp.ZERO), Props.fromPairs()) {
			private static final long serialVersionUID = 1L;
			@Override protected void agentStrategy() {
				quote.set(Iterables.getOnlyElement(markets).getQuote());
			}
		};
		
		submitOrder(BUY, Price.of(237), 4);
		timeline.executeUntil(TimeStamp.ZERO);
		
		assertEquals(view.getQuote(), quote.get());
	}
	
	/** Test if quote is updated in quote view by the time hft strategy is called */
	@Test
	public void testQuoteSellNoLatencyUpdate() {
		final AtomicReference<Quote> quote = new AtomicReference<Quote>();
		new HFTAgent(0, Stats.create(), timeline, Log.nullLogger(), rand, Mock.sip, Mock.fundamental, TimeStamp.ZERO,
				ImmutableMap.of(market, TimeStamp.ZERO), Props.fromPairs()) {
			private static final long serialVersionUID = 1L;
			@Override protected void agentStrategy() {
				quote.set(Iterables.getOnlyElement(markets).getQuote());
			}
		};
		
		submitOrder(SELL, Price.of(237), 4);
		timeline.executeUntil(TimeStamp.ZERO);
		
		assertEquals(view.getQuote(), quote.get());
	}
	
	/** Test if quote is updated in quote view by the time hft strategy is called */
	@Test
	public void testQuoteBuyLatencyUpdate() {
		final AtomicReference<Quote> quote = new AtomicReference<Quote>();
		new HFTAgent(0, Stats.create(), timeline, Log.nullLogger(), rand, Mock.sip, Mock.fundamental, TimeStamp.ZERO,
				ImmutableMap.of(market, TimeStamp.of(5)), Props.fromPairs()) {
			private static final long serialVersionUID = 1L;
			@Override protected void agentStrategy() {
				quote.set(Iterables.getOnlyElement(markets).getQuote());
			}
		};
		
		submitOrder(BUY, Price.of(237), 4);
		timeline.executeUntil(TimeStamp.of(5));
		
		assertEquals(view.getQuote(), quote.get());
	}
	
	/** Test if quote is updated in quote view by the time hft strategy is called */
	@Test
	public void testQuoteSellLatencyUpdate() {
		final AtomicReference<Quote> quote = new AtomicReference<Quote>();
		new HFTAgent(0, Stats.create(), timeline, Log.nullLogger(), rand, Mock.sip, Mock.fundamental, TimeStamp.ZERO,
				ImmutableMap.of(market, TimeStamp.of(5)), Props.fromPairs()) {
			private static final long serialVersionUID = 1L;
			@Override protected void agentStrategy() {
				quote.set(Iterables.getOnlyElement(markets).getQuote());
			}
		};
		
		submitOrder(SELL, Price.of(237), 4);
		timeline.executeUntil(TimeStamp.of(5));
		
		assertEquals(view.getQuote(), quote.get());
	}
	
	/** Test that hft gets notified about it's order being submitted before it acts based on the quote update caused by its order */
	@Test
	public void testSubmitOrderBuyNoLatencyUpdate() {
		// Indicaters of what happened
		final AtomicBoolean orderSubmitted = new AtomicBoolean(false);
		final AtomicBoolean strategyExecuted = new AtomicBoolean(false);
		
		// Agent that logs its actions
		Agent hft = new HFTAgent(0, Stats.create(), timeline, Log.nullLogger(), rand, Mock.sip, Mock.fundamental, TimeStamp.of(1000),
				ImmutableMap.of(market, TimeStamp.ZERO), Props.fromPairs()) {
			private static final long serialVersionUID = 1L;
			@Override protected void agentStrategy() {
				strategyExecuted.set(true);
			}
			@Override
			protected void orderSubmitted(OrderRecord order, MarketView market,
					TimeStamp submittedTime) {
				super.orderSubmitted(order, market, submittedTime);
				assertFalse(strategyExecuted.get()); // Fail if strategy already executed
				orderSubmitted.set(true);
			}
		};
		
		hft.submitOrder(view, BUY, Price.of(123), 4);
		timeline.executeUntil(TimeStamp.ZERO);
		
		// Assert that both steps still happened
		assertTrue(orderSubmitted.get());
		assertTrue(strategyExecuted.get());
	}
	
	/** Test that hft gets notified about it's order being submitted before it acts based on the quote update caused by its order */
	@Test
	public void testSubmitOrderSellLatencyUpdate() {
		// Indicaters of what happened
		final AtomicBoolean orderSubmitted = new AtomicBoolean(false);
		final AtomicBoolean strategyExecuted = new AtomicBoolean(false);
		
		// Agent that logs its actions
		Agent hft = new HFTAgent(0, Stats.create(), timeline, Log.nullLogger(), rand, Mock.sip, Mock.fundamental, TimeStamp.of(1000),
				ImmutableMap.of(market, TimeStamp.of(5)), Props.fromPairs()) {
			private static final long serialVersionUID = 1L;
			@Override protected void agentStrategy() {
				strategyExecuted.set(true);
			}
			@Override
			protected void orderSubmitted(OrderRecord order, MarketView market,
					TimeStamp submittedTime) {
				super.orderSubmitted(order, market, submittedTime);
				assertFalse(strategyExecuted.get()); // Fail if strategy already executed
				orderSubmitted.set(true);
			}
		};
		
		hft.submitOrder(view, SELL, Price.of(654), 3);
		timeline.executeUntil(TimeStamp.of(10));
		
		// Assert that both steps still happened
		assertTrue(orderSubmitted.get());
		assertTrue(strategyExecuted.get());
	}
	
	private OrderRecord submitOrder(OrderType buyOrSell, Price price, int quantity) {
		return mockAgent.submitOrder(view, buyOrSell, price, quantity);
	}

}
