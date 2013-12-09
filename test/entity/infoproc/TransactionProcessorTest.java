package entity.infoproc;

import static fourheap.Order.OrderType.BUY;
import static fourheap.Order.OrderType.SELL;
import static org.junit.Assert.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import logger.Logger;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import activity.Activity;
import activity.SubmitOrder;

import com.google.common.collect.Iterables;

import data.FundamentalValue;
import data.MockFundamental;
import systemmanager.Consts;
import systemmanager.EventManager;
import entity.agent.MockBackgroundAgent;
import entity.market.CDAMarket;
import entity.market.Market;
import entity.market.Price;
import entity.market.Quote;
import entity.market.Transaction;
import event.TimeStamp;

public class TransactionProcessorTest {

	private SIP sip;
	private FundamentalValue fundamental;

	@BeforeClass
	public static void setupClass() {
		Logger.setup(3, new File(Consts.TEST_OUTPUT_DIR + "TransactionProcessorTest.log"));
	}

	@Before
	public void setup() {
		fundamental = new MockFundamental(100000);
		sip = new SIP(TimeStamp.IMMEDIATE);
	}

	@Test
	public void basicProcessTransaction() {
		TimeStamp time = TimeStamp.ZERO;
		TimeStamp time1 = new TimeStamp(1);
		Market market = new CDAMarket(sip, TimeStamp.IMMEDIATE, new Random(), 1);
		TransactionProcessor smip = market.getTransactionProcessor();

		// Verify latency
		assertEquals(TimeStamp.IMMEDIATE, smip.latency);

		// Creating dummy agents
		MockBackgroundAgent agent1 = new MockBackgroundAgent(fundamental, sip, market);
		MockBackgroundAgent agent2 = new MockBackgroundAgent(fundamental, sip, market);

		// Check initial transaction list empty
		List<Transaction> trans = smip.getTransactions();
		assertTrue("Incorrect initial transaction list", trans.isEmpty());

		// Creating and adding bids		
		EventManager em = new EventManager(new Random());
		em.addActivity(new SubmitOrder(agent1, market, BUY, new Price(150), 1, time));
		em.executeUntil(time1);
		em.addActivity(new SubmitOrder(agent2, market, SELL, new Price(140), 1, time));
		em.executeUntil(time1); // should execute Clear-->SendToSIP-->processInformation

		Iterable<? extends Activity> acts = smip.processTransaction(market,
				new ArrayList<Transaction>(), time);
		assertEquals(0, Iterables.size(acts));

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
		TimeStamp time1 = new TimeStamp(1);
		Market market = new CDAMarket(sip, new TimeStamp(100), new Random(), 1);
		TransactionProcessor smip = market.getTransactionProcessor();

		// Verify latency
		assertEquals(new TimeStamp(100), smip.latency);

		// Creating dummy agents
		MockBackgroundAgent agent1 = new MockBackgroundAgent(fundamental, sip, market);
		MockBackgroundAgent agent2 = new MockBackgroundAgent(fundamental, sip, market);

		// Check initial transaction list empty
		List<Transaction> trans = smip.getTransactions();
		assertTrue("Incorrect initial transaction list", trans.isEmpty());

		// Creating and adding bids		
		EventManager em = new EventManager(new Random());
		em.addActivity(new SubmitOrder(agent1, market, BUY, new Price(150), 1, time));
		em.executeUntil(time1);
		em.addActivity(new SubmitOrder(agent2, market, SELL, new Price(140), 1, time));
		em.executeUntil(time1); // should execute Clear-->SendToSIP-->processInformation

		Iterable<? extends Activity> acts = smip.processTransaction(market,
				new ArrayList<Transaction>(), time);
		assertEquals(0, Iterables.size(acts));

		// Still haven't updated transactions list yet
		trans = smip.getTransactions();
		assertTrue("Incorrect transaction list size", trans.isEmpty());

		// Verify that transactions have updated
		em.executeUntil(new TimeStamp(101));
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
		TimeStamp time1 = new TimeStamp(1);
		Market market = new CDAMarket(sip, TimeStamp.IMMEDIATE, new TimeStamp(100), 
				new Random(), 1);
		TransactionProcessor smip = market.getTransactionProcessor();
		QuoteProcessor qp = market.getQuoteProcessor();

		// Verify latency
		assertEquals(TimeStamp.IMMEDIATE, qp.latency);
		assertEquals(new TimeStamp(100), smip.latency);

		// Creating dummy agents
		MockBackgroundAgent agent1 = new MockBackgroundAgent(fundamental, sip, market);
		MockBackgroundAgent agent2 = new MockBackgroundAgent(fundamental, sip, market);

		// Check initial transaction list empty, quote null
		List<Transaction> trans = smip.getTransactions();
		assertTrue("Incorrect initial transaction list", trans.isEmpty());
		Quote q = qp.quote;
		assertEquals("Incorrect last quote time", null, qp.lastQuoteTime);
		assertEquals("Incorrect ASK", null, q.getAskPrice());
		assertEquals("Incorrect BID", null, q.getBidPrice());
		assertEquals("Incorrect ASK quantity", 0, q.getAskQuantity());
		assertEquals("Incorrect BID quantity", 0, q.getBidQuantity());

		// Creating and adding bids		
		EventManager em = new EventManager(new Random());
		em.addActivity(new SubmitOrder(agent1, market, BUY, new Price(150), 2, time));
		em.executeUntil(time1);

		// Verify that quote has updated
		q = qp.quote;
		assertEquals("Incorrect last quote time", time, qp.lastQuoteTime);
		assertEquals("Incorrect ASK", null, q.getAskPrice());
		assertEquals("Incorrect BID", new Price(150), q.getBidPrice());
		assertEquals("Incorrect ASK quantity", 0, q.getAskQuantity());
		assertEquals("Incorrect BID quantity", 2, q.getBidQuantity());
		
		// Check transaction list still empty
		trans = smip.getTransactions();
		assertTrue("Incorrect transaction list size", trans.isEmpty());

		// Submit another order
		em.addActivity(new SubmitOrder(agent2, market, SELL, new Price(140), 1, time));
		em.executeUntil(time1);

		// Verify quote update, but not transactions
		trans = smip.getTransactions();
		assertTrue("Incorrect transaction list size", trans.isEmpty());
		q = qp.quote;
		assertEquals("Incorrect last quote time", time, qp.lastQuoteTime);
		assertEquals("Incorrect ASK", null, q.getAskPrice());
		assertEquals("Incorrect BID", new Price(150), q.getBidPrice());
		assertEquals("Incorrect ASK quantity", 0, q.getAskQuantity());
		assertEquals("Incorrect BID quantity", 1, q.getBidQuantity());

		// Verify that transactions have updated as well as NBBO
		em.executeUntil(new TimeStamp(101));
		trans = smip.getTransactions();
		assertEquals("Incorrect number of transactions", 1, trans.size());
		assertEquals("Incorrect transaction price", new Price(150), trans.get(0).getPrice());
		assertEquals("Incorrect transaction quantity", 1, trans.get(0).getQuantity());
		assertEquals("Incorrect buyer", agent1, trans.get(0).getBuyer());
		assertEquals("Incorrect buyer", agent2, trans.get(0).getSeller());	
	}

	// XXX will the transaction processor ever receive redundant/duplicate transactions?
}
