package entity.infoproc;

import static org.junit.Assert.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Random;

import logger.Logger;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.Iterables;

import activity.Activity;
import activity.ProcessQuote;
import activity.SendToIP;
import systemmanager.Consts;
import systemmanager.EventManager;
import entity.market.*;
import event.TimeStamp;

public class QuoteProcessorTest {

	private Market market1;
	private Market market2;
	private SIP sip;
	private QuoteProcessor smip1;
	private QuoteProcessor smip2;

	@BeforeClass
	public static void setupClass() {
		Logger.setup(3, new File(Consts.TEST_OUTPUT_DIR + "QuoteProcessorTest.log"));
	}

	@Before
	public void setup() {
		sip = new SIP(TimeStamp.IMMEDIATE);
		// market that updates immediately
		market1 = new MockMarket(sip);
		smip1 = market1.getQuoteProcessor();
		// market with latency 100 (means that SM IP will have latency 100)
		market2 = new MockMarket(sip, new TimeStamp(100));
		smip2 = market2.getQuoteProcessor();
	}


	@Test
	public void basicProcessQuote() {
		TimeStamp time = TimeStamp.ZERO;
		MarketTime mktTime = new DummyMarketTime(time, 1);
		
		// Check initial quote is null
		Quote q = smip1.quote;
		assertEquals("Incorrect last quote time", null, smip1.lastQuoteTime);
		assertEquals("Incorrect ASK", null, q.getAskPrice());
		assertEquals("Incorrect BID", null, q.getBidPrice());
		assertEquals("Incorrect ASK quantity", 0, q.getAskQuantity());
		assertEquals("Incorrect BID quantity", 0, q.getBidQuantity());
		// Check initial quote is null
		q = smip2.quote;
		assertEquals("Incorrect last quote time", null, smip2.lastQuoteTime);
		assertEquals("Incorrect ASK", null, q.getAskPrice());
		assertEquals("Incorrect BID", null, q.getBidPrice());
		assertEquals("Incorrect ASK quantity", 0, q.getAskQuantity());
		assertEquals("Incorrect BID quantity", 0, q.getBidQuantity());

		// Test on undelayed market's QuoteProcessor
		q = new Quote(market1, new Price(80), 1, new Price(100), 1, time);
		Iterable<? extends Activity> acts = smip1.processInformation(market1, mktTime, q, 
				new ArrayList<Transaction>(), time);
		assertEquals(0, Iterables.size(acts));
		
		// Check updated quote after process quote (specific to QuoteProcessor)
		assertEquals("Last quote time not updated", mktTime, smip1.lastQuoteTime);
		q = smip1.quote;
		assertEquals("Incorrect ASK", new Price(100), q.getAskPrice());
		assertEquals("Incorrect BID", new Price(80), q.getBidPrice());
		assertEquals("Incorrect ASK quantity", 1, q.getAskQuantity());
		assertEquals("Incorrect BID quantity", 1, q.getBidQuantity());
		
		// Now test for delayed market's QuoteProcessor
		q = new Quote(market2, new Price(80), 1, new Price(100), 1, time);
		acts = smip2.processInformation(market2, mktTime, q, new ArrayList<Transaction>(), time);
		assertEquals(0, Iterables.size(acts));
		
		// Check updated quote after process quote
		assertEquals("Last quote time not updated", mktTime, smip2.lastQuoteTime);
		q = smip2.quote;
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

		// Check initial quote is null
		assertEquals("Incorrect last quote time", null, smip1.lastQuoteTime);
		q = smip1.quote;
		assertEquals("Incorrect ASK", null, q.getAskPrice());
		assertEquals("Incorrect BID", null, q.getBidPrice());
		assertEquals("Incorrect ASK quantity", 0, q.getAskQuantity());
		assertEquals("Incorrect BID quantity", 0, q.getBidQuantity());

		// Add new quote
		q = new Quote(market1, new Price(80), 1, new Price(100), 1, time);
		Iterable<? extends Activity> acts = smip1.sendToIP(market1, mktTime, q, 
				new ArrayList<Transaction>(), time);
		for (Activity a : acts) { // Verify correct process quote activity added
			assertEquals("Incorrect scheduled process quote time", TimeStamp.IMMEDIATE, 
					a.getTime());
			assertTrue("Incorrect activity type scheduled", 
					a instanceof ProcessQuote);
		}
		for (Activity a : acts) a.execute(time);

		// Check updated quote after process quote (specific to QuoteProcessor)
		assertEquals("Last quote time not updated", mktTime, smip1.lastQuoteTime);
		q = smip1.quote;
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
		assertEquals("Incorrect last quote time", null, smip2.lastQuoteTime);
		q = smip2.quote;
		assertEquals("Incorrect ASK", null, q.getAskPrice());
		assertEquals("Incorrect BID", null, q.getBidPrice());
		assertEquals("Incorrect ASK quantity", 0, q.getAskQuantity());
		assertEquals("Incorrect BID quantity", 0, q.getBidQuantity());
		
		// Check that process quote activity scheduled correctly
		q = new Quote(market2, new Price(80), 1, new Price(100), 1, time);
		Iterable<? extends Activity> acts = smip2.sendToIP(market2, mktTime, q, 
				new ArrayList<Transaction>(), time);
		for (Activity a : acts) { // Verify correct process quote activity added
			assertEquals("Incorrect scheduled process quote time", smip2.latency, 
					a.getTime());
			assertTrue("Incorrect activity type scheduled", 
					a instanceof ProcessQuote);
		}
		for (Activity a : acts) a.execute(a.getTime());

		// Check updated quote after process quote (specific to QuoteProcessor)
		assertEquals("Last quote time not updated", mktTime, smip2.lastQuoteTime);
		q = smip2.quote;
		assertEquals("Incorrect ASK", new Price(100), q.getAskPrice());
		assertEquals("Incorrect BID", new Price(80), q.getBidPrice());
		assertEquals("Incorrect ASK quantity", 1, q.getAskQuantity());
		assertEquals("Incorrect BID quantity", 1, q.getBidQuantity());
	}
	
