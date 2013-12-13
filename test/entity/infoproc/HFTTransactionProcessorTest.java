package entity.infoproc;

import static fourheap.Order.OrderType.BUY;
import static fourheap.Order.OrderType.SELL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import logger.Logger;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import activity.SubmitOrder;

import systemmanager.Consts;
import systemmanager.EventManager;
import data.FundamentalValue;
import data.MockFundamental;
import entity.agent.MockBackgroundAgent;
import entity.agent.MockHFTAgent;
import entity.market.CDAMarket;
import entity.market.Market;
import entity.market.Price;
import entity.market.Quote;
import entity.market.Transaction;
import event.TimeStamp;

public class HFTTransactionProcessorTest {

	private SIP sip;
	private FundamentalValue fundamental;
	private Market market1;
	private Market market2;
	private MockHFTAgent hft;
	private AbstractTransactionProcessor tp1;
	private AbstractTransactionProcessor tp2;

	@BeforeClass
	public static void setupClass() {
		Logger.setup(3, new File(Consts.TEST_OUTPUT_DIR + "HFTTransactionProcessorTest.log"));
	}

	@Before
	public void setup() {
		fundamental = new MockFundamental(100000);
		sip = new SIP(TimeStamp.IMMEDIATE);

		market1 = new CDAMarket(sip, new Random(), TimeStamp.IMMEDIATE, 1);
		market2 = new CDAMarket(sip, new Random(), new TimeStamp(100), 1);

		hft = new MockHFTAgent(TimeStamp.IMMEDIATE, fundamental, sip, 
				Arrays.asList(market1, market2));
	}

	@Test
	public void basicProcessTransaction() {
		TimeStamp time = TimeStamp.ZERO;
		TimeStamp time1 = new TimeStamp(1);

		tp1 = hft.getHFTTransactionProcessor(market1);
		tp2 = hft.getHFTTransactionProcessor(market2);
		
		// Verify latency
		assertEquals(TimeStamp.IMMEDIATE, tp1.latency);
		assertEquals(TimeStamp.IMMEDIATE, tp2.latency);

		// Creating dummy agents
		MockBackgroundAgent agent1 = new MockBackgroundAgent(fundamental, sip, market1);
		MockBackgroundAgent agent2 = new MockBackgroundAgent(fundamental, sip, market1);

		// Check initial transaction lists empty
		List<Transaction> trans = hft.getTransactions(market1);
		assertTrue("Incorrect initial transaction list", trans.isEmpty());
		trans = hft.getTransactions(market2);
		assertTrue("Incorrect initial transaction list", trans.isEmpty());

		// Creating and adding bids		
		EventManager em = new EventManager(new Random());
		em.addActivity(new SubmitOrder(agent1, market1, BUY, new Price(150), 1, time));
		em.executeUntil(time1);
		em.addActivity(new SubmitOrder(agent2, market1, SELL, new Price(140), 1, time));
		em.executeUntil(time1); // should execute Clear-->SendToSIP-->processTransactions

		// for now, HFTTransactionProcessors do NOT insert AgentStrategy activities
//		Iterable<? extends Activity> acts = tp1.processTransaction(market1,
//				new ArrayList<Transaction>(), time);
//		assertEquals(1, Iterables.size(acts));
//		assertEquals("Incorrect scheduled agent strategy time", TimeStamp.IMMEDIATE, 
//				Iterables.getFirst(acts, null).getTime());
//		assertTrue("Incorrect activity type scheduled", 
//				Iterables.getFirst(acts, null) instanceof AgentStrategy);

		// Verify that transactions have updated
		trans = hft.getTransactions(market1);
		assertEquals("Incorrect number of transactions", 1, trans.size());
		assertEquals("Incorrect transaction price", new Price(150), trans.get(0).getPrice());
		assertEquals("Incorrect transaction quantity", 1, trans.get(0).getQuantity());
		assertEquals("Incorrect buyer", agent1, trans.get(0).getBuyer());
		assertEquals("Incorrect buyer", agent2, trans.get(0).getSeller());
		trans = hft.getTransactions(market2);
		assertEquals("Incorrect number of transactions", 0, trans.size());
	}

