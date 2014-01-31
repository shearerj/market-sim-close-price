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
import systemmanager.Executor;

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
import event.TimedActivity;
import static fourheap.Order.OrderType.BUY;
import static fourheap.Order.OrderType.SELL;

public class OrderDataAgentTest {

	private static Random rand;

	private Executor exec;
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
		exec = new Executor();
		sip = new SIP(exec, TimeStamp.IMMEDIATE);
		// Creating the MockMarket
		market = new MockMarket(exec, sip);
	}

	private OrderDataAgent addAgent(Iterator<OrderDatum> orderDataIt) {
		OrderDataAgent agent = new OrderDataAgent(exec, fundamental, sip, market, rand,
				orderDataIt);

		return agent;
	}

	
	@Test
	public void strategyTest(){
		Logger.log(Logger.Level.DEBUG," \n Testing OrderDataAgent strategy, "
				+ "should return orderdata as what is passed in \n");
		List<OrderDatum> orders = new LinkedList<OrderDatum>();
		
		TimeStamp t1 = TimeStamp.create(15), t1_ = TimeStamp.create(16);
		TimeStamp t2 = TimeStamp.create(18), t2_ = TimeStamp.create(19);
		TimeStamp t3 = TimeStamp.create(20);
		
		orders.add(new MockOrderDatum(t1, new Price(75000), 1, BUY));
		orders.add(new MockOrderDatum(t3, new Price(60000), 1, SELL));
	    orders.add(new MockOrderDatum(t2, new Price(75000), 1, BUY));

		OrderDataAgent agent = addAgent(orders.iterator());

		// in any market , OrderDataAgent should follow order data as it inputs
		agent.agentStrategy(TimeStamp.ZERO);
		assertEquals("OrderDataAgent Strategy is in order", t1, exec.peek().getTime());
		agent.executeODAStrategy(1, t2);
	    
		//15->18
        exec.executeUntil(t1_);
        TimedActivity nextOrder = exec.peek();
        Logger.log(Logger.Level.DEBUG," \nNext order time: " + nextOrder.getTime());
        assertEquals("OrderDataAgent Strategy is in order", t2, nextOrder.getTime());
        agent.executeODAStrategy(1, t2);

        //18->20
        exec.executeUntil(t2_);
        assertEquals("OrderDataAgent Strategy is in order", t3, exec.peek().getTime());
  	}
	
	@Test
	public void ordersTest(){
		Logger.log(Logger.Level.DEBUG, "\n Testing OrderDataAgent Orders , should be identical to input values\n");
        List<OrderDatum> orders = new LinkedList<OrderDatum>();
        
		OrderDataAgent agent = addAgent(orders.iterator());
		market.submitNMSOrder(agent, BUY, new Price(75000), 1, TimeStamp.create(15));
		
		Collection<Order> orderCollection = agent.getOrders();
	    Order order = Iterables.getFirst(orderCollection, null);
		
		Logger.log(Logger.Level.DEBUG, agent.getOrders());
		assertEquals("OrderDataAgent active order have wrong agent", agent, order.getAgent());
	    assertEquals("OrderDataAgent active order have wrong market", market, order.getMarket());
	    assertEquals("OrderDataAgent active order have wrong price", new Price(75000), order.getPrice());
	    assertEquals("OrderDataAgent active order have wrong price", 1, order.getQuantity());
	    assertEquals("OrderDataAgent active order have wrong timestamp", TimeStamp.create(15), order.getSubmitTime());
	}
}
