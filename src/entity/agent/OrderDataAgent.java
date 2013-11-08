package entity.agent;

import static java.lang.Math.signum;
import static logger.Logger.log;
import static logger.Logger.Level.INFO;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Random;
import java.util.TreeMap;

import utils.Rands;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.sun.org.apache.xpath.internal.operations.Or;

import data.FundamentalValue;
import data.OrderDatum;
import activity.Activity;
import activity.AgentStrategy;
import activity.SubmitNMSOrder;
import entity.agent.BackgroundAgent;
import entity.infoproc.SIP;
import entity.market.Market;
import entity.market.Order;
import entity.market.Price;
import event.TimeStamp;

import java.util.Comparator;
import java.util.PriorityQueue;

public class OrderDataAgent extends SMAgent {

	private static final long serialVersionUID = 1L;
	protected static PriorityQueue<OrderDatum> orderData = new PriorityQueue<OrderDatum>(11,new Comparator<OrderDatum>(){

		@Override
		public int compare(OrderDatum arg0, OrderDatum arg1) {
			System.out.println( "TS " + arg0.getTimestamp() + " | " + arg1.getTimestamp());
			System.out.println(arg1.getTimestamp().compareTo(arg0.getTimestamp()));
			return arg0.getTimestamp().compareTo(arg1.getTimestamp());
			
		}
			
		}
	);
	public OrderDataAgent(FundamentalValue fundamental, SIP sip, Market market, 
	        Random rand,
	        Iterator<OrderDatum> orderData) {
	    super(new TimeStamp(0), fundamental, sip, market, rand,
	                new PrivateValue(), 1);
	    
	    while(orderData.hasNext()){
	        OrderDatum order = orderData.next();
	        this.orderData.add(order);
	    }
	}

	@Override
	public Collection<? extends Activity> agentStrategy(TimeStamp currentTime) {
        OrderDatum nextStrategy = orderData.peek();
        System.out.println("func"+nextStrategy.getTimestamp());
        return ImmutableList.of(new AgentStrategy(this, nextStrategy.getTimestamp()));
	}
	
	public Iterable<? extends Activity> executeODAStrategy(int quantity, TimeStamp currentTime) {
		OrderDatum submitOrder = orderData.poll();
     return ImmutableList.of(new SubmitNMSOrder(this, submitOrder.getPrice(), submitOrder.getQuantity(), primaryMarket, currentTime));
	}

	public Collection<Order> getOrders() {
		return this.activeOrders;
	}
	
	@Override
	public String toString() {
		
		return "OrderDataAgent " + super.toString();
	}
}
