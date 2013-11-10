package entity.infoproc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import logger.Logger;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.Iterables;

import activity.Activity;
import activity.AgentStrategy;
import activity.ProcessQuote;
import activity.SendToIP;
import data.DummyFundamental;
import data.FundamentalValue;
import entity.agent.MockHFTAgent;
import entity.market.DummyMarketTime;
import entity.market.Market;
import entity.market.MarketTime;
import entity.market.MockMarket;
import entity.market.Price;
import entity.market.Quote;
import entity.market.Transaction;
import event.TimeStamp;
import systemmanager.Consts;
import systemmanager.EventManager;

public class HFTIPTest {
	
	private FundamentalValue fundamental = new DummyFundamental(100000);
	private Market market1;
	private Market market2;
	private SIP sip;
	private MockHFTAgent hft;
	private HFTIP mktip1;
	private HFTIP mktip2;

	@BeforeClass
	public static void setupClass() {
		Logger.setup(3, new File(Consts.TEST_OUTPUT_DIR + "HFTIPTest.log"));
	}
	
	@Before
	public void setup() {
		sip = new SIP(TimeStamp.IMMEDIATE);
		// market that updates immediately
		market1 = new MockMarket(sip);
		// market with latency 100
		market2 = new MockMarket(sip, new TimeStamp(100));
		
		hft = new MockHFTAgent(TimeStamp.IMMEDIATE, fundamental, sip, 
				Arrays.asList(market1, market2));
		mktip1 = hft.getHFTIP(market1);
		mktip2 = hft.getHFTIP(market2);
	}
	
	
	@Test
	public void basicProcessQuote() {
		TimeStamp time = TimeStamp.ZERO;
		MarketTime mktTime = new DummyMarketTime(time, 1);
		
		// Check HFT IP latencies
		assertEquals(TimeStamp.IMMEDIATE, mktip1.latency);
		assertEquals(TimeStamp.IMMEDIATE, mktip2.latency);
		
		// Check initial quote is null for both markets
		Quote q = hft.getQuote(market1);
		assertEquals("Incorrect last quote time", null, mktip1.lastQuoteTime);
		assertEquals("Incorrect ASK", null, q.getAskPrice());
		assertEquals("Incorrect BID", null, q.getBidPrice());
		assertEquals("Incorrect ASK quantity", 0, q.getAskQuantity());
		assertEquals("Incorrect BID quantity", 0, q.getBidQuantity());
		q = hft.getQuote(market2);
		assertEquals("Incorrect last quote time", null, mktip2.lastQuoteTime);
		assertEquals("Incorrect ASK", null, q.getAskPrice());
		assertEquals("Incorrect BID", null, q.getBidPrice());
		assertEquals("Incorrect ASK quantity", 0, q.getAskQuantity());
		assertEquals("Incorrect BID quantity", 0, q.getBidQuantity());

		// Test on undelayed market's HFTIP
		q = new Quote(market1, new Price(80), 1, new Price(100), 1, time);
		Iterable<? extends Activity> acts = mktip1.processQuote(market1, mktTime, q, 
				new ArrayList<Transaction>(), time);
		assertEquals("Incorrect scheduled agent strategy time", TimeStamp.IMMEDIATE, 
				Iterables.getFirst(acts, null).getTime());
		assertTrue("Incorrect activity type scheduled", 
				Iterables.getFirst(acts, null) instanceof AgentStrategy);
		
		// Check updated quote after process quote
		assertEquals("Last quote time not updated", mktTime, mktip1.lastQuoteTime);
		q = hft.getQuote(market1);
		assertEquals("Incorrect ASK", new Price(100), q.getAskPrice());
		assertEquals("Incorrect BID", new Price(80), q.getBidPrice());
		assertEquals("Incorrect ASK quantity", 1, q.getAskQuantity());
		assertEquals("Incorrect BID quantity", 1, q.getBidQuantity());

		// Now test for delayed market's SMIP
		q = new Quote(market2, new Price(80), 1, new Price(100), 1, time);
		acts = mktip2.processQuote(market2, mktTime, q, new ArrayList<Transaction>(), time);
		assertEquals(1, Iterables.size(acts)); // agent strategy method added

		// Check second market correct
		q = hft.getQuote(market2);
		assertEquals("Incorrect last quote time", mktTime, mktip2.lastQuoteTime);
		assertEquals("Incorrect ASK", new Price(100), q.getAskPrice());
		assertEquals("Incorrect BID", new Price(80), q.getBidPrice());
		assertEquals("Incorrect ASK quantity", 1, q.getAskQuantity());
		assertEquals("Incorrect BID quantity", 1, q.getBidQuantity());
	}
	
