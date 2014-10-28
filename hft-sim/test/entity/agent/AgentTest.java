package entity.agent;

import static fourheap.Order.OrderType.BUY;
import static fourheap.Order.OrderType.SELL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static utils.Tests.checkQuote;
import static utils.Tests.checkSingleOrder;

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
import entity.agent.position.PrivateValues;
import entity.market.Market;
import entity.market.Market.MarketView;
import entity.market.Price;
import event.TimeStamp;
import fourheap.Order.OrderType;

public class AgentTest {
	private static final double eps = 1e-6;
	private static final Random rand = new Random();
	private MockSim sim;
	private Market trueMarket;
	private MarketView market, fast;
	private Agent agent;
	
	// FIXME Test that NMS ORders update OrderRecord when the route or don't

	@Before
	public void defaultSetup() throws IOException {
		setup(Props.fromPairs());
	}
	
	public void setup(Props parameters) throws IOException {
		sim = MockSim.createCDA(getClass(), Log.Level.NO_LOGGING, 1, parameters);
		trueMarket = Iterables.getOnlyElement(sim.getMarkets());
		market = trueMarket.getPrimaryView();
		fast = trueMarket.getView(TimeStamp.IMMEDIATE);
		agent = mockAgent();
	}

	@Test
	public void basicWithdrawBuy() {
		OrderRecord buy = submitOrder(BUY, Price.of(50), 1);
		sim.executeUntil(TimeStamp.of(1));
		submitOrder(SELL, Price.of(100), 2);
		
		assertEquals(2, agent.activeOrders.size());
		withdrawOrder(buy);
		
		checkSingleOrder(agent.activeOrders, Price.of(100), 2, TimeStamp.of(1), TimeStamp.of(1));
	}
	
	@Test
	public void basicWithdrawSell() {
		submitOrder(BUY, Price.of(50), 1);
		sim.executeUntil(TimeStamp.of(1));
		OrderRecord sell = submitOrder(SELL, Price.of(100), 2);
		
		assertEquals(2, agent.activeOrders.size());
		withdrawOrder(sell);
		
		checkSingleOrder(agent.activeOrders, Price.of(50), 1, TimeStamp.ZERO, TimeStamp.ZERO);
	}
	
	@Test
	public void withdrawOrder() {
		OrderRecord order = submitOrder(BUY, Price.of(100), 1);
		
		// Verify orders added correctly
		checkSingleOrder(agent.activeOrders, Price.of(100), 1, TimeStamp.ZERO, TimeStamp.ZERO);
		checkQuote(market.getQuote(), Price.of(100), 1, null, 0);
		
		// Withdraw order
		withdrawOrder(order);
		
		assertTrue("Order was not withdrawn", agent.activeOrders.isEmpty());
		checkQuote(market.getQuote(), null, 0, null, 0);
	}
	
	// FIXME Premature withdraw test
	// FIXME Slow update of order record
	// FIXME Other agent tests
	// FIXME Private value when it's move to agent
	
	@Test
	public void withdrawOrderDelayed() throws IOException {
		setup(Props.fromPairs(MarketLatency.class, TimeStamp.of(10)));
		
		OrderRecord order = submitOrder(BUY, Price.of(100), 1);
		
		// Verify Orders don't exist yet
		checkSingleOrder(agent.activeOrders, Price.of(100), 1, TimeStamp.ZERO, null);
		checkQuote(fast.getQuote(), null, 0, null, 0);
		checkQuote(market.getQuote(), null, 0, null, 0);
		
		// Verify orders exist in market, but agent has no knowledge
		sim.executeUntil(TimeStamp.of(10));
		
		checkSingleOrder(agent.activeOrders, Price.of(100), 1, TimeStamp.ZERO, null);
		checkQuote(fast.getQuote(), Price.of(100), 1, null, 0);
		checkQuote(market.getQuote(), null, 0, null, 0);
		
		// After quotes have updated and reached agent
		sim.executeUntil(TimeStamp.of(20));
		checkSingleOrder(agent.activeOrders, Price.of(100), 1, TimeStamp.ZERO, TimeStamp.of(10));
		checkQuote(fast.getQuote(), Price.of(100), 1, null, 0);
		checkQuote(market.getQuote(), Price.of(100), 1, null, 0);
		
		// Withdraw order
		withdrawOrder(order);
		
		// Check order removed
		assertEquals(0, order.getQuantity());
		assertTrue(agent.activeOrders.isEmpty());
		
		// Verify that quote is now stale
		sim.executeUntil(TimeStamp.of(30));
		checkQuote(fast.getQuote(), null, 0, null, 0);
		checkQuote(market.getQuote(), Price.of(100), 1, null, 0);
		
		// After quotes have updated
		sim.executeUntil(TimeStamp.of(40));
		checkQuote(fast.getQuote(), null, 0, null, 0);
		checkQuote(market.getQuote(), null, 0, null, 0);
	}
	
