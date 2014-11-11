package entity.market;

import static fourheap.Order.OrderType.BUY;
import static fourheap.Order.OrderType.SELL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static utils.Tests.assertQuote;
import static utils.Tests.assertSingleTransaction;
import static utils.Tests.assertTransaction;
import logger.Log;

import org.junit.Before;
import org.junit.Test;

import systemmanager.Keys.MarketLatency;
import utils.Mock;
import utils.Rand;

import com.google.common.collect.Iterables;

import data.Props;
import data.Stats;
import data.TimeSeries;
import entity.agent.Agent;
import entity.agent.Agent.AgentView;
import entity.agent.OrderRecord;
import entity.market.Market.MarketView;
import entity.market.clearingrule.UniformPriceClear;
import entity.sip.MarketInfo;
import entity.sip.SIP;
import event.EventQueue;
import event.TimeStamp;
import event.Timeline;
import fourheap.Order.OrderType;

public class MarketTest {
	private static final double eps = 1e-6;
	private static final Rand rand = Rand.create();
	private static final Agent agent = Mock.agent();
	
	private Timeline timeline;
	private Market market;
	private MarketView view, fast;
	
	@Before
	public void setup() {
		timeline = Mock.timeline;
		market = mockMarket(Mock.stats, timeline, Mock.sip, Props.fromPairs());
		view = market.getPrimaryView();
		fast = market.getView(TimeStamp.ZERO);
	}

	@Test
	public void addBid() {
		submitOrder(BUY, Price.of(1), 1);
		assertQuote(view.getQuote(), Price.of(1), 1, null, 0);
	}

	@Test
	public void addAsk() {
		submitOrder(SELL, Price.of(1), 1);
		assertQuote(view.getQuote(), null, 0, Price.of(1), 1);
	}

	@Test
	public void basicEqualClear() {
		OrderRecord buy = submitOrder(BUY, Price.of(100), 1);
		OrderRecord sell = submitOrder(SELL, Price.of(100), 1);

		market.clear();
		
		assertSingleTransaction(view.getTransactions(), Price.of(100), TimeStamp.ZERO, 1);
		assertQuote(view.getQuote(), null, 0, null, 0);
		assertEquals(0, buy.getQuantity());
		assertEquals(0, sell.getQuantity());
	}

	@Test
	public void basicOverlapClear() {
		OrderRecord buy = submitOrder(BUY, Price.of(200), 1);
		OrderRecord sell = submitOrder(SELL, Price.of(50), 1);
		market.clear();
		
		assertSingleTransaction(view.getTransactions(), Price.of(125), TimeStamp.ZERO, 1);
		assertQuote(view.getQuote(), null, 0, null, 0);
		assertEquals(0, buy.getQuantity());
		assertEquals(0, sell.getQuantity());
	}

	/** Several bids with one clear have proper transaction */
	@Test
	public void multiBidSingleClear() {
		OrderRecord buyTrans = submitOrder(BUY, Price.of(150), 1);
		OrderRecord buy = submitOrder(BUY, Price.of(100), 1);
		OrderRecord sell = submitOrder(SELL, Price.of(180), 1);
		OrderRecord sellTrans = submitOrder(SELL, Price.of(120), 1);
		market.clear();
		
		// Testing the market for the correct transactions
		assertSingleTransaction(view.getTransactions(), Price.of(135), TimeStamp.ZERO, 1);
		assertQuote(view.getQuote(), Price.of(100), 1, Price.of(180), 1);
		assertEquals(0, buyTrans.getQuantity());
		assertEquals(0, sellTrans.getQuantity());
		assertEquals(1, buy.getQuantity());
		assertEquals(1, sell.getQuantity());
	}

	/** Two sets of bids overlap before clear */
	@Test
	public void multiOverlapClear() {
		OrderRecord buy1 = submitOrder(BUY, Price.of(150), 1);
		OrderRecord sell1 = submitOrder(SELL, Price.of(100), 1);
		OrderRecord buy2 = submitOrder(BUY, Price.of(200), 1);
		OrderRecord sell2 = submitOrder(SELL, Price.of(130), 1);
		market.clear();
		
		assertEquals(2, view.getTransactions().size());
		for (Transaction transaction : view.getTransactions())
			assertTransaction(transaction, Price.of(140), TimeStamp.ZERO, 1);
		assertEquals(0, buy1.getQuantity());
		assertEquals(0, sell1.getQuantity());
		assertEquals(0, buy2.getQuantity());
		assertEquals(0, sell2.getQuantity());

	}

