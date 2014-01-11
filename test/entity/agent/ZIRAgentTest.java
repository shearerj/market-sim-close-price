package entity.agent;

import static org.junit.Assert.*;

import java.io.File;
import java.util.Random;

import logger.Logger;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import activity.AgentStrategy;
import systemmanager.Consts;
import systemmanager.EventManager;
import systemmanager.Keys;
import data.EntityProperties;
import data.FundamentalValue;
import data.MockFundamental;
import entity.infoproc.SIP;
import entity.market.Market;
import entity.market.MockMarket;
import event.TimeStamp;

/**
 * Only testing order withdrawal, everything else is just same as ZIAgent.
 * 
 * @author ewah
 *
 */
public class ZIRAgentTest {

	private FundamentalValue fundamental = new MockFundamental(100000);
	private Market market;
	private SIP sip;
	private static Random rand;
	private static EntityProperties agentProperties;
	
	@BeforeClass
	public static void setUpClass(){
		// Setting up the log file
		Logger.setup(3, new File(Consts.TEST_OUTPUT_DIR + "ZIRAgentTest.log"));

		// Creating the setup properties
		rand = new Random(1);
		
		// Setting up agentProperties
		agentProperties = new EntityProperties();
		agentProperties.put(Keys.REENTRY_RATE, 0);
		agentProperties.put(Keys.MAX_QUANTITY, 2);
		agentProperties.put(Keys.PRIVATE_VALUE_VAR, 0);
	}
	
	@Before
	public void setup(){
		sip = new SIP(TimeStamp.IMMEDIATE);
		// Creating the MockMarket
		market = new MockMarket(sip);
	}
	
	@Test
	public void withdrawTest() {
		TimeStamp time = TimeStamp.ZERO;
		TimeStamp time1 = new TimeStamp(1);
		EventManager em = new EventManager(rand);
		
		EntityProperties testProps = new EntityProperties(agentProperties);
		
		// verify that orders are correctly withdrawn at each re-entry
		ZIRAgent agent = new ZIRAgent(TimeStamp.ZERO, fundamental, sip, market, 
				rand, testProps);
		
		// execute strategy once; then before reenter, change the position balance
		// that way, when execute strategy again, it won't submit new orders
		em.addActivity(new AgentStrategy(agent, time));
		em.executeUntil(time1);
		// verify that order submitted
		assertEquals(1, agent.activeOrders.size());
		agent.positionBalance = 10;
		em.addActivity(new AgentStrategy(agent, time));
		em.executeUntil(time1);
		// verify that order withdrawn
		assertEquals(0, agent.activeOrders.size());
	}
}
