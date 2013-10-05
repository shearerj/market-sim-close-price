package entity.market;

import static org.junit.Assert.*;

import java.io.File;
import java.util.Collection;
import java.util.Random;

import logger.Logger;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import activity.Activity;
import activity.Clear;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

import data.DummyFundamental;
import data.FundamentalValue;
import entity.agent.MockAgent;
import entity.infoproc.SIP;
import entity.market.CDAMarket;
import entity.market.Market;
import entity.market.Price;
import entity.market.Transaction;
import event.TimeStamp;

public class CDAMarketTest {

	private FundamentalValue fundamental = new DummyFundamental(100000);
	private SIP sip;
	private Market market;
	private Market market2;

	@BeforeClass
	public static void setupClass() {
		Logger.setup(3, new File("simulations/unit_testing/CDAMarketTest.log"));
	}

	@Before
	public void setup() {
		sip = new SIP(TimeStamp.IMMEDIATE);
		market = new CDAMarket(sip, TimeStamp.IMMEDIATE, new Random(), 1);
		market2 = new CDAMarket(sip, new TimeStamp(100), new Random(), 1);
	}

	@Test
	public void addBid() {
		TimeStamp time = new TimeStamp(0);

		// Creating the agent
		MockAgent agent = new MockAgent(fundamental, sip, market);

		// Creating and adding the bid
		market.submitOrder(agent, new Price(1), 1, time);

		Collection<Order> orders = market.orderMapping.values();
		assertFalse(orders.isEmpty());
		Order order = Iterables.getFirst(orders, null);
		assertEquals(new Price(1), order.getPrice());
		assertEquals(1, order.getQuantity());
		assertEquals(time, order.getSubmitTime());
		assertEquals(agent, order.getAgent());
		assertEquals(market, order.getMarket());
		
		// Check if market quote correct // TODO quantities failing currently
		market.updateQuote(ImmutableList.<Transaction> of(), time);
		Quote q = market.quote;
		assertTrue("Incorrect ASK", q.ask == null);
		assertTrue("Incorrect BID", q.bid.equals(new Price(1)));
		assertTrue("Incorrect ASK quantity", q.askQuantity == 0);
		assertTrue("Incorrect BID quantity", q.bidQuantity == 1);
	}

	@Test
	public void addAsk() {
		TimeStamp time = new TimeStamp(0);
		
		//Creating the agent
		MockAgent agent = new MockAgent(fundamental, sip, market);
		
		// Creating and adding the bid
		market.submitOrder(agent, new Price(1), -1, time);

		Collection<Order> orders = market.orderMapping.values();
		assertFalse(orders.isEmpty());
		Order order = Iterables.getFirst(orders, null);
		assertEquals(new Price(1), order.getPrice());
		assertEquals(-1, order.getQuantity());
		assertEquals(time, order.getSubmitTime());
		assertEquals(agent, order.getAgent());
		assertEquals(market, order.getMarket());
		
		// Check if market quote correct
		market.updateQuote(ImmutableList.<Transaction> of(), time);
		Quote q = market.quote;
		assertTrue("Incorrect ASK", q.ask.equals(new Price(1)));
		assertTrue("Incorrect BID", q.bid == null);
		assertTrue("Incorrect ASK quantity", q.askQuantity == 1);
		assertTrue("Incorrect BID quantity", q.bidQuantity == 0);
	}
	
	@Test
	public void selfTrade() {
		// Verify that agent can't trade with itself
		TimeStamp time0 = TimeStamp.ZERO;
		
		MockAgent agent1 = new MockAgent(fundamental, sip, market);
		
		market.submitOrder(agent1, new Price(100),-1, time0);
		market.submitOrder(agent1, new Price(150), 1, time0);
		market.clear(time0);
		
		assertTrue(market.getTransactions().size() == 0);
	}

	@Test
	public void basicEqualClear() {
		TimeStamp time = new TimeStamp(0);
		
		//Creating dummy agents
		MockAgent agent1 = new MockAgent(fundamental, sip, market);
		MockAgent agent2 = new MockAgent(fundamental, sip, market);
		

		// Creating and adding bids
		market.submitOrder(agent1, new Price(100), 1, time);
		market.submitOrder(agent2, new Price(100), -1, time);

		// Testing the market for the correct transaction
		market.clear(time);
		assertTrue(market.getTransactions().size() == 1);
		for (Transaction tr : market.getTransactions()) {
			assertTrue("Incorrect Buyer", tr.getBuyer().equals(agent1));
			assertTrue("Incorrect Seller", tr.getSeller().equals(agent2));
			assertTrue("Incorrect Price", tr.getPrice().equals(new Price(100)));
			assertTrue("Incorrect Quantity", tr.getQuantity() == 1);
		}
	}

