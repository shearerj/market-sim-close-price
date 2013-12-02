package entity.infoproc;

import static org.junit.Assert.*;
import static fourheap.Order.OrderType.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import logger.Logger;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.Iterables;

import activity.Activity;
import activity.ProcessQuote;
import activity.SendToIP;
import activity.SubmitNMSOrder;
import activity.SubmitOrder;
import data.DummyFundamental;
import data.FundamentalValue;
import systemmanager.Consts;
import systemmanager.EventManager;
import entity.agent.MockBackgroundAgent;
import entity.market.CDAMarket;
import entity.market.DummyMarketTime;
import entity.market.Market;
import entity.market.MarketTime;
import entity.market.MockMarket;
import entity.market.Price;
import entity.market.Quote;
import entity.market.Transaction;
import event.TimeStamp;

public class SIPTest {

	private Market market1;
	private Market market2;
	private SIP sip;
	private SIP sip2;
	
	@BeforeClass
	public static void setupClass() {
		Logger.setup(3, new File(Consts.TEST_OUTPUT_DIR + "SIPTest.log"));
	}
	
	@Before
	public void setup() {
		sip = new SIP(TimeStamp.IMMEDIATE);
		sip2 = new SIP(new TimeStamp(50));
		// market that updates immediately
		market1 = new MockMarket(sip);
		// market with latency 100
		market2 = new MockMarket(sip, new TimeStamp(100));
	}
	
	@Test
	public void basicQuote() {
		TimeStamp time = TimeStamp.ZERO;
		
		// Test initial NBBO quote
		BestBidAsk nbbo = sip.getNBBO();
		assertEquals(null, nbbo.bestAsk);
		assertEquals(null, nbbo.bestBid);
		assertEquals(0, nbbo.bestAskQuantity);
		assertEquals(0, nbbo.bestBidQuantity);
		assertEquals(null, nbbo.bestAskMarket);
		assertEquals(null, nbbo.bestBidMarket);
		assertEquals(0, sip.marketQuotes.size());
		assertEquals(0, sip.quoteTimes.size());
		
		Quote q1 = new Quote(market1, new Price(80), 1, new Price(100), 1, time);
		sip.processQuote(market1, new DummyMarketTime(time, 1), q1, new ArrayList<Transaction>(), time);
		
		// Test that NBBO quote is correct
		nbbo = sip.getNBBO();
		assertEquals("Incorrect ASK", new Price(100), nbbo.bestAsk);
		assertEquals("Incorrect BID", new Price(80), nbbo.bestBid);
		assertEquals("Incorrect ASK quantity", 1, nbbo.bestAskQuantity);
		assertEquals("Incorrect BID quantity", 1, nbbo.bestBidQuantity);
		assertEquals("Incorrect ASK market", market1, nbbo.bestAskMarket);
		assertEquals("Incorrect BID market", market1, nbbo.bestBidMarket);
		assertEquals(1, sip.marketQuotes.size());
		assertEquals(1, sip.quoteTimes.size());
	}
	
	@Test
	public void multiQuote() {
		TimeStamp time = TimeStamp.ZERO;
		Quote q1 = new Quote(market1, new Price(80), 1, new Price(100), 1, time);
		sip.processQuote(market1, new DummyMarketTime(time, 1), q1, new ArrayList<Transaction>(), time);
		
		Quote q2 = new Quote(market1, new Price(70), 1, new Price(90), 1, time);
		sip.processQuote(market1, new DummyMarketTime(time, 2), q2, new ArrayList<Transaction>(), time);
		
		// Test that NBBO quote is correct (completely replaces old quote of [80, 100])
		BestBidAsk nbbo = sip.getNBBO();
		assertEquals("Incorrect ASK", new Price(90), nbbo.bestAsk);
		assertEquals("Incorrect BID", new Price(70), nbbo.bestBid);
		assertEquals("Incorrect ASK quantity", 1, nbbo.bestAskQuantity);
		assertEquals("Incorrect BID quantity", 1, nbbo.bestBidQuantity);
		assertEquals("Incorrect ASK market", market1, nbbo.bestAskMarket);
		assertEquals("Incorrect BID market", market1, nbbo.bestBidMarket);
		assertEquals(1, sip.marketQuotes.size());
		assertEquals(1, sip.quoteTimes.size());
	}
	
