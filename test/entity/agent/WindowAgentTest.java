package entity.agent;

import static fourheap.Order.OrderType.BUY;
import static fourheap.Order.OrderType.SELL;
import static org.junit.Assert.*;

import java.io.File;
import java.util.List;
import java.util.Random;

import logger.Logger;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import activity.SubmitOrder;
import systemmanager.Consts;
import systemmanager.EventManager;
import data.DummyFundamental;
import data.FundamentalValue;
import entity.infoproc.SIP;
import entity.market.CDAMarket;
import entity.market.Market;
import entity.market.Price;
import entity.market.Transaction;
import event.TimeStamp;

public class WindowAgentTest {

	private FundamentalValue fundamental = new DummyFundamental(100000);
	private Market market;
	private SIP sip;
	
	@BeforeClass
	public static void setUpClass(){
		// Setting up the log file
		Logger.setup(3, new File(Consts.TEST_OUTPUT_DIR + "WindowAgentTest.log"));
	}

	@Before
	public void setup(){
		sip = new SIP(TimeStamp.IMMEDIATE);
		market = new CDAMarket(sip, TimeStamp.IMMEDIATE, new Random(), 1);
	}
	
	@Test
	public void basicWindowTest() {
		TimeStamp time = TimeStamp.ZERO;
		TimeStamp time1 = new TimeStamp(1);
		TimeStamp time10 = new TimeStamp(10);
		EventManager em = new EventManager(new Random());
		
		WindowAgent agent = new MockWindowAgent(fundamental, sip, market, 10);
		
		assertEquals("Incorrect initial transactions in window", 0, 
				agent.getWindowTransactions(time).size());
		
		// populate market with a transaction
		MockBackgroundAgent background1 = new MockBackgroundAgent(fundamental, sip, market);
		MockBackgroundAgent background2 = new MockBackgroundAgent(fundamental, sip, market);
		em.addActivity(new SubmitOrder(background1, market, BUY, new Price(111), 1, time));
		em.addActivity(new SubmitOrder(background2, market, SELL, new Price(110), 1, time));
		em.executeUntil(time.plus(new TimeStamp(1)));
		
		// basic window check
		Transaction trans = market.getTransactions().get(0);
		assertEquals("Incorrect # transactions", 1, agent.getWindowTransactions(time1).size());
		assertEquals("Incorrect window transactions", trans, agent.getWindowTransactions(time1).get(0));
		trans = sip.getTransactions().get(0);
		assertEquals("Incorrect window transactions", trans, agent.getWindowTransactions(time1).get(0));
	
		// checking just inside window
		trans = market.getTransactions().get(0);
		assertEquals("Incorrect # transactions", 1, agent.getWindowTransactions(time10.minus(time1)).size());
		assertEquals("Incorrect window transactions", trans, agent.getWindowTransactions(time10.minus(time1)).get(0));
		// check outside window
		assertEquals("Incorrect # transactions", 0, agent.getWindowTransactions(time10).size());
	}
	
	@Test
	public void delayedSIPTest() {
		TimeStamp time = TimeStamp.ZERO;
		TimeStamp time1 = new TimeStamp(1);
		TimeStamp time5 = new TimeStamp(5);
		TimeStamp time10 = new TimeStamp(10);
		EventManager em = new EventManager(new Random());
		SIP sipDelayed = new SIP(new TimeStamp(5));
		Market market = new CDAMarket(sipDelayed, TimeStamp.IMMEDIATE, new Random(), 1);
		
		WindowAgent agent = new MockWindowAgent(fundamental, sipDelayed, market, 10);
		
		// populate market with a transaction
		MockBackgroundAgent background1 = new MockBackgroundAgent(fundamental, sipDelayed, market);
		MockBackgroundAgent background2 = new MockBackgroundAgent(fundamental, sipDelayed, market);
		em.addActivity(new SubmitOrder(background1, market, BUY, new Price(111), 1, time));
		em.executeUntil(time1);
		em.addActivity(new SubmitOrder(background2, market, SELL, new Price(110), 1, time));
		em.executeUntil(time1);
		
		em.addActivity(new SubmitOrder(background1, market, BUY, new Price(104), 1, time1));
		em.executeUntil(time1.plus(time1));
		em.addActivity(new SubmitOrder(agent, market, SELL, new Price(102), 1, time1));
		em.executeUntil(time1.plus(time1));
		
		// test getting transactions from primary market (but not SIP) at time 1
		assertEquals("Incorrect # transactions", 1, agent.getWindowTransactions(time1).size());
		Transaction tr = agent.getWindowTransactions(time1).get(0);
		assertEquals("Transaction price incorrect", new Price(104), tr.getPrice());
		assertEquals("Transaction seller incorrect", agent, tr.getSeller());
		assertEquals("Transaction buyer incorrect", background1, tr.getBuyer());
		
		// test getting transactions from both at time 5
		em.executeUntil(time5.plus(time1));
		assertEquals("Incorrect # transactions", 2, agent.getWindowTransactions(time5).size());
		List<Transaction> trans = agent.getWindowTransactions(time5);
		assertTrue("SIP transaction missing", trans.contains(sipDelayed.getTransactions().get(0)));
		assertTrue("Primary market transaction missing", 
				trans.contains(market.getTransactions().get(0)));
		
		// check outside SIP latency at time 10
		em.executeUntil(time10.plus(time1));
		trans = agent.getWindowTransactions(time10);
		assertEquals("Incorrect # transactions", 1, trans.size());
		assertEquals("Transaction price incorrect", new Price(104), trans.get(0).getPrice());
		assertEquals("Transaction seller incorrect", agent, trans.get(0).getSeller());
		assertEquals("Transaction buyer incorrect", background1, trans.get(0).getBuyer());
		
		// check outside window (at time 11)
		em.executeUntil(new TimeStamp(12));
		trans = agent.getWindowTransactions(time10.plus(time1));
		assertEquals("Incorrect # transactions", 0, trans.size());
	}
	
	@Test
	public void extraTest() {
		for (int i = 0; i < 100; i++) {
			delayedSIPTest();
		}
	}
}
