package entity.infoproc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;

import logger.Logger;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import systemmanager.Consts;
import systemmanager.Executor;
import activity.ProcessQuote;
import activity.SendToQP;
import entity.market.DummyMarketTime;
import entity.market.Market;
import entity.market.MarketTime;
import entity.market.MockMarket;
import entity.market.Price;
import entity.market.Quote;
import event.TimeStamp;
import event.TimedActivity;

public class QuoteProcessorTest {

	private static final TimeStamp one = TimeStamp.create(1);
	
	private Executor exec;
	private Market market1;
	private Market market2;
	private SIP sip;
	private AbstractQuoteProcessor smip1;
	private AbstractQuoteProcessor smip2;

	@BeforeClass
	public static void setupClass() {
		Logger.setup(3, new File(Consts.TEST_OUTPUT_DIR + "QuoteProcessorTest.log"));
	}

	@Before
	public void setup() {
		exec = new Executor();
		sip = new SIP(exec, TimeStamp.IMMEDIATE);
		// market that updates immediately
		market1 = new MockMarket(exec, sip);
		smip1 = market1.getQuoteProcessor();
		// market with latency 100 (means that QuoteProcessor will have latency 100)
		market2 = new MockMarket(exec, sip, TimeStamp.create(100));
		smip2 = market2.getQuoteProcessor();
	}


	@Test
	public void basicProcessQuote() {
		TimeStamp time = TimeStamp.ZERO;
		MarketTime mktTime = new DummyMarketTime(time, 1);
		
		assertEquals(TimeStamp.IMMEDIATE, smip1.getLatency());
		assertEquals(TimeStamp.create(100), smip2.getLatency());
		
		// Check initial quote is null
		Quote q = smip1.quote;
		assertEquals("Incorrect last quote time", TimeStamp.ZERO, q.getQuoteTime());
		assertEquals("Incorrect ASK", null, q.getAskPrice());
		assertEquals("Incorrect BID", null, q.getBidPrice());
		assertEquals("Incorrect ASK quantity", 0, q.getAskQuantity());
		assertEquals("Incorrect BID quantity", 0, q.getBidQuantity());
		// Check initial quote is null
		q = smip2.quote;
		assertEquals("Incorrect last quote time", TimeStamp.ZERO, q.getQuoteTime());
		assertEquals("Incorrect ASK", null, q.getAskPrice());
		assertEquals("Incorrect BID", null, q.getBidPrice());
		assertEquals("Incorrect ASK quantity", 0, q.getAskQuantity());
		assertEquals("Incorrect BID quantity", 0, q.getBidQuantity());

		// Test on undelayed market's QuoteProcessor
		q = new Quote(market1, new Price(80), 1, new Price(100), 1, mktTime);
		smip1.processQuote(market1, q, time);
		assertTrue(exec.isEmpty());
		
		// Check updated quote after process quote (specific to QuoteProcessor)
		q = smip1.quote;
		assertEquals("Last quote time not updated", mktTime, q.getQuoteTime());
		assertEquals("Incorrect ASK", new Price(100), q.getAskPrice());
		assertEquals("Incorrect BID", new Price(80), q.getBidPrice());
		assertEquals("Incorrect ASK quantity", 1, q.getAskQuantity());
		assertEquals("Incorrect BID quantity", 1, q.getBidQuantity());
		
		// Now test for delayed market's QuoteProcessor
		q = new Quote(market2, new Price(80), 1, new Price(100), 1, mktTime);
		smip2.processQuote(market2, q, time);
		assertTrue(exec.isEmpty());
		
		// Check updated quote after process quote

		q = smip2.quote;
		assertEquals("Last quote time not updated", mktTime, q.getQuoteTime());
		assertEquals("Incorrect ASK", new Price(100), q.getAskPrice());
		assertEquals("Incorrect BID", new Price(80), q.getBidPrice());
		assertEquals("Incorrect ASK quantity", 1, q.getAskQuantity());
		assertEquals("Incorrect BID quantity", 1, q.getBidQuantity());
	}
	
