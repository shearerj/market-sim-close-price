package entity.agent;

import static logger.Logger.log;
import static logger.Logger.Level.INFO;

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
import fourheap.Order.OrderType;

import java.util.Comparator;
import java.util.PriorityQueue;

public class OrderDataAgent extends SMAgent {

	private static final long serialVersionUID = -8572291999924780979L;
	
	protected PriorityQueue<OrderDatum> orderData;
	
	public OrderDataAgent(FundamentalValue fundamental, SIP sip, Market market, 
			Random rand, Iterator<OrderDatum> orderDataIterator) {
		super(new TimeStamp(0), fundamental, sip, market, rand, 1);

		this.orderData = new PriorityQueue<OrderDatum>(11, new OrderDatumComparator() );
		while(orderDataIterator.hasNext()){
			OrderDatum order = orderDataIterator.next();
			this.orderData.add(order);
		}
	}

	@Override
	public Collection<? extends Activity> agentStrategy(TimeStamp currentTime) {
		OrderDatum nextStrategy = orderData.peek();
		StringBuilder sb = new StringBuilder().append(this).append(" ");
		sb.append(getName()).append(':');
		log(INFO, sb.append(" Next entry at ").append(nextStrategy.getTimeStamp()));
		return ImmutableList.of(new AgentStrategy(this, nextStrategy.getTimeStamp()));
	}

	public Iterable<? extends Activity> executeODAStrategy(int quantity, TimeStamp currentTime) {
		OrderDatum submitOrder = orderData.poll();
		return ImmutableList.of(new SubmitNMSOrder(this, primaryMarket, OrderType.BUY,	// FIXME
				submitOrder.getPrice(), submitOrder.getQuantity(), currentTime));
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
			StringBuilder sb = new StringBuilder();
			sb.append("Time ").append(arg0.getTimeStamp());
			sb.append(" | ").append(arg1.getTimeStamp()).append("\n");
			sb.append("Output ").append(arg1.getTimeStamp().compareTo(arg0.getTimeStamp()));
			return arg0.getTimeStamp().compareTo(arg1.getTimeStamp());
		}
	}
}