	@Test
	public void basicNoDelay() {
		Quote q;
		TimeStamp time = TimeStamp.ZERO;
		MarketTime mktTime = new DummyMarketTime(time, 1);

		// Check initial quote is null for both markets
		q = hft.getQuote(market1);
		assertEquals("Incorrect last quote time", null, mktip1.lastQuoteTime);
		assertEquals("Incorrect ASK", null, q.getAskPrice());
		assertEquals("Incorrect BID", null, q.getBidPrice());
		assertEquals("Incorrect ASK quantity", 0, q.getAskQuantity());
		assertEquals("Incorrect BID quantity", 0, q.getBidQuantity());

		// Add new quote
		q = new Quote(market1, new Price(80), 1, new Price(100), 1, time);
		Iterable<? extends Activity> acts = mktip1.sendToIP(market1, mktTime, q, 
				new ArrayList<Transaction>(), time);
		// Verify correct process quote & agent strategy activity added
		assertEquals(1, Iterables.size(acts));
		assertEquals("Incorrect scheduled process quote time", TimeStamp.IMMEDIATE, 
				Iterables.getFirst(acts, null).getTime());
		assertTrue("Incorrect activity type scheduled", 
				Iterables.getFirst(acts, null) instanceof ProcessQuote);
		for (Activity a : acts) a.execute(time);

		// Check updated quote after process quote
		assertEquals("Last quote time not updated", mktTime, mktip1.lastQuoteTime);
		q = hft.getQuote(market1);
		assertEquals("Incorrect ASK", new Price(100), q.getAskPrice());
		assertEquals("Incorrect BID", new Price(80), q.getBidPrice());
		assertEquals("Incorrect ASK quantity", 1, q.getAskQuantity());
		assertEquals("Incorrect BID quantity", 1, q.getBidQuantity());
	}
	
	@Test
	public void basicDelay() {
		Quote q;
		TimeStamp time = TimeStamp.ZERO;
		MarketTime mktTime = new DummyMarketTime(time, 1);
		
		// Check initial quote is null
		assertEquals("Incorrect last quote time", null, mktip2.lastQuoteTime);
		q = mktip2.quote;
		assertEquals("Incorrect ASK", null, q.getAskPrice());
		assertEquals("Incorrect BID", null, q.getBidPrice());
		assertEquals("Incorrect ASK quantity", 0, q.getAskQuantity());
		assertEquals("Incorrect BID quantity", 0, q.getBidQuantity());
		
		// Check that process quote activity scheduled correctly
		q = new Quote(market2, new Price(80), 1, new Price(100), 1, time);
		Iterable<? extends Activity> acts = mktip2.sendToIP(market2, mktTime, q, 
				new ArrayList<Transaction>(), time);
		assertEquals(1, Iterables.size(acts));
		assertEquals("Incorrect scheduled process quote time", TimeStamp.IMMEDIATE, 
				Iterables.getFirst(acts, null).getTime());
		assertTrue("Incorrect activity type scheduled", 
				Iterables.getFirst(acts, null) instanceof ProcessQuote);
		for (Activity a : acts) a.execute(a.getTime());

		// Check updated quote after process quote (specific to SMIP)
		assertEquals("Last quote time not updated", mktTime, mktip2.lastQuoteTime);
		q = mktip2.quote;
		assertEquals("Incorrect ASK", new Price(100), q.getAskPrice());
		assertEquals("Incorrect BID", new Price(80), q.getBidPrice());
		assertEquals("Incorrect ASK quantity", 1, q.getAskQuantity());
		assertEquals("Incorrect BID quantity", 1, q.getBidQuantity());
	}
	
