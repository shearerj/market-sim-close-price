package entity.agent;

import static logger.Logger.log;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import logger.Logger;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import systemmanager.Keys;
import activity.Activity;
import activity.AgentStrategy;
import activity.ProcessQuote;
import activity.SendToIP;
import activity.SubmitNMSOrder;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

import data.DummyFundamental;
import data.EntityProperties;
import data.FundamentalValue;
import data.OrderData;
import data.OrderDatum;
import entity.agent.DummyAgent;
import entity.infoproc.SIP;
import entity.market.Market;
import entity.market.MarketTime;
import entity.market.MockMarket;
import entity.market.Order;
import entity.market.Price;
import event.TimeStamp;

public class OrderDataAgentTest {

	private static Random rand;

	private FundamentalValue fundamental = new DummyFundamental(100000);//?
	private Market market;
	private SIP sip;

	@BeforeClass
	public static void setupClass() {
		// Setting up the log file
		Logger.setup(3, new File("simulations/unit_testing/OrderDataAgentTest.log"));

		rand = new Random(1);
	}

	@Before
	public void setupTest() {
		sip = new SIP(TimeStamp.IMMEDIATE);
		// Creating the MockMarket
		market = new MockMarket(sip);
	}

	private OrderDataAgent addAgent(Iterator<OrderDatum> orderDataIt) {
		OrderDataAgent agent = new OrderDataAgent(fundamental, sip, market, rand,
				orderDataIt);

		return agent;
	}

	
	@Test
	public void strategyTest(){
		Logger.log(Logger.Level.DEBUG," \n Testing OrderDataAgent strategy , should return orderdata as what is passed in \n");
		List<OrderDatum> orders = new LinkedList<OrderDatum>();
		
		orders.add(new OrderDatum(new TimeStamp(15), new Price(75000), 1, true));
		orders.add(new OrderDatum(new TimeStamp(20), new Price(60000), 1, false));
	    orders.add(new OrderDatum(new TimeStamp(18), new Price(75000), 1, true));

		OrderDataAgent agent = addAgent(orders.iterator());
		
		//0->15
		TimeStamp t1 = new TimeStamp(0);
		// on any market , OrderDataAgent should follow order data as it inputs
		Collection<? extends Activity> c = agent.agentStrategy(t1);
	    TimeStamp t2 = new TimeStamp(15);
	    Collection<? extends Activity> nextOrder = ImmutableList.of(new AgentStrategy(agent, t2));
	    
		assertTrue("OrderDataAgent Strategy is in order",c.iterator().next().getTime().equals(nextOrder.iterator().next().getTime()));
	    
		//15->18
        t1 = new TimeStamp(15);
        c = agent.agentStrategy(t1);
        t2 = new TimeStamp(18);
        nextOrder = ImmutableList.of(new AgentStrategy(agent, t2));

        assertTrue("OrderDataAgent Strategy is in order",c.iterator().next().getTime().equals(nextOrder.iterator().next().getTime()));
	
        //18->20
        t1 = new TimeStamp(18);
        c = agent.agentStrategy(t1);
        t2 = new TimeStamp(20);
        nextOrder = ImmutableList.of(new AgentStrategy(agent, t2));

        assertTrue("OrderDataAgent Strategy is in order",c.iterator().next().getTime().equals(nextOrder.iterator().next().getTime()));
	}
	
	@Test
	public void ordersTest(){
		Logger.log(Logger.Level.DEBUG, "\n Testing OrderDataAgent Orders , should be identical to input values\n");
        List<OrderDatum> orders = new LinkedList<OrderDatum>();
        
		OrderDataAgent agent = addAgent(orders.iterator());
		market.submitNMSOrder(agent, new Price(75000), +1, new TimeStamp(15));
		
		Collection<Order> orderCollection = agent.getOrders();
	    Order order = Iterables.getFirst(orderCollection, null);
		
		Logger.log(Logger.Level.DEBUG, agent.getOrders());
		assertTrue("OrderDataAgent active order have wrong agent",order.getAgent().equals(agent));
	    assertTrue("OrderDataAgent active order have wrong market",order.getMarket().equals(market));
	    assertTrue("OrderDataAgent active order have wrong price",order.getPrice().equals(new Price(75000)));
	    assertTrue("OrderDataAgent active order have wrong price", order.getQuantity() == 1);
	    assertTrue("OrderDataAgent active order have wrong timestamp",order.getSubmitTime().equals(new TimeStamp(15)));

	}
	
}