	/** Scenario with two possible matches, but only one pair transacts at the match. */
	@Test
	public void partialOverlapClear() {
		OrderRecord buyTrans = submitOrder(BUY, Price.of(200), 1);
		OrderRecord buy = submitOrder(SELL, Price.of(130), 1);
		OrderRecord sell = submitOrder(BUY, Price.of(110), 1);
		OrderRecord sellTrans = submitOrder(SELL, Price.of(100), 1);

		market.clear();

		assertSingleTransaction(view.getTransactions(), Price.of(150), TimeStamp.ZERO, 1);
		assertQuote(view.getQuote(), Price.of(110), 1, Price.of(130), 1);
		assertEquals(0, buyTrans.getQuantity());
		assertEquals(0, sellTrans.getQuantity());
		assertEquals(1, buy.getQuantity());
		assertEquals(1, sell.getQuantity());
	}

	/**
	 * Test clearing when there are ties in price. Should match at uniform price.
	 * Also checks tie-breaking by time.
	 */
	@Test
	public void priceTimeTest() {
		OrderRecord order1 = submitOrder(SELL, Price.of(100), 1);
		OrderRecord order2 = submitOrder(SELL, Price.of(100), 1);
		OrderRecord order3 = submitOrder(BUY, Price.of(150), 1);
		market.clear();
		
		// Check that earlier order (order1) is trading with order3
		// Testing the market for the correct transactions
		assertSingleTransaction(view.getTransactions(), Price.of(125), TimeStamp.ZERO, 1);
		assertEquals(0, order1.getQuantity());
		assertEquals(0, order3.getQuantity());

		// Existing order2 S1@100
		submitOrder(SELL, Price.of(100), 1);
		submitOrder(SELL, Price.of(100), 1);
		submitOrder(SELL, Price.of(100), 1);
		market.clear();
		OrderRecord order5 = submitOrder(BUY, Price.of(130), 1);
		market.clear();
		
		// Check that the first submitted order2 S1@100 transacts
		assertEquals(2, view.getTransactions().size());
		assertTransaction(view.getTransactions().get(0), Price.of(115), TimeStamp.ZERO, 1);
		assertEquals(0, order2.getQuantity());
		assertEquals(0, order5.getQuantity());
		
		// Let's try populating the market with random orders 
		// 3 S1@100 remain
		order5 = submitOrder(SELL, Price.of(90), 1);
		submitOrder(SELL, Price.of(100), 1);
		submitOrder(SELL, Price.of(110), 1);
		submitOrder(SELL, Price.of(120), 1);
		submitOrder(BUY, Price.of(80), 1);
		submitOrder(BUY, Price.of(70), 1);
		submitOrder(BUY, Price.of(60), 1);
		market.clear();
		assertEquals(2, view.getTransactions().size()); // no change

		// Check basic overlap - between order5 (@90) and next order (order2)
		order2 = submitOrder(BUY, Price.of(130), 1);
		market.clear();
		
		assertEquals(3, view.getTransactions().size());
		assertTransaction(view.getTransactions().get(0), Price.of(110), TimeStamp.ZERO, 1);
		assertEquals(0, order2.getQuantity());
		assertEquals(0, order5.getQuantity());
		
		// Check additional overlapping orders
		submitOrder(BUY, Price.of(110), 4);
		market.clear();
		
		assertEquals(7, view.getTransactions().size());
		for (Transaction trans : view.getTransactions().subList(0, 4))
			assertTransaction(trans, Price.of(105), TimeStamp.ZERO, 1);
	}

	@Test
	public void partialQuantity() {
		OrderRecord sell = submitOrder(SELL, Price.of(100), 2);
		OrderRecord buy = submitOrder(BUY, Price.of(150), 5);
		market.clear();

		// Check that two units transact and that post-trade BID is correct (3 buy units at 150)
		assertSingleTransaction(view.getTransactions(), Price.of(125), TimeStamp.ZERO, 2);
		assertQuote(view.getQuote(),Price.of(150), 3, null, 0);
		assertEquals(0, sell.getQuantity());
		assertEquals(3, buy.getQuantity());
	}

