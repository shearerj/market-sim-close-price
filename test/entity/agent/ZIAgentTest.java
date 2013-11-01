package entity.agent;

import static org.junit.Assert.*;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import logger.Logger;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import activity.Activity;
import activity.SubmitNMSOrder;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.Iterables;

import data.DummyFundamental;
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
		

		
	}

	@Before
	public void setUpTest(){
		sip = new SIP(TimeStamp.IMMEDIATE);
		// Creating the MockMarket
		market = new MockMarket(sip);
	}
	
	private ZIAgent addAgent(int min, int max, Random rand, PrivateValue pv){
		//Initialize ZIAgent with default properties:
		//REENTRY_RATE, 0
		//TICK_SIZE, 1
		ZIAgent agent = new ZIAgent(new TimeStamp(0), fundamental, sip, market, rand, 0, pv, 1, min, max);
		
		return agent;
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
		Logger.log(Logger.Level.DEBUG, ">>initialQuantityZI:");
		Logger.log(Logger.Level.DEBUG, "Testing ZI submitted quantity is correct");
		
		// New ZIAgent
		ZIAgent testAgent = addAgent();
		
		// Execute Strategy
		executeAgentStrategy(testAgent, 100);
		
		assertCorrectBid(testAgent);
		
	}
	
	@Test
	public void initialPriceZI() {
		Logger.log(Logger.Level.DEBUG, ">>initialPriceZI:");
		Logger.log(Logger.Level.DEBUG, "Testing ZI submitted bid range is correct");
		
		// New ZIAgent
		ZIAgent testAgent = addAgent();
		
		// Execute Strategy
		executeAgentStrategy(testAgent, 100);
		
		//Fundamental = 100000 ($100.00), Stdev = sqrt(100000000) = 10000 ($10.000)
		//Bid Range = 0, 1000 ($0.00, $1.00)
		//99.7% of bids should fall between 100000 +/- (3*10000 + 1000)
		// = 70000, 13000
		assertCorrectBid(testAgent, 70000, 130000);
		
	}
	
	@Test
	public void randQuantityZI(){
		Logger.log(Logger.Level.DEBUG, ">>RandQuantityZI:");
		Logger.log(Logger.Level.DEBUG, "Testing ZI 100 submitted quantities are correct");
		for(int r = 0; r<100; r++){
			// New ZIAgent
			ZIAgent testAgent = addAgent(0,1000, new Random(r));
			
			// Execute Strategy
			executeAgentStrategy(testAgent, r*100);
			
			assertCorrectBid(testAgent);

			
		}
		
	}
	
	@Test
	public void randPriceZI(){
		Logger.log(Logger.Level.DEBUG, ">>RandPriceZI:");
		Logger.log(Logger.Level.DEBUG, "Testing ZI 100 submitted bid ranges are correct");
		for(int r = 0; r<100; r++){
			// New ZIAgent
			ZIAgent testAgent = addAgent(0,1000, new Random(r));
			
			// Execute Strategy
			executeAgentStrategy(testAgent, r*100);
			
			assertCorrectBid(testAgent, 70000, 130000);

			
		}
	}
	
	@Test
	public void testPrivateValue(){
		Logger.log(Logger.Level.DEBUG, ">>testPrivateValue:");
		Logger.log(Logger.Level.DEBUG, "Testing ZI 100 MockPrivateValue arguments are correct");
		int offset = 1;
		Builder<Price> builder = ImmutableList.builder();
		builder.add(new Price(10000)); 			//$10.00
		builder.add(new Price(0));     			//$0.00
		builder.add(new Price(-10000));  		//$-10.00
		List<Price> prices = builder.build();	//Prices = [$10, $0, $-10]
		DummyPrivateValue testpv = new DummyPrivateValue(offset, prices);
		
		ZIAgent testAgent = addAgent(0, 1000, rand, testpv);
		
		for(int i = 0; i<100; i++){
			
			executeAgentStrategy(testAgent, 100);
			
			Collection<Order> orders = testAgent.activeOrders;
			Order order = Iterables.getFirst(orders, null);
			int quantity = order.getQuantity();
			Price bidPrice = order.getPrice();
			switch(quantity){
			case -1:
				assertTrue("Ask Price (" + bidPrice + ") less than $109.00", bidPrice.greaterThan(new Price(109000)));
				assertTrue("Ask Price (" + bidPrice + ") greater than $110.00", bidPrice.lessThan(new Price(110000)));
				//Expected ask range min = fundamental + (PV[-1] - PV[0]) - bidRangeMax
				//Expected ask range max = fundamental + (PV[-1] - PV[0]) - bidRangeMin
				//*Index values expressed relative to median index [1]
				break;
			case 1:
				assertTrue("Bid Price (" + bidPrice + ") less than $90.00", bidPrice.greaterThan(new Price(90000)));
				assertTrue("Bid Price (" + bidPrice + ") greater than $91.00", bidPrice.lessThan(new Price(91000)));
				//Expected bid range min = fundamental + (PV[1] - PV[0]) + bidRangeMin
				//Expected bid range max = fundamental + (PV[1] - PV[0]) + bidRangeMax
				//*Index values expressed relative to median index [1]
				break;
			default:
				break;
			}
		}
	}
	
	@Test
	public void randTestZI(){
		Logger.log(Logger.Level.DEBUG, ">>RandTestZI:");
		Logger.log(Logger.Level.DEBUG, "Testing ZI 100 random argument bids are correct");

		
		for(int i = 0; i<100; i++){
			
			int offset = 1;
		
			Builder<Price> builder = ImmutableList.builder();
			builder.add(new Price(rand.nextInt(90000))); 	//[$0.00, $90.00]	
			builder.add(new Price(0));     						//[$0.00]
			builder.add(new Price(-1*rand.nextInt(90000)));  	//[$0.00, -$90.00]
			List<Price> prices = builder.build();	
			DummyPrivateValue testpv = new DummyPrivateValue(offset, prices);
			
			int min = rand.nextInt(5000); 		//[$0.00, $5.00]
			int max = min + rand.nextInt(5000);	//[min, $10.00]
			
			ZIAgent testAgent = addAgent(min, max, rand, testpv);
			
			Logger.log(Logger.Level.DEBUG, "Agent bid minimum: " + new Price(min) + ", maximum: " + new Price(max));
			
			executeAgentStrategy(testAgent, 100);
			
			Collection<Order> orders = testAgent.activeOrders;
			Order order = Iterables.getFirst(orders, null);
			int quantity = order.getQuantity();
			Price bidPrice = order.getPrice();
			switch(quantity){
			case -1:
				Price ask_min = new Price(100000 + (prices.get(0).intValue() - prices.get(1).intValue())- max);
				Price ask_max = new Price(100000 + (prices.get(0).intValue() - prices.get(1).intValue())- min);
				assertTrue("Ask Price (" + bidPrice + ") less than " + ask_min, bidPrice.greaterThan(ask_min));
				assertTrue("Ask Price (" + bidPrice + ") greater than " + ask_max, bidPrice.lessThan(ask_max));
				//Expected ask range min = fundamental + (PV[-1] - PV[0]) - bidRangeMax
				//Expected ask range max = fundamental + (PV[-1] - PV[0]) - bidRangeMin
				//*Index values expressed relative to median index [1]
				break;
			case 1:
				Price bid_min = new Price(100000 + (prices.get(2).intValue() - prices.get(1).intValue()) + min);
				Price bid_max = new Price(100000 + (prices.get(2).intValue() - prices.get(1).intValue()) + max);
				assertTrue("Bid Price (" + bidPrice + ") less than " + bid_min, bidPrice.greaterThan(bid_min));
				assertTrue("Bid Price (" + bidPrice + ") greater than " + bid_max, bidPrice.lessThan(bid_max));
				//Expected bid range min = fundamental + (PV[1] - PV[0]) + bidRangeMin
				//Expected bid range max = fundamental + (PV[1] - PV[0]) + bidRangeMax
				//*Index values expressed relative to median index [1]
				break;
			default:
				break;
			}
		
		}
	}
}