	@Test
	public void staleQuote() {
		TimeStamp time = new TimeStamp(10);
		
		Quote q1 = new Quote(market1, new Price(80), 1, new Price(100), 1, time);
		sip.processQuote(market1, new DummyMarketTime(time, 2), q1, new ArrayList<Transaction>(), time);
		
		// Test that NBBO quote is correct
		BestBidAsk nbbo = sip.getNBBO();
		assertEquals("Incorrect ASK", new Price(100), nbbo.bestAsk);
		assertEquals("Incorrect BID", new Price(80), nbbo.bestBid);
		assertEquals("Incorrect ASK quantity", 1, nbbo.bestAskQuantity);
		assertEquals("Incorrect BID quantity", 1, nbbo.bestBidQuantity);
		assertEquals("Incorrect ASK market", market1, nbbo.bestAskMarket);
		assertEquals("Incorrect BID market", market1, nbbo.bestBidMarket);
		assertEquals(new DummyMarketTime(time, 2), sip.quoteTimes.get(market1));
		
		// Note that staleness is based solely on MarketTime (not timestamp)
		Quote q2 = new Quote(market1, new Price(70), 1, new Price(90), 1, TimeStamp.ZERO);
		sip.processQuote(market1, new DummyMarketTime(time, 1), q2, new ArrayList<Transaction>(), time);
		
		// Test that NBBO quote is correct (ignores stale quote q2)
		nbbo = sip.getNBBO();
		assertEquals("Incorrect ASK", new Price(100), nbbo.bestAsk);
		assertEquals("Incorrect BID", new Price(80), nbbo.bestBid);
		assertEquals("Incorrect ASK quantity", 1, nbbo.bestAskQuantity);
		assertEquals("Incorrect BID quantity", 1, nbbo.bestBidQuantity);
		assertEquals("Incorrect ASK market", market1, nbbo.bestAskMarket);
		assertEquals("Incorrect BID market", market1, nbbo.bestBidMarket);
		assertEquals(new DummyMarketTime(time, 2), sip.quoteTimes.get(market1));
	}
	
	@Test
	public void twoMarketQuote() {
		TimeStamp time = TimeStamp.ZERO;
		Quote q1 = new Quote(market1, new Price(80), 1, new Price(100), 1, time);
		sip.processQuote(market1, new DummyMarketTime(time, 1), q1, new ArrayList<Transaction>(), time);
		
		Quote q2 = new Quote(market2, new Price(70), 1, new Price(90), 1, time);
		sip.processQuote(market2, new DummyMarketTime(time, 2), q2, new ArrayList<Transaction>(), time);
		
		// Test that NBBO quote is correct (computes best quote between both markets)
		BestBidAsk nbbo = sip.getNBBO();
		assertEquals("Incorrect ASK", new Price(90), nbbo.bestAsk);
		assertEquals("Incorrect BID", new Price(80), nbbo.bestBid);
		assertEquals("Incorrect ASK quantity", 1, nbbo.bestAskQuantity);
		assertEquals("Incorrect BID quantity", 1, nbbo.bestBidQuantity);
		assertEquals("Incorrect ASK market", market2, nbbo.bestAskMarket);
		assertEquals("Incorrect BID market", market1, nbbo.bestBidMarket);
		assertEquals(2, sip.marketQuotes.size());
		assertEquals(2, sip.quoteTimes.size());
	}
	
