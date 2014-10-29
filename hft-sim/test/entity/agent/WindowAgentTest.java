package entity.agent;

import static com.google.common.base.Preconditions.checkArgument;
import static fourheap.Order.OrderType.BUY;
import static fourheap.Order.OrderType.SELL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static utils.Tests.assertSingleTransaction;
import static utils.Tests.checkTransaction;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import logger.Log;

import org.junit.Before;
import org.junit.Test;

import systemmanager.Keys.MarketLatency;
import systemmanager.Keys.WindowLength;
import utils.Mock;

import com.google.common.collect.Lists;

import data.Props;
import entity.market.CDAMarket;
import entity.market.Market;
import entity.market.Market.MarketView;
import entity.market.Price;
import entity.market.Transaction;
import event.EventQueue;
import event.TimeStamp;

/**
 * @author yngchen, ewah
 *
 */
public class WindowAgentTest {
	private static final Random rand = new Random();
	private static final Agent mockAgent = Mock.agent();
	
	private EventQueue timeline;
	private Market market;
	private MarketView fast;
	
	// FIXME should a window account for latency?

	@Before
	public void setup() throws IOException {
		timeline = EventQueue.create(Log.nullLogger(), rand);
		market = CDAMarket.create(0, Mock.stats, timeline, Log.nullLogger(), rand, Mock.sip, Props.fromPairs());
		fast = market.getView(TimeStamp.ZERO);
	}
	
	@Test
	public void basicWindowTest() {		
		WindowAgent agent = windowAgent();
		assertTrue("Incorrect initial transactions in window", agent.getWindowTransactions().isEmpty());
		
		addTransaction(Price.of(110), 1, TimeStamp.ZERO);
		
		// basic window check
		assertEquals("Incorrect # transactions", 1, agent.getWindowTransactions().size());
	
		// checking just inside window
		timeline.executeUntil(TimeStamp.of(9));
		assertEquals("Incorrect # transactions", 1, agent.getWindowTransactions().size());
		
		// check outside window
		timeline.executeUntil(TimeStamp.of(10));
		assertTrue("Window not empty", agent.getWindowTransactions().isEmpty());
	}
	
	@Test
	public void delayedTransactionProcessorTest() throws IOException {
		latencySetup(TimeStamp.of(5));
		WindowAgent agent = windowAgent();
		
		// populate market with a transaction
		addTransaction(Price.of(110), 1, TimeStamp.ZERO);
		addTransaction(Price.of(103), 1, TimeStamp.of(1));
		
		// test getting transactions at time 5 - should be missing the second transaction
		timeline.executeUntil(TimeStamp.of(5));
		assertEquals("Incorrect # transactions", 1, agent.getWindowTransactions().size());
		
		// check time 10
		timeline.executeUntil(TimeStamp.of(10));
		assertEquals("Incorrect # transactions", 1, agent.getWindowTransactions().size());
		
		// check outside window (at time 11)
		timeline.executeUntil(TimeStamp.of(11));
		assertTrue("Incorrect # transactions", agent.getWindowTransactions().isEmpty());
	}
	
	@Test
	public void extraTest() throws IOException {
		for (int i = 0; i < 100; i++) {
			setup();
			randMultipleTransactionWindow();
		}
	}
	
	@Test
	public void singleTransactionWindow() {
		WindowAgent agent = windowAgent();
		assertEquals("WindowAgent Window Length is incorrect", TimeStamp.of(10), agent.getWindowLength());
		
		addTransaction(Price.of(20), 1, TimeStamp.of(10));
		timeline.executeUntil(TimeStamp.of(15));
		assertSingleTransaction(agent.getWindowTransactions(), Price.of(20), TimeStamp.of(10), 1);
	}
	
	@Test
	public void multipleTransactionWindow() {
		WindowAgent agent = windowAgent();

		addTransaction(Price.of(40), 1, TimeStamp.of(2));
		addTransaction(Price.of(30), 1, TimeStamp.of(3));
		addTransaction(Price.of(60), 1, TimeStamp.of(5));	// ^ Outside Window ^
		addTransaction(Price.of(10), 1, TimeStamp.of(10));
		addTransaction(Price.of(20), 1, TimeStamp.of(11));
		addTransaction(Price.of(50), 1, TimeStamp.of(14));	// ^ Inside Window ^

		timeline.executeUntil(TimeStamp.of(15));
		
		Iterator<Transaction> windowTransactions = agent.getWindowTransactions().iterator(); // Window is from (5,15]

		assertTrue("Incorrect Window Size", windowTransactions.hasNext());
		checkTransaction(windowTransactions.next(), Price.of(50), TimeStamp.of(14), 1);
		
		assertTrue("Incorrect Window Size", windowTransactions.hasNext());
		checkTransaction(windowTransactions.next(), Price.of(20), TimeStamp.of(11), 1);
		
		assertTrue("Incorrect Window Size", windowTransactions.hasNext());
		checkTransaction(windowTransactions.next(), Price.of(10), TimeStamp.of(10), 1);
		
		assertFalse("Incorrect Window Size", windowTransactions.hasNext());
	}
	