	@Test
	public void basicOverlapClear() {
		TimeStamp time = new TimeStamp(0);
		TimeStamp time2 = new TimeStamp(1);
		
		//Creating dummy agents
		MockAgent agent1 = new MockAgent(fundamental, sip, market);
		MockAgent agent2 = new MockAgent(fundamental, sip, market);
		
		// Creating and adding bids
		market.submitOrder(agent1, new Price(200), 1, time);
		market.submitOrder(agent2, new Price(50), -1, time2);

		// Testing the market for the correct transaction
		market.clear(time2);
		assertTrue(market.getTransactions().size() == 1);
		for (Transaction tr : market.getTransactions()) {
			assertTrue("Incorrect Buyer", tr.getBuyer().equals(agent1));
			assertTrue("Incorrect Seller", tr.getSeller().equals(agent2));
			assertTrue("Incorrect Price", tr.getPrice().equals(new Price(200)));
			assertTrue("Incorrect Quantity", tr.getQuantity() == 1);
		}
	}

	@Test
	public void multiBidSingleClear() {
		TimeStamp time = TimeStamp.ZERO;
		
		//Creating dummy agents
		MockAgent agent1 = new MockAgent(fundamental, sip, market);
		MockAgent agent2 = new MockAgent(fundamental, sip, market);
		MockAgent agent3 = new MockAgent(fundamental, sip, market);
		MockAgent agent4 = new MockAgent(fundamental, sip, market);
		
		// Creating and adding bids
		market.submitOrder(agent1, new Price(150), 1, time);
		market.submitOrder(agent2, new Price(100), 1, time);
		market.submitOrder(agent3, new Price(175),-1, time);
		market.submitOrder(agent4, new Price(125),-1, time);
		market.clear(time);
		
		// Testing the market for the correct transactions
		assertTrue(market.getTransactions().size() == 1);
		Transaction tr = market.getTransactions().get(0);
		assertTrue("Incorrect Buyer", tr.getBuyer().equals(agent1));
		assertTrue("Incorrect Seller", tr.getSeller().equals(agent4));
		assertTrue("Incorrect Price", tr.getPrice().equals(new Price(125)));
		// TODO what price to clear at when the two orders at diff prices at same time? Right now always seller's price
		assertTrue("Incorrect Quantity", tr.getQuantity() == 1);
		
		// Creating and adding bids
		market.submitOrder(agent1, new Price(150), 1, time.plus(new TimeStamp(1)));
		market.submitOrder(agent4, new Price(75), -1, time.plus(new TimeStamp(2)));
		market.clear(time.plus(new TimeStamp(2)));
		
		// Testing the market for the correct transactions
		assertTrue(market.getTransactions().size() == 2);
		tr = market.getTransactions().get(1);
		assertTrue("Incorrect Buyer", tr.getBuyer().equals(agent1));
		assertTrue("Incorrect Seller", tr.getSeller().equals(agent4));
		assertTrue("Incorrect Price", tr.getPrice().equals(new Price(150)));
		assertTrue("Incorrect Quantity", tr.getQuantity() == 1);
	}
	
	@Test
	public void multiOverlapClear() {
		TimeStamp time = TimeStamp.ZERO;
		
		MockAgent agent1 = new MockAgent(fundamental, sip, market);
		MockAgent agent2 = new MockAgent(fundamental, sip, market);
		MockAgent agent3 = new MockAgent(fundamental, sip, market);
		MockAgent agent4 = new MockAgent(fundamental, sip, market);
		
		// Creating and adding bids
		// Added for-loop so market clear will happen appropriately
		// Note: A clear should be inserted after EVERY order submitted
		Iterable<? extends Activity> bidActs = 
				market.submitOrder(agent1, new Price(150), 1, time);
		for (Activity act : bidActs) if (act instanceof Clear) act.execute(time);
		bidActs = market.submitOrder(agent2, new Price(100),-1, time);
		for (Activity act : bidActs) if (act instanceof Clear) act.execute(time);
		bidActs = market.submitOrder(agent3, new Price(175), 1, time);
		for (Activity act : bidActs) if (act instanceof Clear) act.execute(time);
		bidActs = market.submitOrder(agent4, new Price(125),-1, time);
		for (Activity act : bidActs) if (act instanceof Clear) act.execute(time);
		
		// Testing the market for the correct transactions
		assertTrue(market.getTransactions().size() == 2);
		Transaction tr = market.getTransactions().get(0);
		assertTrue("Incorrect Buyer", tr.getBuyer().equals(agent1));
		assertTrue("Incorrect Seller", tr.getSeller().equals(agent2));
		assertTrue("Incorrect Price", tr.getPrice().equals(new Price(100))); // TODO should clear at 150 actually (incumbent order)
		assertTrue("Incorrect Quantity", tr.getQuantity() == 1);
		tr = market.getTransactions().get(1);
		assertTrue("Incorrect Buyer", tr.getBuyer().equals(agent3));
		assertTrue("Incorrect Seller", tr.getSeller().equals(agent4));
		assertTrue("Incorrect Price", tr.getPrice().equals(new Price(125))); // TODO should be 175 since first...
		assertTrue("Incorrect Quantity", tr.getQuantity() == 1);
	}
	
