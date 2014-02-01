package entity.agent;

import static org.junit.Assert.*;

import java.io.File;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
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
import data.MockOrderDatum;
import data.OrderDatum;
import entity.infoproc.SIP;
import entity.market.Market;
import entity.market.MockMarket;
import entity.market.Order;
import entity.market.Price;
import event.TimeStamp;
import static fourheap.Order.OrderType.BUY;
import static fourheap.Order.OrderType.SELL;

public class OrderDataAgentTest {

	private static Random rand;

	private FundamentalValue fundamental = new MockFundamental(100000);
	private Market market;
	private SIP sip;

	@BeforeClass
	public static void setupClass() {
		// Setting up the log file
		Logger.setup(3, new File(Consts.TEST_OUTPUT_DIR + "OrderDataAgentTest.log"));

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
		Logger.log(Logger.Level.DEBUG," \n Testing OrderDataAgent strategy, "
				+ "should return orderdata as what is passed in \n");
		List<OrderDatum> orders = new LinkedList<OrderDatum>();
		
		orders.add(new MockOrderDatum(new TimeStamp(15), new Price(75000), 1, BUY));
		orders.add(new MockOrderDatum(new TimeStamp(20), new Price(60000), 1, SELL));
	    orders.add(new MockOrderDatum(new TimeStamp(18), new Price(75000), 1, BUY));

		OrderDataAgent agent = addAgent(orders.iterator());
		
		//0->15
		TimeStamp t1 = new TimeStamp(0);
		// in any market , OrderDataAgent should follow order data as it inputs
		Collection<? extends Activity> c = agent.agentStrategy(t1);
		Activity nextOrder = Iterables.getOnlyElement(c);
		TimeStamp t2 = new TimeStamp(15);
		assertEquals("OrderDataAgent Strategy is in order", t2, nextOrder.getTime());
		
		agent.executeODAStrategy(1, t2);
	    
		//15->18
        t1 = new TimeStamp(15);
        c = agent.agentStrategy(t1);
        nextOrder = Iterables.getOnlyElement(c);
        Logger.log(Logger.Level.DEBUG," \nNext order time: " + nextOrder.getTime());
        t2 = new TimeStamp(18);
        assertEquals("OrderDataAgent Strategy is in order", t2, nextOrder.getTime());
        
        agent.executeODAStrategy(1, t2);

        //18->20
        t1 = new TimeStamp(18);
        c = agent.agentStrategy(t1);
        nextOrder = Iterables.getOnlyElement(c);
        t2 = new TimeStamp(20);
        assertEquals("OrderDataAgent Strategy is in order", t2, nextOrder.getTime());
  	}
	
	@Test
	public void ordersTest(){
		Logger.log(Logger.Level.DEBUG, "\n Testing OrderDataAgent Orders , should be identical to input values\n");
        List<OrderDatum> orders = new LinkedList<OrderDatum>();
        
		OrderDataAgent agent = addAgent(orders.iterator());
		market.submitNMSOrder(agent, BUY, new Price(75000), 1, new TimeStamp(15));
		
		Collection<Order> orderCollection = agent.getOrders();
	    Order order = Iterables.getFirst(orderCollection, null);
		
		Logger.log(Logger.Level.DEBUG, agent.getOrders());
		assertEquals("OrderDataAgent active order have wrong agent", agent, order.getAgent());
	    assertEquals("OrderDataAgent active order have wrong market", market, order.getMarket());
	    assertEquals("OrderDataAgent active order have wrong price", new Price(75000), order.getPrice());
	    assertEquals("OrderDataAgent active order have wrong price", 1, order.getQuantity());
	    assertEquals("OrderDataAgent active order have wrong timestamp", new TimeStamp(15), order.getSubmitTime());
	}
}
