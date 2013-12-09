package entity.agent;

import static org.junit.Assert.*;

import java.io.File;
import java.util.Iterator;
import java.util.Random;

import logger.Logger;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.Iterables;

import activity.Activity;
import activity.AgentStrategy;
import data.FundamentalValue;
import data.MockFundamental;
import entity.infoproc.SIP;
import entity.market.Market;
import entity.market.MockMarket;
import event.TimeStamp;
import systemmanager.Consts;

public class ReentryAgentTest {

	private Market market;
	private SIP sip;
	
	@BeforeClass
	public static void setupClass() {
		Logger.setup(3, new File(Consts.TEST_OUTPUT_DIR + "ReentryAgentTest.log"));
	}
	
	@Before
	public void setup() {
		sip = new SIP(TimeStamp.IMMEDIATE);
		market = new MockMarket(sip);
	}
	
	@Test
	public void reentryTest() {
		TimeStamp time = new TimeStamp(100);
		FundamentalValue fundamental = new MockFundamental(100000);
		
		MockReentryAgent agent = new MockReentryAgent(fundamental, sip, market, new Random(), 0.1, 1);
		
		// Test reentries
		Iterator<TimeStamp> reentries = agent.getReentryTimes(); 
		assertTrue(reentries.hasNext());
		TimeStamp next = reentries.next();
		assertTrue(next.getInTicks() >= 0);
		
		// Test agent strategy
		Iterable<? extends Activity> acts = agent.agentStrategy(time);
		Activity act = Iterables.getFirst(acts, null);
		assertTrue( act instanceof AgentStrategy );
		assertTrue( act.getTime().getInTicks() >= time.getInTicks());
	}
	
	@Test
	public void extraTest() {
		for (int i = 0; i <= 100; i++) {
			setup();
			reentryTest();
		}
	}
}
