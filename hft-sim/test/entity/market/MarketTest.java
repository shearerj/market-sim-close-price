package entity.market;

import static fourheap.Order.OrderType.BUY;
import static fourheap.Order.OrderType.SELL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static utils.Tests.checkQuote;
import static utils.Tests.checkSingleTransaction;
import static utils.Tests.checkTransaction;

import java.io.IOException;
import java.util.Random;

import logger.Log;

import org.junit.Before;
import org.junit.Test;

import systemmanager.Keys.MarketLatency;
import systemmanager.MockSim;

import com.google.common.collect.Iterables;

import data.Props;
import data.Stats;
import data.TimeSeries;
import entity.agent.Agent;
import entity.agent.Agent.AgentView;
import entity.agent.position.PrivateValues;
import entity.agent.OrderRecord;
import entity.market.Market.MarketView;
import event.TimeStamp;
import fourheap.Order.OrderType;

public class MarketTest {
	private static final double eps = 1e-6;
	private static final Random rand = new Random();
	private MockSim sim;
	private Market nbboUpdate, market;
	private MarketView info, fast;

	@Before
	public void defaultSetup() throws IOException {
		setup(Props.fromPairs());
	}
	
	public void setup(Props marketParams) throws IOException {
		sim = MockSim.createCDA(getClass(), Log.Level.NO_LOGGING, 1);
		nbboUpdate = Iterables.getOnlyElement(sim.getMarkets());
		// MockMarket that executes instantaneous information
		market = new Market(sim, new UniformPriceClear(0.5, 1), rand, marketParams) {
			private static final long serialVersionUID = 1L;
			@Override protected void submitOrder(MarketView view, AgentView agent, OrderRecord orderRecord) {
				super.submitOrder(view, agent, orderRecord);
				updateQuote();
			}
			@Override protected void withdrawOrder(OrderRecord order, int quantity) {
				super.withdrawOrder(order, quantity);
				updateQuote();
			}
		};
		info = market.getPrimaryView();
		fast = market.getView(TimeStamp.IMMEDIATE);
	}

	@Test
	public void addBid() {
		Agent agent = mockAgent();
		submitOrder(agent, BUY, Price.of(1), 1);
		checkQuote(info.getQuote(), Price.of(1), 1, null, 0);
	}

	@Test
	public void addAsk() {
		Agent agent = mockAgent();
		submitOrder(agent, SELL, Price.of(1), 1);
		checkQuote(info.getQuote(), null, 0, Price.of(1), 1);
	}

	@Test
	public void basicEqualClear() {
		Agent agent1 = mockAgent();
		Agent agent2 = mockAgent();

		// Creating and adding bids
		OrderRecord buy = submitOrder(agent1, BUY, Price.of(100), 1);
		OrderRecord sell = submitOrder(agent2, SELL, Price.of(100), 1);

		// Testing the market for the correct transaction
		clear();
		
		checkSingleTransaction(info.getTransactions(), Price.of(100), TimeStamp.ZERO, 1);
		checkQuote(info.getQuote(), null, 0, null, 0);
		assertEquals(0, buy.getQuantity());
		assertEquals(0, sell.getQuantity());
	}

	@Test
	public void basicOverlapClear() {
		Agent agent1 = mockAgent();
		Agent agent2 = mockAgent();

		// Creating and adding bids
		OrderRecord buy = submitOrder(agent1, BUY, Price.of(200), 1);
		OrderRecord sell = submitOrder(agent2, SELL, Price.of(50), 1);

		// Testing the market for the correct transaction
		clear();
		
		checkSingleTransaction(info.getTransactions(), Price.of(125), TimeStamp.ZERO, 1);
		checkQuote(info.getQuote(), null, 0, null, 0);
		assertEquals(0, buy.getQuantity());
		assertEquals(0, sell.getQuantity());
	}

	/** Several bids with one clear have proper transaction */
	@Test
	public void multiBidSingleClear() {
		Agent agent1 = mockAgent();
		Agent agent2 = mockAgent();
		Agent agent3 = mockAgent();
		Agent agent4 = mockAgent();

		// Creating and adding bids
		OrderRecord buyTrans = submitOrder(agent1, BUY, Price.of(150), 1);
		OrderRecord buy = submitOrder(agent2, BUY, Price.of(100), 1);
		OrderRecord sell = submitOrder(agent3, SELL, Price.of(180), 1);
		OrderRecord sellTrans = submitOrder(agent4, SELL, Price.of(120), 1);
		clear();
		
		// Testing the market for the correct transactions
		checkSingleTransaction(info.getTransactions(), Price.of(135), TimeStamp.ZERO, 1);
		checkQuote(info.getQuote(), Price.of(100), 1, Price.of(180), 1);
		assertEquals(0, buyTrans.getQuantity());
		assertEquals(0, sellTrans.getQuantity());
		assertEquals(1, buy.getQuantity());
		assertEquals(1, sell.getQuantity());
	}

