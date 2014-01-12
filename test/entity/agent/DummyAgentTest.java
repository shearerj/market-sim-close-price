package entity.agent;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Collection;

import logger.Logger;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import systemmanager.Consts;
import activity.Activity;
import data.FundamentalValue;
import data.MockFundamental;
import entity.agent.DummyAgent;
import entity.infoproc.SIP;
import entity.market.Market;
import entity.market.MockMarket;
import entity.market.Order;
import event.TimeStamp;

public class DummyAgentTest {

	private FundamentalValue fundamental = new MockFundamental(100000);
	private Market market;
	private SIP sip;

	@BeforeClass
	public static void setupClass() {
		// Setting up the log file
		Logger.setup(3, new File(Consts.TEST_OUTPUT_DIR + "DummyAgentTest.log"));
	}

	@Before
	public void setupTest() {
		sip = new SIP(TimeStamp.IMMEDIATE);
		// Creating the MockMarket
		market = new MockMarket(sip);
	}

	private DummyAgent addAgent() {
		DummyAgent agent = new DummyAgent(fundamental, sip, market);

		return agent;
	}

	
	@Test
	public void strategyTest(){
		Logger.log(Logger.Level.DEBUG," \n Testing DummyAgent strategy , should return an empty set \n");
		DummyAgent agent = addAgent();
		TimeStamp t = new TimeStamp(0);
		// on any market , DummyAgent should have no strategy
		Collection<? extends Activity> c = agent.agentStrategy(t);
		
		assertTrue("DummyAgent Strategy is not empty",c.isEmpty());
	}
	
	@Test
	public void ordersTest(){
		Logger.log(Logger.Level.DEBUG, "\n Testing DummyAgent Orders , should be an empty set\n");
		DummyAgent agent = addAgent();
		Collection<Order> orderCollection = agent.getOrders();
		
		assertTrue("DummyAgent has Orders",orderCollection.isEmpty());
	}
	
}
