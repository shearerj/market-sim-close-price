package entity.infoproc;

import static fourheap.Order.OrderType.BUY;
import static fourheap.Order.OrderType.SELL;
import static logger.Log.log;
import static logger.Log.Level.DEBUG;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Random;

import logger.Log;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import systemmanager.Consts;
import systemmanager.Executor;
import activity.SubmitOrder;

import com.google.common.collect.ImmutableList;

import data.FundamentalValue;
import data.MockFundamental;
import entity.agent.MockBackgroundAgent;
import entity.market.CDAMarket;
import entity.market.Market;
import entity.market.Price;
import entity.market.Quote;
import entity.market.Transaction;
import event.TimeStamp;

public class TransactionProcessorTest {

	private Executor exec;
	private SIP sip;
	private FundamentalValue fundamental = new MockFundamental(100000);

	@BeforeClass
	public static void setupClass() throws IOException {
		log = Log.create(DEBUG, new File(Consts.TEST_OUTPUT_DIR + "TransactionProcessorTest.log"));
	}

	@Before
	public void setup() {
		exec = new Executor();
		sip = new SIP(exec, TimeStamp.IMMEDIATE);
	}

	@Test
	public void basicProcessTransaction() {
		TimeStamp time = TimeStamp.ZERO;
		Market market = new CDAMarket(exec, sip, new Random(), TimeStamp.IMMEDIATE, 1);
		AbstractTransactionProcessor smip = market.getTransactionProcessor();

		// Verify latency
		assertEquals(TimeStamp.IMMEDIATE, smip.getLatency());

		// Creating dummy agents
		MockBackgroundAgent agent1 = new MockBackgroundAgent(exec, fundamental, sip, market);
		MockBackgroundAgent agent2 = new MockBackgroundAgent(exec, fundamental, sip, market);

		// Check initial transaction list empty
		List<Transaction> trans = smip.getTransactions();
		assertTrue("Incorrect initial transaction list", trans.isEmpty());

		exec.executeActivity(new SubmitOrder(agent1, market, BUY, new Price(150), 1));
		exec.executeActivity(new SubmitOrder(agent2, market, SELL, new Price(140), 1));
		// should execute Clear-->SendToSIP-->processInformation

		smip.processTransactions(market, ImmutableList.<Transaction> of(), time);
		assertTrue(exec.isEmpty());

		// Verify that transactions have updated
		trans = smip.getTransactions();
		assertEquals("Incorrect number of transactions", 1, trans.size());
		assertEquals("Incorrect transaction price", new Price(150), trans.get(0).getPrice());
		assertEquals("Incorrect transaction quantity", 1, trans.get(0).getQuantity());
		assertEquals("Incorrect buyer", agent1, trans.get(0).getBuyer());
		assertEquals("Incorrect buyer", agent2, trans.get(0).getSeller());
	}

	@Test
	public void basicDelayProcessTransaction() {
		TimeStamp time = TimeStamp.ZERO;
		Market market = new CDAMarket(exec, sip, new Random(), TimeStamp.create(100), 1);
		AbstractTransactionProcessor smip = market.getTransactionProcessor();

		// Verify latency
		assertEquals(TimeStamp.create(100), smip.latency);

		// Creating dummy agents
		MockBackgroundAgent agent1 = new MockBackgroundAgent(exec, fundamental, sip, market);
		MockBackgroundAgent agent2 = new MockBackgroundAgent(exec, fundamental, sip, market);

		// Check initial transaction list empty
		List<Transaction> trans = smip.getTransactions();
		assertTrue("Incorrect initial transaction list", trans.isEmpty());

		exec.executeActivity(new SubmitOrder(agent1, market, BUY, new Price(150), 1));
		exec.executeActivity(new SubmitOrder(agent2, market, SELL, new Price(140), 1));
		// should execute Clear-->SendToSIP-->processInformation

		smip.processTransactions(market, ImmutableList.<Transaction> of(), time);

		// Still haven't updated transactions list yet
		trans = smip.getTransactions();
		assertTrue("Incorrect transaction list size", trans.isEmpty());

		// Verify that transactions have updated
		exec.executeUntil(TimeStamp.create(101));
		trans = smip.getTransactions();
		assertEquals("Incorrect number of transactions", 1, trans.size());
		assertEquals("Incorrect transaction price", new Price(150), trans.get(0).getPrice());
		assertEquals("Incorrect transaction quantity", 1, trans.get(0).getQuantity());
		assertEquals("Incorrect buyer", agent1, trans.get(0).getBuyer());
		assertEquals("Incorrect buyer", agent2, trans.get(0).getSeller());
	}