	/** Two sets of bids overlap before clear */
	@Test
	public void multiOverlapClear() {
		Agent agent1 = mockAgent();
		Agent agent2 = mockAgent();
		Agent agent3 = mockAgent();
		Agent agent4 = mockAgent();

		// Creating and adding bids
		OrderRecord buy1 = submitOrder(agent1, BUY, Price.of(150), 1);
		OrderRecord sell1 = submitOrder(agent2, SELL, Price.of(100), 1);
		OrderRecord buy2 = submitOrder(agent3, BUY, Price.of(200), 1);
		OrderRecord sell2 = submitOrder(agent4, SELL, Price.of(130), 1);
		assertTrue(info.getTransactions().isEmpty());

		// Testing the market for the correct transactions
		clear();
		assertEquals(2, info.getTransactions().size());
		for (Transaction transaction : info.getTransactions())
			checkTransaction(transaction, Price.of(140), TimeStamp.ZERO, 1);
		assertEquals(0, buy1.getQuantity());
		assertEquals(0, sell1.getQuantity());
		assertEquals(0, buy2.getQuantity());
		assertEquals(0, sell2.getQuantity());

	}

	/** Scenario with two possible matches, but only one pair transacts at the match. */
	@Test
	public void partialOverlapClear() {
		Agent agent1 = mockAgent();
		Agent agent2 = mockAgent();
		Agent agent3 = mockAgent();
		Agent agent4 = mockAgent();

		// Creating and adding bids
		OrderRecord buyTrans = submitOrder(agent3, BUY, Price.of(200), 1);
		OrderRecord buy = submitOrder(agent4, SELL, Price.of(130), 1);
		OrderRecord sell = submitOrder(agent1, BUY, Price.of(110), 1);
		OrderRecord sellTrans = submitOrder(agent2, SELL, Price.of(100), 1);

		// Testing the market for the correct transactions
		clear();

		checkSingleTransaction(info.getTransactions(), Price.of(150), TimeStamp.ZERO, 1);
		checkQuote(info.getQuote(), Price.of(110), 1, Price.of(130), 1);
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
		Agent agent1 = mockAgent();
		Agent agent2 = mockAgent();
		Agent agent3 = mockAgent();
		Agent agent4 = mockAgent();
		Agent agent5 = mockAgent();
		
		OrderRecord order1 = submitOrder(agent1, SELL, Price.of(100), 1);
		sim.executeUntil(TimeStamp.of(1));
		OrderRecord order2 = submitOrder(agent2, SELL, Price.of(100), 1);
		OrderRecord order3 = submitOrder(agent3, BUY, Price.of(150), 1);
		clear();
		
		// Check that earlier agent (agent1) is trading with agent3
		// Testing the market for the correct transactions
		checkSingleTransaction(info.getTransactions(), Price.of(125), TimeStamp.of(1), 1);
		assertEquals(0, order1.getQuantity());
		assertEquals(0, order3.getQuantity());

		// Agent 2 has S1@100
		submitOrder(agent1, SELL, Price.of(100), 1);
		sim.executeUntil(TimeStamp.of(2));
		submitOrder(agent3, SELL, Price.of(100), 1);
		submitOrder(agent4, SELL, Price.of(100), 1);
		clear();
		OrderRecord order5 = submitOrder(agent5, BUY, Price.of(130), 1);
		clear();
		
		// Check that the first submitted S1@100 transacts (from agent2)
		assertEquals(2, info.getTransactions().size());
		checkTransaction(info.getTransactions().get(0), Price.of(115), TimeStamp.of(2), 1);
		assertEquals(0, order2.getQuantity());
		assertEquals(0, order5.getQuantity());
		
		// Let's try populating the market with random orders 
		// agent 1's order S1@100 at 1 remains
		// agent 3's order S1@100 at 2 remains
		// agent 4's order S1@100 at 2 remains
		order5 = submitOrder(agent5, SELL, Price.of(90), 1);
		submitOrder(agent5, SELL, Price.of(100), 1);
		submitOrder(agent5, SELL, Price.of(110), 1);
		submitOrder(agent5, SELL, Price.of(120), 1);
		submitOrder(agent5, BUY, Price.of(80), 1);
		submitOrder(agent5, BUY, Price.of(70), 1);
		submitOrder(agent5, BUY, Price.of(60), 1);
		clear();
		assertEquals(2, market.getPrimaryView().getTransactions().size()); // no change

		// Check basic overlap - between agent5 (@90) and agent2
		order2 = submitOrder(agent2, BUY, Price.of(130), 1);
		clear();
		
		assertEquals(3, info.getTransactions().size());
		checkTransaction(info.getTransactions().get(0), Price.of(110), TimeStamp.of(2), 1);
		assertEquals(0, order2.getQuantity());
		assertEquals(0, order5.getQuantity());
		
		// Check additional overlapping orders
		// Transactions between:
		// - agent 2 and agent 1
		// - agent 2 and agent 3
		// - agent 2 and agent 4
		// - agent 2 and agent 5
		submitOrder(agent2, BUY, Price.of(110), 4);
		clear();
		
		assertEquals(7, info.getTransactions().size());
		for (Transaction trans : info.getTransactions().subList(0, 4))
			checkTransaction(trans, Price.of(105), TimeStamp.of(2), 1);
	}