	@Test
	public void multipleTransactionWindowLatency() throws IOException{
		latencySetup(TimeStamp.of(100));
		WindowAgent agent = windowAgent(Props.fromPairs(WindowLength.class, TimeStamp.of(160)));

		addTransaction(Price.of(40), 1, TimeStamp.of(50));
		// Agent can't see the transaction due to latency
		assertTrue("Window Transactions should be empty", agent.getWindowTransactions().isEmpty());
		
		addTransaction(Price.of(60), 1, TimeStamp.of(100));
		// Agent can't see the transaction due to latency
		assertTrue("Window Transactions should be empty", agent.getWindowTransactions().isEmpty());
		
		// First transaction comes into the window
		timeline.executeUntil(TimeStamp.of(150));
		assertEquals("Window Transactions should be size 1", 1, agent.getWindowTransactions().size());
		
		// Second transaction comes into the window
		timeline.executeUntil(TimeStamp.of(200));
		assertEquals("Window Transactions should be size 2", 2, agent.getWindowTransactions().size());
		
		// First transaction leaves the window
		timeline.executeUntil(TimeStamp.of(250));
		//Assert that the window returns one transaction
		assertEquals("Window Transactions should be size 1", 1, agent.getWindowTransactions().size());
		
		// All transactions leave the window
		timeline.executeUntil(TimeStamp.of(300));
		assertTrue("Window Transactions should be empty", agent.getWindowTransactions().isEmpty());
	}
	
	
	@Test
	public void randMultipleTransactionWindow(){
		int numberOfTransactions = 10;
		TimeStamp windowLength = TimeStamp.of(rand.nextInt(100));
		TimeStamp reentryTime = TimeStamp.of(windowLength.getInTicks() + rand.nextInt(100));
		WindowAgent myAgent = windowAgent(Props.fromPairs(WindowLength.class, windowLength));
		
		//Keep track of how many transactions should be in the window
		int numWindow = 0;
		List<TimeStamp> transactionTimes = Lists.newArrayListWithCapacity(numberOfTransactions);
		for(int j = 0; j < numberOfTransactions; j++){
			//Transaction times must be before re entry time
			TimeStamp transactionTime = TimeStamp.of(rand.nextInt((int) reentryTime.getInTicks()));
			transactionTimes.add(transactionTime);
			if (transactionTime.after(reentryTime.minus(windowLength)))
				numWindow++;
		}
		
		//Sort transaction times into ascending order so we can add transactions in the proper order
		Collections.sort(transactionTimes);
		for(TimeStamp time : transactionTimes)
			addTransaction(Price.of(100), 1, time);
		
		timeline.executeUntil(reentryTime);
		assertEquals("Number of transactions is not correct", numWindow, myAgent.getWindowTransactions().size());
	}
	
	//Testing methods==============================================================================

	private void latencySetup(TimeStamp marketLatency) {
		market = CDAMarket.create(0, Mock.stats, timeline, Log.nullLogger(), rand, Mock.sip, Props.fromPairs(MarketLatency.class, marketLatency));
		fast = market.getView(TimeStamp.ZERO);
	}
	
	private void addTransaction(Price price, int quantity, TimeStamp time) {
		checkArgument(!time.before(timeline.getCurrentTime()), "Can't execute earlier than current time");
		timeline.executeUntil(time);
		mockAgent.submitOrder(fast, BUY, price, quantity);
		mockAgent.submitOrder(fast, SELL, price, quantity);
		timeline.executeUntil(time);
	}
	
	private WindowAgent windowAgent(Props parameters) {
		return new WindowAgent(0, Mock.stats, timeline, Log.nullLogger(), rand, Mock.sip, Mock.fundamental, market,
				Props.merge(Props.fromPairs(WindowLength.class, TimeStamp.of(10)), parameters)) {
			private static final long serialVersionUID = 1L;
		};
	}
	
	private WindowAgent windowAgent() {
		return windowAgent(Props.fromPairs());
	}
	
}