	@Test
	public void twoMarketMultiQuote() {
		TimeStamp time = TimeStamp.ZERO;
		Quote q1 = new Quote(market1, new Price(85), 1, new Price(100), 1, time);
		Quote q2 = new Quote(market2, new Price(75), 1, new Price(95), 1, time);
		Quote q3 = new Quote(market1, new Price(65), 1, new Price(90), 1, time);
		
		sip.processQuote(market1, new DummyMarketTime(time, 1), q1, new ArrayList<Transaction>(), time);
		sip.processQuote(market2, new DummyMarketTime(time, 1), q2, new ArrayList<Transaction>(), time);
		sip.processQuote(market1, new DummyMarketTime(time, 2), q3, new ArrayList<Transaction>(), time);
				
		// Test that NBBO quote is correct & that market 1's quote was replaced
		BestBidAsk nbbo = sip.getNBBO();
		assertEquals("Incorrect ASK", new Price(90), nbbo.bestAsk);
		assertEquals("Incorrect BID", new Price(75), nbbo.bestBid);
		assertEquals("Incorrect ASK quantity", 1, nbbo.bestAskQuantity);
		assertEquals("Incorrect BID quantity", 1, nbbo.bestBidQuantity);
		assertEquals("Incorrect ASK market", market1, nbbo.bestAskMarket);
		assertEquals("Incorrect BID market", market2, nbbo.bestBidMarket);
		assertEquals(new DummyMarketTime(time, 2), sip.quoteTimes.get(market1));
		assertEquals(new DummyMarketTime(time, 1), sip.quoteTimes.get(market2));
		
		Quote q4 = new Quote(market2, new Price(60), 1, new Price(91), 1, time);
		sip.processQuote(market2, new DummyMarketTime(time, 2), q4, new ArrayList<Transaction>(), time);
		
		// Test that NBBO quote is correct & that market 2's quote was replaced
		nbbo = sip.getNBBO();
		assertEquals("Incorrect ASK", new Price(90), nbbo.bestAsk);
		assertEquals("Incorrect BID", new Price(65), nbbo.bestBid);
		assertEquals("Incorrect ASK quantity", 1, nbbo.bestAskQuantity);
		assertEquals("Incorrect BID quantity", 1, nbbo.bestBidQuantity);
		assertEquals("Incorrect ASK market", market1, nbbo.bestAskMarket);
		assertEquals("Incorrect BID market", market1, nbbo.bestBidMarket);
		assertEquals(new DummyMarketTime(time, 2), sip.quoteTimes.get(market1));
		assertEquals(new DummyMarketTime(time, 2), sip.quoteTimes.get(market2));

		/*
		 * XXX NOTE: if tie in price, nondeterminism in which market has best
		 * price.
		 */
	}
	
	@Test
	public void basicNoDelay() {
		TimeStamp time = TimeStamp.ZERO;
		MarketTime mktTime = new DummyMarketTime(time, 1);

		// Add new quote
		Quote q = new Quote(market1, new Price(80), 1, new Price(100), 2, time);
		Iterable<? extends Activity> acts = sip.sendToIP(market1, mktTime, q, 
				new ArrayList<Transaction>(), time);
		// Verify correct process quote activity inserted right after
		for (Activity a : acts) {
			assertEquals("Incorrect scheduled process quote time",
					TimeStamp.IMMEDIATE, a.getTime());
			assertTrue("Incorrect activity type scheduled", 
					a instanceof ProcessQuote);
		}
		for (Activity a : acts) a.execute(time);	// can't execute at IMMED

		// Test that NBBO quote is correct
		BestBidAsk nbbo = sip.getNBBO();
		assertEquals("Incorrect ASK", new Price(100), nbbo.bestAsk);
		assertEquals("Incorrect BID", new Price(80), nbbo.bestBid);
		assertEquals("Incorrect ASK quantity", 2, nbbo.bestAskQuantity);
		assertEquals("Incorrect BID quantity", 1, nbbo.bestBidQuantity);
		assertEquals("Incorrect ASK market", market1, nbbo.bestAskMarket);
		assertEquals("Incorrect BID market", market1, nbbo.bestBidMarket);
	}

	@Test
	public void basicDelay() {
		TimeStamp time = TimeStamp.ZERO;
		MarketTime mktTime = new DummyMarketTime(time, 1);
		
		// Check that process quote activity scheduled correctly
		Quote q = new Quote(market1, new Price(80), 1, new Price(100), 1, time);
		Iterable<? extends Activity> acts = sip2.sendToIP(market1, mktTime, q, 
				new ArrayList<Transaction>(), time);
		// Verify correct process quote activity added to execute at time 50
		for (Activity a : acts) { 
			assertEquals("Incorrect scheduled process quote time",
					new TimeStamp(50), a.getTime());
			assertTrue("Incorrect activity type scheduled", 
					a instanceof ProcessQuote);
		}
		for (Activity a : acts) a.execute(a.getTime());

		// Test that NBBO quote is correct
		BestBidAsk nbbo = sip2.getNBBO();
		assertEquals("Incorrect ASK", new Price(100), nbbo.bestAsk);
		assertEquals("Incorrect BID", new Price(80), nbbo.bestBid);
		assertEquals("Incorrect ASK quantity", 1, nbbo.bestAskQuantity);
		assertEquals("Incorrect BID quantity", 1, nbbo.bestBidQuantity);
		assertEquals("Incorrect ASK market", market1, nbbo.bestAskMarket);
		assertEquals("Incorrect BID market", market1, nbbo.bestBidMarket);
	}
	