	@Test
	public void partialQuantity() {
		Agent agent1 = mockAgent();
		Agent agent2 = mockAgent();
		
		OrderRecord sell = submitOrder(agent1, SELL, Price.of(100), 2);
		OrderRecord buy = submitOrder(agent2, BUY, Price.of(150), 5);
		clear();

		// Check that two units transact and that post-trade BID is correct (3 buy units at 150)
		checkSingleTransaction(info.getTransactions(), Price.of(125), TimeStamp.ZERO, 2);
		checkQuote(info.getQuote(),Price.of(150), 3, null, 0);
		assertEquals(0, sell.getQuantity());
		assertEquals(3, buy.getQuantity());
	}

	@Test
	public void multiQuantity() {
		Agent agent1 = mockAgent();
		Agent agent2 = mockAgent();

		OrderRecord sell1 = submitOrder(agent1, SELL, Price.of(150), 1);
		OrderRecord sell2 = submitOrder(agent1, SELL, Price.of(140), 1);
		clear();

		sim.executeUntil(TimeStamp.of(1));
		// Both agents' sell orders should transact
		OrderRecord buy = submitOrder(agent2, BUY, Price.of(160), 2);
		clear();
		
		assertEquals(2, info.getTransactions().size());
		for (Transaction tr : info.getTransactions())
			assertEquals("Incorrect Quantity", 1, tr.getQuantity());
		checkQuote(info.getQuote(), null, 0, null, 0);
		assertEquals(0, sell1.getQuantity());
		assertEquals(0, sell2.getQuantity());
		assertEquals(0, buy.getQuantity());
	}

	@Test
	public void basicWithdraw() {
		Agent agent1 = mockAgent();
		Agent agent2 = mockAgent();

		OrderRecord order = submitOrder(agent1, SELL, Price.of(100), 1);
		clear();
		
		// Check that quotes are correct (no bid, ask @100)
		checkQuote(info.getQuote(), null, 0, Price.of(100), 1);

		// Withdraw order
		withdrawOrder(order);

		// Check that quotes are correct (no bid, no ask)
		checkQuote(info.getQuote(), null, 0, null, 0);

		// Check that no transaction, because agent1 withdrew its order
		order = submitOrder(agent2, BUY, Price.of(125), 1);
		assertTrue(info.getTransactions().isEmpty());
		submitOrder(agent2, BUY, Price.of(115), 1);

		withdrawOrder(order);

		// Check that it transacts with order (@115) that was not withdrawn
		submitOrder(agent1, SELL, Price.of(105), 1);
		clear();
		checkSingleTransaction(info.getTransactions(), Price.of(110), TimeStamp.ZERO, 1);
		checkQuote(info.getQuote(), null, 0, null, 0);
	}

