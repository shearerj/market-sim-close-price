package entity.agent;

import static fourheap.Order.OrderType.BUY;
import static fourheap.Order.OrderType.SELL;
import static org.junit.Assert.*;

import java.io.File;
import java.util.Random;

import logger.Logger;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.Iterables;

import activity.AgentStrategy;
import activity.SubmitNMSOrder;
import activity.SubmitOrder;
import systemmanager.Consts;
import systemmanager.EventManager;
import systemmanager.Keys;
import data.EntityProperties;
import data.FundamentalValue;
import data.MockFundamental;
import entity.infoproc.BestBidAsk;
import entity.infoproc.SIP;
import entity.market.CDAMarket;
import entity.market.Market;
import entity.market.MockMarket;
import entity.market.Order;
import entity.market.Price;
import entity.market.Transaction;
import event.TimeStamp;

/**
 * Only testing order withdrawal, everything else is just same as ZIAgent.
 * 
 * @author ewah
 *
 */
public class ZIRAgentTest {

	private FundamentalValue fundamental = new MockFundamental(100000);
	private Market market;
	private SIP sip;
	private static Random rand;
	private static EntityProperties agentProperties;

	@BeforeClass
	public static void setUpClass(){
		// Setting up the log file
		Logger.setup(3, new File(Consts.TEST_OUTPUT_DIR + "ZIRAgentTest.log"));

		// Creating the setup properties
		rand = new Random(1);

		// Setting up agentProperties
		agentProperties = new EntityProperties();
		agentProperties.put(Keys.REENTRY_RATE, 0);
		agentProperties.put(Keys.MAX_QUANTITY, 2);
		agentProperties.put(Keys.PRIVATE_VALUE_VAR, 0);
	}

	@Before
	public void setup(){
		sip = new SIP(TimeStamp.IMMEDIATE);
		// Creating the MockMarket
		market = new MockMarket(sip);
	}

	@Test
	public void withdrawTest() {
		TimeStamp time = TimeStamp.ZERO;
		TimeStamp time1 = new TimeStamp(1);
		EventManager em = new EventManager(rand);

		EntityProperties testProps = new EntityProperties(agentProperties);
		testProps.put(Keys.WITHDRAW_ORDERS, true);

		// verify that orders are correctly withdrawn at each re-entry
		ZIRAgent agent = new ZIRAgent(TimeStamp.ZERO, fundamental, sip, market, 
				rand, testProps);

		// execute strategy once; then before reenter, change the position balance
		// that way, when execute strategy again, it won't submit new orders
		em.addActivity(new AgentStrategy(agent, time));
		em.executeUntil(time1);
		// verify that order submitted
		assertEquals(1, agent.activeOrders.size());
		agent.positionBalance = 10;
		em.addActivity(new AgentStrategy(agent, time));
		em.executeUntil(time1);
		// verify that order withdrawn
		assertEquals(0, agent.activeOrders.size());
	}

