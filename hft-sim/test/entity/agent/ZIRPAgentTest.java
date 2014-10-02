package entity.agent;

import static logger.Log.log;
import static logger.Log.Level.DEBUG;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.Random;

import logger.Log;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import data.EntityProperties;
import data.FundamentalValue;
import data.MockFundamental;
import entity.infoproc.SIP;
import entity.market.Market;
import entity.market.MockMarket;
import event.TimeStamp;

import systemmanager.Consts;
import systemmanager.Executor;
import systemmanager.Keys;
import activity.AgentStrategy;

public class ZIRPAgentTest {
	
	private Executor exec;
	private FundamentalValue fundamental = new MockFundamental(100000);
	private Market market;
	private SIP sip;
	private static Random rand;
	private static EntityProperties agentProperties = 
		EntityProperties.fromPairs(
			Keys.REENTRY_RATE, 0,
			Keys.MAX_QUANTITY, 2,
			Keys.PRIVATE_VALUE_VAR, 0,
			Keys.BID_RANGE_MIN, 0,
			Keys.BID_RANGE_MAX, 5000,
			Keys.SIMULATION_LENGTH, 60000,
			Keys.FUNDAMENTAL_KAPPA, 0.05,
			Keys.FUNDAMENTAL_MEAN, 100000,
			Keys.ACCEPTABLE_PROFIT_FRACTION, 0.8
		);
	
	public ZIRPAgent createAgent(Object... parameters) {
		return createAgent(
			fundamental, 
			market, 
			rand, 
			parameters
		);
	}
	
	public ZIRPAgent createAgent(
		final FundamentalValue fundamental, 
		final Market market, 
		final Random rand, 
		final Object... parameters
	) {
		return new ZIRPAgent(
			exec, 
			TimeStamp.ZERO, 
			fundamental, 
			sip, 
			market,
			rand, 
			EntityProperties.copyFromPairs(
				agentProperties,	
				parameters
			)
		);
	}
	
	@BeforeClass
	public static void setUpClass() throws IOException{
		// Setting up the log file
		log = Log.create(DEBUG, new File(Consts.TEST_OUTPUT_DIR + "ZIRAgentTest.log"));

		// Creating the setup properties
		rand = new Random();
	}

	@Before
	public void setup(){
		exec = new Executor();
		sip = new SIP(exec, TimeStamp.IMMEDIATE);
		// Creating the MockMarket
		market = new MockMarket(exec, sip);
	}

	@Test
	public void withdrawTest() {
		// verify that orders are correctly withdrawn at each re-entry
		ZIRPAgent agent = createAgent(
				Keys.MAX_QUANTITY, 2,
				Keys.WITHDRAW_ORDERS, true);

		// execute strategy once; then before reenter, change the position balance
		// that way, when execute strategy again, it won't submit new orders
		exec.executeActivity(new AgentStrategy(agent));
		// verify that order submitted
		assertEquals(1, agent.activeOrders.size());
		agent.positionBalance = 10;
		exec.executeActivity(new AgentStrategy(agent));
		// verify that order withdrawn
		assertEquals(0, agent.activeOrders.size());
	}
}