	@Test
	public void multiQuantityWithdraw() {
		Agent agent1 = mockAgent();
		Agent agent2 = mockAgent();

		submitOrder(agent1, SELL, Price.of(150), 1);
		OrderRecord order = submitOrder(agent1, SELL, Price.of(140), 2);
		clear();
		
		market.withdrawOrder(order, 1);

		// Both agents' sell orders should transact b/c partial quantity withdrawn
		submitOrder(agent2, BUY, Price.of(160), 1);
		submitOrder(agent2, BUY, Price.of(160), 2);
		clear();
		assertEquals(2, info.getTransactions().size());
		for (Transaction tr : info.getTransactions())
			assertEquals("Incorrect Quantity", 1, tr.getQuantity());

		checkQuote(info.getQuote(), Price.of(160), 1, null, 0);
	}

	/** Information propagates at proper times */
	@Test
	public void latencyTest() throws IOException {
		setup(Props.fromPairs(MarketLatency.class, TimeStamp.of(100)));

		Agent agent = mockAgent();
		info.submitOrder(agent, OrderRecord.create(info, sim.getCurrentTime(), SELL, Price.of(100), 1));
		
		sim.executeUntil(TimeStamp.of(99));
		// Nothing has happened
		checkQuote(fast.getQuote(), null, 0, null, 0);
		checkQuote(info.getQuote(), null, 0, null, 0);

		// Market got order and updated it's quote, but it hasn't reached the agents yet
		sim.executeUntil(TimeStamp.of(100));
		checkQuote(fast.getQuote(), null, 0, Price.of(100), 1);
		checkQuote(info.getQuote(), null, 0, null, 0);
		
		// Still hasn't reached
		sim.executeUntil(TimeStamp.of(199));
		checkQuote(fast.getQuote(), null, 0, Price.of(100), 1);
		checkQuote(info.getQuote(), null, 0, null, 0);
		
		// Finally reached agent
		sim.executeUntil(TimeStamp.of(200));
		checkQuote(fast.getQuote(), null, 0, Price.of(100), 1);
		checkQuote(info.getQuote(), null, 0, Price.of(100), 1);
	}

	/** Verify that earlier orders transact */
	@Test
	public void timePriorityBuy() {
		Agent agent1 = mockAgent();
		Agent agent2 = mockAgent();
		
		OrderRecord first = submitOrder(agent1, BUY, Price.of(100), 1);
		OrderRecord second = submitOrder(agent1, BUY, Price.of(100), 1);
		submitOrder(agent2, SELL, Price.of(100), 1);
		clear();
		
		assertEquals(0, first.getQuantity());
		assertEquals(1, second.getQuantity());
	}
	
	/** Verify that earlier orders transact */
	@Test
	public void timePrioritySell() {
		Agent agent1 = mockAgent();
		Agent agent2 = mockAgent();
		
		OrderRecord first = submitOrder(agent1, SELL, Price.of(100), 1);
		OrderRecord second = submitOrder(agent1, SELL, Price.of(100), 1);
		submitOrder(agent2, BUY, Price.of(100), 1);
		clear();
		
		assertEquals(0, first.getQuantity());
		assertEquals(1, second.getQuantity());
	}

	@Test
	public void updateQuoteLatency() throws IOException {
		setup(Props.fromPairs(MarketLatency.class, TimeStamp.of(100)));

		// Test that before Time 200 nothing has been updated
		Agent agent = mockAgent();
		submitOrder(agent, SELL, Price.of(100), 1);
		
		sim.executeUntil(TimeStamp.of(99));
		checkQuote(info.getQuote(), null, 0, null, 0);

		// Update QP
		sim.executeUntil(TimeStamp.of(100));
		checkQuote(info.getQuote(), null, 0, Price.of(100), 1);
		
		// Add new quote
		submitOrder(agent, BUY, Price.of(80), 1);
		
		// Update QP
		sim.executeUntil(TimeStamp.of(200));
		checkQuote(info.getQuote(), Price.of(80), 1, Price.of(100), 1);

	}