	/**
	 * Specific scenario where an order withdrawal changes the market to which 
	 * an order is routed (market quote latency is immediate)
	 */
	@Test
	public void withdrawQuoteUpdateTest() {
		TimeStamp time0 = TimeStamp.ZERO;
		TimeStamp time50 = new TimeStamp(50);
		EventManager em = new EventManager(rand);
		SIP sip = new SIP(new TimeStamp(50));

		// Set up CDA markets (market quote latency = 0) and their quotes
		Market nasdaq = new CDAMarket(sip, rand, time0, 1);
		Market nyse = new CDAMarket(sip, rand, time0, 1);
		MockBackgroundAgent background1 = new MockBackgroundAgent(fundamental, sip, nyse);
		MockBackgroundAgent background2 = new MockBackgroundAgent(fundamental, sip, nasdaq);
		em.addActivity(new SubmitOrder(background1, nyse, SELL, new Price(111000), 1, time0));
		em.addActivity(new SubmitOrder(background1, nasdaq, BUY, new Price(104000), 1, time0));
		em.addActivity(new SubmitOrder(background2, nasdaq, SELL, new Price(108000), 1, time0));
		em.addActivity(new SubmitOrder(background2, nyse, BUY, new Price(102000), 1, time0));
		em.executeUntil(new TimeStamp(51));
		// Verify that NBBO quote is (104, 108) at time 50 (after quotes have been processed by SIP)
		BestBidAsk nbbo = sip.getNBBO();
		assertEquals("Incorrect ASK", new Price(108000), nbbo.getBestAsk());
		assertEquals("Incorrect BID", new Price(104000), nbbo.getBestBid());
		assertEquals("Incorrect ASK market", nasdaq, nbbo.getBestAskMarket());
		assertEquals("Incorrect BID market", nasdaq, nbbo.getBestBidMarket());

		///////////////
		// Creating ZIR agent that WILL withdraw its orders; & submit buy order
		FundamentalValue fundamental2 = new MockFundamental(110000);
		EntityProperties testProps = new EntityProperties(agentProperties);
		testProps.put(Keys.WITHDRAW_ORDERS, true);
		testProps.put(Keys.REENTRY_RATE, 0);
		testProps.put(Keys.BID_RANGE_MAX, 1000);
		testProps.put(Keys.BID_RANGE_MIN, 1000);
		testProps.put(Keys.MAX_QUANTITY, 1);
		ZIRAgent agent1 = new ZIRAgent(TimeStamp.ZERO, fundamental2, sip, nasdaq, 
				new Random(4), testProps);	// rand seed selected to insert BUY

		// ZIR submits sell at 105 (is routed to nyse)
		// Verify that NBBO quote is (104, 105) at time 100
		em.addActivity(new SubmitNMSOrder(agent1, nyse, SELL, new Price(105000), 1, time50));
		em.executeUntil(new TimeStamp(101));
		nbbo = sip.getNBBO();
		assertEquals("Incorrect ASK", new Price(105000), nbbo.getBestAsk());
		assertEquals("Incorrect BID", new Price(104000), nbbo.getBestBid());
		assertEquals("Incorrect ASK market", nyse, nbbo.getBestAskMarket());
		assertEquals("Incorrect BID market", nasdaq, nbbo.getBestBidMarket());

		// Execute ZIR agent strategy
		// ZIR agent should withdraw its order and submit a buy @ 109 to nasdaq
		em.addActivity(new AgentStrategy(agent1, new TimeStamp(100)));
		em.executeUntil(new TimeStamp(101));

		// Verify that order submitted with correct price (109)
		// Order is routed to NYSE because of out of date SIP (104, 105) even
		// though it is really (104, 111) because the SELL @ 105 was withdrawn
		assertEquals(1, agent1.activeOrders.size());
		for (Order o : agent1.activeOrders) {
			assertEquals(new Price(109000), o.getPrice());
			assertEquals(BUY, o.getOrderType());
			assertEquals(nyse, o.getMarket());
		}

		// Verify NBBO quote is (104, 111)
		// Notice that the BID/ASK cross; if the SIP had been immediate, then
		// the BUY order at 109 would have been routed to Nasdaq and it
		// would have transacted immediately
		em.executeUntil(new TimeStamp(151));
		nbbo = sip.getNBBO();
		assertEquals("Incorrect ASK", new Price(108000), nbbo.getBestAsk());
		assertEquals("Incorrect BID", new Price(109000), nbbo.getBestBid());
		assertEquals("Incorrect ASK market", nasdaq, nbbo.getBestAskMarket());
		assertEquals("Incorrect BID market", nyse, nbbo.getBestBidMarket());
	}