	/**
	 * Test creation of additional HFTIP with a different latency.
	 */
	@Test
	public void alternateDelayHFTIP() {
		TimeStamp time = TimeStamp.ZERO;
		MarketTime mktTime = new DummyMarketTime(time, 1);
		Quote q = new Quote(market2, new Price(80), 1, new Price(100), 1, time);
		
		MockHFTAgent hft2 = new MockHFTAgent(TimeStamp.IMMEDIATE, fundamental, sip, 
				Arrays.asList(market1, market2));
		HFTIP hftip = hft2.getHFTIP(market2);
		assertEquals(TimeStamp.IMMEDIATE, hftip.latency);
		
		// Send quotes to appropriate IPs
		EventManager em = new EventManager(new Random());
		em.addActivity(new SendToIP(market2, mktTime, q, new ArrayList<Transaction>(), hftip, time));
		em.addActivity(new SendToIP(market2, mktTime, q, new ArrayList<Transaction>(), hftip, time));
		
		em.executeUntil(time.plus(new TimeStamp(1)));
		// Check HFTIP, which should be immediate
		q = hftip.quote;
		assertEquals("Last quote time not updated", mktTime, hftip.lastQuoteTime);
		assertEquals("Incorrect ASK", new Price(100), q.getAskPrice());
		assertEquals("Incorrect BID", new Price(80), q.getBidPrice());
		assertEquals("Incorrect ASK quantity", 1, q.getAskQuantity());
		assertEquals("Incorrect BID quantity", 1, q.getBidQuantity());
	}
	
	/**
	 * Testing latency with EventManager.
	 */
	@Test
	public void eventManagerLatencyTest() {
		Quote q;
		TimeStamp time = TimeStamp.ZERO;
		MarketTime mktTime = new DummyMarketTime(time, 1);
		Quote q1 = new Quote(market1, new Price(80), 1, new Price(100), 1, time);
		Quote q2 = new Quote(market2, new Price(75), 1, new Price(95), 1, time);
		
		// Send quotes to appropriate IPs
		EventManager em = new EventManager(new Random());
		em.addActivity(new SendToIP(market1, mktTime, q1, new ArrayList<Transaction>(), mktip1, time));
		em.addActivity(new SendToIP(market2, mktTime, q2, new ArrayList<Transaction>(), mktip2, time));
		
		// Check that no quotes have updated yet
		em.executeUntil(time);
		q = mktip1.quote;
		assertEquals("Updated HFT IP 1 too early", null, mktip1.lastQuoteTime);
		assertEquals("Incorrect ASK", null, q.getAskPrice());
		assertEquals("Incorrect BID", null, q.getBidPrice());
		assertEquals("Incorrect ASK quantity", 0, q.getAskQuantity());
		assertEquals("Incorrect BID quantity", 0, q.getBidQuantity());
		q = mktip2.quote;
		assertEquals("Updated HFT IP 2 too early", null, mktip2.lastQuoteTime);
		assertEquals("Incorrect ASK", null, q.getAskPrice());
		assertEquals("Incorrect BID", null, q.getBidPrice());
		assertEquals("Incorrect ASK quantity", 0, q.getAskQuantity());
		assertEquals("Incorrect BID quantity", 0, q.getBidQuantity());
		
		em.executeUntil(time.plus(new TimeStamp(1)));
		// Check HFT IP for market 1 updated
		q = mktip1.quote;
		assertEquals("Last quote time not updated", mktTime, mktip1.lastQuoteTime);
		assertEquals("Incorrect ASK", new Price(100), q.getAskPrice());
		assertEquals("Incorrect BID", new Price(80), q.getBidPrice());
		assertEquals("Incorrect ASK quantity", 1, q.getAskQuantity());
		assertEquals("Incorrect BID quantity", 1, q.getBidQuantity());
		// Check HFT IP for market 2 not updated
		q = mktip2.quote;
		assertEquals("Last quote time not updated", mktTime, mktip2.lastQuoteTime);
		assertEquals("Incorrect ASK", new Price(95), q.getAskPrice());
		assertEquals("Incorrect BID", new Price(75), q.getBidPrice());
		assertEquals("Incorrect ASK quantity", 1, q.getAskQuantity());
		assertEquals("Incorrect BID quantity", 1, q.getBidQuantity());
	}
	
