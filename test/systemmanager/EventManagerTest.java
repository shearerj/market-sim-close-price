package systemmanager;

import static org.junit.Assert.*;

import java.io.File;
import java.util.Random;

import logger.Logger;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import activity.MockActivity;
import activity.MockAgentActivity;
import data.DummyFundamental;
import data.FundamentalValue;
import entity.agent.MockBackgroundAgent;
import entity.infoproc.SIP;
import entity.market.MockMarket;
import event.TimeStamp;

public class EventManagerTest {

	private FundamentalValue fundamental = new DummyFundamental(100000);
	private EventManager em;
	private MockMarket market;
	private SIP sip;
	
	@BeforeClass
	public static void setupClass() {
		Logger.setup(3, new File(Consts.TEST_OUTPUT_DIR + "EventManagerTest.log"));
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
		assertEquals(em.eventQueue.size(), 0 );
		assertEquals(em.getCurrentTime(), TimeStamp.ZERO);
		
		em.addActivity(new MockActivity(time));
		assertEquals(em.eventQueue.size(), 1 );
		assertTrue(em.eventQueue.peek() instanceof MockActivity);
		assertEquals(em.eventQueue.peek().getTime(), time);
		// Verify that current time is 0 until first activity is executed
		assertEquals(em.getCurrentTime(), TimeStamp.ZERO);
		
		em.executeNext();
		// Check that activity did in fact execute
		assertEquals(em.eventQueue.size(), 0 );
		assertEquals(em.getCurrentTime(), time);
	}
	
	@Test
	public void executeUntilTest() {
		TimeStamp time = new TimeStamp(100);
		em.addActivity(new MockActivity(time));
		em.addActivity(new MockActivity(time.plus(time)));
		
		assertEquals(em.eventQueue.size(), 2 );
		assertEquals(em.getCurrentTime(), TimeStamp.ZERO);
		
		// Check that second activity hasn't executed yet
		em.executeUntil(time.plus(time));
		assertEquals(em.eventQueue.size(), 1 );
		assertEquals(em.getCurrentTime(), time);
		assertEquals(em.eventQueue.peek().getTime(), time.plus(time));
		
		em.executeNext();
		// Check that activity did in fact execute
		assertEquals(em.eventQueue.size(), 0 );
		assertEquals(em.getCurrentTime(), time.plus(time));	
		assertEquals(em.eventQueue.peek(), null);
	}
	
	
	@Test
	public void chainingActivityExecution() {
		// testing activities that insert further activities
		// MockAgentActivity will insert a MockActivity
		TimeStamp time = new TimeStamp(10);
		
		MockBackgroundAgent agent = new MockBackgroundAgent(fundamental, sip, market);
		assertEquals(em.eventQueue.size(), 0 );
		assertEquals(em.getCurrentTime(), TimeStamp.ZERO);
		
		em.addActivity(new MockAgentActivity(agent, time));
		// Verify that new activity has been added correctly 
		// and that current time is 0 until first activity is executed
		assertEquals(em.eventQueue.size(), 1 );
		assertTrue(em.eventQueue.peek() instanceof MockAgentActivity);
		assertEquals(em.eventQueue.peek().getTime(), time);
		assertEquals(em.getCurrentTime(), TimeStamp.ZERO);
		
		em.executeNext();
		// Check that MockAgentActivity did in fact execute
		assertEquals(em.eventQueue.size(), 1 );
		assertTrue(em.eventQueue.peek() instanceof MockActivity);
		assertEquals(em.eventQueue.peek().getTime(), time);
		assertEquals(em.getCurrentTime(), time);

		em.executeNext();
		// Check that MockActivity did in fact execute
		assertEquals(em.eventQueue.size(), 0 );
		assertEquals(em.getCurrentTime(), time);
		assertEquals(em.eventQueue.peek(), null);
	}
}