	/**
	 * Test creation of additional QuoteProcessor with a different latency.
	 */
	@Test
	public void alternateDelayQuoteProcessor() {
		TimeStamp time = TimeStamp.ZERO;
		MarketTime mktTime = new DummyMarketTime(time, 1);
		Quote q = new Quote(market2, new Price(80), 1, new Price(100), 1, time);
		QuoteProcessor smipImmed = new QuoteProcessor(TimeStamp.IMMEDIATE, market2);
		
		// Send quotes to appropriate IPs
		EventManager em = new EventManager(new Random());
		em.addActivity(new SendToIP(market2, mktTime, q, new ArrayList<Transaction>(), smip2, time));
		em.addActivity(new SendToIP(market2, mktTime, q, new ArrayList<Transaction>(), smipImmed, time));
		
		em.executeUntil(time.plus(new TimeStamp(1)));
		// Check immediate QuoteProcessor
		assertEquals("Last quote time not updated", mktTime, smipImmed.lastQuoteTime);
		q = smipImmed.quote;
		assertEquals("Incorrect ASK", new Price(100), q.getAskPrice());
		assertEquals("Incorrect BID", new Price(80), q.getBidPrice());
		assertEquals("Incorrect ASK quantity", 1, q.getAskQuantity());
		assertEquals("Incorrect BID quantity", 1, q.getBidQuantity());
		// Check delayed QuoteProcessor
		assertEquals("Updated QuoteProcessor 2 too early", null, smip2.lastQuoteTime);
		q = smip2.quote;
		assertEquals("Incorrect ASK", null, q.getAskPrice());
		assertEquals("Incorrect BID", null, q.getBidPrice());
		assertEquals("Incorrect ASK quantity", 0, q.getAskQuantity());
		assertEquals("Incorrect BID quantity", 0, q.getBidQuantity());
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
		em.addActivity(new SendToIP(market1, mktTime, q1, new ArrayList<Transaction>(), smip1, time));
		em.addActivity(new SendToIP(market2, mktTime, q2, new ArrayList<Transaction>(), smip2, time));
		
		// Check that no quotes have updated yet
		em.executeUntil(time);
		q = smip1.quote;
		assertEquals("Updated QuoteProcessor 1 too early", null, smip1.lastQuoteTime);
		assertEquals("Incorrect ASK", null, q.getAskPrice());
		assertEquals("Incorrect BID", null, q.getBidPrice());
		assertEquals("Incorrect ASK quantity", 0, q.getAskQuantity());
		assertEquals("Incorrect BID quantity", 0, q.getBidQuantity());
		q = smip2.quote;
		assertEquals("Updated QuoteProcessor 2 too early", null, smip2.lastQuoteTime);
		assertEquals("Incorrect ASK", null, q.getAskPrice());
		assertEquals("Incorrect BID", null, q.getBidPrice());
		assertEquals("Incorrect ASK quantity", 0, q.getAskQuantity());
		assertEquals("Incorrect BID quantity", 0, q.getBidQuantity());
		
		em.executeUntil(time.plus(new TimeStamp(1)));
		// Check QuoteProcessor for market 1 updated
		q = smip1.quote;
		assertEquals("Last quote time not updated", mktTime, smip1.lastQuoteTime);
		assertEquals("Incorrect ASK", new Price(100), q.getAskPrice());
		assertEquals("Incorrect BID", new Price(80), q.getBidPrice());
		assertEquals("Incorrect ASK quantity", 1, q.getAskQuantity());
		assertEquals("Incorrect BID quantity", 1, q.getBidQuantity());
		// Check QuoteProcessor for market 2 not updated
		q = smip2.quote;
		assertEquals("Updated QuoteProcessor 2 too early", null, smip2.lastQuoteTime);
		assertEquals("Incorrect ASK", null, q.getAskPrice());
		assertEquals("Incorrect BID", null, q.getBidPrice());
		assertEquals("Incorrect ASK quantity", 0, q.getAskQuantity());
		assertEquals("Incorrect BID quantity", 0, q.getBidQuantity());
		
		em.executeUntil(smip2.latency.plus(new TimeStamp(1)));
		// Check QuoteProcessor for market 2 updated
		q = smip2.quote;
		assertEquals("Last quote time not updated", mktTime, smip2.lastQuoteTime);
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
		em.addActivity(new SendToIP(market1, mktTime1, q1, new ArrayList<Transaction>(), smip1, time));
		em.addActivity(new SendToIP(market2, mktTime1, q4, new ArrayList<Transaction>(), smip2, time));
		// Send updated quotes at time2
		em.addActivity(new SendToIP(market1, mktTime2, q3, new ArrayList<Transaction>(), smip1, time2));
		em.addActivity(new SendToIP(market2, mktTime2, q2, new ArrayList<Transaction>(), smip2, time2));
		
		// Check that market1's QuoteProcessor has updated but not market2's after time2=50
		em.executeUntil(time2.plus(new TimeStamp(1)));
		q = smip1.quote;
		assertEquals("Last quote time not updated", mktTime2, smip1.lastQuoteTime);
		assertEquals("Incorrect ASK", new Price(95), q.getAskPrice());
		assertEquals("Incorrect BID", new Price(75), q.getBidPrice());
		assertEquals("Incorrect ASK quantity", 1, q.getAskQuantity());
		assertEquals("Incorrect BID quantity", 1, q.getBidQuantity());
		q = smip2.quote;
		assertEquals("Updated QuoteProcessor 2 too early", null, smip2.lastQuoteTime);
		assertEquals("Incorrect ASK", null, q.getAskPrice());
		assertEquals("Incorrect BID", null, q.getBidPrice());
		assertEquals("Incorrect ASK quantity", 0, q.getAskQuantity());
		assertEquals("Incorrect BID quantity", 0, q.getBidQuantity());
		
		// Check that market2's QuoteProcessor received (75,95) by time 101
		em.executeUntil(new TimeStamp(101));
		q = smip2.quote;
		assertEquals("Incorrect last quote time", mktTime1, smip2.lastQuoteTime);
		assertEquals("Incorrect ASK", new Price(95), q.getAskPrice());
		assertEquals("Incorrect BID", new Price(75), q.getBidPrice());
		assertEquals("Incorrect ASK quantity", 1, q.getAskQuantity());
		assertEquals("Incorrect BID quantity", 1, q.getBidQuantity());
		
		// Quote (80,100) is processed by market2's QuoteProcessor after time 150
		em.executeUntil(new TimeStamp(151));
		q = smip2.quote;
		assertEquals("Last quote time not updated", mktTime2, smip2.lastQuoteTime);
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
		em.addActivity(new SendToIP(market1, mktTime1, q1, new ArrayList<Transaction>(), smip1, time));
		em.addActivity(new SendToIP(market2, mktTime1, q4, new ArrayList<Transaction>(), smip2, time));
		// Send updated quotes (also at time 0)
		em.addActivity(new SendToIP(market1, mktTime2, q3, new ArrayList<Transaction>(), smip1, time));
		em.addActivity(new SendToIP(market2, mktTime2, q2, new ArrayList<Transaction>(), smip2, time));
		
		// Check market1's QuoteProcessor has updated but not market2's after time 0
		em.executeUntil(new TimeStamp(1));
		q = smip1.quote;
		assertEquals("Last quote time not updated", mktTime2, smip1.lastQuoteTime);
		assertEquals("Incorrect ASK", new Price(95), q.getAskPrice());
		assertEquals("Incorrect BID", new Price(75), q.getBidPrice());
		assertEquals("Incorrect ASK quantity", 1, q.getAskQuantity());
		assertEquals("Incorrect BID quantity", 1, q.getBidQuantity());
		q = smip2.quote;
		assertEquals("Updated QuoteProcessor 2 too early", null, smip2.lastQuoteTime);
		assertEquals("Incorrect ASK", null, q.getAskPrice());
		assertEquals("Incorrect BID", null, q.getBidPrice());
		assertEquals("Incorrect ASK quantity", 0, q.getAskQuantity());
		assertEquals("Incorrect BID quantity", 0, q.getBidQuantity());

		// Check market2's QuoteProcessor has received most recent quote(80,100) after time 100
		em.executeUntil(new TimeStamp(101));
		q = smip2.quote;
		assertEquals("Incorrect last quote time", mktTime2, smip2.lastQuoteTime);
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
		em.addActivity(new SendToIP(market1, mktTime2, q1, new ArrayList<Transaction>(), smip1, time));
		em.addActivity(new SendToIP(market2, mktTime2, q4, new ArrayList<Transaction>(), smip2, time));
		
		// Check market1's QuoteProcessor has updated but not market2's after time 0
		em.executeUntil(new TimeStamp(1));
		q = smip1.quote;
		assertEquals("Last quote time not updated", mktTime2, smip1.lastQuoteTime);
		assertEquals("Incorrect ASK", new Price(100), q.getAskPrice());
		assertEquals("Incorrect BID", new Price(80), q.getBidPrice());
		assertEquals("Incorrect ASK quantity", 1, q.getAskQuantity());
		assertEquals("Incorrect BID quantity", 1, q.getBidQuantity());
		q = smip2.quote;
		assertEquals("Updated QuoteProcessor 2 too early", null, smip2.lastQuoteTime);
		assertEquals("Incorrect ASK", null, q.getAskPrice());
		assertEquals("Incorrect BID", null, q.getBidPrice());
		assertEquals("Incorrect ASK quantity", 0, q.getAskQuantity());
		assertEquals("Incorrect BID quantity", 0, q.getBidQuantity());
				
		// Send stale quotes to QuoteProcessors
		em.addActivity(new SendToIP(market1, mktTime1, q3, new ArrayList<Transaction>(), smip1, time));
		em.addActivity(new SendToIP(market2, mktTime1, q2, new ArrayList<Transaction>(), smip2, time));
		
		// Check that market1's QuoteProcessor quote doesn't change
		em.executeUntil(new TimeStamp(1));
		q = smip1.quote;
		assertEquals("Incorrect last quote time", mktTime2, smip1.lastQuoteTime);
		assertEquals("Incorrect ASK", new Price(100), q.getAskPrice());
		assertEquals("Incorrect BID", new Price(80), q.getBidPrice());
		assertEquals("Incorrect ASK quantity", 1, q.getAskQuantity());
		assertEquals("Incorrect BID quantity", 1, q.getBidQuantity());
		// Check that market2's QuoteProcessor hasn't updated yet
		q = smip2.quote;
		assertEquals("Incorrect last quote time", null, smip2.lastQuoteTime);
		assertEquals("Incorrect ASK", null, q.getAskPrice());
		assertEquals("Incorrect BID", null, q.getBidPrice());
		assertEquals("Incorrect ASK quantity", 0, q.getAskQuantity());
		assertEquals("Incorrect BID quantity", 0, q.getBidQuantity());
		
		// Check market2's QuoteProcessor quote after time 100
		em.executeUntil(new TimeStamp(101));
		q = smip2.quote;
		assertEquals("Incorrect last quote time", mktTime2, smip2.lastQuoteTime);
		assertEquals("Incorrect ASK", new Price(95), q.getAskPrice());
		assertEquals("Incorrect BID", new Price(75), q.getBidPrice());
		assertEquals("Incorrect ASK quantity", 1, q.getAskQuantity());
		assertEquals("Incorrect BID quantity", 1, q.getBidQuantity());		
	}
	