	@Test
	public void multiQuoteUpdates() {
		Quote q;
		TimeStamp time = TimeStamp.ZERO;
		TimeStamp time2 = new TimeStamp(50);
		MarketTime mktTime1 = new DummyMarketTime(time, 1);
		MarketTime mktTime2 = new DummyMarketTime(time, 2);
		Quote q1 = new Quote(market1, new Price(80), 1, new Price(100), 1, time);
		Quote q2 = new Quote(market2, new Price(80), 1, new Price(100), 1, time);
		Quote q3 = new Quote(market1, new Price(75), 1, new Price(95), 1, time2);
		Quote q4 = new Quote(market2, new Price(75), 1, new Price(95), 1, time2);
		
		// Send quotes to appropriate IPs
		EventManager em = new EventManager(new Random());
		em.addActivity(new SendToIP(market1, mktTime1, q1, new ArrayList<Transaction>(), mktip1, time));
		em.addActivity(new SendToIP(market2, mktTime1, q4, new ArrayList<Transaction>(), mktip2, time));
		// Send updated quotes at time2
		em.addActivity(new SendToIP(market1, mktTime2, q3, new ArrayList<Transaction>(), mktip1, time2));
		em.addActivity(new SendToIP(market2, mktTime2, q2, new ArrayList<Transaction>(), mktip2, time2));
		
		// Check that both HFT IPs have updated after time2=50
		em.executeUntil(time2.plus(new TimeStamp(1)));
		q = mktip1.quote;
		assertEquals("Incorrect last quote time", mktTime2, mktip1.lastQuoteTime);
		assertEquals("Incorrect ASK", new Price(95), q.getAskPrice());
		assertEquals("Incorrect BID", new Price(75), q.getBidPrice());
		assertEquals("Incorrect ASK quantity", 1, q.getAskQuantity());
		assertEquals("Incorrect BID quantity", 1, q.getBidQuantity());
		q = mktip2.quote;
		assertEquals("Incorrect last quote time", mktTime2, mktip2.lastQuoteTime);
		assertEquals("Incorrect ASK", new Price(100), q.getAskPrice());
		assertEquals("Incorrect BID", new Price(80), q.getBidPrice());
		assertEquals("Incorrect ASK quantity", 1, q.getAskQuantity());
		assertEquals("Incorrect BID quantity", 1, q.getBidQuantity());
	}
	
	/**
	 * Test markets updating twice in the same TimeStamp.
	 */
	@Test
	public void multiQuoteUpdatesAtSameTime() {
		Quote q;
		TimeStamp time = TimeStamp.ZERO;
		MarketTime mktTime1 = new DummyMarketTime(time, 1);
		MarketTime mktTime2 = new DummyMarketTime(time, 2);
		Quote q1 = new Quote(market1, new Price(80), 1, new Price(100), 1, time);
		Quote q2 = new Quote(market2, new Price(80), 1, new Price(100), 1, time);
		Quote q3 = new Quote(market1, new Price(75), 1, new Price(95), 1, time);
		Quote q4 = new Quote(market2, new Price(75), 1, new Price(95), 1, time);
		
		// Send quotes to appropriate IPs
		EventManager em = new EventManager(new Random());
		em.addActivity(new SendToIP(market1, mktTime1, q1, new ArrayList<Transaction>(), mktip1, time));
		em.addActivity(new SendToIP(market2, mktTime1, q4, new ArrayList<Transaction>(), mktip2, time));
		// Send updated quotes (also at time 0)
		em.addActivity(new SendToIP(market1, mktTime2, q3, new ArrayList<Transaction>(), mktip1, time));
		em.addActivity(new SendToIP(market2, mktTime2, q2, new ArrayList<Transaction>(), mktip2, time));
		
		// Check market1's SMIP has updated but not market2's after time 0
		em.executeUntil(new TimeStamp(1));
		q = mktip1.quote;
		assertEquals("Incorrect last quote time", mktTime2, mktip1.lastQuoteTime);
		assertEquals("Incorrect ASK", new Price(95), q.getAskPrice());
		assertEquals("Incorrect BID", new Price(75), q.getBidPrice());
		assertEquals("Incorrect ASK quantity", 1, q.getAskQuantity());
		assertEquals("Incorrect BID quantity", 1, q.getBidQuantity());
		q = mktip2.quote;
		assertEquals("Incorrect last quote time", mktTime2, mktip2.lastQuoteTime);
		assertEquals("Incorrect ASK", new Price(100), q.getAskPrice());
		assertEquals("Incorrect BID", new Price(80), q.getBidPrice());
		assertEquals("Incorrect ASK quantity", 1, q.getAskQuantity());
		assertEquals("Incorrect BID quantity", 1, q.getBidQuantity());
	}
	
