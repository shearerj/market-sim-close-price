package entity.agent;

import static org.junit.Assert.*;
import static fourheap.Order.OrderType.*;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import logger.Logger;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import systemmanager.Consts;
import systemmanager.Executor;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.Iterables;

import data.MockFundamental;
import data.FundamentalValue;
import entity.infoproc.SIP;
import entity.market.MockMarket;
import entity.market.Order;
import entity.market.Price;
import event.TimeStamp;

/**
 * ZIAgent Test
 * 
 * Testing of ZI strategy 
 * Uses loops and randomized parameters to account for randomized bid ranges and private values
 * 
 * NOTE: Just because a test fails *does not* mean that the ZIAgent is not performing to spec,
 * 		 because of the normal distribution of private values. There is a 0.3% chance of 
 * 		 a private value falling outside the +/- 3 stdev range. This corresponds to
 * 		 1 in 370 tests.
 * 
 * @author yngchen
 * 
 */
public class ZIAgentTest {	

	private Executor exec;
	private FundamentalValue fundamental = new MockFundamental(100000);
	private MockMarket market;
	private SIP sip;
	private static Random rand;

	@BeforeClass
	public static void setUpClass(){
		// Setting up the log file
		Logger.setup(3, new File(Consts.TEST_OUTPUT_DIR + "ZIAgentTest.log"));

		// Creating the setup properties
		rand = new Random(1);
	}

	@Before
	public void setup(){
		exec = new Executor();
		sip = new SIP(exec, TimeStamp.IMMEDIATE);
		// Creating the MockMarket
		market = new MockMarket(exec, sip);
	}
	
	//Agent Constructor methods====================================================================
	
	private ZIAgent addAgent(int min, int max, Random rand, PrivateValue pv){
		//Initialize ZIAgent with default properties:
		//REENTRY_RATE, 0
		//TICK_SIZE, 1
		return new ZIAgent(exec, new TimeStamp(0), fundamental, sip, market, rand, 0, pv, 1, min, max);
	}
	
	
	private ZIAgent addAgent(int min, int max, Random rand) {
		//Initialize ZIAgent with default properties:
		//REENTRY_RATE, 0
		//PRIVATE_VALUE_VAR, 100000000
		//TICK_SIZE, 1
		return addAgent(min, max, rand, new PrivateValue(1, 100000000, rand));
	}
	
	private ZIAgent addAgent(int min, int max){
		//Initialize ZIAgent with default properties:
		//REENTRY_RATE, 0
		//PRIVATE_VALUE_VAR, 100000000 = +/- $10.00
		//TICK_SIZE, 1
		//Random rand
		return addAgent(min, max, rand);
	}
	
	private ZIAgent addAgent(){
		//Initialize ZIAgent with default properties:
		//REENTRY_RATE, 0
		//PRIVATE_VALUE_VAR, 100000000 = +/- $10.00
		//TICK_SIZE, 1
		//Random rand
		//BID_RANGE_MIN, 0
		//BID_RANGE_MAX, 1000 = +/- $1.00
		return addAgent(0, 1000); 
	}
	
	//Testing methods==============================================================================
	private void assertCorrectBid(Agent agent, int low, int high) {
		Collection<Order> orders = agent.activeOrders;
		// Asserting the bid is correct
		assertNotEquals("OrderSize is incorrect", 0, orders.size());
		Order order = Iterables.getFirst(orders, null);

		assertNotEquals("Order's agent is null", null, order.getAgent());
		assertEquals("Order agent is incorrect", agent, order.getAgent());

		Price bidPrice = order.getPrice();
		assertTrue("Order price (" + bidPrice + ") less than " + low,
				bidPrice.greaterThan(new Price(low)));
		assertTrue("Order price (" + bidPrice + ") greater than " + high,
				bidPrice.lessThan(new Price(high)));

		// Quantity must be 1 or -1
		assertEquals("Quantity is incorrect", 1, order.getQuantity());
	}
	