	@Test
	public void basicZeroDelay() {
		// SIP with zero not immediate latency
		SIP sip3 = new SIP(new TimeStamp(0));
		TimeStamp time = TimeStamp.ZERO;
		MarketTime mktTime = new DummyMarketTime(time, 1);
		
		// Check that process quote activity scheduled correctly
		Quote q = new Quote(market1, new Price(80), 1, new Price(100), 1, time);
		EventManager em = new EventManager(new Random());
		em.addActivity(new SendToIP(market1, mktTime, q, new ArrayList<Transaction>(), sip3, time));
		
		// Test that NBBO quote is correct after time 0
		em.executeUntil(new TimeStamp(1));
		BestBidAsk nbbo = sip3.getNBBO();
		assertEquals("Incorrect ASK", new Price(100), nbbo.bestAsk);
		assertEquals("Incorrect BID", new Price(80), nbbo.bestBid);
		assertEquals("Incorrect ASK quantity", 1, nbbo.bestAskQuantity);
		assertEquals("Incorrect BID quantity", 1, nbbo.bestBidQuantity);
		assertEquals("Incorrect ASK market", market1, nbbo.bestAskMarket);
		assertEquals("Incorrect BID market", market1, nbbo.bestBidMarket);
	}
	
	@Test
	public void alternateDelay() {
		TimeStamp time = TimeStamp.ZERO;
		MarketTime mktTime = new DummyMarketTime(time, 1);
		Quote q = new Quote(market2, new Price(80), 1, new Price(100), 1, time);
		
		// Send quotes to SIP. Market 2 has a delay of 100, SIP2 has delay of 50
		EventManager em = new EventManager(new Random());
		em.addActivity(new SendToIP(market2, mktTime, q, 
				new ArrayList<Transaction>(), sip2, time));
		em.addActivity(new SendToIP(market2, mktTime, q, 
				new ArrayList<Transaction>(), sip, time));
		
		em.executeUntil(time.plus(new TimeStamp(1)));
		// Check immediate SIP
		assertEquals("Last quote time not updated", mktTime, sip.quoteTimes.get(market2));
		BestBidAsk nbbo = sip.getNBBO();
		assertEquals("Incorrect ASK", q.getAskPrice(), nbbo.bestAsk);
		assertEquals("Incorrect BID", q.getBidPrice(), nbbo.bestBid);
		assertEquals("Incorrect ASK quantity", 1, nbbo.bestAskQuantity);
		assertEquals("Incorrect BID quantity", 1, nbbo.bestBidQuantity);
		assertEquals("Incorrect ASK market", market2, nbbo.bestAskMarket);
		assertEquals("Incorrect BID market", market2, nbbo.bestBidMarket);
		// Check delayed SIP not updated
		assertEquals("Updated delayed SIP too early", null, sip2.quoteTimes.get(market2));
		assertEquals("Incorrect ASK", null, sip2.getNBBO().bestAsk);
		assertEquals("Incorrect BID", null, sip2.getNBBO().bestBid);
		assertEquals("Incorrect ASK quantity", 0, sip2.getNBBO().bestAskQuantity);
		assertEquals("Incorrect BID quantity", 0, sip2.getNBBO().bestBidQuantity);
		assertEquals("Incorrect ASK market", null, sip2.getNBBO().bestAskMarket);
		assertEquals("Incorrect BID market", null, sip2.getNBBO().bestBidMarket);
		assertEquals(0, sip2.marketQuotes.size());
		assertEquals(0, sip2.quoteTimes.size());
		
		em.executeUntil(sip2.latency.plus(new TimeStamp(1)));
		// Check delayed SIP has been updated
		assertEquals("Last quote time not updated", mktTime, sip2.quoteTimes.get(market2));
		assertEquals("Incorrect ASK", new Price(100), sip2.getNBBO().bestAsk);
		assertEquals("Incorrect BID", new Price(80), sip2.getNBBO().bestBid);
		assertEquals("Incorrect ASK quantity", 1, sip2.getNBBO().bestAskQuantity);
		assertEquals("Incorrect BID quantity", 1, sip2.getNBBO().bestBidQuantity);
		assertEquals("Incorrect ASK market", market2, sip2.getNBBO().bestAskMarket);
		assertEquals("Incorrect BID market", market2, sip2.getNBBO().bestBidMarket);
		assertEquals(1, sip2.marketQuotes.size());
		assertEquals(1, sip2.quoteTimes.size());
	}
	
