package entity.agent;

import static logger.Logger.log;
import static org.junit.Assert.*;

import java.io.File;
import java.util.Collection;
import java.util.Random;

import logger.Logger;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import systemmanager.Keys;
import activity.Activity;
import activity.ProcessQuote;
import activity.SendToIP;
import activity.SubmitNMSOrder;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.Iterables;

import data.DummyFundamental;
import data.EntityProperties;
import data.FundamentalValue;
import entity.infoproc.SIP;
import entity.market.Market;
import entity.market.MockMarket;
import entity.market.Order;
import entity.market.Price;
import event.TimeStamp;

public class ZIAgentTest {	
	
private FundamentalValue fundamental = new DummyFundamental(100000);
private Market market;
private SIP sip;
private static Random rand;





	@BeforeClass
	public static void setUpClass(){
		// Setting up the log file
		Logger.setup(3, new File("simulations/unit_testing/ZIAgentTest.log"));

		// Creating the setup properties
		rand = new Random(4);
		
		//Setting up properties


		
	}

	@Before
	public void setUpTest(){
		sip = new SIP(TimeStamp.IMMEDIATE);
		// Creating the MockMarket
		market = new MockMarket(sip);
	}
	
	private ZIAgent addAgent(int min, int max) {
		//Initialize ZIAgent with default properties:
		//REENTRY_RATE, 0
		//PRIVATE_VALUE_VAR, 100000000
		//TICK_SIZE, 1
		EntityProperties agentProperties = new EntityProperties();
		agentProperties.put(Keys.BID_RANGE_MIN, min);
		agentProperties.put(Keys.BID_RANGE_MAX, max);
		ZIAgent agent = new ZIAgent(new TimeStamp(0), fundamental, sip, market, rand, agentProperties);
		
		return agent;
	}
	
	private ZIAgent addAgent(){
		//Initialize ZIAgent with default properties:
		//REENTRY_RATE, 0
		//PRIVATE_VALUE_VAR, 100000000
		//TICK_SIZE, 1
		//BID_RANGE_MIN, 0
		//BID_RANGE_MAX, 5000
		return addAgent(0,5000);
	}
	
	
	private void executeAgentStrategy(Agent agent, int time) {
		TimeStamp currentTime = new TimeStamp(time);
		Iterable<? extends Activity> test = agent.agentStrategy(currentTime);

		// executing the bid submission - will go to the market
		for (Activity act : test)
			if (act instanceof SubmitNMSOrder)
				act.execute(currentTime);
	}

	private void assertCorrectBid(Agent agent, int low, int high) {
		Collection<Order> orders = agent.activeOrders;
		// Asserting the bid is correct
		assertTrue("OrderSize is incorrect", !orders.isEmpty());
		Order order = Iterables.getFirst(orders, null);

		assertTrue("Order agent is null", order.getAgent() != null);
		assertTrue("Order agent is incorrect", order.getAgent().equals(agent));

		Price bidPrice = order.getPrice();
		assertTrue("Order price (" + bidPrice + ") less than " + low,
				bidPrice.greaterThan(new Price(low)));
		assertTrue("Order price (" + bidPrice + ") greater than " + high,
				bidPrice.lessThan(new Price(high)));

		// Quantity must be 1 or -1
		assertTrue("Quantity is incorrect", order.getQuantity() == 1 || order.getQuantity() == -1);
	}
	
	private void assertCorrectBid(Agent agent) {
		assertCorrectBid(agent, -1, Integer.MAX_VALUE);
	}


	@Test
	public void initialQuantityZI() {
		Logger.log(Logger.Level.DEBUG, "Testing ZI submitted quantity is correct");
		
		// New ZIAgent
		ZIAgent testAgent = addAgent(0,1000);
		
		// Execute strategy
		executeAgentStrategy(testAgent, 100);
		
		assertCorrectBid(testAgent);
		
	}
	
	@Test
	public void initialPriceZI() {
		Logger.log(Logger.Level.DEBUG, "Testing ZI submitted bid range is correct");
		
		// New ZIAgent
		ZIAgent testAgent = addAgent(0,1000);
		
		// Execute strategy
		executeAgentStrategy(testAgent, 100);
		
		assertCorrectBid(testAgent, 80000, 120000);
		
	}
}
