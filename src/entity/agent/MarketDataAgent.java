package entity.agent;

import java.io.FileNotFoundException;
import java.util.List;
import java.util.Random;

import systemmanager.Keys;
import systemmanager.Scheduler;
import activity.AgentStrategy;
import activity.SubmitOrder;
import data.AgentProperties;
import data.FundamentalValue;
import data.OrderDatum;
import data.OrderParser;
import data.OrderParserNYSE;
import data.OrderParserNasdaq;
import entity.infoproc.SIP;
import entity.market.Market;
import event.TimeStamp;

public class MarketDataAgent extends SMAgent {
	
	/**
	 * TODO - what does this thing do?
	 */
	private static final long serialVersionUID = 1L;
	
	protected OrderParser orderParser;
	protected List<OrderDatum> orderDatumList;

	public MarketDataAgent(Scheduler scheduler, FundamentalValue fundamental, SIP sip, Market market, 
			Random rand, String fileName) {
		super(scheduler, TimeStamp.ZERO, fundamental, sip, market, rand, 1);
		
		// Determining the market file type
		if(fileName.toLowerCase().contains("nyse")){
			this.orderParser = new OrderParserNYSE();
		}
		else {
			this.orderParser = new OrderParserNasdaq();
		}
		
		// Processing the file
		try {
			this.orderDatumList = orderParser.process(fileName);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println("Error: could not open file: " + fileName);
			System.exit(1);
		}
		
	}
	
	public MarketDataAgent(Scheduler scheduler, FundamentalValue fundamental, SIP sip, Market market, 
			Random rand, AgentProperties props) {
		this(scheduler, fundamental, sip, market, rand, props.getAsString(Keys.FILENAME));
	}
	
	public void agentStrategy(TimeStamp currentTime) {
//		TimeStamp waitTime = reentry.next();
//		if (waitTime.equals(TimeStamp.INFINITE))
//		return ImmutableList.of(new AgentStrategy(this, TimeStamp.INFINITE));
		// If there are no orders on the list, sleep forever
		if (this.orderDatumList.isEmpty()) {
			return;
		}
		
		// Creating the order to submit
		OrderDatum nextOrder = orderDatumList.get(0);
		orderDatumList.remove(0); // FIXME This is super slow. The list should probably be reversed or better yet, put in a queue data structure
		// If you have a list that sorted in order, you could do the following to get a queue that works properly
		// Collections3.asLifoQueue(Lists.newArrayList(Lists.reverse(orderData)))
		// This will first reverse the list, but it's only a view, so you need to copy it to a new list, finally the last wraps it as a queue so you can use pretty
		// ADT syntax
		
		scheduler.executeActivity(new SubmitOrder(this, primaryMarket, nextOrder.getType(),
				nextOrder.getPrice(), nextOrder.getQuantity()));
		
		// Schedule reentry
		if(!orderDatumList.isEmpty())
			scheduler.scheduleActivity(orderDatumList.get(0).getTimeStamp(),  new AgentStrategy(this));
	}

}