	@Test
	public void eventManagerLatencyTest() {
		TimeStamp time = TimeStamp.ZERO;
		MarketTime mktTime = new DummyMarketTime(time, 1);
		MarketTime mktTime2 = new DummyMarketTime(time, 2);
		Quote q1 = new Quote(market1, new Price(80), 1, new Price(100), 1, time);
		Quote q2 = new Quote(market2, new Price(75), 1, new Price(95), 2, new TimeStamp(30));
		
		// Send quotes to both IPs
		EventManager em = new EventManager(new Random());
		em.addActivity(new SendToIP(market1, mktTime, q1, 
				new ArrayList<Transaction>(), sip, time));
		em.addActivity(new SendToIP(market1, mktTime, q1, 
				new ArrayList<Transaction>(), sip2, time));
		
		// Check that no quotes have updated yet
		em.executeUntil(time);
		assertEquals("Updated SIP too early", null, sip.quoteTimes.get(market2));
		assertEquals("Incorrect ASK", null, sip.getNBBO().bestAsk);
		assertEquals("Incorrect BID", null, sip.getNBBO().bestBid);
		assertEquals("Incorrect ASK quantity", 0, sip.getNBBO().bestAskQuantity);
		assertEquals("Incorrect BID quantity", 0, sip.getNBBO().bestBidQuantity);
		assertEquals("Incorrect ASK market", null, sip.getNBBO().bestAskMarket);
		assertEquals("Incorrect BID market", null, sip.getNBBO().bestBidMarket);
		assertEquals("Updated delayed SIP too early", null, sip2.quoteTimes.get(market2));
		assertEquals("Incorrect ASK", null, sip2.getNBBO().bestAsk);
		assertEquals("Incorrect BID", null, sip2.getNBBO().bestBid);
		assertEquals("Incorrect ASK quantity", 0, sip2.getNBBO().bestAskQuantity);
		assertEquals("Incorrect BID quantity", 0, sip2.getNBBO().bestBidQuantity);
		assertEquals("Incorrect ASK market", null, sip2.getNBBO().bestAskMarket);
		assertEquals("Incorrect BID market", null, sip2.getNBBO().bestBidMarket);
		
		em.executeUntil(time.plus(new TimeStamp(1)));
		// Check immediate SIP updated with quote 1
		assertEquals("Last quote time not updated", mktTime, sip.quoteTimes.get(market1));
		BestBidAsk nbbo = sip.getNBBO();
		assertEquals("Incorrect ASK", q1.getAskPrice(), nbbo.bestAsk);
		assertEquals("Incorrect BID", q1.getBidPrice(), nbbo.bestBid);
		assertEquals("Incorrect ASK quantity", 1, nbbo.bestAskQuantity);
		assertEquals("Incorrect BID quantity", 1, nbbo.bestBidQuantity);
		assertEquals("Incorrect ASK market", market1, nbbo.bestAskMarket);
		assertEquals("Incorrect BID market", market1, nbbo.bestBidMarket);
		// Check delayed SIP not updated
		assertEquals("Updated delayed SIP too early", null, sip2.quoteTimes.get(market1));
		assertEquals("Incorrect ASK", null, sip2.getNBBO().bestAsk);
		assertEquals("Incorrect BID", null, sip2.getNBBO().bestBid);
		assertEquals("Incorrect ASK quantity", 0, sip2.getNBBO().bestAskQuantity);
		assertEquals("Incorrect BID quantity", 0, sip2.getNBBO().bestBidQuantity);
		assertEquals("Incorrect ASK market", null, sip2.getNBBO().bestAskMarket);
		assertEquals("Incorrect BID market", null, sip2.getNBBO().bestBidMarket);
		
		// Send more quotes to SIPs but only execute up to SIP2 latency of 100
		// so only first quote of [80, 100] should reach the delayed SIP
		em.addActivity(new SendToIP(market2, mktTime2, q2, 
				new ArrayList<Transaction>(), sip, new TimeStamp(30)));
		em.addActivity(new SendToIP(market2, mktTime2, q2, 
				new ArrayList<Transaction>(), sip2, new TimeStamp(30)));
		em.executeUntil(sip2.latency.plus(new TimeStamp(1)));
		// Check immediate SIP updated with quote 2
		assertEquals("Last quote time not updated", mktTime, sip.quoteTimes.get(market1));
		assertEquals("Last quote time not updated", mktTime2, sip.quoteTimes.get(market2));
		nbbo = sip.getNBBO();
		assertEquals("Incorrect ASK", q2.getAskPrice(), nbbo.bestAsk);
		assertEquals("Incorrect BID", q1.getBidPrice(), nbbo.bestBid);
		assertEquals("Incorrect ASK quantity", 2, nbbo.bestAskQuantity);
		assertEquals("Incorrect BID quantity", 1, nbbo.bestBidQuantity);
		assertEquals("Incorrect ASK market", market2, nbbo.bestAskMarket);
		assertEquals("Incorrect BID market", market1, nbbo.bestBidMarket);
		// Check delayed SIP updated only with quote 1
		assertEquals("Last quote time not updated", mktTime, sip2.quoteTimes.get(market1));
		assertEquals("Updated delayed SIP too early", null, sip2.quoteTimes.get(market2));
		assertEquals("Incorrect ASK", q1.getAskPrice(), sip2.getNBBO().bestAsk);
		assertEquals("Incorrect BID", q1.getBidPrice(), sip2.getNBBO().bestBid);
		assertEquals("Incorrect ASK quantity", 1, sip2.getNBBO().bestAskQuantity);
		assertEquals("Incorrect BID quantity", 1, sip2.getNBBO().bestBidQuantity);
		assertEquals("Incorrect ASK market", market1, sip2.getNBBO().bestAskMarket);
		assertEquals("Incorrect BID market", market1, sip2.getNBBO().bestBidMarket);
		
		// Delayed SIP won't update until after 100 time steps after the second 
		// quote was submitted (which was at time 30), so need to execute up to time 130
		em.executeUntil(sip2.latency.plus(new TimeStamp(31)));
		// Check delayed SIP updated finally with quote 2
		assertEquals("Last quote time not updated", mktTime, sip2.quoteTimes.get(market1));
		assertEquals("Last quote time not updated", mktTime2, sip2.quoteTimes.get(market2));
		assertEquals("Incorrect ASK", q2.getAskPrice(), sip2.getNBBO().bestAsk);
		assertEquals("Incorrect BID", q1.getBidPrice(), sip2.getNBBO().bestBid);
		assertEquals("Incorrect ASK quantity", 2, sip2.getNBBO().bestAskQuantity);
		assertEquals("Incorrect BID quantity", 1, sip2.getNBBO().bestBidQuantity);
		assertEquals("Incorrect ASK market", market2, sip2.getNBBO().bestAskMarket);
		assertEquals("Incorrect BID market", market1, sip2.getNBBO().bestBidMarket);
	}
	
