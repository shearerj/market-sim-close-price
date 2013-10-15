package entity.market;

import static org.junit.Assert.*;

import java.io.File;
import java.util.Collection;
import java.util.Random;

import logger.Logger;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import systemmanager.EventManager;

import activity.Activity;
import activity.Clear;
import activity.SubmitOrder;

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

	@BeforeClass
	public static void setupClass() {
		Logger.setup(3, new File("simulations/unit_testing/CDAMarketTest.log"));
	}

	@Before
	public void setup() {
		sip = new SIP(TimeStamp.IMMEDIATE);
		market = new CDAMarket(sip, TimeStamp.IMMEDIATE, new Random(), 1);
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
		assertEquals("Incorrect ASK",  null,  q.ask );
		assertEquals("Incorrect BID", new Price(1),  q.bid);
		assertEquals("Incorrect ASK quantity",  0,  q.askQuantity );
		assertEquals("Incorrect BID quantity",  1,  q.bidQuantity );
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
		assertEquals("Incorrect ASK", new Price(1),  q.ask);
		assertEquals("Incorrect BID",  null,  q.bid );
		assertEquals("Incorrect ASK quantity",  1,  q.askQuantity );
		assertEquals("Incorrect BID quantity",  0,  q.bidQuantity );
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
			assertEquals("Incorrect Buyer", agent1, tr.getBuyer());
			assertEquals("Incorrect Seller", agent2, tr.getSeller());
			assertEquals("Incorrect Price", new Price(100), tr.getPrice());
			assertEquals("Incorrect Quantity", 1, tr.getQuantity());
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
			assertEquals("Incorrect Buyer", agent1, tr.getBuyer());
			assertEquals("Incorrect Seller", agent2, tr.getSeller());
			assertEquals("Incorrect Price", new Price(200), tr.getPrice());
			assertEquals("Incorrect Quantity", 1, tr.getQuantity());
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
		market.submitOrder(agent3, new Price(175), -1, time);
		market.submitOrder(agent4, new Price(125), -1, time);
		market.clear(time);

		// Testing the market for the correct transactions
		assertTrue(market.getTransactions().size() == 1);
		Transaction tr = market.getTransactions().get(0);
		assertEquals("Incorrect Buyer", agent1, tr.getBuyer());
		assertEquals("Incorrect Seller", agent4, tr.getSeller());
		assertEquals("Incorrect Price", new Price(125), tr.getPrice());
		// TODO what price to clear at when the two orders at diff prices at same time? Right now always seller's price
		assertEquals("Incorrect Quantity", 1, tr.getQuantity());
		
		// Creating and adding bids
		market.submitOrder(agent1, new Price(150), 1, time.plus(new TimeStamp(1)));
		market.submitOrder(agent4, new Price(75), -1, time.plus(new TimeStamp(2)));
		market.clear(time.plus(new TimeStamp(2)));
		
		// Testing the market for the correct transactions
		assertTrue(market.getTransactions().size() == 2);
		tr = market.getTransactions().get(1);
		assertEquals("Incorrect Buyer", agent1, tr.getBuyer());
		assertEquals("Incorrect Seller", agent4, tr.getSeller());
		assertEquals("Incorrect Price", new Price(150), tr.getPrice());
		assertEquals("Incorrect Quantity", 1, tr.getQuantity());
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
		Iterable<? extends Activity> bidActs = market.submitOrder(agent1, new Price(150), 1, time);
		for (Activity act : bidActs)
			if (act instanceof Clear) act.execute(time);
		bidActs = market.submitOrder(agent2, new Price(100), -1, time);
		for (Activity act : bidActs)
			if (act instanceof Clear) act.execute(time);
		bidActs = market.submitOrder(agent3, new Price(175), 1, time);
		for (Activity act : bidActs)
			if (act instanceof Clear) act.execute(time);
		bidActs = market.submitOrder(agent4, new Price(125), -1, time);
		for (Activity act : bidActs)
			if (act instanceof Clear) act.execute(time);
		
		// Testing the market for the correct transactions
		assertTrue(market.getTransactions().size() == 2);
		Transaction tr = market.getTransactions().get(0);
		assertEquals("Incorrect Buyer", agent1, tr.getBuyer());
		assertEquals("Incorrect Seller", agent2, tr.getSeller());
		assertEquals("Incorrect Price", new Price(100), tr.getPrice()); // TODO should clear at 150 actually (incumbent order)
		assertEquals("Incorrect Quantity", 1, tr.getQuantity());
		tr = market.getTransactions().get(1);
		assertEquals("Incorrect Buyer", agent3, tr.getBuyer());
		assertEquals("Incorrect Seller", agent4, tr.getSeller());
		assertEquals("Incorrect Price", new Price(125), tr.getPrice()); // TODO should be 175 since first...
		assertEquals("Incorrect Quantity", 1, tr.getQuantity());
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
		
		market.submitOrder(agent1, new Price(100), -2, time);
		market.submitOrder(agent2, new Price(150), 5, time2);
		market.clear(time2);
		
		// Check that two units transact
		assertTrue(market.getTransactions().size() == 1);
		Transaction tr = market.getTransactions().get(0);
		assertEquals("Incorrect Buyer", agent2, tr.getBuyer());
		assertEquals("Incorrect Seller", agent1, tr.getSeller());
		assertEquals("Incorrect Price", new Price(100), tr.getPrice());
		assertEquals("Incorrect Quantity", 2, tr.getQuantity());

		// Check that post-trade BID is correct (3 buy units at 150)
		market.updateQuote(ImmutableList.<Transaction> of(), time2);
		Quote q = market.quote;
		assertEquals("Incorrect ASK", null, q.ask);
		assertEquals("Incorrect BID", new Price(150), q.bid);
		assertEquals("Incorrect ASK quantity", 0, q.askQuantity);
		assertEquals("Incorrect BID quantity", 3, q.bidQuantity);
	}
	
	@Test
	public void multiQuantity() {
		TimeStamp time0 = TimeStamp.ZERO;
		TimeStamp time1 = new TimeStamp(1);

		MockAgent agent1 = new MockAgent(fundamental, sip, market);
		MockAgent agent2 = new MockAgent(fundamental, sip, market);

		market.submitOrder(agent1, new Price(150), -1, time0);
		market.submitOrder(agent1, new Price(140), -1, time0);
		market.clear(time0);

		// Both agents' sell orders should transact b/c partial quantity withdrawn
		market.submitOrder(agent2, new Price(155), 2, time1);
		market.clear(time1);	// TODO this throws a null pointer exception
		// on second loop the buy order is null, as it gets removed from the data structure
		assertTrue(market.getTransactions().size() == 2);
		Transaction tr = market.getTransactions().get(0);
		assertEquals("Incorrect Price", new Price(140), tr.getPrice());
		assertEquals("Incorrect Quantity", 1, tr.getQuantity());
		tr = market.getTransactions().get(1);
		assertEquals("Incorrect Price", new Price(150), tr.getPrice());
		assertEquals("Incorrect Quantity", 1, tr.getQuantity());
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

		market.submitOrder(agent1, new Price(100), -1, time0);
		market.clear(time0);
		Iterable<? extends Activity> acts = agent1.withdrawAllOrders();
		for (Activity a : acts)
			a.execute(time0);
		
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
		market.submitOrder(agent1, new Price(100), -1, time2);
		market.clear(time2);
		assertTrue(market.getTransactions().size() == 1);
		Transaction tr = market.getTransactions().get(0);
		assertEquals("Incorrect Price", new Price(110), tr.getPrice());
		assertEquals("Incorrect Quantity", 1, tr.getQuantity());
	}
	
	@Test
	public void multiQuantityWithdraw() {
		TimeStamp time0 = TimeStamp.ZERO;
		TimeStamp time1 = new TimeStamp(1);

		MockAgent agent1 = new MockAgent(fundamental, sip, market);
		MockAgent agent2 = new MockAgent(fundamental, sip, market);
		
		market.submitOrder(agent1, new Price(150), -1, time0);
		market.submitOrder(agent1, new Price(140), -2, time0);
		Collection<Order> orders = agent1.getOrders();
		Order toWithdraw = null;
		for (Order o : orders)
			if (o.getPrice().equals(new Price(140))) toWithdraw = o;
		market.withdrawOrder(toWithdraw, -1, time0);
		market.clear(time0);

		// Both agents' sell orders should transact b/c partial quantity withdrawn
		market.submitOrder(agent2, new Price(155), 1, time1);
		market.clear(time1);
		market.submitOrder(agent2, new Price(155), 1, time1);
		market.clear(time1);
		assertTrue(market.getTransactions().size() == 2);
		Transaction tr = market.getTransactions().get(0);
		assertEquals("Incorrect Price", new Price(140), tr.getPrice());
		assertEquals("Incorrect Quantity", 1, tr.getQuantity());
		tr = market.getTransactions().get(1);
		assertEquals("Incorrect Price", new Price(150), tr.getPrice());
		assertEquals("Incorrect Quantity", 1, tr.getQuantity());
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
		
		market.submitOrder(agent1, new Price(100), -1, time0);
		market.submitOrder(agent2, new Price(100), -1, time1);
		market.submitOrder(agent3, new Price(150), 1, time1);
		market.clear(time1);
		
		// Check that earlier agent (agent1) is trading with agent3
		// Testing the market for the correct transactions
		assertTrue(market.getTransactions().size() == 1);
		Transaction tr = market.getTransactions().get(0);
		assertEquals("Incorrect Buyer", agent3, tr.getBuyer());
		assertEquals("Incorrect Seller", agent1, tr.getSeller());
		assertEquals("Incorrect Price", new Price(100), tr.getPrice());
		assertEquals("Incorrect Quantity", 1, tr.getQuantity());

		market.submitOrder(agent1, new Price(100), -1, time1);
		market.submitOrder(agent3, new Price(100), -1, time2);
		market.submitOrder(agent4, new Price(100), -1, time2);
		market.clear(time2); // would be inserted onto Q, but hard-coded here
		market.submitOrder(agent0, new Price(125), 1, time2);
		market.clear(time2);
		
		// Check that the first submitted -1@100 transacts (from agent2)
		assertTrue(market.getTransactions().size() == 2);
		tr = market.getTransactions().get(1);
		assertEquals("Incorrect Buyer", agent0,  tr.getBuyer());
		assertEquals("Incorrect Seller", agent2,  tr.getSeller()); // TODO is this correct behavior? or should it be agent1?
		assertEquals("Incorrect Price", new Price(100), tr.getPrice());
		assertEquals("Incorrect Quantity", 1, tr.getQuantity());
		
		// Let's try populating the market with random orders 
		// agent 1's order -1@100 at time1 remains
		// agent 3's order -1@100 at time2 remains
		// agent 4's order -1@100 at time2 remains
		market.submitOrder(agent0, new Price(95), -1, time2);
		market.submitOrder(agent0, new Price(100), -1, time2);
		market.submitOrder(agent0, new Price(110), -1, time2);
		market.submitOrder(agent0, new Price(115), -1, time2);
		market.submitOrder(agent0, new Price(90), 1, time2);
		market.submitOrder(agent0, new Price(85), 1, time2);
		market.submitOrder(agent0, new Price(80), 1, time2);
		market.clear(time2);
		assertTrue(market.getTransactions().size() == 2); // no change

		// Check basic overlap - between agent0 and agent2
		market.submitOrder(agent2, new Price(125), 1, time2);
		market.clear(time2);
		assertTrue(market.getTransactions().size() == 3);
		tr = market.getTransactions().get(2);
		assertEquals("Incorrect Buyer", agent2, tr.getBuyer());
		assertEquals("Incorrect Seller", agent0, tr.getSeller());
		assertEquals("Incorrect Price", new Price(95), tr.getPrice());
		assertEquals("Incorrect Quantity", 1, tr.getQuantity());
		
		// Check that earliest time (agent1) at price 100 trades
		// Check that the transaction was between agent1 (earliest @100) and agent2
		market.submitOrder(agent2, new Price(105), 1, time2);
		market.clear(time2);
		assertTrue(market.getTransactions().size() == 4);
		tr = market.getTransactions().get(3);
		assertEquals("Incorrect Buyer", agent2, tr.getBuyer());
		assertEquals("Incorrect Seller", agent1, tr.getSeller());
		assertEquals("Incorrect Price", new Price(100), tr.getPrice());
		assertEquals("Incorrect Quantity", 1, tr.getQuantity());
		
		// Check that next earliest (agent1) although same price & time trades (vs. agent4 or 0)
		// Check that the transaction was between agent1 (first submitted) and agent2
		// TODO why does agent4 trade before agent3
		market.submitOrder(agent2, new Price(105), 1, time2);
		market.clear(time2);
		assertTrue(market.getTransactions().size() == 5);
		tr = market.getTransactions().get(4);
		assertEquals("Incorrect Buyer", agent2, tr.getBuyer());
		assertEquals("Incorrect Seller", agent4, tr.getSeller());
		assertEquals("Incorrect Price", new Price(100), tr.getPrice());
		assertEquals("Incorrect Quantity", 1, tr.getQuantity());
		
		// Check that next earliest (agent4) although same price & time trades (vs. agent0)
		// TODO why does agent0 trade before agent3?
		market.submitOrder(agent2, new Price(105), 1, time2);
		market.clear(time2);
		assertTrue(market.getTransactions().size() == 6);
		tr = market.getTransactions().get(5);
		assertEquals("Incorrect Buyer", agent2, tr.getBuyer());
		assertEquals("Incorrect Seller", agent0, tr.getSeller());
		assertEquals("Incorrect Price", new Price(100), tr.getPrice());
		assertEquals("Incorrect Quantity", 1, tr.getQuantity());
		
		// Check that final order at -1@100 trades with agent2
		market.submitOrder(agent2, new Price(105), 1, time2);
		market.clear(time2);
		assertTrue(market.getTransactions().size() == 7);
		tr = market.getTransactions().get(6);
		assertEquals("Incorrect Buyer", agent2, tr.getBuyer());
		assertEquals("Incorrect Seller", agent3, tr.getSeller());
		assertEquals("Incorrect Price", new Price(100), tr.getPrice());
		assertEquals("Incorrect Quantity", 1, tr.getQuantity());
	}
	
	@Test
	public void lackOfLatencyTest() {
		Quote quote;
		EventManager em = new EventManager(new Random());

		// Forces execution of execution but none of the resulting activities
		MockAgent agent = new MockAgent(fundamental, sip, market);
		Iterable<? extends Activity> acts = market.submitOrder(agent, new Price(100), -1, TimeStamp.ZERO);
		for (Activity a : acts)
			em.addActivity(a);
		
		quote = market.getSMIP().getQuote();
		assertEquals("Updated Ask price too early", null, quote.getAskPrice());
		assertEquals("Updated Ask quantity too early", 0, quote.getAskQuantity());
		assertEquals("Incorrect Bid price initialization", null, quote.getBidPrice());
		assertEquals("Incorrect Bid quantity initialization", 0, quote.getBidQuantity());
		
		// This will execute all of the remaining activities
		em.executeUntil(TimeStamp.ZERO);
		
		quote = market.getSMIP().getQuote();
		assertEquals("Didn't update ask price", new Price(100), quote.getAskPrice());
		assertEquals("Didn't update ask quantity", 1, quote.getAskQuantity());
		assertEquals("Changed bid price unnecessarily", null, quote.getBidPrice());
		assertEquals("Changed bid quantity unnecessarily", 0, quote.getBidQuantity());
	}
	
	@Test
	public void latencyTest() {
		Quote quote;
		EventManager em = new EventManager(new Random());
		CDAMarket market = new CDAMarket(sip, new TimeStamp(100), new Random(), 1);

		// Test that before Time 100 nothing has been updated
		MockAgent agent = new MockAgent(fundamental, sip, market);
		em.addActivity(new SubmitOrder(agent, market, new Price(100), -1, TimeStamp.ZERO));
		em.executeUntil(new TimeStamp(100));
		
		quote = market.getSMIP().getQuote();
		assertEquals("Updated Ask price too early", null, quote.getAskPrice());
		assertEquals("Updated Ask quantity too early", 0, quote.getAskQuantity());
		assertEquals("Incorrect Bid price initialization", null, quote.getBidPrice());
		assertEquals("Incorrect Bid quantity initialization", 0, quote.getBidQuantity());
		
		// Test that after 100 they did get updated
		em.executeUntil(new TimeStamp(101));
		
		quote = market.getSMIP().getQuote();
		assertEquals("Didn't update ask price", new Price(100), quote.getAskPrice());
		assertEquals("Didn't update ask quantity", 1, quote.getAskQuantity());
		assertEquals("Changed bid price unnecessarily", null, quote.getBidPrice());
		assertEquals("Changed bid quantity unnecessarily", 0, quote.getBidQuantity());
	}
	
	@Test
	public void todo() {
		// TODO check that market quotes are updating correctly
		// test one market where immediately, and one where not.
		
		// TODO test market where IP has a latency, test that bid/ask updated correctly
	}
}