	/**
	 * Test handling of stale quotes
	 */
	@Test
	public void staleQuotes() {
		Quote q;
		TimeStamp time = TimeStamp.ZERO;
		MarketTime mktTime1 = new DummyMarketTime(time, 1);
		MarketTime mktTime2 = new DummyMarketTime(time, 2);
		Quote q1 = new Quote(market1, new Price(80), 1, new Price(100), 1, time);
		Quote q2 = new Quote(market2, new Price(80), 1, new Price(100), 1, time);
		Quote q3 = new Quote(market1, new Price(75), 1, new Price(95), 1, time);
		Quote q4 = new Quote(market2, new Price(75), 1, new Price(95), 1, time);
		
		// Send quotes to appropriate IPs
		EventManager em = new EventManager(new Random());
		em.addActivity(new SendToIP(market1, mktTime2, q1, new ArrayList<Transaction>(), mktip1, time));
		em.addActivity(new SendToIP(market2, mktTime2, q4, new ArrayList<Transaction>(), mktip2, time));
		
		// Check market1's HFT IP has updated but not market2's after time 0
		em.executeUntil(new TimeStamp(1));
		q = mktip1.quote;
		assertEquals("Incorrect last quote time", mktTime2, mktip1.lastQuoteTime);
		assertEquals("Incorrect ASK", new Price(100), q.getAskPrice());
		assertEquals("Incorrect BID", new Price(80), q.getBidPrice());
		assertEquals("Incorrect ASK quantity", 1, q.getAskQuantity());
		assertEquals("Incorrect BID quantity", 1, q.getBidQuantity());
		q = mktip2.quote;
		assertEquals("Incorrect last quote time", mktTime2, mktip2.lastQuoteTime);
		assertEquals("Incorrect ASK", new Price(95), q.getAskPrice());
		assertEquals("Incorrect BID", new Price(75), q.getBidPrice());
		assertEquals("Incorrect ASK quantity", 1, q.getAskQuantity());
		assertEquals("Incorrect BID quantity", 1, q.getBidQuantity());
				
		// Send stale quotes to HFT IPs
		em.addActivity(new SendToIP(market1, mktTime1, q3, new ArrayList<Transaction>(), mktip1, time));
		em.addActivity(new SendToIP(market2, mktTime1, q2, new ArrayList<Transaction>(), mktip2, time));
		
		// Check that market1's HFT IP quote doesn't change
		em.executeUntil(new TimeStamp(1));
		q = mktip1.quote;
		assertEquals("Incorrect last quote time", mktTime2, mktip1.lastQuoteTime);
		assertEquals("Incorrect ASK", new Price(100), q.getAskPrice());
		assertEquals("Incorrect BID", new Price(80), q.getBidPrice());
		assertEquals("Incorrect ASK quantity", 1, q.getAskQuantity());
		assertEquals("Incorrect BID quantity", 1, q.getBidQuantity());
		q = mktip2.quote;
		assertEquals("Incorrect last quote time", mktTime2, mktip2.lastQuoteTime);
		assertEquals("Incorrect ASK", new Price(95), q.getAskPrice());
		assertEquals("Incorrect BID", new Price(75), q.getBidPrice());
		assertEquals("Incorrect ASK quantity", 1, q.getAskQuantity());
		assertEquals("Incorrect BID quantity", 1, q.getBidQuantity());		
	}
	