	@Test
	public void transactionsInSIP() {
		TimeStamp time = TimeStamp.ZERO;
		TimeStamp time1 = new TimeStamp(1);
		FundamentalValue fundamental = new DummyFundamental(100000);
		Market market = new CDAMarket(sip, TimeStamp.IMMEDIATE, new Random(), 1);
		
		//Creating dummy agents
		MockBackgroundAgent agent1 = new MockBackgroundAgent(fundamental, sip, market);
		MockBackgroundAgent agent2 = new MockBackgroundAgent(fundamental, sip, market);

		// Creating and adding bids		
		EventManager em = new EventManager(new Random());
		em.addActivity(new SubmitOrder(agent1, market, BUY, new Price(150), 2, time));
		em.executeUntil(time1); // should execute clear since CDA
		
		// Verify that NBBO quote has updated
		BestBidAsk nbbo = sip.getNBBO();
		assertEquals("Incorrect ASK", null, nbbo.bestAsk);
		assertEquals("Incorrect BID", new Price(150), nbbo.bestBid);
		assertEquals("Incorrect ASK quantity", 0, nbbo.bestAskQuantity);
		assertEquals("Incorrect BID quantity", 2, nbbo.bestBidQuantity);
		assertEquals("Incorrect ASK market", null, nbbo.bestAskMarket);
		assertEquals("Incorrect BID market", market, nbbo.bestBidMarket);
		
		em.addActivity(new SubmitOrder(agent2, market, SELL, new Price(140), 1, time));
		em.executeUntil(time1); // should execute Clear-->SendToSIP-->ProcessQuotes
		
		// Verify that transactions has updated as well as NBBO
		nbbo = sip.getNBBO();
		assertEquals("Incorrect ASK", null, nbbo.bestAsk);
		assertEquals("Incorrect BID", new Price(150), nbbo.bestBid);
		assertEquals("Incorrect ASK quantity", 0, nbbo.bestAskQuantity);
		assertEquals("Incorrect BID quantity", 1, nbbo.bestBidQuantity);
		assertEquals("Incorrect ASK market", null, nbbo.bestAskMarket);
		assertEquals("Incorrect BID market", market, nbbo.bestBidMarket);
		List<Transaction> trans = sip.getTransactions();
		assertEquals("Incorrect number of transactions", 1, trans.size());
		assertEquals("Incorrect transaction price", new Price(150), trans.get(0).getPrice());
		assertEquals("Incorrect transaction quantity", 1, trans.get(0).getQuantity());
		assertEquals("Incorrect buyer", agent1, trans.get(0).getBuyer());
		assertEquals("Incorrect buyer", agent2, trans.get(0).getSeller());
	}
	
