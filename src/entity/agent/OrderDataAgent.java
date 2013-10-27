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
import data.OrderData;
import activity.Activity;
import activity.AgentStrategy;
import activity.SubmitNMSOrder;
import entity.agent.BackgroundAgent;
import entity.infoproc.SIP;
import entity.market.Market;
import entity.market.Order;
import entity.market.Price;
import event.TimeStamp;

public class OrderDataAgent extends SMAgent {

	private static final long serialVersionUID = 1L;
	protected static TreeMap<TimeStamp, OrderData> orderData = new TreeMap<TimeStamp, OrderData>();

	public OrderDataAgent(FundamentalValue fundamental, SIP sip, Market market, 
	        Random rand, PrivateValue privateValue, 
	        Iterator<OrderData> orderData) {
	    super(new TimeStamp(0), fundamental, sip, market, new Random(),
	                new PrivateValue(), 1);
	    
	    while(orderData.hasNext()){
	        OrderData order = orderData.next();
	        this.orderData.put(order.getTimestamp(), order);
	    }
	}

	@Override
	public Collection<? extends Activity> agentStrategy(TimeStamp currentTime) {
        TimeStamp nextStrategy = orderData.higherKey(currentTime);
        return ImmutableList.of(new AgentStrategy(this, nextStrategy));
	}
	
	public Iterable<? extends Activity> executeODAStrategy(int quantity, TimeStamp currentTime) {
     return ImmutableList.of(new SubmitNMSOrder(this, orderData.get(currentTime).getPrice(), orderData.get(currentTime).getQuantity(), 
             primaryMarket, TimeStamp.IMMEDIATE));
	}

	/*public Collection<Order> getOrders() {
		//String message = "PHAgent has no orders!";
		//log(INFO, message);
		return this.activeOrders;
	}
	*/
	@Override
	public String toString() {
		
		return "OrderDataAgent " + super.toString();
	}
}