	/**
	 * Test for when quote latency != transaction latency
	 */
	@Test
	public void diffQuoteTransLatency() {
		TimeStamp time = TimeStamp.ZERO;
		Market market = new CDAMarket(exec, sip, new Random(), TimeStamp.IMMEDIATE, 
				TimeStamp.create(100), 1);
		AbstractTransactionProcessor smip = market.getTransactionProcessor();
		AbstractQuoteProcessor qp = market.getQuoteProcessor();

		// Verify latency
		assertEquals(TimeStamp.IMMEDIATE, qp.latency);
		assertEquals(TimeStamp.create(100), smip.latency);

		// Creating dummy agents
		MockBackgroundAgent agent1 = new MockBackgroundAgent(exec, fundamental, sip, market);
		MockBackgroundAgent agent2 = new MockBackgroundAgent(exec, fundamental, sip, market);

		// Check initial transaction list empty, quote null
		List<Transaction> trans = smip.getTransactions();
		assertTrue("Incorrect initial transaction list", trans.isEmpty());
		Quote q = qp.quote;
		assertEquals("Incorrect last quote time", TimeStamp.ZERO, qp.getQuote().getQuoteTime());
		assertEquals("Incorrect ASK", null, q.getAskPrice());
		assertEquals("Incorrect BID", null, q.getBidPrice());
		assertEquals("Incorrect ASK quantity", 0, q.getAskQuantity());
		assertEquals("Incorrect BID quantity", 0, q.getBidQuantity());

		// Creating and adding bids
		exec.executeActivity(new SubmitOrder(agent1, market, BUY, new Price(150), 2));

		// Verify that quote has updated
		q = qp.quote;
		assertEquals("Incorrect last quote time", time, qp.getQuote().getQuoteTime());
		assertEquals("Incorrect ASK", null, q.getAskPrice());
		assertEquals("Incorrect BID", new Price(150), q.getBidPrice());
		assertEquals("Incorrect ASK quantity", 0, q.getAskQuantity());
		assertEquals("Incorrect BID quantity", 2, q.getBidQuantity());
		
		// Check transaction list still empty
		trans = smip.getTransactions();
		assertTrue("Incorrect transaction list size", trans.isEmpty());

		// Submit another order
		exec.executeActivity(new SubmitOrder(agent2, market, SELL, new Price(140), 1));

		// Verify quote update, but not transactions
		trans = smip.getTransactions();
		assertTrue("Incorrect transaction list size", trans.isEmpty());
		q = qp.quote;
		assertEquals("Incorrect last quote time", time, qp.getQuote().getQuoteTime());
		assertEquals("Incorrect ASK", null, q.getAskPrice());
		assertEquals("Incorrect BID", new Price(150), q.getBidPrice());
		assertEquals("Incorrect ASK quantity", 0, q.getAskQuantity());
		assertEquals("Incorrect BID quantity", 1, q.getBidQuantity());

		// Verify that transactions have updated as well as NBBO
		exec.executeUntil(TimeStamp.create(101));
		trans = smip.getTransactions();
		assertEquals("Incorrect number of transactions", 1, trans.size());
		assertEquals("Incorrect transaction price", new Price(150), trans.get(0).getPrice());
		assertEquals("Incorrect transaction quantity", 1, trans.get(0).getQuantity());
		assertEquals("Incorrect buyer", agent1, trans.get(0).getBuyer());
		assertEquals("Incorrect buyer", agent2, trans.get(0).getSeller());	
	}

	// XXX will the transaction processor ever receive redundant/duplicate transactions?
}