	@Test
	public void basicOrderRoutingNMS() {
		EventManager em = new EventManager(new Random());
		FundamentalValue fundamental = new DummyFundamental(100000);
		TimeStamp time0 = TimeStamp.ZERO;
		TimeStamp time = new TimeStamp(50);
		
		// Set up CDA markets and their quotes
		Market nasdaq = new CDAMarket(sip2, time, new Random(), 1);
		Market nyse = new CDAMarket(sip2, time, new Random(), 1);
		MockBackgroundAgent background1 = new MockBackgroundAgent(fundamental, sip2, nyse);
		MockBackgroundAgent background2 = new MockBackgroundAgent(fundamental, sip2, nasdaq);
		em.addActivity(new SubmitOrder(background1, nyse, SELL, new Price(111), 1, time0));
		em.addActivity(new SubmitOrder(background1, nasdaq, BUY, new Price(104), 1, time0));
		em.addActivity(new SubmitOrder(background2, nasdaq, SELL, new Price(110), 1, time0));
		em.addActivity(new SubmitOrder(background2, nyse, BUY, new Price(102), 1, time0));
		em.executeUntil(time.plus(new TimeStamp(1)));

		// Verify that NBBO quote is (104, 110) at time 50 (after quotes have been processed by SIP)
		BestBidAsk nbbo = sip2.getNBBO();
		assertEquals("Incorrect ASK", new Price(110), nbbo.bestAsk);
		assertEquals("Incorrect BID", new Price(104), nbbo.bestBid);
		assertEquals("Incorrect ASK quantity", 1, nbbo.bestAskQuantity);
		assertEquals("Incorrect BID quantity", 1, nbbo.bestBidQuantity);
		assertEquals("Incorrect ASK market", nasdaq, nbbo.bestAskMarket);
		assertEquals("Incorrect BID market", nasdaq, nbbo.bestBidMarket);
		
		///////////////
		// Creating dummy agent & submit sell order
		MockBackgroundAgent agent1 = new MockBackgroundAgent(fundamental, sip2, nyse);
		em.addActivity(new SubmitNMSOrder(agent1, nyse, SELL, new Price(105), 1, time));
		em.executeUntil(new TimeStamp(100));
		nbbo = sip2.getNBBO();
		assertEquals("Incorrect ASK", new Price(110), nbbo.bestAsk);
		assertEquals("Incorrect BID", new Price(104), nbbo.bestBid);
		assertEquals("Incorrect ASK quantity", 1, nbbo.bestAskQuantity);
		assertEquals("Incorrect BID quantity", 1, nbbo.bestBidQuantity);
		assertEquals("Incorrect ASK market", nasdaq, nbbo.bestAskMarket);
		assertEquals("Incorrect BID market", nasdaq, nbbo.bestBidMarket);
		
		// Verify that NBBO quote is (104, 105) at time 100 (after quotes have been processed by SIP)
		em.executeUntil(new TimeStamp(101));
		nbbo = sip2.getNBBO();
		assertEquals("Incorrect ASK", new Price(105), nbbo.bestAsk);
		assertEquals("Incorrect BID", new Price(104), nbbo.bestBid);
		assertEquals("Incorrect ASK quantity", 1, nbbo.bestAskQuantity);
		assertEquals("Incorrect BID quantity", 1, nbbo.bestBidQuantity);
		assertEquals("Incorrect ASK market", nyse, nbbo.bestAskMarket);
		assertEquals("Incorrect BID market", nasdaq, nbbo.bestBidMarket);
		
		// Another agent submits a buy order
		MockBackgroundAgent agent2 = new MockBackgroundAgent(fundamental, sip2, nasdaq);
		em.addActivity(new SubmitNMSOrder(agent2, nasdaq, BUY, new Price(109), 1, new TimeStamp(100)));
		em.executeUntil(new TimeStamp(101));
		
		// Verify that order is routed to nyse and transacts immediately w/ agent1's order
		assertEquals(1, nyse.getTransactions().size());
		assertEquals(0, nasdaq.getTransactions().size());
		Transaction t = Iterables.getFirst(nyse.getTransactions(), null);
		assertEquals(nyse, t.getMarket());
		assertEquals(new Price(105), t.getPrice());
		assertEquals(agent1, t.getSeller());
		assertEquals(agent2, t.getBuyer());
	}

