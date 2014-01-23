package entity.agent;

import static logger.Logger.log;
import static logger.Logger.Level.INFO;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.Random;

import systemmanager.Scheduler;
import activity.AgentStrategy;
import activity.SubmitNMSOrder;
import data.FundamentalValue;
import data.OrderDatum;
import entity.infoproc.SIP;
import entity.market.Market;
import entity.market.Order;
import event.TimeStamp;
import fourheap.Order.OrderType;

public class OrderDataAgent extends SMAgent {

	private static final long serialVersionUID = -8572291999924780979L;
	
	protected PriorityQueue<OrderDatum> orderData;

	// XXX Erik: This is pretty inefficient. Why is it using an iterator? Why do
	// you need a priority queue? Shouldn't you have some notion of how it's
	// sorted? Also, instead of making a comparator, orderData should probably
	// be comaprable.
	public OrderDataAgent(Scheduler scheduler, FundamentalValue fundamental,
			SIP sip, Market market,
			Random rand, Iterator<OrderDatum> orderDataIterator) {
		super(scheduler, TimeStamp.ZERO, fundamental, sip, market, rand, 1);
		
		this.orderData = new PriorityQueue<OrderDatum>(11, new OrderDatumComparator() );
		while(orderDataIterator.hasNext()){
			OrderDatum order = orderDataIterator.next();
			this.orderData.add(order);
		}
	}

	@Override
	public void agentStrategy(TimeStamp currentTime) {
		OrderDatum nextStrategy = orderData.peek();
		log(INFO, this + ": Next entry at " + nextStrategy.getTimeStamp());
		scheduler.scheduleActivity(nextStrategy.getTimeStamp(), new AgentStrategy(this));
	}

	// XXX Erik: Why is quantity part of this? Shouldn't that be encoded in
	// OrderDatum?
	public void executeODAStrategy(int quantity, TimeStamp currentTime) {
		OrderDatum submitOrder = orderData.poll();
		scheduler.executeActivity(new SubmitNMSOrder(this, primaryMarket, OrderType.BUY,	// FIXME
				submitOrder.getPrice(), submitOrder.getQuantity()));
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
//			StringBuilder sb = new StringBuilder();
//			sb.append("Time ").append(arg0.getTimeStamp());
//			sb.append(" | ").append(arg1.getTimeStamp()).append("\n");
//			sb.append("Output ").append(arg1.getTimeStamp().compareTo(arg0.getTimeStamp()));
			return arg0.getTimeStamp().compareTo(arg1.getTimeStamp());
		}
	}
}
