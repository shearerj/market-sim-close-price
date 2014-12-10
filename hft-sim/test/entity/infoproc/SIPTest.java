package entity.infoproc;

import static fourheap.Order.OrderType.BUY;
import static fourheap.Order.OrderType.SELL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static utils.Tests.assertNBBO;
import static utils.Tests.assertSingleTransaction;
import logger.Log;

import org.junit.Before;
import org.junit.Test;

import utils.Mock;
import utils.Rand;
import data.Props;
import data.Stats;
import data.TimeSeries;
import entity.agent.Agent;
import entity.agent.OrderRecord;
import entity.market.CDAMarket;
import entity.market.Market;
import entity.market.Market.MarketView;
import entity.market.Price;
import entity.sip.SIP;
import event.EventQueue;
import event.TimeStamp;
import event.Timeline;
import fourheap.Order.OrderType;

public class SIPTest {
	
	/*
	 * XXX Undefined order between markets? Transactions for a market should be
	 * in proper order, but if several markets had a transaction at the same
	 * time, the order will be undefined. Transactions are no longer available from SIP
	 */
	private static final Rand rand = Rand.create();
	private static final Agent agent = Mock.agent();
	
	private Timeline timeline;
	private SIP sip;
	private Market nyse;
	private Market nasdaq;
	
	@Before
	public void setup() {
		timeline = Mock.timeline;
		sip = SIP.create(Mock.stats, timeline, Log.nullLogger(), rand, TimeStamp.ZERO);
		nyse = CDAMarket.create(1, Mock.stats, timeline, Log.nullLogger(), rand, sip, Props.fromPairs());
		nasdaq = CDAMarket.create(2, Mock.stats, timeline, Log.nullLogger(), rand, sip, Props.fromPairs());
	}
	
	@Test
	public void basicQuote() {
		// Test initial NBBO quote
		assertNBBO(sip.getNBBO(), null, null, 0, null, null, 0);
		
		setQuote(nyse, Price.of(80), Price.of(100));
		
		// Test that NBBO quote is correct
		assertNBBO(sip.getNBBO(), Price.of(80), nyse, 1, Price.of(100), nyse, 1);
	}
	
	@Test
	public void multiQuote() {
		setQuote(nyse, Price.of(80), Price.of(100));
		setQuote(nyse, Price.of(70), Price.of(90));
		
		// Test that NBBO quote is correct (completely replaces old quote of [80, 100])
		assertNBBO(sip.getNBBO(), Price.of(70), nyse, 1, Price.of(90), nyse, 1);
	}
	
	@Test
	public void staleQuote() {
		EventQueue timeline = latencySetup(TimeStamp.of(1));
		setQuote(nyse, Price.of(80), Price.of(100));
		
		// Test that NBBO quote is correct
		timeline.executeUntil(TimeStamp.of(1));
		assertNBBO(sip.getNBBO(), Price.of(80), nyse, 1, Price.of(100), nyse, 1);
		
		setQuote(nyse, Price.of(70), Price.of(90));
		timeline.executeUntil(TimeStamp.of(1));
		
		// Test that NBBO quote is correct (ignores stale quote q2)
		assertNBBO(sip.getNBBO(), Price.of(80), nyse, 1, Price.of(100), nyse, 1);
	}
	
	@Test
	public void twoMarketQuote() {
		setQuote(nyse, Price.of(80), Price.of(100));
		setQuote(nasdaq, Price.of(70), Price.of(90));
		
		// Test that NBBO quote is correct (computes best quote between both markets)
		assertNBBO(sip.getNBBO(), Price.of(80), nyse, 1, Price.of(90), nasdaq, 1);
	}
	
	@Test
	public void twoMarketMultiQuote() {
		setQuote(nyse, Price.of(85), Price.of(100));
		setQuote(nasdaq, Price.of(75), Price.of(95));
		setQuote(nyse, Price.of(65), Price.of(90));
				
		// Test that NBBO quote is correct & that market 1's quote was replaced
		assertNBBO(sip.getNBBO(), Price.of(75), nasdaq, 1, Price.of(90), nyse, 1);
		
		setQuote(nasdaq, Price.of(60), Price.of(91));
		
		// Test that NBBO quote is correct & that market 2's quote was replaced
		assertNBBO(sip.getNBBO(), Price.of(65), nyse, 1, Price.of(90), nyse, 1);

		/*
		 * XXX NOTE: if tie in price, nondeterminism in which market has best
		 * price.
		 */
	}
	
	@Test
	public void basicNoDelay() {
		setQuote(nyse, Price.of(80), Price.of(100));

		// Test that NBBO quote is correct
		assertNBBO(sip.getNBBO(), Price.of(80), nyse, 1, Price.of(100), nyse, 1);
	}