	@Test
	public void extraTest() {
		for(int i=0; i < 100; i++) {
			setup();
			multiBidSingleClear();
			setup();
			multiOverlapClear();
			setup();
			priceTies();
		}
	}
	
	
	/**
	 * Test quantities of partially transacted orders. 
	 */
	@Test
	public void partialQuantity() {
		TimeStamp time = TimeStamp.ZERO;
		TimeStamp time2 = new TimeStamp(1);
		
		MockAgent agent1 = new MockAgent(fundamental, sip, market);
		MockAgent agent2 = new MockAgent(fundamental, sip, market);
		
		market.submitOrder(agent1, new Price(100),-2, time);
		market.submitOrder(agent2, new Price(150), 5, time2);
		market.clear(time2);
		
		// Check that two units transact
		assertTrue(market.getTransactions().size() == 1);
		Transaction tr = market.getTransactions().get(0);
		assertTrue("Incorrect Buyer", tr.getBuyer().equals(agent2));
		assertTrue("Incorrect Seller", tr.getSeller().equals(agent1));
		assertTrue("Incorrect Price", tr.getPrice().equals(new Price(100)));
		assertTrue("Incorrect Quantity", tr.getQuantity() == 2);
		
		// Check that post-trade BID is correct (3 buy units at 150)
		market.updateQuote(ImmutableList.<Transaction> of(), time2);
		Quote q = market.quote;
		assertTrue("Incorrect ASK", q.ask == null);
		assertTrue("Incorrect BID", q.bid.equals(new Price(150)));
		assertTrue("Incorrect ASK quantity", q.askQuantity == 0);
		assertTrue("Incorrect BID quantity", q.bidQuantity == 3);
	}
	
	@Test
	public void multiQuantity() {
		TimeStamp time0 = TimeStamp.ZERO;
		TimeStamp time1 = new TimeStamp(1);

		MockAgent agent1 = new MockAgent(fundamental, sip, market);
		MockAgent agent2 = new MockAgent(fundamental, sip, market);
		
		market.submitOrder(agent1, new Price(150),-1, time0);
		market.submitOrder(agent1, new Price(140),-1, time0);
		market.clear(time0);

		// Both agents' sell orders should transact b/c partial quantity withdrawn
		market.submitOrder(agent2, new Price(155), 2, time1);
		market.clear(time1);	// TODO this throws a null pointer exception
		// on second loop the buy order is null, as it gets removed from the data structure
		assertTrue(market.getTransactions().size() == 2);
		Transaction tr = market.getTransactions().get(0);
		assertTrue("Incorrect Price", tr.getPrice().equals(new Price(140)));
		assertTrue("Incorrect Quantity", tr.getQuantity() == 1);
		tr = market.getTransactions().get(1);
		assertTrue("Incorrect Price", tr.getPrice().equals(new Price(150)));
		assertTrue("Incorrect Quantity", tr.getQuantity() == 1);
	}
	
	
	/**
	 * Tests order withdrawals. Via agent withdrawals & market withdrawals.
	 */
	@Test
	public void basicWithdraw() {
		TimeStamp time0 = TimeStamp.ZERO;
		TimeStamp time1 = new TimeStamp(1);
		TimeStamp time2 = new TimeStamp(2);
		
		MockAgent agent1 = new MockAgent(fundamental, sip, market);
		MockAgent agent2 = new MockAgent(fundamental, sip, market);
		
		market.submitOrder(agent1, new Price(100),-1, time0);
		market.clear(time0);
		Iterable<? extends Activity> acts = agent1.withdrawAllOrders();
		for (Activity a : acts) a.execute(time0);
		
		// Check that no transaction, because agent1 withdrew its order
		market.submitOrder(agent2, new Price(125), 1, time1);
		assertTrue(market.getTransactions().size() == 0);

		market.submitOrder(agent2, new Price(110), 1, time1);
		Collection<Order> orders = agent2.getOrders();
		Order toWithdraw = null;
		for (Order o : orders)
			if (o.getPrice().equals(new Price(125))) toWithdraw = o;
		market.withdrawOrder(toWithdraw, time1);
		market.clear(time1);
		
		// Check that it transacts at 110, price of order that was not withdrawn
		market.submitOrder(agent1, new Price(100),-1, time2);
		market.clear(time2);
		assertTrue(market.getTransactions().size() == 1);
		Transaction tr = market.getTransactions().get(0);
		assertTrue("Incorrect Price", tr.getPrice().equals(new Price(110)));
		assertTrue("Incorrect Quantity", tr.getQuantity() == 1);
	}
	