	@Test
	public void multiQuantity() {
		OrderRecord sell1 = submitOrder(SELL, Price.of(150), 1);
		OrderRecord sell2 = submitOrder(SELL, Price.of(140), 1);
		market.clear();
		assertTrue(view.getTransactions().isEmpty());

		// Both sell orders should transact
		OrderRecord buy = submitOrder(BUY, Price.of(160), 2);
		market.clear();
		
		assertEquals(2, view.getTransactions().size());
		for (Transaction tr : view.getTransactions())
			assertEquals("Incorrect Quantity", 1, tr.getQuantity());
		assertQuote(view.getQuote(), null, 0, null, 0);
		assertEquals(0, sell1.getQuantity());
		assertEquals(0, sell2.getQuantity());
		assertEquals(0, buy.getQuantity());
	}

	@Test
	public void basicWithdraw() {
		OrderRecord order = submitOrder(SELL, Price.of(100), 1);
		market.clear();
		
		// Check that quotes are correct (no bid, ask @100)
		assertQuote(view.getQuote(), null, 0, Price.of(100), 1);

		// Withdraw order
		withdrawOrder(order);

		// Check that quotes are correct (no bid, no ask)
		assertQuote(view.getQuote(), null, 0, null, 0);

		// Check that no transaction, because order withdrawn
		order = submitOrder(BUY, Price.of(125), 1);
		assertTrue(view.getTransactions().isEmpty());
		submitOrder(BUY, Price.of(115), 1);

		withdrawOrder(order);

		// Check that it transacts with order (@115) that was not withdrawn
		submitOrder(SELL, Price.of(105), 1);
		market.clear();
		assertSingleTransaction(view.getTransactions(), Price.of(110), TimeStamp.ZERO, 1);
		assertQuote(view.getQuote(), null, 0, null, 0);
	}

	@Test
	public void multiQuantityWithdraw() {
		submitOrder(SELL, Price.of(150), 1);
		OrderRecord order = submitOrder(SELL, Price.of(140), 2);
		market.clear();
		
		market.withdrawOrder(order, 1);

		// Both agents' sell orders should transact b/c partial quantity withdrawn
		submitOrder(BUY, Price.of(160), 1);
		submitOrder(BUY, Price.of(160), 2);
		market.clear();
		assertEquals(2, view.getTransactions().size());
		for (Transaction tr : view.getTransactions())
			assertEquals("Incorrect Quantity", 1, tr.getQuantity());

		assertQuote(view.getQuote(), Price.of(160), 1, null, 0);
	}

	/** Information propagates at proper times */
	@Test
	public void latencyTest() {
		EventQueue timeline = latencySetup(TimeStamp.of(100));
		
		submitOrder(SELL, Price.of(100), 1);
		
		// In market
		timeline.executeUntil(TimeStamp.of(99));
		assertQuote(fast.getQuote(), null, 0, Price.of(100), 1);
		assertQuote(view.getQuote(), null, 0, null, 0);

		// Reached primary view
		timeline.executeUntil(TimeStamp.of(100));
		assertQuote(fast.getQuote(), null, 0, Price.of(100), 1);
	}

	/** Verify that earlier orders transact */
	@Test
	public void timePriorityBuy() {
		OrderRecord first = submitOrder(BUY, Price.of(100), 1);
		OrderRecord second = submitOrder(BUY, Price.of(100), 1);
		submitOrder(SELL, Price.of(100), 1);
		market.clear();
		
		assertEquals(0, first.getQuantity());
		assertEquals(1, second.getQuantity());
	}
	
	/** Verify that earlier orders transact */
	@Test
	public void timePrioritySell() {
		OrderRecord first = submitOrder(SELL, Price.of(100), 1);
		OrderRecord second = submitOrder(SELL, Price.of(100), 1);
		submitOrder(BUY, Price.of(100), 1);
		market.clear();
		
		assertEquals(0, first.getQuantity());
		assertEquals(1, second.getQuantity());
	}

	@Test
	public void updateQuoteLatency() {
		EventQueue timeline = latencySetup(TimeStamp.of(100));
		
		submitOrder(SELL, Price.of(100), 1);
		
		timeline.executeUntil(TimeStamp.of(99));
		assertQuote(view.getQuote(), null, 0, null, 0);

		// Update QP
		timeline.executeUntil(TimeStamp.of(100));
		assertQuote(view.getQuote(), null, 0, Price.of(100), 1);
		
		// Add new quote
		submitOrder(BUY, Price.of(80), 1);
		
		// Update QP
		timeline.executeUntil(TimeStamp.of(200));
		assertQuote(view.getQuote(), Price.of(80), 1, Price.of(100), 1);
	}