	private void assertCorrectBid(Agent agent) {
		assertCorrectBid(agent, -1, Integer.MAX_VALUE);
	}
	
	//Tests========================================================================================
	
	@Test
	public void initialActivityZI(){
		Logger.log(Logger.Level.DEBUG, "Testing ZI Activity is correct");
		// New ZIAgent
		ZIAgent testAgent = addAgent();
		TimeStamp currentTime = new TimeStamp(100);
		testAgent.agentStrategy(currentTime);
		int submit_counter = market.getActiveOrders().size();
		assertEquals("ZI SubmitNMSOrder quantity (" + submit_counter + ") not equal to 1", 1, submit_counter);
	}


	@Test
	public void initialQuantityZI() {
		Logger.log(Logger.Level.DEBUG, "Testing ZI submitted quantity is correct");
		
		// New ZIAgent
		ZIAgent testAgent = addAgent();
		
		// Execute Strategy
		testAgent.agentStrategy(new TimeStamp(100));
		assertCorrectBid(testAgent);
	}
	
	@Test
	public void initialPriceZI() {
		Logger.log(Logger.Level.DEBUG, "Testing ZI submitted bid range is correct");
		
		// New ZIAgent
		ZIAgent testAgent = addAgent();
		
		// Execute Strategy
		testAgent.agentStrategy(new TimeStamp(100));
		
		//Fundamental = 100000 ($100.00), Stdev = sqrt(100000000) = 10000 ($10.000)
		//Bid Range = 0, 1000 ($0.00, $1.00)
		//99.7% of bids should fall between 100000 +/- (3*10000 + 1000)
		// = 70000, 13000
		assertCorrectBid(testAgent, 70000, 130000);
	}
	
	@Test
	public void randQuantityZI(){
		Logger.log(Logger.Level.DEBUG, "Testing ZI 100 submitted quantities are correct");
		for(int r = 0; r<100; r++){
			// New ZIAgent
			ZIAgent testAgent = addAgent(0,1000, new Random(r));
			
			// Execute Strategy
			testAgent.agentStrategy(new TimeStamp(r*100));
			assertCorrectBid(testAgent);
		}
	}
	
	@Test
	public void randPriceZI(){
		Logger.log(Logger.Level.DEBUG, "Testing ZI 100 submitted bid ranges are correct");
		for(int r = 0; r<100; r++){
			// New ZIAgent
			ZIAgent testAgent = addAgent(0,1000, new Random(r));
			
			// Execute Strategy
			testAgent.agentStrategy(new TimeStamp(r*100));
			assertCorrectBid(testAgent, 70000, 130000);	
		}
	}
	
	@Test
	public void testPrivateValue(){
		Logger.log(Logger.Level.DEBUG, "Testing ZI 100 DummyPrivateValue arguments are correct");
		
		//Creating set private value
		int offset = 1;
		Builder<Price> builder = ImmutableList.builder();
		builder.add(new Price(10000)); 			//$10.00
		builder.add(new Price(-10000));  		//$-10.00
		List<Price> prices = builder.build();	//PVs = [$10, $-10]
		DummyPrivateValue testpv = new DummyPrivateValue(offset, prices);
		
		//Creating ZIAgent
		ZIAgent testAgent = addAgent(0, 1000, rand, testpv); 	// bid range [0, 1000]

		//Executing agent strategy
		testAgent.agentStrategy(new TimeStamp(100));

		//Retrieving orders
		Collection<Order> orders = testAgent.activeOrders;
		assertEquals(1, orders.size());
		Order order = Iterables.getFirst(orders, null);

		//Extracting bid quantity and price
		assertEquals("Incorrect order quantity", 1, order.getQuantity());
		Price p = order.getPrice();

		//Checking bid quantity and price comply with set private values
		// Sellers always sell at price higher than valuation ($100 + sell PV = $110)
		// Buyers always buy at price lower than valuation ($100 + buy PV = $90)
		switch(order.getOrderType()){
		case SELL:
			assertTrue("Ask Price (" + p + ") less than $110.00", p.greaterThanEqual(new Price(110000)));
			assertTrue("Ask Price (" + p + ") greater than $111.00", p.lessThan(new Price(111000)));
			// Expected ask range min = fundamental + PV[0] + bidRangeMin
			// Expected ask range max = fundamental + PV[0] + bidRangeMax
			break;
		case BUY:
			assertTrue("Bid Price (" + p + ") less than $89.00", p.greaterThan(new Price(89000)));
			assertTrue("Bid Price (" + p + ") greater than $90.00", p.lessThanEqual(new Price(90000)));
			//Expected bid range min = fundamental + PV[1] - bidRangeMax
			//Expected bid range max = fundamental + PV[1] - bidRangeMin
			break;
		default:
			fail("Invalid order type");
			break;
		}
	}
	
