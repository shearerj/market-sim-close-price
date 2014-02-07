package entity.agent;

import static logger.Logger.logger;
import static logger.Logger.Level.INFO;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import com.google.common.collect.ImmutableList;

import data.FundamentalValue;
import data.OrderDatum;
import data.OrderParser;
import data.OrderParserNYSE;
import data.OrderParserNasdaq;
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
	
	protected List<OrderDatum> orderDatumList;
	
	public OrderDataAgent(FundamentalValue fundamental, SIP sip, Market market,
			Random rand, String filename) throws IOException {
		super(new TimeStamp(0), fundamental, sip, market, rand, 1);
		
		// Opening the orderParser
		OrderParser orderParser;
		if (filename.contains("nyse")) {
			orderParser = new OrderParserNYSE();
		}
		else {
			orderParser = new OrderParserNasdaq();
		}
		
//		this.orderDatumList = orderParser.process();
		
	}
	
	
	public OrderDataAgent(FundamentalValue fundamental, SIP sip, Market market, 
			Random rand, Iterator<OrderDatum> orderDataIterator) {
		super(new TimeStamp(0), fundamental, sip, market, rand, 1);

		this.orderDatumList = new ArrayList<OrderDatum>();
		while(orderDataIterator.hasNext()){
			OrderDatum order = orderDataIterator.next();
			this.orderDatumList.add(order);
		}
	}
	
	@Override
	public Collection<? extends Activity> agentStrategy(TimeStamp currentTime) {
		OrderDatum nextStrategy = orderDatumList.get(0);
		logger.log(INFO, "%s: Next entry at %s", this, nextStrategy.getTimeStamp());
		return ImmutableList.of(new AgentStrategy(this, nextStrategy.getTimeStamp()));
	}

//	public Iterable<? extends Activity> executeODAStrategy(int quantity, TimeStamp currentTime) {
//		OrderDatum submitOrder = orderDatumList.
//		return ImmutableList.of(new SubmitNMSOrder(this, primaryMarket, OrderType.BUY,	// FIXME
//				submitOrder.getPrice(), submitOrder.getQuantity(), currentTime));
//	}

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