	/**
	 * Test scenario where marketTime of two quotes is the same. Only works for
	 * markets with latency IMMEDIATE (otherwise will be nondeterministic).
	 * 
	 * It is an invariant that every quote in a market will have a unique MarketTime.
	 */
	@Test
	public void sameMarketTime() {
		Quote q;
		TimeStamp time = TimeStamp.ZERO;
		MarketTime mktTime = new DummyMarketTime(time, 1);
		Quote q1 = new Quote(market1, new Price(80), 1, new Price(100), 1, time);
		Quote q2 = new Quote(market2, new Price(80), 1, new Price(100), 1, time);
		Quote q3 = new Quote(market1, new Price(75), 1, new Price(95), 1, time);
		Quote q4 = new Quote(market2, new Price(75), 1, new Price(95), 1, time);
		
		// Send quotes to appropriate IPs
		EventManager em = new EventManager(new Random());
		em.addActivity(new SendToIP(market1, mktTime, q1, new ArrayList<Transaction>(), mktip1, time));
		em.addActivity(new SendToIP(market2, mktTime, q4, new ArrayList<Transaction>(), mktip2, time));
		
		// Check market1's SMIP has updated but not market2's after time 0
		em.executeUntil(new TimeStamp(1));
		q = mktip1.quote;
		assertEquals("Incorrect last quote time", mktTime, mktip1.lastQuoteTime);
		assertEquals("Incorrect ASK", new Price(100), q.getAskPrice());
		assertEquals("Incorrect BID", new Price(80), q.getBidPrice());
		assertEquals("Incorrect ASK quantity", 1, q.getAskQuantity());
		assertEquals("Incorrect BID quantity", 1, q.getBidQuantity());
		q = mktip2.quote;
		assertEquals("Updated SMIP 2 too early", mktTime, mktip2.lastQuoteTime);
		assertEquals("Incorrect ASK", new Price(95), q.getAskPrice());
		assertEquals("Incorrect BID", new Price(75), q.getBidPrice());
		assertEquals("Incorrect ASK quantity", 1, q.getAskQuantity());
		assertEquals("Incorrect BID quantity", 1, q.getBidQuantity());
		
		// Send stale quotes to SMIPs
		em.addActivity(new SendToIP(market1, mktTime, q3, new ArrayList<Transaction>(), mktip1, time));
		em.addActivity(new SendToIP(market2, mktTime, q2, new ArrayList<Transaction>(), mktip2, time));
		
		// Check that market1's SMIP quote updates to most recent quote (of same market time)
		em.executeUntil(new TimeStamp(1));
		q = mktip1.quote;
		assertEquals("Incorrect last quote time", mktTime, mktip1.lastQuoteTime);
		assertEquals("Incorrect ASK", new Price(95), q.getAskPrice());
		assertEquals("Incorrect BID", new Price(75), q.getBidPrice());
		assertEquals("Incorrect ASK quantity", 1, q.getAskQuantity());
		assertEquals("Incorrect BID quantity", 1, q.getBidQuantity());
		q = mktip2.quote;
		assertEquals("Updated SMIP 2 too early", mktTime, mktip2.lastQuoteTime);
		assertEquals("Incorrect ASK", new Price(100), q.getAskPrice());
		assertEquals("Incorrect BID", new Price(80), q.getBidPrice());
		assertEquals("Incorrect ASK quantity", 1, q.getAskQuantity());
		assertEquals("Incorrect BID quantity", 1, q.getBidQuantity());
	}
	
	@Test
	public void extraTest() {
		for(int i=0; i < 100; i++) {
			setup();
			staleQuotes();
			setup();
			sameMarketTime();
		}
	}
}