	@Test
	public void updateTransactionsLatency() {
		EventQueue timeline = latencySetup(TimeStamp.of(100));
		
		OrderRecord buy = submitOrder(BUY, Price.of(150), 1);
		OrderRecord sell = submitOrder(SELL, Price.of(140), 1);
		market.clear();

		// Verify that transactions have not updated yet (for primary view)
		timeline.executeUntil(TimeStamp.of(99));
		assertTrue(view.getTransactions().isEmpty());
		assertSingleTransaction(fast.getTransactions(), Price.of(145), TimeStamp.ZERO, 1);
		assertEquals(1, buy.getQuantity());
		assertEquals(1, sell.getQuantity());
		
		// Test that after 100 new transaction did get updated
		timeline.executeUntil(TimeStamp.of(100));
		assertSingleTransaction(view.getTransactions(), Price.of(145), TimeStamp.ZERO, 1);
	}
	
	@Test
	public void basicTransactionTest() {
		assertTrue("Incorrect initial transaction list", view.getTransactions().isEmpty());

		addTransaction(Price.of(150), 2);
		assertSingleTransaction(view.getTransactions(), Price.of(150), TimeStamp.ZERO, 2);
		
		addTransaction(Price.of(170), 1);
		assertTransaction(Iterables.getFirst(view.getTransactions(), null), Price.of(170), TimeStamp.ZERO, 1);
	}

	@Test
	public void basicDelayProcessTransaction() {
		EventQueue timeline = latencySetup(TimeStamp.of(100));

		assertTrue("Incorrect initial transaction list", view.getTransactions().isEmpty());

		// Transaction in market, but not seen via slow view
		addTransaction(Price.of(150), 2);
		
		timeline.executeUntil(TimeStamp.of(99));
		assertTrue("Primary view updated too early", view.getTransactions().isEmpty());
		assertSingleTransaction(fast.getTransactions(), Price.of(150), TimeStamp.ZERO, 2);

		timeline.executeUntil(TimeStamp.of(100));
		assertSingleTransaction(view.getTransactions(), Price.of(150), TimeStamp.ZERO, 2);
	}

	/** Test handling of stale quotes when better order happened later */
	@Test
	public void staleQuotesFirst() {
		EventQueue timeline = latencySetup(TimeStamp.of(100));

		submitOrder(BUY, Price.of(50), 1);
		submitOrder(BUY, Price.of(60), 1);
		
		timeline.executeUntil(TimeStamp.of(100));
		assertQuote(view.getQuote(), Price.of(60), 1, null, 0);
		
		submitOrder(SELL, Price.of(110), 1);
		submitOrder(SELL, Price.of(100), 1);
		
		timeline.executeUntil(TimeStamp.of(200));
		assertQuote(view.getQuote(), Price.of(60), 1, Price.of(100), 1);
	}
	
	/** Test handling of stale quotes when better order happened earlier */
	@Test
	public void staleQuotesLast() {
		EventQueue timeline = latencySetup(TimeStamp.of(100));
		
		submitOrder(BUY, Price.of(60), 1);
		submitOrder(BUY, Price.of(50), 1);
		
		timeline.executeUntil(TimeStamp.of(100));
		assertQuote(view.getQuote(), Price.of(60), 1, null, 0);
		
		submitOrder(SELL, Price.of(100), 1);
		submitOrder(SELL, Price.of(110), 1);
		
		timeline.executeUntil(TimeStamp.of(200));
		assertQuote(view.getQuote(), Price.of(60), 1, Price.of(100), 1);
	}
	
	/** Test that routes if transacts in other market */
	@Test
	public void nbboRoutingBuy() {
		nbboSetup(Price.of(80), Price.of(100));
		submitNMSOrder(BUY, Price.of(100), 1);
		
		// Verify it got routed due to empty quote
		assertQuote(view.getQuote(), null, 0, null, 0);
	}
	
	/** Test that routes when other is better even if it would transact locally */
	@Test
	public void nbboBetterRoutingBuy() {
		nbboSetup(Price.of(80), Price.of(100));
		submitOrder(SELL, Price.of(120), 1);
		
		submitNMSOrder(BUY, Price.of(120), 1);
		
		// Verify it got routed due to better price
		assertQuote(view.getQuote(), null, 0, Price.of(120), 1);
	}
	