	@Test
	public void updateTransactionsLatency() throws IOException {
		setup(Props.fromPairs(MarketLatency.class, TimeStamp.of(100)));
		Agent agent1 = mockAgent();
		Agent agent2 = mockAgent();
		
		OrderRecord buy = submitOrder(agent1, BUY, Price.of(150), 1);
		OrderRecord sell = submitOrder(agent2, SELL, Price.of(140), 1);
		clear();

		// Verify that transactions have not updated yet (for primary view)
		sim.executeUntil(TimeStamp.of(99));
		assertTrue(info.getTransactions().isEmpty());
		checkSingleTransaction(fast.getTransactions(), Price.of(145), TimeStamp.ZERO, 1);
		assertEquals(1, buy.getQuantity());
		assertEquals(1, sell.getQuantity());
		
		// Test that after 100 new transaction did get updated
		sim.executeUntil(TimeStamp.of(100));
		checkSingleTransaction(info.getTransactions(), Price.of(145), TimeStamp.ZERO, 1);
		assertEquals(0, buy.getQuantity());
		assertEquals(0, sell.getQuantity());
	}
	
	@Test
	public void basicTransactionTest() {
		assertTrue("Incorrect initial transaction list", info.getTransactions().isEmpty());

		addTransaction(Price.of(150), 2);
		checkSingleTransaction(info.getTransactions(), Price.of(150), TimeStamp.ZERO, 2);
		
		addTransaction(Price.of(170), 1);
		checkTransaction(Iterables.getFirst(info.getTransactions(), null), Price.of(170), TimeStamp.ZERO, 1);
	}

	@Test
	public void basicDelayProcessTransaction() throws IOException {
		setup(Props.fromPairs(MarketLatency.class, TimeStamp.of(100)));

		assertTrue("Incorrect initial transaction list", info.getTransactions().isEmpty());

		// Transaction in market, but not seen via slow view
		addTransaction(Price.of(150), 2);
		assertTrue("List updated too early", info.getTransactions().isEmpty());
		checkSingleTransaction(fast.getTransactions(), Price.of(150), TimeStamp.ZERO, 2);

		sim.executeUntil(TimeStamp.of(100));
		checkSingleTransaction(info.getTransactions(), Price.of(150), TimeStamp.ZERO, 2);
	}

	/** Test handling of stale quotes when better order happened later */
	@Test
	public void staleQuotesFirst() throws IOException {
		setup(Props.fromPairs(MarketLatency.class, TimeStamp.of(100)));
		Agent agent = mockAgent();
		submitOrder(agent, BUY, Price.of(50), 1);
		submitOrder(agent, BUY, Price.of(60), 1);
		
		sim.executeUntil(TimeStamp.of(100));
		checkQuote(info.getQuote(), Price.of(60), 1, null, 0);
		
		submitOrder(agent, SELL, Price.of(110), 1);
		submitOrder(agent, SELL, Price.of(100), 1);
		
		sim.executeUntil(TimeStamp.of(200));
		checkQuote(info.getQuote(), Price.of(60), 1, Price.of(100), 1);
	}
	
	/** Test handling of stale quotes when better order happened earlier */
	@Test
	public void staleQuotesLast() throws IOException {
		setup(Props.fromPairs(MarketLatency.class, TimeStamp.of(100)));
		Agent agent = mockAgent();
		
		submitOrder(agent, BUY, Price.of(60), 1);
		submitOrder(agent, BUY, Price.of(50), 1);
		
		sim.executeUntil(TimeStamp.of(100));
		checkQuote(info.getQuote(), Price.of(60), 1, null, 0);
		
		submitOrder(agent, SELL, Price.of(100), 1);
		submitOrder(agent, SELL, Price.of(110), 1);
		
		sim.executeUntil(TimeStamp.of(200));
		checkQuote(info.getQuote(), Price.of(60), 1, Price.of(100), 1);
	}
	
	@Test
	public void nbboRoutingBuy() {
		Agent agent = mockAgent();
		
		setNBBO(Price.of(80), Price.of(100));
		submitNMSOrder(agent, BUY, Price.of(100), 1);
		
		// Verify it got routed due to empty quote
		checkQuote(info.getQuote(), null, 0, null, 0);
	}
	
	@Test
	public void nbboRoutingSell() {
		Agent agent = mockAgent();
		
		setNBBO(Price.of(80), Price.of(100));
		submitNMSOrder(agent, SELL, Price.of(80), 1);
		
		// Verify it got routed due to empty quote
		checkQuote(info.getQuote(), null, 0, null, 0);
	}
	
	// FIXME More tests for proper nms order routing
	
	@Test
	public void spreadsPostTest() {
		Agent agent = mockAgent();
		submitOrder(agent, SELL, Price.of(100), 1);
		submitOrder(agent, BUY, Price.of(50), 1);
		
		sim.executeUntil(TimeStamp.of(100));
		submitOrder(agent, SELL, Price.of(80), 1);
		submitOrder(agent, BUY, Price.of(60), 1);
		
		TimeSeries truth = TimeSeries.create();
		truth.add(0, 50);
		truth.add(100, 20);
		
		assertEquals(truth, sim.getStats().getTimeStats().get(Stats.SPREAD + market));
	}
	