	@Test
	public void basicDelay() {
		// Check that process quote activity scheduled correctly
		EventQueue timeline = latencySetup(TimeStamp.of(50));
		setQuote(nyse, Price.of(80), Price.of(100));
		
		// Verify correct process quote activity added to execute at time 50
		timeline.executeUntil(TimeStamp.of(49));
		assertNBBO(sip.getNBBO(), null, null, 0, null, null, 0);
		
		timeline.executeUntil(TimeStamp.of(50));

		// Test that NBBO quote is correct
		assertNBBO(sip.getNBBO(), Price.of(80), nyse, 1, Price.of(100), nyse, 1);
	}
	
	@Test
	public void transactionsInSIP() {
		// Creating and adding bids
		submitOrder(nyse, BUY, Price.of(150), 2);
		// should execute clear since CDA
		
		// Verify that NBBO quote has updated
		assertNBBO(sip.getNBBO(), Price.of(150), nyse, 2, null, null, 0);
		
		submitOrder(nyse, SELL, Price.of(140), 1);
		
		// Verify that transactions has updated as well as NBBO
		assertNBBO(sip.getNBBO(), Price.of(150), nyse, 1, null, null, 0);
	}
	
	@Test
	public void transactionsInDelayedSIP() {
		EventQueue timeline = latencySetup(TimeStamp.of(100));

		// Creating and adding bids
		submitOrder(nyse, BUY, Price.of(150), 2);
		// should execute clear since CDA
		
		// Verify that no NBBO quote yet
		assertNBBO(sip.getNBBO(), null, null, 0, null, null, 0);
		
		submitOrder(nyse, SELL, Price.of(140), 1);
		
		// Verify that transactions has updated as well as NBBO
		timeline.executeUntil(TimeStamp.of(100)); // because of SIP latency
		assertNBBO(sip.getNBBO(), Price.of(150), nyse, 1, null, null, 0);
	}
	
	@Test
	public void basicOrderRoutingNMS() {
		EventQueue timeline = latencySetup(TimeStamp.of(50));
		
		// Set up CDA markets and their quotes
		setQuote(nyse, Price.of(104), Price.of(110));
		setQuote(nasdaq, Price.of(102), Price.of(111));

		// Verify that NBBO quote is (104, 110) at time 50 (after quotes have been processed by SIP)
		timeline.executeUntil(TimeStamp.of(50));
		assertNBBO(sip.getNBBO(), Price.of(104), nyse, 1, Price.of(110), nyse, 1);

		submitOrder(nasdaq, SELL, Price.of(105), 1);
		
		timeline.executeUntil(TimeStamp.of(99));
		assertNBBO(sip.getNBBO(), Price.of(104), nyse, 1, Price.of(110), nyse, 1);
		
		// Verify that NBBO quote is (104, 105) at time 100 (after quotes have been processed by SIP)
		timeline.executeUntil(TimeStamp.of(100));
		assertNBBO(sip.getNBBO(), Price.of(104), nyse, 1, Price.of(105), nasdaq, 1);
		
		// Another agent submits a buy order
		submitNMSOrder(nyse, BUY, Price.of(109), 1);
		timeline.executeUntil(TimeStamp.of(100));
		
		// Verify that order is routed to nasdaq and transacts immediately w/ agent1's order
		assertTrue(nyse.getPrimaryView().getTransactions().isEmpty());
		assertSingleTransaction(nasdaq.getPrimaryView().getTransactions(), Price.of(105), TimeStamp.of(100), 1);
	}

	@Test
	public void latencyArbRoutingNMS() {
		EventQueue timeline = latencySetup(TimeStamp.of(50));
		
		setQuote(nyse, Price.of(104), Price.of(110));
		setQuote(nasdaq, Price.of(102), Price.of(111));
		timeline.executeUntil(TimeStamp.of(50));

		submitOrder(nasdaq, SELL, Price.of(105), 1);
		timeline.executeUntil(TimeStamp.of(50));
		
		// Verify that NBBO quote is still (104, 110) (hasn't updated yet)
		assertNBBO(sip.getNBBO(), Price.of(104), nyse, 1, Price.of(110), nyse, 1);
		
		// Another agent submits a buy order
		submitNMSOrder(nyse, BUY, Price.of(109), 1);
		timeline.executeUntil(TimeStamp.of(99));
		
		// Verify that NBBO quote is still (104, 110) (hasn't updated yet)
		assertNBBO(sip.getNBBO(), Price.of(104), nyse, 1, Price.of(110), nyse, 1);
		
		// The buy order was still routed to nyse, so the NBBO will cross and 
		// there is a latency arbitrage opportunity
		timeline.executeUntil(TimeStamp.of(100));
		assertNBBO(sip.getNBBO(), Price.of(109), nyse, 1, Price.of(105), nasdaq, 1);
	}
	