	/** Test that doesn't route when it won't transact in other market */
	@Test
	public void nbboNoRoutingBuy() {
		nbboSetup(Price.of(80), Price.of(100));
		
		submitNMSOrder(BUY, Price.of(90), 1);
		
		// Verify it did route because it wouldn't transact
		assertQuote(view.getQuote(), Price.of(90), 1, null, 0);
	}
	
	/** Test that doesn't route when the price locally is better */
	@Test
	public void nbboNoRoutingBetterBuy() {
		nbboSetup(Price.of(80), Price.of(100));
		submitOrder(SELL, Price.of(90), 1);
		
		submitNMSOrder(BUY, Price.of(100), 1);
		market.clear(); // Orders would transact but market doesn't automatically clear
		
		// Verify it didn't route because price is better locally
		assertQuote(view.getQuote(), null, 0, null, 0);
	}
	
	@Test
	public void nbboRoutingSell() {
		nbboSetup(Price.of(80), Price.of(100));
		submitNMSOrder(SELL, Price.of(80), 1);
		
		// Verify it got routed due to empty quote
		assertQuote(view.getQuote(), null, 0, null, 0);
	}
	
	/** Test that routes when other is better even if it would transact locally */
	@Test
	public void nbboBetterRoutingSell() {
		nbboSetup(Price.of(80), Price.of(100));
		submitOrder(BUY, Price.of(60), 1);
		
		submitNMSOrder(SELL, Price.of(80), 1);
		
		// Verify it got routed due to better price
		assertQuote(view.getQuote(), Price.of(60), 1, null, 0);
	}
	
	/** Test that doesn't route when it won't transact in other market */
	@Test
	public void nbboNoRoutingSell() {
		nbboSetup(Price.of(80), Price.of(100));
		
		submitNMSOrder(SELL, Price.of(90), 1);
		
		// Verify it did route because it wouldn't transact
		assertQuote(view.getQuote(), null, 0, Price.of(90), 1);
	}
	
	/** Test that doesn't route when the price locally is better */
	@Test
	public void nbboNoRoutingBetterSell() {
		nbboSetup(Price.of(80), Price.of(100));
		submitOrder(BUY, Price.of(90), 1);
		
		submitNMSOrder(SELL, Price.of(80), 1);
		market.clear(); // Orders would transact but market doesn't automatically clear
		
		// Verify it didn't route because price is better locally
		assertQuote(view.getQuote(), null, 0, null, 0);
	}
	
	@Test
	public void spreadsPostTest() {
		EventQueue timeline = EventQueue.create(Log.nullLogger(), rand);
		Stats stats = Stats.create();
		market = mockMarket(stats, timeline, Mock.sip, Props.fromPairs());
		view = market.getPrimaryView();
		TimeSeries expected = TimeSeries.create();
		expected.add(0, Double.POSITIVE_INFINITY);
		
		timeline.executeUntil(TimeStamp.of(2));
		submitOrder(SELL, Price.of(100), 1);
		submitOrder(BUY, Price.of(50), 1);
		expected.add(2, 50);
		
		timeline.executeUntil(TimeStamp.of(100));
		submitOrder(SELL, Price.of(80), 1);
		submitOrder(BUY, Price.of(60), 1);
		expected.add(100, 20);
		
		assertEquals(expected, stats.getTimeStats().get(Stats.SPREAD + market));
	}
	
	@Test
	public void midpointPostTest() {
		EventQueue timeline = EventQueue.create(Log.nullLogger(), rand);
		Stats stats = Stats.create();
		market = mockMarket(stats, timeline, Mock.sip, Props.fromPairs());
		view = market.getPrimaryView();
		TimeSeries expected = TimeSeries.create();
		expected.add(0, Double.NaN);
		
		timeline.executeUntil(TimeStamp.of(2));
		submitOrder(SELL, Price.of(100), 1);
		submitOrder(BUY, Price.of(50), 1);
		expected.add(2, 75);
		
		timeline.executeUntil(TimeStamp.of(100));
		submitOrder(SELL, Price.of(80), 1);
		submitOrder(BUY, Price.of(60), 1);
		expected.add(100, 70);
		
		assertEquals(expected, stats.getTimeStats().get(Stats.MIDQUOTE + market));
	}
	