	@Test
	public void withdrawNewestOrder() {
		submitOrder(BUY, Price.of(50), 1);
		sim.executeUntil(TimeStamp.of(1));
		submitOrder(SELL, Price.of(100), 1);
		
		// Verify orders added correctly
		assertEquals(2, agent.activeOrders.size());
		checkQuote(market.getQuote(), Price.of(50), 1, Price.of(100), 1);
		
		// Withdraw newest order (sell)
		agent.withdrawNewestOrder();
		sim.executeImmediate();
		
		checkSingleOrder(agent.activeOrders, Price.of(50), 1, TimeStamp.ZERO, TimeStamp.ZERO);
		checkQuote(market.getQuote(), Price.of(50), 1, null, 0);
	}
	
	@Test
	public void withdrawOldestOrder() {
		submitOrder(BUY, Price.of(50), 1);
		sim.executeUntil(TimeStamp.of(1));
		submitOrder(SELL, Price.of(100), 1);
		
		// Verify orders added correctly
		assertEquals(2, agent.activeOrders.size());
		checkQuote(market.getQuote(), Price.of(50), 1, Price.of(100), 1);
		
		agent.withdrawOldestOrder();
		sim.executeImmediate();
		
		checkSingleOrder(agent.activeOrders, Price.of(100), 1, TimeStamp.of(1), TimeStamp.of(1));
		checkQuote(market.getQuote(), null, 0, Price.of(100), 1);
	}
	
	@Test
	public void withdrawAllOrders() {
		submitOrder(BUY, Price.of(50), 1);
		sim.executeUntil(TimeStamp.of(1));
		submitOrder(SELL, Price.of(100), 1);
		
		// Verify orders added correctly
		assertEquals(2, agent.activeOrders.size());
		checkQuote(market.getQuote(), Price.of(50), 1, Price.of(100), 1);
		
		// Withdraw all orders
		agent.withdrawAllOrders();
		sim.executeImmediate();
		
		assertTrue(agent.activeOrders.isEmpty());	
		checkQuote(market.getQuote(), null, 0, null, 0);
	}
	
	/** Test that withdraw still works even when agent has no orders */
	@Test
	public void noOrderWithdraw() {
		agent.withdrawNewestOrder();
		sim.executeImmediate();
		
		agent.withdrawOldestOrder();
		sim.executeImmediate();
		
		agent.withdrawAllOrders();
		sim.executeImmediate();
	}
	
	@Test
	public void processTransaction() {
		Agent other = mockAgent();
		
		// Market is actuall a standard agent view of the market
		assertTrue(market.getTransactions().isEmpty());
		
		// Creating and adding bids
		submitOrder(BUY, Price.of(110), 1);
		submitOrder(other, SELL, Price.of(100), 1);
		
		assertEquals(1, market.getTransactions().size());
		assertEquals(1, agent.getPosition());
		assertEquals(-110, agent.getProfit(), eps);
		assertEquals(-1, other.getPosition());
		assertEquals(110, other.getProfit(), eps);
	}
	