	/**
	 * Specific scenario where not withdrawing order means that the order will
	 * be routed and will transact immediately.
	 */
	@Test
	public void noWithdrawQuoteUpdateTest() {
		TimeStamp time0 = TimeStamp.ZERO;
		EventManager em = new EventManager(rand);
		SIP sip = new SIP(new TimeStamp(50));

		// Set up CDA markets (market quote latency = 0) and their quotes
		Market nasdaq = new CDAMarket(sip, rand, time0, 1);
		Market nyse = new CDAMarket(sip, rand, time0, 1);
		MockBackgroundAgent background1 = new MockBackgroundAgent(fundamental, sip, nyse);
		MockBackgroundAgent background2 = new MockBackgroundAgent(fundamental, sip, nasdaq);
		em.addActivity(new SubmitOrder(background1, nyse, SELL, new Price(111000), 1, time0));
		em.addActivity(new SubmitOrder(background1, nasdaq, BUY, new Price(104000), 1, time0));
		em.addActivity(new SubmitOrder(background2, nasdaq, SELL, new Price(108000), 1, time0));
		em.addActivity(new SubmitOrder(background2, nyse, BUY, new Price(102000), 1, time0));
		em.executeUntil(new TimeStamp(51));
		// Verify that NBBO quote is (104, 108) at time 50 (after quotes have been processed by SIP)
		BestBidAsk nbbo = sip.getNBBO();
		assertEquals("Incorrect ASK", new Price(108000), nbbo.getBestAsk());
		assertEquals("Incorrect BID", new Price(104000), nbbo.getBestBid());
		assertEquals("Incorrect ASK market", nasdaq, nbbo.getBestAskMarket());
		assertEquals("Incorrect BID market", nasdaq, nbbo.getBestBidMarket());

		///////////////
		// Creating ZIR agent that WILL NOT withdraw its orders; & submit buy order
		FundamentalValue fundamental2 = new MockFundamental(110000);
		EntityProperties testProps = new EntityProperties(agentProperties);
		testProps.put(Keys.WITHDRAW_ORDERS, false);
		testProps.put(Keys.REENTRY_RATE, 0);
		testProps.put(Keys.BID_RANGE_MAX, 1000);
		testProps.put(Keys.BID_RANGE_MIN, 1000);
		testProps.put(Keys.MAX_QUANTITY, 1);
		ZIRAgent agent1 = new ZIRAgent(TimeStamp.ZERO, fundamental2, sip, nasdaq, 
				new Random(4), testProps);	// rand seed selected to insert BUY

		// ZIR submits sell at 105 (is routed to nyse)
		// Verify that NBBO quote is (104, 105) at time 100
		em.addActivity(new SubmitNMSOrder(agent1, nyse, SELL, new Price(105000), 1, time0));
		em.executeUntil(new TimeStamp(101));
		nbbo = sip.getNBBO();
		nbbo = sip.getNBBO();
		assertEquals("Incorrect ASK", new Price(105000), nbbo.getBestAsk());
		assertEquals("Incorrect BID", new Price(104000), nbbo.getBestBid());
		assertEquals("Incorrect ASK market", nyse, nbbo.getBestAskMarket());
		assertEquals("Incorrect BID market", nasdaq, nbbo.getBestBidMarket());

		// Execute ZIR agent strategy
		// ZIR agent should withdraw its order and submit a sell @ 109
		em.addActivity(new AgentStrategy(agent1, new TimeStamp(100)));
		em.executeUntil(new TimeStamp(101));

		// Verify that order submitted with correct price (109)
		// Order is routed to NYSE but can't trade with itself
		assertEquals(0, agent1.activeOrders.size());

		// Verify that order transacts in Nasdaq
		// for testing purposes, have agent trade with itself (possible when
		// it doesn't withdraw its order at each reentry)
		assertEquals(0, nasdaq.getTransactions().size());
		assertEquals(1, nyse.getTransactions().size());
		Transaction t = Iterables.getFirst(nyse.getTransactions(), null);
		assertEquals(new Price(105000), t.getPrice());
		assertEquals(agent1, t.getSeller());
		assertEquals(agent1, t.getBuyer());
		
		// Verify NBBO quote is (104, 111)
		em.executeUntil(new TimeStamp(151));
		nbbo = sip.getNBBO();
		assertEquals("Incorrect ASK", new Price(108000), nbbo.getBestAsk());
		assertEquals("Incorrect BID", new Price(104000), nbbo.getBestBid());
		assertEquals("Incorrect ASK market", nasdaq, nbbo.getBestAskMarket());
		assertEquals("Incorrect BID market", nasdaq, nbbo.getBestBidMarket());
	}
}