	@Test
	public void multiQuantityWithdraw() {
		TimeStamp time0 = TimeStamp.ZERO;
		TimeStamp time1 = new TimeStamp(1);

		MockAgent agent1 = new MockAgent(fundamental, sip, market);
		MockAgent agent2 = new MockAgent(fundamental, sip, market);
		
		market.submitOrder(agent1, new Price(150),-1, time0);
		market.submitOrder(agent1, new Price(140),-2, time0);
		Collection<Order> orders = agent1.getOrders();
		Order toWithdraw = null;
		for (Order o : orders)
			if (o.getPrice().equals(new Price(140))) toWithdraw = o;
		market.withdrawOrder(toWithdraw, -1, time0);
		market.clear(time0);

		// Both agents' sell orders should transact b/c partial quantity withdrawn
		market.submitOrder(agent2, new Price(155), 1, time1);
		market.clear(time1);
		market.submitOrder(agent2,  new Price(155), 1, time1);
		market.clear(time1);
		assertTrue(market.getTransactions().size() == 2);
		Transaction tr = market.getTransactions().get(0);
		assertTrue("Incorrect Price", tr.getPrice().equals(new Price(140)));
		assertTrue("Incorrect Quantity", tr.getQuantity() == 1);
		tr = market.getTransactions().get(1);
		assertTrue("Incorrect Price", tr.getPrice().equals(new Price(150)));
		assertTrue("Incorrect Quantity", tr.getQuantity() == 1);
	}
	
	
	/**
	 * Test clearing when there are ties in price. Should always match the 
	 * order that arrived first if there is a tie in price.
	 */
	@Test
	public void priceTies() {
		TimeStamp time0 = TimeStamp.ZERO;
		TimeStamp time1 = new TimeStamp(1);
		TimeStamp time2 = new TimeStamp(2);

		MockAgent agent0 = new MockAgent(fundamental, sip, market);
		MockAgent agent1 = new MockAgent(fundamental, sip, market);
		MockAgent agent2 = new MockAgent(fundamental, sip, market);
		MockAgent agent3 = new MockAgent(fundamental, sip, market);
		MockAgent agent4 = new MockAgent(fundamental, sip, market);
		
		market.submitOrder(agent1, new Price(100),-1, time0);
		market.submitOrder(agent2, new Price(100),-1, time1);
		market.submitOrder(agent3, new Price(150), 1, time1);
		market.clear(time1);
		
		// Check that earlier agent (agent1) is trading with agent3
		// Testing the market for the correct transactions
		assertTrue(market.getTransactions().size() == 1);
		Transaction tr = market.getTransactions().get(0);
		assertTrue("Incorrect Buyer", tr.getBuyer().equals(agent3));
		assertTrue("Incorrect Seller", tr.getSeller().equals(agent1));
		assertTrue("Incorrect Price", tr.getPrice().equals(new Price(100)));
		assertTrue("Incorrect Quantity", tr.getQuantity() == 1);
		
		market.submitOrder(agent1, new Price(100),-1, time1);
		market.submitOrder(agent3, new Price(100),-1, time2);
		market.submitOrder(agent4, new Price(100),-1, time2);
		market.clear(time2); // would be inserted onto Q, but hard-coded here
		market.submitOrder(agent0, new Price(125), 1, time2);
		market.clear(time2);
		
		// Check that the first submitted -1@100 transacts (from agent2)
		assertTrue(market.getTransactions().size() == 2);
		tr = market.getTransactions().get(1);
		assertTrue("Incorrect Buyer", tr.getBuyer().equals(agent0));
		assertTrue("Incorrect Seller", tr.getSeller().equals(agent2)); // TODO is this correct behavior? or should it be agent1?
		assertTrue("Incorrect Price", tr.getPrice().equals(new Price(100)));
		assertTrue("Incorrect Quantity", tr.getQuantity() == 1);
		
		// Let's try populating the market with random orders 
		// agent 1's order -1@100 at time1 remains
		// agent 3's order -1@100 at time2 remains
		// agent 4's order -1@100 at time2 remains
		market.submitOrder(agent0, new Price(95), -1, time2);
		market.submitOrder(agent0, new Price(100),-1, time2);
		market.submitOrder(agent0, new Price(110),-1, time2);
		market.submitOrder(agent0, new Price(115),-1, time2);
		market.submitOrder(agent0, new Price(90),  1, time2);
		market.submitOrder(agent0, new Price(85),  1, time2);
		market.submitOrder(agent0, new Price(80),  1, time2);
		market.clear(time2);
		assertTrue(market.getTransactions().size() == 2); // no change
		
		// Check basic overlap - between agent0 and agent2
		market.submitOrder(agent2, new Price(125), 1, time2);
		market.clear(time2);
		assertTrue(market.getTransactions().size() == 3);
		tr = market.getTransactions().get(2);
		assertTrue("Incorrect Buyer", tr.getBuyer().equals(agent2));
		assertTrue("Incorrect Seller", tr.getSeller().equals(agent0));
		assertTrue("Incorrect Price", tr.getPrice().equals(new Price(95)));
		assertTrue("Incorrect Quantity", tr.getQuantity() == 1);
		
		// Check that earliest time (agent1) at price 100 trades
		// Check that the transaction was between agent1 (earliest @100) and agent2
		market.submitOrder(agent2, new Price(105), 1, time2);
		market.clear(time2);
		assertTrue(market.getTransactions().size() == 4);
		tr = market.getTransactions().get(3);
		assertTrue("Incorrect Buyer", tr.getBuyer().equals(agent2));
		assertTrue("Incorrect Seller", tr.getSeller().equals(agent1));
		assertTrue("Incorrect Price", tr.getPrice().equals(new Price(100)));
		assertTrue("Incorrect Quantity", tr.getQuantity() == 1);
		
		// Check that next earliest (agent1) although same price & time trades (vs. agent4 or 0)
		// Check that the transaction was between agent1 (first submitted) and agent2
		// TODO why does agent4 trade before agent3
		market.submitOrder(agent2, new Price(105), 1, time2);
		market.clear(time2);
		assertTrue(market.getTransactions().size() == 5);
		tr = market.getTransactions().get(4);
		assertTrue("Incorrect Buyer", tr.getBuyer().equals(agent2));
		assertTrue("Incorrect Seller", tr.getSeller().equals(agent4));
		assertTrue("Incorrect Price", tr.getPrice().equals(new Price(100)));
		assertTrue("Incorrect Quantity", tr.getQuantity() == 1);
		
		// Check that next earliest (agent4) although same price & time trades (vs. agent0)
		// TODO why does agent0 trade before agent3?
		market.submitOrder(agent2, new Price(105), 1, time2);
		market.clear(time2);
		assertTrue(market.getTransactions().size() == 6);
		tr = market.getTransactions().get(5);
		assertTrue("Incorrect Buyer", tr.getBuyer().equals(agent2));
		assertTrue("Incorrect Seller", tr.getSeller().equals(agent0));
		assertTrue("Incorrect Price", tr.getPrice().equals(new Price(100)));
		assertTrue("Incorrect Quantity", tr.getQuantity() == 1);
		
		// Check that final order at -1@100 trades with agent2
		market.submitOrder(agent2, new Price(105), 1, time2);
		market.clear(time2);
		assertTrue(market.getTransactions().size() == 7);
		tr = market.getTransactions().get(6);
		assertTrue("Incorrect Buyer", tr.getBuyer().equals(agent2));
		assertTrue("Incorrect Seller", tr.getSeller().equals(agent3));
		assertTrue("Incorrect Price", tr.getPrice().equals(new Price(100)));
		assertTrue("Incorrect Quantity", tr.getQuantity() == 1);
	}	
	
	public void latencyTest() {
		// TODO check that market quotes are updating correctly
		// test one market where immediately, and one where not.
		
		// TODO test market where IP has a latency, test that bid/ask updated correctly
	}
}
