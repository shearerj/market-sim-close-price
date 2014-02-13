package entity.agent;

import static logger.Logger.log;
import static logger.Logger.Level.INFO;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import systemmanager.Scheduler;
import activity.AgentStrategy;
import data.FundamentalValue;
import data.OrderDatum;
import data.OrderParser;
import data.OrderParserNYSE;
import data.OrderParserNasdaq;
import entity.infoproc.SIP;
import entity.market.Market;
import entity.market.Order;
import event.TimeStamp;

public class OrderDataAgent extends SMAgent {

	private static final long serialVersionUID = -8572291999924780979L;
	
	protected List<OrderDatum> orderDatumList;
	
	public OrderDataAgent(Scheduler scheduler, FundamentalValue fundamental, SIP sip, Market market,
			Random rand, String filename) throws IOException {
		super(scheduler, TimeStamp.ZERO, fundamental, sip, market, rand, 1);
		
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
	
	
	public OrderDataAgent(Scheduler scheduler, FundamentalValue fundamental, SIP sip, Market market, 
			Random rand, Iterator<OrderDatum> orderDataIterator) {
		super(scheduler, TimeStamp.ZERO, fundamental, sip, market, rand, 1);

		this.orderDatumList = new ArrayList<OrderDatum>();
		while(orderDataIterator.hasNext()){
			OrderDatum order = orderDataIterator.next();
			this.orderDatumList.add(order);
		}
	}
	
	@Override
	public void agentStrategy(TimeStamp currentTime) {
		OrderDatum nextStrategy = orderDatumList.get(0);
		StringBuilder sb = new StringBuilder().append(this).append(" ");
		sb.append(getName()).append(':');
		log(INFO, sb.append(" Next entry at ").append(nextStrategy.getTimeStamp()));
		scheduler.scheduleActivity(nextStrategy.getTimeStamp(),  new AgentStrategy(this));
	}

//	public Iterable<? extends Activity> executeODAStrategy(int quantity, TimeStamp currentTime) {
//		OrderDatum submitOrder = orderDatumList.
//		return ImmutableList.of(new SubmitNMSOrder(this, primaryMarket, OrderType.BUY,	// FIXME
//				submitOrder.getPrice(), submitOrder.getQuantity(), currentTime));
//  }

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
