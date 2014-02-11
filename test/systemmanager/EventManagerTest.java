package systemmanager;

import static org.junit.Assert.*;
import static logger.Logger.Level.*;
import static logger.Logger.logger;

import java.io.File;
import java.io.IOException;
import java.util.Random;

import logger.Logger;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import activity.MockActivity;
import activity.MockAgentActivity;
import data.MockFundamental;
import data.FundamentalValue;
import entity.agent.MockBackgroundAgent;
import entity.infoproc.SIP;
import entity.market.MockMarket;
import event.TimeStamp;

public class EventManagerTest {

	private FundamentalValue fundamental = new MockFundamental(100000);
	private EventManager em;
	private MockMarket market;
	private SIP sip;
	
	@BeforeClass
	public static void setupClass() throws IOException {
		logger = Logger.create(DEBUG, new File(Consts.TEST_OUTPUT_DIR + "EventManagerTest.log"));
	}

	@Before
	public void setup() {
		em = new EventManager(new Random());
		sip = new SIP(TimeStamp.IMMEDIATE);
		market = new MockMarket(sip, TimeStamp.IMMEDIATE);
	}
	
	@Test
	public void basicExecution() {
		TimeStamp time = new TimeStamp(10);
		
		// Initially empty, time 0
		assertEquals(0, em.eventQueue.size());
		assertEquals(TimeStamp.ZERO, em.getCurrentTime());
		
		em.addActivity(new MockActivity(time));
		assertEquals(1, em.eventQueue.size());
		assertTrue(em.eventQueue.peek() instanceof MockActivity);
		assertEquals(time, em.eventQueue.peek().getTime());
		// Verify that current time is 0 until first activity is executed
		assertEquals(TimeStamp.ZERO, em.getCurrentTime());
		
		em.executeNext();
		// Check that activity did in fact execute
		assertEquals(0, em.eventQueue.size());
		assertEquals(time, em.getCurrentTime());
	}
	
	@Test
	public void executeUntilTest() {
		TimeStamp time = new TimeStamp(100);
		em.addActivity(new MockActivity(time));
		em.addActivity(new MockActivity(time.plus(time)));
		
		assertEquals(2, em.eventQueue.size());
		assertEquals(TimeStamp.ZERO, em.getCurrentTime());
		
		// Check that second activity hasn't executed yet
		em.executeUntil(time.plus(time));
		assertEquals(1, em.eventQueue.size());
		assertEquals(time, em.getCurrentTime());
		assertEquals(time.plus(time), em.eventQueue.peek().getTime());
		
		em.executeNext();
		// Check that activity did in fact execute
		assertEquals(0, em.eventQueue.size());
		assertEquals(time.plus(time), em.getCurrentTime());	
		assertEquals(null, em.eventQueue.peek());
	}
	
	
	@Test
	public void chainingActivityExecution() {
		// testing activities that insert further activities
		// MockAgentActivity will insert a MockActivity
		TimeStamp time = new TimeStamp(10);
		
		MockBackgroundAgent agent = new MockBackgroundAgent(fundamental, sip, market);
		assertEquals(0, em.eventQueue.size());
		assertEquals(TimeStamp.ZERO, em.getCurrentTime());
		
		em.addActivity(new MockAgentActivity(agent, time));
		// Verify that new activity has been added correctly 
		// and that current time is 0 until first activity is executed
		assertEquals(1, em.eventQueue.size());
		assertTrue(em.eventQueue.peek() instanceof MockAgentActivity);
		assertEquals(time, em.eventQueue.peek().getTime());
		assertEquals(TimeStamp.ZERO, em.getCurrentTime());
		
		em.executeNext();
		// Check that MockAgentActivity did in fact execute
		assertEquals(1, em.eventQueue.size());
		assertTrue(em.eventQueue.peek() instanceof MockActivity);
		assertEquals(time, em.eventQueue.peek().getTime());
		assertEquals(time, em.getCurrentTime());

		em.executeNext();
		// Check that MockActivity did in fact execute
		assertEquals(0, em.eventQueue.size());
		assertEquals(time, em.getCurrentTime());
		assertEquals(null, em.eventQueue.peek());
	}
}