	@Test
	public void basicNoDelay() {
		Quote q;
		TimeStamp time = TimeStamp.create(1);
		MarketTime mktTime = new DummyMarketTime(time, 1);

		// Check initial quote is null

		q = smip1.quote;
		assertEquals("Incorrect last quote time", TimeStamp.ZERO, q.getQuoteTime());
		assertEquals("Incorrect ASK", null, q.getAskPrice());
		assertEquals("Incorrect BID", null, q.getBidPrice());
		assertEquals("Incorrect ASK quantity", 0, q.getAskQuantity());
		assertEquals("Incorrect BID quantity", 0, q.getBidQuantity());

		// Add new quote
		q = new Quote(market1, new Price(80), 1, new Price(100), 1, mktTime);
		smip1.sendToQuoteProcessor(market1, q, time);

		// Check updated quote after process quote (specific to QuoteProcessor)

		q = smip1.quote;
		assertEquals("Last quote time not updated", mktTime, q.getQuoteTime());
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

		q = smip2.quote;
		assertEquals("Incorrect last quote time", TimeStamp.ZERO, q.getQuoteTime());
		assertEquals("Incorrect ASK", null, q.getAskPrice());
		assertEquals("Incorrect BID", null, q.getBidPrice());
		assertEquals("Incorrect ASK quantity", 0, q.getAskQuantity());
		assertEquals("Incorrect BID quantity", 0, q.getBidQuantity());
		
		// Check that process quote activity scheduled correctly
		q = new Quote(market2, new Price(80), 1, new Price(100), 1, mktTime);
		smip2.sendToQuoteProcessor(market2, q, time);
		TimedActivity act = exec.peek();
		assertEquals("Incorrect scheduled process quote time", smip2.latency,
				act.getTime());
		assertTrue("Incorrect activity type scheduled",
				act.getActivity() instanceof ProcessQuote);
		
		exec.executeUntil(smip2.latency.plus(one));

		// Check updated quote after process quote (specific to QuoteProcessor)
		q = smip2.quote;
		assertEquals("Last quote time not updated", mktTime, q.getQuoteTime());
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
		Quote q = new Quote(market2, new Price(80), 1, new Price(100), 1, mktTime);
		MarketQuoteProcessor smipImmed = new MarketQuoteProcessor(exec, TimeStamp.IMMEDIATE, market2);
		
		exec.executeActivity(new SendToQP(market2, q, smip2));
		exec.executeActivity(new SendToQP(market2, q, smipImmed));

		// Check immediate QuoteProcessor
		q = smipImmed.quote;
		assertEquals("Last quote time not updated", mktTime, q.getQuoteTime());
		assertEquals("Incorrect ASK", new Price(100), q.getAskPrice());
		assertEquals("Incorrect BID", new Price(80), q.getBidPrice());
		assertEquals("Incorrect ASK quantity", 1, q.getAskQuantity());
		assertEquals("Incorrect BID quantity", 1, q.getBidQuantity());
		// Check delayed QuoteProcessor
		q = smip2.quote;
		assertEquals("Updated QuoteProcessor 2 too early", TimeStamp.ZERO, q.getQuoteTime());
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
		Quote q1 = new Quote(market1, new Price(80), 1, new Price(100), 1, mktTime);
		Quote q2 = new Quote(market2, new Price(75), 1, new Price(95), 1, mktTime);
		
		exec.executeActivity(new SendToQP(market1, q1, smip1));
		exec.executeActivity(new SendToQP(market2, q2, smip2));
		
		// Check QuoteProcessor for market 1 updated
		q = smip1.quote;
		assertEquals("Last quote time not updated", mktTime, q.getQuoteTime());
		assertEquals("Incorrect ASK", new Price(100), q.getAskPrice());
		assertEquals("Incorrect BID", new Price(80), q.getBidPrice());
		assertEquals("Incorrect ASK quantity", 1, q.getAskQuantity());
		assertEquals("Incorrect BID quantity", 1, q.getBidQuantity());
		// Check QuoteProcessor for market 2 not updated
		q = smip2.quote;
		assertEquals("Updated QuoteProcessor 2 too early", TimeStamp.ZERO, q.getQuoteTime());
		assertEquals("Incorrect ASK", null, q.getAskPrice());
		assertEquals("Incorrect BID", null, q.getBidPrice());
		assertEquals("Incorrect ASK quantity", 0, q.getAskQuantity());
		assertEquals("Incorrect BID quantity", 0, q.getBidQuantity());
		
		exec.executeUntil(smip2.latency.plus(TimeStamp.create(1)));
		// Check QuoteProcessor for market 2 updated
		q = smip2.quote;
		assertEquals("Last quote time not updated", mktTime, q.getQuoteTime());
		assertEquals("Incorrect ASK", new Price(95), q.getAskPrice());
		assertEquals("Incorrect BID", new Price(75), q.getBidPrice());
		assertEquals("Incorrect ASK quantity", 1, q.getAskQuantity());
		assertEquals("Incorrect BID quantity", 1, q.getBidQuantity());
	}
	