	@Test
	public void latencyArbRoutingNMS() {
		EventManager em = new EventManager(new Random());
		FundamentalValue fundamental = new DummyFundamental(100000);
		TimeStamp time0 = TimeStamp.ZERO;
		TimeStamp time = new TimeStamp(50);
		
		// Set up markets and their quotes
		// both markets are undelayed, although SIP is delayed by 50
		Market nasdaq = new CDAMarket(sip2, time, new Random(), 1);
		Market nyse = new CDAMarket(sip2, time, new Random(), 1);
		MockBackgroundAgent background1 = new MockBackgroundAgent(fundamental, sip2, nyse);
		MockBackgroundAgent background2 = new MockBackgroundAgent(fundamental, sip2, nasdaq);
		em.addActivity(new SubmitOrder(background1, nyse, SELL, new Price(111), 1, time0));
		em.addActivity(new SubmitOrder(background1, nasdaq, BUY, new Price(104), 1, time0));
		em.addActivity(new SubmitOrder(background2, nasdaq, SELL, new Price(110), 1, time0));
		em.addActivity(new SubmitOrder(background2, nyse, BUY, new Price(102), 1, time0));
		em.executeUntil(time.plus(new TimeStamp(1)));

		///////////////
		// Creating dummy agent & submit sell order
		MockBackgroundAgent agent1 = new MockBackgroundAgent(fundamental, sip2, nyse);
		em.addActivity(new SubmitNMSOrder(agent1, nyse, SELL, new Price(105), 1, time));

		// Verify that NBBO quote is still (104, 110) (hasn't updated yet)
		BestBidAsk nbbo = sip2.getNBBO();
		assertEquals("Incorrect ASK", new Price(110), nbbo.bestAsk);
		assertEquals("Incorrect BID", new Price(104), nbbo.bestBid);
		assertEquals("Incorrect ASK quantity", 1, nbbo.bestAskQuantity);
		assertEquals("Incorrect BID quantity", 1, nbbo.bestBidQuantity);
		assertEquals("Incorrect ASK market", nasdaq, nbbo.bestAskMarket);
		assertEquals("Incorrect BID market", nasdaq, nbbo.bestBidMarket);
		
		// Another agent submits a buy order
		MockBackgroundAgent agent2 = new MockBackgroundAgent(fundamental, sip2, nasdaq);
		em.addActivity(new SubmitNMSOrder(agent2, nasdaq, BUY, new Price(109), 1, time));
		em.executeUntil(time.plus(time));
		// Verify that NBBO quote is still (104, 110) (hasn't updated yet)
		nbbo = sip2.getNBBO();
		assertEquals("Incorrect ASK", new Price(110), nbbo.bestAsk);
		assertEquals("Incorrect BID", new Price(104), nbbo.bestBid);
		assertEquals("Incorrect ASK quantity", 1, nbbo.bestAskQuantity);
		assertEquals("Incorrect BID quantity", 1, nbbo.bestBidQuantity);
		assertEquals("Incorrect ASK market", nasdaq, nbbo.bestAskMarket);
		assertEquals("Incorrect BID market", nasdaq, nbbo.bestBidMarket);
		
		// The buy order was still routed to Nasdaq, so the NBBO will cross and 
		// there is a latency arbitrage opportunity
		em.executeUntil(new TimeStamp(101));
		// Verify that NBBO quote is now (109, 105)
		nbbo = sip2.getNBBO();
		assertEquals("Incorrect ASK", new Price(105), nbbo.bestAsk);
		assertEquals("Incorrect BID", new Price(109), nbbo.bestBid);
		assertEquals("Incorrect ASK quantity", 1, nbbo.bestAskQuantity);
		assertEquals("Incorrect BID quantity", 1, nbbo.bestBidQuantity);
		assertEquals("Incorrect ASK market", nyse, nbbo.bestAskMarket);
		assertEquals("Incorrect BID market", nasdaq, nbbo.bestBidMarket);
	}
	
	
	/*
	 * XXX SIP has uniform latency - would we ever want different latency from
	 * certain markets? Probably. This change shouldn't be too difficult.
	 */
}
