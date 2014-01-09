package entity.agent;

import java.util.Collection;
import java.util.Iterator;
import java.util.Random;

import com.google.common.collect.ImmutableList;

import data.FundamentalValue;
import data.OrderDatum;
import activity.Activity;
import activity.AgentStrategy;
import activity.SubmitNMSOrder;
import entity.infoproc.SIP;
import entity.market.Market;
import entity.market.Order;
import event.TimeStamp;

import java.util.Comparator;
import java.util.PriorityQueue;

public class OrderDataAgent extends SMAgent {

	private static final long serialVersionUID = -8572291999924780979L;
	
	protected PriorityQueue<OrderDatum> orderData;
	
	public OrderDataAgent(FundamentalValue fundamental, SIP sip, Market market, 
			Random rand, Iterator<OrderDatum> orderDataIterator) {
		super(new TimeStamp(0), fundamental, sip, market, rand,
				new PrivateValue(), 1);

		this.orderData = new PriorityQueue<OrderDatum>(11, new OrderDatumComparator() );
		while(orderDataIterator.hasNext()){
			OrderDatum order = orderDataIterator.next();
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
		return ImmutableList.of(new SubmitNMSOrder(this, submitOrder.getPrice(), 
				submitOrder.getQuantity(), primaryMarket, currentTime));
	}

	public Collection<Order> getOrders() {
		return this.activeOrders;
	}

	@Override
	public String toString() {
		return "OrderDataAgent " + super.toString();
	}
	
	/**
	 * Comparator for OrderDatum objects, only compares based on TimeStamp
	 *
	 */
	protected class OrderDatumComparator implements Comparator<OrderDatum> {

		@Override
		public int compare(OrderDatum arg0, OrderDatum arg1) {
			System.out.println( "TS " + arg0.getTimestamp() + " | " + arg1.getTimestamp());
			System.out.println(arg1.getTimestamp().compareTo(arg0.getTimestamp()));
			return arg0.getTimestamp().compareTo(arg1.getTimestamp());
		}
	}
}
