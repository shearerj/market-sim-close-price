package entity.agent;

import static org.junit.Assert.assertTrue;
import static logger.Logger.Level.*;
import static logger.Logger.logger;

import java.io.File;
import java.io.IOException;
import java.util.Random;

import logger.Logger;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import systemmanager.Consts;
import activity.Activity;

import com.google.common.collect.Iterables;

import data.FundamentalValue;
import data.MockFundamental;
import entity.infoproc.SIP;
import event.TimeStamp;

public class NoOpAgentTest {

	private FundamentalValue fundamental = new MockFundamental(100000);
	private SIP sip;

	@BeforeClass
	public static void setupClass() throws IOException {
		// Setting up the log file
		logger = Logger.create(DEBUG, new File(Consts.TEST_OUTPUT_DIR + "NoOpAgentTest.log"));
	}

	@Before
	public void setupTest() {
		sip = new SIP(TimeStamp.IMMEDIATE);
	}
	
	// TODO Test orders and strategy over the course of a simulation
	@Test
	public void strategyTest(){
		NoOpAgent agent = new NoOpAgent(fundamental, sip, new Random(), 1);
		Iterable<? extends Activity> c = agent.agentStrategy(TimeStamp.ZERO);
		assertTrue("NoOpAgent Strategy is not empty", Iterables.isEmpty(c));
	}
	
	@Test
	public void ordersTest(){
		//NoOpAgent agent = new NoOpAgent(fundamental, sip, new Random(), 1);
	}
	
}