	@Test
	public void basicDelayProcessTransaction() {
		TimeStamp time = TimeStamp.ZERO;
		TimeStamp time1 = new TimeStamp(1);

		// Creating dummy agents
		MockBackgroundAgent agent1 = new MockBackgroundAgent(fundamental, sip, market2);
		MockBackgroundAgent agent2 = new MockBackgroundAgent(fundamental, sip, market2);
		MockHFTAgent hft = new MockHFTAgent(new TimeStamp(100), fundamental, sip, 
				Arrays.asList(market1, market2));

		tp1 = hft.getHFTTransactionProcessor(market1);
		tp2 = hft.getHFTTransactionProcessor(market2);

		// Creating and adding bids		
		EventManager em = new EventManager(new Random());
		em.addActivity(new SubmitOrder(agent1, market2, BUY, new Price(150), 1, time));
		em.executeUntil(time1);
		em.addActivity(new SubmitOrder(agent2, market2, SELL, new Price(140), 1, time));
		em.executeUntil(time1); // should execute Clear-->SendToSIP-->processInformations

		// Verify no update yet
		List<Transaction> trans = hft.getTransactions(market2);
		assertEquals("Incorrect number of transactions", 0, trans.size());

		// Verify that transactions have updated
		em.executeUntil(new TimeStamp(101));
		trans = hft.getTransactions(market2);
		assertEquals("Incorrect number of transactions", 1, trans.size());
		assertEquals("Incorrect transaction price", new Price(150), trans.get(0).getPrice());
		assertEquals("Incorrect transaction quantity", 1, trans.get(0).getQuantity());
		assertEquals("Incorrect buyer", agent1, trans.get(0).getBuyer());
		assertEquals("Incorrect buyer", agent2, trans.get(0).getSeller());
		trans = hft.getTransactions(market1);
		assertEquals("Incorrect number of transactions", 0, trans.size());
	}

	/**
	 * Test for when quote latency != transaction latency
	 */
	@Test
	public void diffQuoteTransLatency() {
		TimeStamp time = TimeStamp.ZERO;
		TimeStamp time1 = new TimeStamp(1);

		// Creating dummy agents
		MockBackgroundAgent agent1 = new MockBackgroundAgent(fundamental, sip, market2);
		MockBackgroundAgent agent2 = new MockBackgroundAgent(fundamental, sip, market2);
		MockHFTAgent hft = new MockHFTAgent(TimeStamp.IMMEDIATE, new TimeStamp(100), 
				fundamental, sip, Arrays.asList(market1, market2));

		tp1 = hft.getHFTTransactionProcessor(market1);
		AbstractQuoteProcessor qp = hft.getHFTQuoteProcessor(market2);
		tp2 = hft.getHFTTransactionProcessor(market2);

		// Verify latency
		assertEquals(new TimeStamp(100), tp1.latency);
		assertEquals(TimeStamp.IMMEDIATE, qp.latency);
		assertEquals(new TimeStamp(100), tp2.latency);
		
		// Verify quote null
		Quote q = hft.getQuote(market2);
		assertEquals("Incorrect last quote time", null, qp.lastQuoteTime);
		assertEquals("Incorrect ASK", null, q.getAskPrice());
		assertEquals("Incorrect BID", null, q.getBidPrice());
		assertEquals("Incorrect ASK quantity", 0, q.getAskQuantity());
		assertEquals("Incorrect BID quantity", 0, q.getBidQuantity());

		// Creating and adding bids		
		EventManager em = new EventManager(new Random());
		em.addActivity(new SubmitOrder(agent1, market2, BUY, new Price(150), 2, time));
		em.executeUntil(time1);
		em.addActivity(new SubmitOrder(agent2, market2, SELL, new Price(140), 1, time));
		em.executeUntil(time1);
		
		// Verify no transaction update yet
		List<Transaction> trans = hft.getTransactions(market2);
		assertEquals("Incorrect number of transactions", 0, trans.size());
		
		// Verify that quote has updated
		q = hft.getQuote(market2);
		assertEquals("Incorrect last quote time", time, qp.lastQuoteTime);
		assertEquals("Incorrect ASK", null, q.getAskPrice());
		assertEquals("Incorrect BID", new Price(150), q.getBidPrice());
		assertEquals("Incorrect ASK quantity", 0, q.getAskQuantity());
		assertEquals("Incorrect BID quantity", 1, q.getBidQuantity());
		
		// Verify that transactions have updated
		em.executeUntil(new TimeStamp(101));
		trans = hft.getTransactions(market2);
		assertEquals("Incorrect number of transactions", 1, trans.size());
		assertEquals("Incorrect transaction price", new Price(150), trans.get(0).getPrice());
		assertEquals("Incorrect transaction quantity", 1, trans.get(0).getQuantity());
		assertEquals("Incorrect buyer", agent1, trans.get(0).getBuyer());
		assertEquals("Incorrect buyer", agent2, trans.get(0).getSeller());
		trans = hft.getTransactions(market1);
		assertEquals("Incorrect number of transactions", 0, trans.size());
	}
}
