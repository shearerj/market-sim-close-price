package entity.agent;

import java.io.FileNotFoundException;
import java.util.List;
import java.util.Random;

import systemmanager.Keys;
import activity.Activity;
import activity.AgentStrategy;
import activity.SubmitOrder;

import com.google.common.collect.ImmutableList;

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

	public MarketDataAgent(FundamentalValue fundamental, SIP sip, Market market, 
			Random rand, String fileName) {
		super(new TimeStamp(0), fundamental, sip, market, rand, 1);
		
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
	
	public MarketDataAgent(FundamentalValue fundamental, SIP sip, Market market, 
			Random rand, AgentProperties props) {
		this(fundamental, sip, market, rand, props.getAsString(Keys.FILENAME));
	}
	
	public Iterable<? extends Activity> agentStrategy(TimeStamp currentTime) {
//		TimeStamp waitTime = reentry.next();
//		if (waitTime.equals(TimeStamp.INFINITE))
//		return ImmutableList.of(new AgentStrategy(this, TimeStamp.INFINITE));
		// If there are no orders on the list, sleep forever
		if (this.orderDatumList.size() == 0) {
			return ImmutableList.of(new AgentStrategy(this, TimeStamp.INFINITE));
		}
		
		// Creating the order to submit
		OrderDatum nextOrder = orderDatumList.get(0);
		orderDatumList.remove(0);
		SubmitOrder order = new SubmitOrder(this, primaryMarket, nextOrder.getType(),
				nextOrder.getPrice(), nextOrder.getQuantity(), nextOrder.getTimeStamp());
		
		// Schedule reentry
		AgentStrategy reentry;
		if(orderDatumList.size() > 0) {
			reentry = new AgentStrategy(this, orderDatumList.get(0).getTimeStamp());
		}
		else {
			reentry = new AgentStrategy(this, TimeStamp.INFINITE);
		}
		
		return ImmutableList.of(order, reentry);
	}

}