	/**
	 * Test scenario where marketTime of two quotes is the same. Only works for
	 * markets with latency IMMEDIATE (otherwise will be nondeterministic).
	 * 
	 * XXX Cannot test market2's HFT IP quote due to nondeterminism when equal market times
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
		em.addActivity(new SendToIP(market1, mktTime, q1, new ArrayList<Transaction>(), smip1, time));
		em.addActivity(new SendToIP(market2, mktTime, q4, new ArrayList<Transaction>(), smip2, time));
		
		// Check market1's QuoteProcessor has updated but not market2's after time 0
		em.executeUntil(new TimeStamp(1));
		q = smip1.quote;
		assertEquals("Incorrect last quote time", mktTime, smip1.lastQuoteTime);
		assertEquals("Incorrect ASK", new Price(100), q.getAskPrice());
		assertEquals("Incorrect BID", new Price(80), q.getBidPrice());
		assertEquals("Incorrect ASK quantity", 1, q.getAskQuantity());
		assertEquals("Incorrect BID quantity", 1, q.getBidQuantity());
		q = smip2.quote;
		assertEquals("Updated QuoteProcessor 2 too early", null, smip2.lastQuoteTime);
		assertEquals("Incorrect ASK", null, q.getAskPrice());
		assertEquals("Incorrect BID", null, q.getBidPrice());
		assertEquals("Incorrect ASK quantity", 0, q.getAskQuantity());
		assertEquals("Incorrect BID quantity", 0, q.getBidQuantity());
		
		// Send stale quotes to QuoteProcessors
		em.addActivity(new SendToIP(market1, mktTime, q3, new ArrayList<Transaction>(), smip1, time));
		em.addActivity(new SendToIP(market2, mktTime, q2, new ArrayList<Transaction>(), smip2, time));
		
		// Check that market1's QuoteProcessor quote updates to most recent quote
		em.executeUntil(new TimeStamp(1));
		q = smip1.quote;
		assertEquals("Incorrect last quote time", mktTime, smip1.lastQuoteTime);
		assertEquals("Incorrect ASK", new Price(95), q.getAskPrice());
		assertEquals("Incorrect BID", new Price(75), q.getBidPrice());
		assertEquals("Incorrect ASK quantity", 1, q.getAskQuantity());
		assertEquals("Incorrect BID quantity", 1, q.getBidQuantity());
	}

	
	/**
	 * Testing QuoteProcessor with zero, not immediate, latency.
	 */
	@Test
	public void zeroNotImmedLatency() {
		Quote q;
		TimeStamp time = TimeStamp.ZERO;
		MarketTime mktTime1 = new DummyMarketTime(time, 1);
		MarketTime mktTime2 = new DummyMarketTime(time, 2);
		MarketTime mktTime3 = new DummyMarketTime(time, 3);
		
		// Create market with latency 0
		Market market3 = new MockMarket(sip, TimeStamp.ZERO);
		QuoteProcessor smip3 = market3.getQuoteProcessor();
		
		// Add new quote
		Quote q5 = new Quote(market3, new Price(80), 1, new Price(100), 1, time);
		Iterable<? extends Activity> acts = smip3.sendToIP(market3, mktTime1, q5, 
				new ArrayList<Transaction>(), time);
		for (Activity a : acts) { // Verify correct process quote activity added
			assertEquals("Incorrect scheduled process quote time", 
					TimeStamp.ZERO, a.getTime()); // not immediate
			assertTrue("Incorrect activity type scheduled", 
					a instanceof ProcessQuote);
		}
		for (Activity a : acts) a.execute(a.getTime());

		// Check updated quote after process quote
		q = smip3.quote;
		assertEquals("Last quote time not updated", mktTime1, smip3.lastQuoteTime);
		assertEquals("Incorrect ASK", new Price(100), q.getAskPrice());
		assertEquals("Incorrect BID", new Price(80), q.getBidPrice());
		assertEquals("Incorrect ASK quantity", 1, q.getAskQuantity());
		assertEquals("Incorrect BID quantity", 1, q.getBidQuantity());
		
		// Now try inserting multiple quotes all at the same time, but different marketTimes
		Quote q6 = new Quote(market3, new Price(75), 1, new Price(95), 1, time);
		Quote q7 = new Quote(market3, new Price(60), 1, new Price(90), 1, time);
		
		// Send quotes to appropriate IPs
		EventManager em = new EventManager(new Random());
		em.addActivity(new SendToIP(market3, mktTime2, q6, new ArrayList<Transaction>(), smip3, time));
		em.addActivity(new SendToIP(market3, mktTime3, q7, new ArrayList<Transaction>(), smip3, time));
		
		// Check market3's QuoteProcessor has updated after time 0
		em.executeUntil(new TimeStamp(1));
		q = smip3.quote;
		assertEquals("Last quote time not updated", mktTime3, smip3.lastQuoteTime);
		assertEquals("Incorrect ASK", new Price(90), q.getAskPrice());
		assertEquals("Incorrect BID", new Price(60), q.getBidPrice());
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
			setup();
			zeroNotImmedLatency();
		}
	}
}