	@Test
	public void processTransactionMultiQuantity() {
		Agent agent2 = mockAgent();
		
		// Market is actuall a standard agent view of the market
				assertTrue(market.getTransactions().isEmpty());
		
		// Creating and adding bids
		submitOrder(BUY, Price.of(110), 3);
		submitOrder(agent2, SELL, Price.of(100), 2);
		
		// Testing the market for the correct transactions
		assertEquals(1, market.getTransactions().size());
		assertEquals(2, agent.getPosition());
		assertEquals(-220, agent.getProfit(), eps);
		assertEquals(-2, agent2.getPosition());
		assertEquals(220, agent2.getProfit(), eps);
	}
	
	@Test
	public void classPostTransactionTest() {
		ZIRAgent zir = ZIRAgent.create(sim, trueMarket, rand, Props.fromPairs());
		NoOpAgent noop1 = NoOpAgent.create(sim, rand, Props.fromPairs());
		NoOpAgent noop2 = NoOpAgent.create(sim, rand, Props.fromPairs());
		
		submitOrder(zir, BUY, Price.of(50), 2);
		submitOrder(noop1, SELL, Price.of(50), 3);
		submitOrder(noop2, BUY, Price.of(50), 1);
		
		// XXX Number of transactions is independent of quantity
		assertEquals(1, sim.getStats().getSummaryStats().get(Stats.NUM_TRANS + "z_i_r_agent").sum(), eps);
		assertEquals(3, sim.getStats().getSummaryStats().get(Stats.NUM_TRANS + "no_op_agent").sum(), eps);
		// XXX Total transactions is double the number of times actually transacted
		assertEquals(4, sim.getStats().getSummaryStats().get(Stats.NUM_TRANS_TOTAL).sum(), eps);
	}
	
	@Test
	public void liquidation() {
		Agent other = mockAgent();
		
		// Check that no change if position 0
		agent.liquidateAtPrice(Price.of(100000));
		assertEquals(0, agent.getProfit());
		
		// Check liquidation when position > 0 (sell 1 unit)
		submitOrder(agent, BUY, Price.ZERO, 1);
		submitOrder(other, SELL, Price.ZERO, 1);
		
		assertEquals(1, agent.getPosition());
		agent.liquidateAtPrice(Price.of(100000));
		assertEquals(100000, agent.getProfit());
		
		// Check liquidation when position < 0 (buy 2 units)
		submitOrder(agent, SELL, Price.ZERO, 2);
		submitOrder(other, BUY, Price.ZERO, 2);
		
		assertEquals(-2, agent.getPosition());
		agent.liquidateAtPrice(Price.of(100000));
		assertEquals(-100000, agent.getProfit());
	}
	
	@Test
	public void payoffTest() {
		Agent agent1 = mockAgent();
		
		submitOrder(agent1, BUY, Price.of(200), 2);
		submitOrder(agent, SELL, Price.of(200), 2);
		
		agent1.liquidateAtPrice(Price.of(100));
		agent.liquidateAtPrice(Price.of(100));
		
		assertEquals(200, agent.getPayoff(), eps);
		assertEquals(-200, agent1.getPayoff(), eps);
	}
	
	@Test
	public void extraTest() throws IOException {
		for (int i = 0; i < 100; i++) {
			defaultSetup();
			basicWithdrawBuy();
			defaultSetup();
			basicWithdrawSell();
		}
	}
	
	private void withdrawOrder(OrderRecord order) {
		agent.withdrawOrder(order);
		sim.executeImmediate();
	}
	
	private OrderRecord submitOrder(OrderType buyOrSell, Price price, int quantity) {
		return submitOrder(agent, buyOrSell, price, quantity);
	}
	
	private OrderRecord submitOrder(Agent agent, OrderType buyOrSell, Price price, int quantity) {
		OrderRecord order = agent.submitOrder(market, buyOrSell, price, quantity);
		sim.executeImmediate();
		return order;
	}
	
	private Agent mockAgent() {
		return new Agent(sim, PrivateValues.zero(), TimeStamp.ZERO, rand, Props.fromPairs()) {
			private static final long serialVersionUID = 1L;
			@Override public void agentStrategy() { }
			@Override public String toString() { return "TestAgent " + id; }
		};
	}
}
