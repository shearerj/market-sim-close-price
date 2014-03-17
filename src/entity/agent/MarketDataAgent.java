package entity.agent;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import logger.Log;
import systemmanager.Keys;
import systemmanager.Scheduler;
import systemmanager.Consts.AgentType;
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

	/*
	 * When building from eclipse you should use the generated serialVersionUID
	 * (which generates a random long) instead of the default 1. serialization
	 * is a java interface that allows all objects to be saved. This random
	 * number essentially says what version this object is, so it knows when it
	 * trys to load an object if its actually trying to load the same object.
	 */
	private static final long serialVersionUID = 7690956351534734324L;

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
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Error: could not open file: " + fileName.toString());
			System.exit(1);
		}
		
		scheduler.scheduleActivity(TimeStamp.create(0), new AgentStrategy(this));
//		scheduler.executeActivity(new AgentStrategy(this));
	}
	
	public MarketDataAgent(Scheduler scheduler, FundamentalValue fundamental, SIP sip, Market market, 
			Random rand, AgentProperties props) {
		this(scheduler, fundamental, sip, market, rand, props.getAsString(Keys.FILENAME));
	}
	
	public void agentStrategy(TimeStamp currentTime) {
		
		// If there are no orders on the list, sleep forever
		if (this.orderDatumList.isEmpty()) {
			return;
		}
		
		// Creating the order to submit
		OrderDatum nextOrder = orderDatumList.get(0);
		orderDatumList.remove(0);
		SubmitOrder act = new SubmitOrder(this, primaryMarket, nextOrder.getOrderType(), nextOrder.getPrice(), nextOrder.getQuantity());
		// FIXME This is super slow. The list should probably be reversed or better yet, put in a queue data structure
		// If you have a list that sorted in order, you could do the following to get a queue that works properly
		// Collections3.asLifoQueue(Lists.newArrayList(Lists.reverse(orderData)))
		// This will first reverse the list, but it's only a view, so you need to copy it to a new list, finally the last wraps it as a queue so you can use pretty
		// ADT syntax
		scheduler.scheduleActivity(nextOrder.getTimeStamp(), act);
		
		// Schedule reentry
		if(!orderDatumList.isEmpty())
			scheduler.scheduleActivity(orderDatumList.get(0).getTimeStamp(),  new AgentStrategy(this));
	}
	
	public List<OrderDatum> getOrderDatumList() {
		return orderDatumList;
	}

}