	@Test
	public void midpointPostTest() {
		Agent agent = mockAgent();
		submitOrder(agent, SELL, Price.of(100), 1);
		submitOrder(agent, BUY, Price.of(50), 1);
		
		sim.executeUntil(TimeStamp.of(100));
		submitOrder(agent, SELL, Price.of(80), 1);
		submitOrder(agent, BUY, Price.of(60), 1);
		
		TimeSeries truth = TimeSeries.create();
		truth.add(0, 75);
		truth.add(100, 70);
		
		assertEquals(truth, sim.getStats().getTimeStats().get(Stats.MIDQUOTE + market));
	}
	
	@Test
	public void transactionPricePostTest() {
		addTransaction(Price.of(100), 1);
		
		sim.executeUntil(TimeStamp.of(50));
		setNBBO(Price.of(50), Price.of(50)); // Cheat to submit transaction to other market
		
		sim.executeUntil(TimeStamp.of(100));
		addTransaction(Price.of(200), 2);
		addTransaction(Price.of(150), 1);
		
		TimeSeries truth = TimeSeries.create();
		truth.add(0, 100);
		truth.add(50, 50);
		truth.add(100, 150);
		
		assertEquals(truth, sim.getStats().getTimeStats().get(Stats.TRANSACTION_PRICE));
		
		// XXX Doesn't account for order quantity
		assertEquals(125, sim.getStats().getSummaryStats().get(Stats.PRICE).mean(), eps);
		assertEquals(4, sim.getStats().getSummaryStats().get(Stats.PRICE).n());
	}
	
	@Test
	public void randomTest() throws IOException {
		for(int i=0; i < 100; i++) {
			defaultSetup();
			multiBidSingleClear();
			defaultSetup();
			multiOverlapClear();
			defaultSetup();
			partialOverlapClear();
			defaultSetup();
			staleQuotesFirst();
			defaultSetup();
			staleQuotesLast();
		}
	}

	private Agent mockAgent() {
		return new Agent(sim, PrivateValues.zero(), TimeStamp.ZERO, rand, Props.fromPairs()) {
			private static final long serialVersionUID = 1L;
			@Override public void agentStrategy() { }
			@Override public String toString() { return "TestAgent " + id; }
		};
	}
	
	private void clear() {
		market.clear();
		sim.executeImmediate();
	}
	
	private void withdrawOrder(OrderRecord order) {
		market.withdrawOrder(order, order.getQuantity());
		sim.executeImmediate();
	}
	
	private void addTransaction(Price price, int quantity) {
		Agent buy = mockAgent();
		Agent sell = mockAgent();
		submitOrder(buy, BUY, price, quantity);
		submitOrder(sell, SELL, price, quantity);
		market.clear();
		sim.executeImmediate();
	}
	
	private OrderRecord submitOrder(Agent agent, OrderType buyOrSell, Price price, int quantity) {
		OrderRecord order = OrderRecord.create(info, sim.getCurrentTime(), buyOrSell, price, quantity);
		market.submitOrder(info, agent.getView(info.getLatency()), order);
		sim.executeImmediate();
		return order;
	}
	
	private OrderRecord submitNMSOrder(Agent agent, OrderType buyOrSell, Price price, int quantity) {
		OrderRecord order = OrderRecord.create(info, sim.getCurrentTime(), buyOrSell, price, quantity);
		market.submitNMSOrder(info, agent, agent.getView(info.getLatency()), order);
		sim.executeImmediate();
		return order;
	}
	
	private void setNBBO(Price bid, Price ask) {
		Agent agent = mockAgent();
		nbboUpdate.submitOrder(info,
				agent.getView(nbboUpdate.getPrimaryView().getLatency()), OrderRecord.create(nbboUpdate.getPrimaryView(), sim.getCurrentTime(), BUY, bid, 1));
		nbboUpdate.submitOrder(info,
				agent.getView(nbboUpdate.getPrimaryView().getLatency()), OrderRecord.create(nbboUpdate.getPrimaryView(), sim.getCurrentTime(), SELL, ask, 1));
		sim.executeImmediate();
	}
	
}