	@Test
	public void extraTest() {
		for(int i = 0; i<100; i++){
			setup();
			testPrivateValue();
		}
		
	}
	
	@Test
	public void randTestZI(){
		Logger.log(Logger.Level.DEBUG, "Testing ZI 100 random argument bids are correct");

		//Testing 100 times
		for(int i = 0; i<100; i++){
			
			int currentTime = 100;
			
			//Creating randomized private value
			int offset = 1;
			PrivateValue testpv = new PrivateValue(offset, 1000000, rand);
			
			//Creating randomized min and max bid range
			int min = rand.nextInt(5000); 			//[$0.00, $5.00];
			int max = 5000 + rand.nextInt(5000);	//[$5.00, $10.00];
			
			//Creating ZIAgent
			ZIAgent testAgent = addAgent(min, max, rand, testpv);
			
			//Logging bid range min and max
			Logger.log(Logger.Level.DEBUG, "Agent bid range min: " + new Price(min) + ", maximum: " + new Price(max));
			
			//Execute strategy
			testAgent.agentStrategy(new TimeStamp(currentTime));
			
			//Retrieve orders
			Collection<Order> orders = testAgent.activeOrders;
			assertEquals(1, orders.size());
			Order order = Iterables.getFirst(orders, null);
			
			//Extracting bid quantity and price
			assertEquals("Incorrect order quantity", 1, order.getQuantity());
			Price p = order.getPrice();
			Price fund = fundamental.getValueAt(new TimeStamp(currentTime));
			Price buyPV = testAgent.privateValue.getValue(0, BUY);
			Price sellPV = testAgent.privateValue.getValue(0, SELL);
			
			//Checking bid quantity and price comply with randomized range
			// Sellers always sell at price higher than valuation ($100 + sell PV)
			// Buyers always buy at price lower than valuation ($100 + buy PV)
			switch(order.getOrderType()){
			case SELL:
				Price ask_min = new Price(fund.intValue() + sellPV.intValue() + min);
				Price ask_max = new Price(fund.intValue() + sellPV.intValue() + max);
				assertTrue(ask_min.lessThanEqual(ask_max));
				assertTrue("Ask Price (" + p + ") less than " + ask_min, p.greaterThan(ask_min));
				assertTrue("Ask Price (" + p + ") greater than " + ask_max, p.lessThan(ask_max));
				//Expected ask range min = fundamental + PV[0] + bidRangeMin
				//Expected ask range max = fundamental + PV[0] + bidRangeMax
				break;
			case BUY:
				Price bid_min = new Price(fund.intValue() + buyPV.intValue() - max);
				Price bid_max = new Price(fund.intValue() + buyPV.intValue() - min);
				assertTrue(bid_min.lessThanEqual(bid_max));
				assertTrue("Bid Price (" + p + ") less than " + bid_min, p.greaterThan(bid_min));
				assertTrue("Bid Price (" + p + ") greater than " + bid_max, p.lessThan(bid_max));
				//Expected bid range min = fundamental + PV[1] - bidRangeMax
				//Expected bid range max = fundamental + PV[1] - bidRangeMin
				break;
			default:
				fail("Invalid order type");
				break;
			}
		
		}
	}
}