	@Test
	public void transactionPricePostTest() {
		EventQueue timeline = EventQueue.create(Log.nullLogger(), rand);
		Stats stats = Stats.create();
		market = mockMarket(stats, timeline, Mock.sip, Props.fromPairs());
		view = market.getPrimaryView();
		Market other = mockMarket(stats, timeline, Mock.sip, Props.fromPairs());
		
		addTransaction(Price.of(100), 1);
		
		timeline.executeUntil(TimeStamp.of(50));
		addTransaction(other, Price.of(50), 1);
				
		timeline.executeUntil(TimeStamp.of(100));
		addTransaction(Price.of(200), 2);
		addTransaction(Price.of(150), 1);
		
		TimeSeries truth = TimeSeries.create();
		truth.add(0, 100);
		truth.add(50, 50);
		truth.add(100, 150);
		
		assertEquals(truth, stats.getTimeStats().get(Stats.TRANSACTION_PRICE));
		
		// XXX Doesn't account for order quantity
		assertEquals(125, stats.getSummaryStats().get(Stats.PRICE).mean(), eps);
		assertEquals(4, stats.getSummaryStats().get(Stats.PRICE).n());
	}
	
	@Test
	public void randomTest() {
		for(int i=0; i < 100; i++) {
			setup();
			multiBidSingleClear();
			setup();
			multiOverlapClear();
			setup();
			partialOverlapClear();
			setup();
			staleQuotesFirst();
			setup();
			staleQuotesLast();
		}
	}
	
	private Market mockMarket(Stats stats, Timeline timeline, MarketInfo sip, Props props) {
		return new Market(rand.nextInt(), stats, timeline, Log.nullLogger(), rand, sip, new UniformPriceClear(0.5, 1), props) {
			private static final long serialVersionUID = 1L;
			@Override protected void submitOrder(MarketView thisView, AgentView agent, OrderRecord orderRecord) {
				super.submitOrder(thisView, agent, orderRecord);
				updateQuote();
			}
			@Override protected void withdrawOrder(OrderRecord orderRecord, int quantity) {
				super.withdrawOrder(orderRecord, quantity);
				updateQuote();
			}
		};
	}
	
	private EventQueue latencySetup(TimeStamp latency) {
		EventQueue timeline = EventQueue.create(Log.nullLogger(), rand);
		this.timeline = timeline;
		market = mockMarket(Mock.stats, timeline, Mock.sip, Props.fromPairs(MarketLatency.class, latency));
		view = market.getPrimaryView();
		fast = market.getView(TimeStamp.ZERO);
		return timeline;
	}
	
	private MarketInfo nbboSetup(Price bid, Price ask) {
		SIP sip = SIP.create(Mock.stats, Mock.timeline, Log.nullLogger(), rand, TimeStamp.ZERO);
		market = mockMarket(Mock.stats, Mock.timeline, sip, Props.fromPairs());
		MarketView other = mockMarket(Mock.stats, Mock.timeline, sip, Props.fromPairs()).getPrimaryView();
		view = market.getPrimaryView();
		fast = market.getView(TimeStamp.ZERO);
		other.submitOrder(agent, OrderRecord.create(other, TimeStamp.ZERO, BUY, bid, 1));
		other.submitOrder(agent, OrderRecord.create(other, TimeStamp.ZERO, SELL, ask, 1));
		return sip;
	}
	
	private void withdrawOrder(OrderRecord order) {
		market.withdrawOrder(order, order.getQuantity());
	}
	
	private void addTransaction(Price price, int quantity) {
		addTransaction(market, price, quantity);
	}
	
	private void addTransaction(Market market, Price price, int quantity) {
		submitOrder(market, BUY, price, quantity);
		submitOrder(market, SELL, price, quantity);
		market.clear();
	}
	
	private OrderRecord submitOrder(OrderType buyOrSell, Price price, int quantity) {
		return submitOrder(market, buyOrSell, price, quantity);
	}
	
	private OrderRecord submitOrder(Market market, OrderType buyOrSell, Price price, int quantity) {
		OrderRecord order = OrderRecord.create(market.getPrimaryView(), timeline.getCurrentTime(), buyOrSell, price, quantity);
		market.submitOrder(market.getPrimaryView(), agent.getView(market.getPrimaryView().getLatency()), order);
		return order;
	}
	
	private OrderRecord submitNMSOrder(OrderType buyOrSell, Price price, int quantity) {
		OrderRecord order = OrderRecord.create(market.getPrimaryView(), TimeStamp.ZERO, buyOrSell, price, quantity);
		market.submitNMSOrder(market.getPrimaryView(), agent, agent.getView(market.getPrimaryView().getLatency()), order);
		return order;
	}
	
}
