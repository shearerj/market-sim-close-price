package entity.agent;

import static org.junit.Assert.*;

import java.io.File;
import java.util.Iterator;
import java.util.Random;

import logger.Logger;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import activity.AgentStrategy;
import data.FundamentalValue;
import data.MockFundamental;
import entity.infoproc.SIP;
import entity.market.Market;
import entity.market.MockMarket;
import event.TimeStamp;
import event.TimedActivity;
import systemmanager.Consts;
import systemmanager.Executor;

public class ReentryAgentTest {

	private Executor exec;
	private Market market;
	private SIP sip;
	
	@BeforeClass
	public static void setupClass() {
		Logger.setup(3, new File(Consts.TEST_OUTPUT_DIR + "ReentryAgentTest.log"));
	}
	
	@Before
	public void setup() {
		exec = new Executor();
		sip = new SIP(exec, TimeStamp.IMMEDIATE);
		market = new MockMarket(exec, sip);
	}
	
	@Test
	public void reentryTest() {
		TimeStamp time = TimeStamp.create(100);
		FundamentalValue fundamental = new MockFundamental(100000);
		
		MockReentryAgent agent = new MockReentryAgent(exec, fundamental, sip, market, new Random(), 0.1, 1);
		
		// Test reentries
		Iterator<TimeStamp> reentries = agent.getReentryTimes(); 
		assertTrue(reentries.hasNext());
		TimeStamp next = reentries.next();
		assertTrue(next.getInTicks() >= 0);
		
		// Test agent strategy
		agent.agentStrategy(time);
		TimedActivity act = exec.peek();
		assertTrue( act.getActivity() instanceof AgentStrategy );
		assertTrue( act.getTime().getInTicks() >= time.getInTicks());
	}
	
	// FIXME This test doesn't make sense. Rate 0 shouldn't be allowed.
//	@Test
//	public void reentryRateZeroTest() {
//		TimeStamp time = TimeStamp.create(100);
//		FundamentalValue fundamental = new MockFundamental(100000);
//		
//		// Test reentries - note should never iterate past INFINITE b/c it 
//		// will never execute
//		MockReentryAgent agent = new MockReentryAgent(exec, fundamental, sip, market, new Random(), 0, 1);
//		Iterator<TimeStamp> reentries = agent.getReentryTimes(); 
//		assertTrue(reentries.hasNext());
//		TimeStamp next = reentries.next();
//		assertTrue(next.getInTicks() >= 0);
//		assertEquals(TimeStamp.INFINITE, next);
//
//		// Now test for agent, which should arrive at time 0
//		MockReentryAgent agent2 = new MockReentryAgent(exec, fundamental, sip, market, new Random(), 0, 1);
//		agent2.agentStrategy(time);
//		TimedActivity act = exec.peek();
//		assertTrue( act.getActivity() instanceof AgentStrategy );
//		assertEquals( TimeStamp.INFINITE, act.getTime());
//	}
	
	@Test
	public void extraTest() {
		for (int i = 0; i <= 100; i++) {
			setup();
			reentryTest();
		}
	}
}