	@Test
	public void multiQuoteUpdates() {
		Quote q;
		TimeStamp time = TimeStamp.ZERO;
		TimeStamp time2 = TimeStamp.create(50);
		MarketTime mktTime1 = new DummyMarketTime(time, 1);
		MarketTime mktTime2 = new DummyMarketTime(time, 2);
		Quote q1 = new Quote(market1, new Price(80), 1, new Price(100), 1, mktTime1);
		Quote q2 = new Quote(market2, new Price(80), 1, new Price(100), 1, mktTime2);
		Quote q3 = new Quote(market1, new Price(75), 1, new Price(95), 1, mktTime2);
		Quote q4 = new Quote(market2, new Price(75), 1, new Price(95), 1, mktTime1);
		
		exec.executeActivity(new SendToQP(market1, q1, smip1));
		exec.executeActivity(new SendToQP(market2, q4, smip2));
		// Send updated quotes at time2
		exec.scheduleActivity(time2, new SendToQP(market1, q3, smip1));
		exec.scheduleActivity(time2, new SendToQP(market2, q2, smip2));
		
		// Check that market1's QuoteProcessor has updated but not market2's after time2=50
		exec.executeUntil(time2.plus(one));
		q = smip1.quote;
		assertEquals("Last quote time not updated", mktTime2, q.getQuoteTime());
		assertEquals("Incorrect ASK", new Price(95), q.getAskPrice());
		assertEquals("Incorrect BID", new Price(75), q.getBidPrice());
		assertEquals("Incorrect ASK quantity", 1, q.getAskQuantity());
		assertEquals("Incorrect BID quantity", 1, q.getBidQuantity());
		q = smip2.quote;
		assertEquals("Updated QuoteProcessor 2 too early", TimeStamp.ZERO, q.getQuoteTime());
		assertEquals("Incorrect ASK", null, q.getAskPrice());
		assertEquals("Incorrect BID", null, q.getBidPrice());
		assertEquals("Incorrect ASK quantity", 0, q.getAskQuantity());
		assertEquals("Incorrect BID quantity", 0, q.getBidQuantity());
		
		// Check that market2's QuoteProcessor received (75,95) by time 101
		exec.executeUntil(TimeStamp.create(101));
		q = smip2.quote;
		assertEquals("Incorrect last quote time", mktTime1, q.getQuoteTime());
		assertEquals("Incorrect ASK", new Price(95), q.getAskPrice());
		assertEquals("Incorrect BID", new Price(75), q.getBidPrice());
		assertEquals("Incorrect ASK quantity", 1, q.getAskQuantity());
		assertEquals("Incorrect BID quantity", 1, q.getBidQuantity());
		
		// Quote (80,100) is processed by market2's QuoteProcessor after time 150
		exec.executeUntil(TimeStamp.create(151));
		q = smip2.quote;
		assertEquals("Last quote time not updated", mktTime2, q.getQuoteTime());
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
		Quote q1 = new Quote(market1, new Price(80), 1, new Price(100), 1, mktTime1);
		Quote q2 = new Quote(market2, new Price(80), 1, new Price(100), 1, mktTime2);
		Quote q3 = new Quote(market1, new Price(75), 1, new Price(95), 1, mktTime2);
		Quote q4 = new Quote(market2, new Price(75), 1, new Price(95), 1, mktTime1);
		
		exec.executeActivity(new SendToQP(market1, q1, smip1));
		exec.executeActivity(new SendToQP(market2, q4, smip2));
		// Send updated quotes (also at time 0)
		exec.executeActivity(new SendToQP(market1, q3, smip1));
		exec.executeActivity(new SendToQP(market2, q2, smip2));
		
		// Check market1's QuoteProcessor has updated but not market2's after time 0
		exec.executeUntil(one);
		q = smip1.quote;
		assertEquals("Last quote time not updated", mktTime2, q.getQuoteTime());
		assertEquals("Incorrect ASK", new Price(95), q.getAskPrice());
		assertEquals("Incorrect BID", new Price(75), q.getBidPrice());
		assertEquals("Incorrect ASK quantity", 1, q.getAskQuantity());
		assertEquals("Incorrect BID quantity", 1, q.getBidQuantity());
		q = smip2.quote;
		assertEquals("Updated QuoteProcessor 2 too early", TimeStamp.ZERO, q.getQuoteTime());
		assertEquals("Incorrect ASK", null, q.getAskPrice());
		assertEquals("Incorrect BID", null, q.getBidPrice());
		assertEquals("Incorrect ASK quantity", 0, q.getAskQuantity());
		assertEquals("Incorrect BID quantity", 0, q.getBidQuantity());

		// Check market2's QuoteProcessor has received most recent quote(80,100) after time 100
		exec.executeUntil(TimeStamp.create(101));
		q = smip2.quote;
		assertEquals("Incorrect last quote time", mktTime2, q.getQuoteTime());
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
		Quote q1 = new Quote(market1, new Price(80), 1, new Price(100), 1, mktTime2);
		Quote q2 = new Quote(market2, new Price(80), 1, new Price(100), 1, mktTime1);
		Quote q3 = new Quote(market1, new Price(75), 1, new Price(95), 1, mktTime1);
		Quote q4 = new Quote(market2, new Price(75), 1, new Price(95), 1, mktTime2);
		
		exec.executeActivity(new SendToQP(market1, q1, smip1));
		exec.executeActivity(new SendToQP(market2, q4, smip2));
		
		// Check market1's QuoteProcessor has updated but not market2's after time 0
		q = smip1.quote;
		assertEquals("Last quote time not updated", mktTime2, q.getQuoteTime());
		assertEquals("Incorrect ASK", new Price(100), q.getAskPrice());
		assertEquals("Incorrect BID", new Price(80), q.getBidPrice());
		assertEquals("Incorrect ASK quantity", 1, q.getAskQuantity());
		assertEquals("Incorrect BID quantity", 1, q.getBidQuantity());
		q = smip2.quote;
		assertEquals("Updated QuoteProcessor 2 too early", TimeStamp.ZERO, q.getQuoteTime());
		assertEquals("Incorrect ASK", null, q.getAskPrice());
		assertEquals("Incorrect BID", null, q.getBidPrice());
		assertEquals("Incorrect ASK quantity", 0, q.getAskQuantity());
		assertEquals("Incorrect BID quantity", 0, q.getBidQuantity());
				
		// Send stale quotes to QuoteProcessors
		exec.executeActivity(new SendToQP(market1, q3, smip1));
		exec.executeActivity(new SendToQP(market2, q2, smip2));
		
		// Check that market1's QuoteProcessor quote doesn't change
		q = smip1.quote;
		assertEquals("Incorrect last quote time", mktTime2, q.getQuoteTime());
		assertEquals("Incorrect ASK", new Price(100), q.getAskPrice());
		assertEquals("Incorrect BID", new Price(80), q.getBidPrice());
		assertEquals("Incorrect ASK quantity", 1, q.getAskQuantity());
		assertEquals("Incorrect BID quantity", 1, q.getBidQuantity());
		// Check that market2's QuoteProcessor hasn't updated yet
		q = smip2.quote;
		assertEquals("Incorrect last quote time", TimeStamp.ZERO, q.getQuoteTime());
		assertEquals("Incorrect ASK", null, q.getAskPrice());
		assertEquals("Incorrect BID", null, q.getBidPrice());
		assertEquals("Incorrect ASK quantity", 0, q.getAskQuantity());
		assertEquals("Incorrect BID quantity", 0, q.getBidQuantity());
		
		// Check market2's QuoteProcessor quote after time 100
		exec.executeUntil(TimeStamp.create(101));
		q = smip2.quote;
		assertEquals("Incorrect last quote time", mktTime2, q.getQuoteTime());
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
		Quote q1 = new Quote(market1, new Price(80), 1, new Price(100), 1, mktTime);
		Quote q2 = new Quote(market2, new Price(80), 1, new Price(100), 1, mktTime);
		Quote q3 = new Quote(market1, new Price(75), 1, new Price(95), 1, mktTime);
		Quote q4 = new Quote(market2, new Price(75), 1, new Price(95), 1, mktTime);
		
		exec.executeActivity(new SendToQP(market1, q1, smip1));
		exec.executeActivity(new SendToQP(market2, q4, smip2));
		
		// Check market1's QuoteProcessor has updated but not market2's after time 0
		q = smip1.quote;
		assertEquals("Incorrect last quote time", mktTime, q.getQuoteTime());
		assertEquals("Incorrect ASK", new Price(100), q.getAskPrice());
		assertEquals("Incorrect BID", new Price(80), q.getBidPrice());
		assertEquals("Incorrect ASK quantity", 1, q.getAskQuantity());
		assertEquals("Incorrect BID quantity", 1, q.getBidQuantity());
		q = smip2.quote;
		assertEquals("Updated QuoteProcessor 2 too early", TimeStamp.ZERO, q.getQuoteTime());
		assertEquals("Incorrect ASK", null, q.getAskPrice());
		assertEquals("Incorrect BID", null, q.getBidPrice());
		assertEquals("Incorrect ASK quantity", 0, q.getAskQuantity());
		assertEquals("Incorrect BID quantity", 0, q.getBidQuantity());
		
		// Send stale quotes to QuoteProcessors
		exec.executeActivity(new SendToQP(market1, q3, smip1));
		exec.executeActivity(new SendToQP(market2, q2, smip2));
		
		// Check that market1's QuoteProcessor quote updates to most recent quote
		exec.executeUntil(TimeStamp.ZERO);
		q = smip1.quote;
		assertEquals("Incorrect last quote time", mktTime, q.getQuoteTime());
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
		Market market3 = new MockMarket(exec, sip, TimeStamp.ZERO);
		MarketQuoteProcessor smip3 = market3.getQuoteProcessor();
		
		// Add new quote
		Quote q5 = new Quote(market3, new Price(80), 1, new Price(100), 1, mktTime1);
		smip3.sendToQuoteProcessor(market3, q5, time);
		TimedActivity act = exec.peek();
		assertEquals("Incorrect scheduled process quote time", TimeStamp.ZERO,
				act.getTime()); // not immediate
		assertTrue("Incorrect activity type scheduled",
				act.getActivity() instanceof ProcessQuote);
		exec.executeUntil(one);

		// Check updated quote after process quote
		q = smip3.quote;
		assertEquals("Last quote time not updated", mktTime1, q.getQuoteTime());
		assertEquals("Incorrect ASK", new Price(100), q.getAskPrice());
		assertEquals("Incorrect BID", new Price(80), q.getBidPrice());
		assertEquals("Incorrect ASK quantity", 1, q.getAskQuantity());
		assertEquals("Incorrect BID quantity", 1, q.getBidQuantity());
		
		// Now try inserting multiple quotes all at the same time, but different marketTimes
		Quote q6 = new Quote(market3, new Price(75), 1, new Price(95), 1, mktTime2);
		Quote q7 = new Quote(market3, new Price(60), 1, new Price(90), 1, mktTime3);
		
		exec.executeActivity(new SendToQP(market3, q6, smip3));
		exec.executeActivity(new SendToQP(market3, q7, smip3));
		
		// Check market3's QuoteProcessor has updated after time 0
		exec.executeUntil(one);
		q = smip3.quote;
		assertEquals("Last quote time not updated", mktTime3, q.getQuoteTime());
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