	@Test
	public void postSpreadsTest() {
		Stats stats = Stats.create();
		EventQueue timeline = latencySetup(stats, TimeStamp.of(50));
		
		TimeSeries expected = TimeSeries.create();
		expected.add(TimeStamp.of(0), Double.POSITIVE_INFINITY); // empty initially
		
		setQuote(nyse, Price.of(104), Price.of(110));
		setQuote(nasdaq, Price.of(102), Price.of(111));
		timeline.executeUntil(TimeStamp.of(50));
		expected.add(TimeStamp.of(50), 6); // 110 - 104

		submitOrder(nasdaq, SELL, Price.of(105), 1);
		timeline.executeUntil(TimeStamp.of(100));
		expected.add(TimeStamp.of(100), 1); // 105 - 104
				
		// Another agent submits a buy order
		submitOrder(nasdaq, BUY, Price.of(109), 1);
		timeline.executeUntil(TimeStamp.of(150)); // Order transacts with SELL @ 105
		expected.add(TimeStamp.of(150), 6); // 110 - 104
		
		assertEquals(expected, stats.getTimeStats().get(Stats.NBBO_SPREAD));
	}
	
	@Test
	public void postSpreadCrossTest() {
		Stats stats = Stats.create();
		EventQueue timeline = latencySetup(stats, TimeStamp.of(50));
		
		TimeSeries expected = TimeSeries.create();
		expected.add(TimeStamp.of(0), Double.POSITIVE_INFINITY); // empty initially
		
		// Initial Quote
		setQuote(nyse, Price.of(104), Price.of(110));
		setQuote(nasdaq, Price.of(102), Price.of(111));
		timeline.executeUntil(TimeStamp.of(50));
		expected.add(TimeStamp.of(50), 6); // 110 - 104

		submitOrder(nasdaq, SELL, Price.of(105), 1);
		submitOrder(nyse, BUY, Price.of(109), 1);
		timeline.executeUntil(TimeStamp.of(100));
		expected.add(TimeStamp.of(100), Double.POSITIVE_INFINITY); // XXX NBBO Crossed, so spread is positive infinity
		
		assertEquals(expected, stats.getTimeStats().get(Stats.NBBO_SPREAD));
	}
	
	private EventQueue latencySetup(TimeStamp latency) {
		return latencySetup(Mock.stats, latency);
	}
	
	private EventQueue latencySetup(Stats stats, TimeStamp latency) {
		EventQueue queue = EventQueue.create(Log.nullLogger(), rand);
		timeline = queue;
		
		sip = SIP.create(stats, timeline, Log.nullLogger(), rand, latency);
		nyse = CDAMarket.create(1, stats, timeline, Log.nullLogger(), rand, sip, Props.fromPairs());
		nasdaq = CDAMarket.create(2, stats, timeline, Log.nullLogger(), rand, sip, Props.fromPairs());
		
		return queue;
	}
	
	private void setQuote(Market market, Price buy, Price sell) {
		setQuote(market, buy, 1, sell, 1);
	}
	
	private OrderRecord submitOrder(Market market, OrderType type, Price price, int quantity) {
		OrderRecord order = OrderRecord.create(market.getPrimaryView(), timeline.getCurrentTime(), type, price, quantity);
		market.getPrimaryView().submitOrder(agent, order);
		return order;
	}
	
	private OrderRecord submitNMSOrder(Market market, OrderType type, Price price, int quantity) {
		OrderRecord order = OrderRecord.create(market.getPrimaryView(), timeline.getCurrentTime(), type, price, quantity);
		market.getPrimaryView().submitNMSOrder(agent, order);
		return order;
	}
	
	private void setQuote(Market market, Price buy, int buyQuantity, Price sell, int sellQuantity) {
		// Clears orders from markets, but only works if orders are one layer deep
		MarketView view = market.getPrimaryView();
		if (view.getQuote().getBidPrice().isPresent())
			submitOrder(market, SELL, view.getQuote().getBidPrice().get(), view.getQuote().getBidQuantity());
		if (view.getQuote().getAskPrice().isPresent())
			submitOrder(market, BUY, view.getQuote().getAskPrice().get(), view.getQuote().getAskQuantity());
		
		submitOrder(market, BUY, buy, buyQuantity);
		submitOrder(market, SELL, sell, sellQuantity);
	}
	
}
