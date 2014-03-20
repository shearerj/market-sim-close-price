package entity.agent;

import static fourheap.Order.OrderType.BUY;
import static fourheap.Order.OrderType.SELL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;
import java.util.Random;

import org.junit.Before;
import org.junit.Test;

import systemmanager.Scheduler;
import data.FundamentalValue;
import data.MockFundamental;
import data.OrderDatum;
import entity.infoproc.SIP;
import entity.market.Market;
import entity.market.MockMarket;
import entity.market.Price;
import event.TimeStamp;


public class MarketDataAgentTest {
	
	private Random rand;
	private FundamentalValue fundamental;
	private Scheduler scheduler;
	private SIP sip;
	private Market market;

	/*
	 * TODO: The files loaded are small. Probably better to allow market data
	 * agents to take a "Reader" instead of just a file name. That way you can
	 * just pass in the text that would be in a file, without having to have a
	 * separate file that needs to be loaded. e.g. everything can be present in
	 * the text
	 */

	@Before
	public void setupTest() {
		rand = new Random();
		fundamental = new MockFundamental(100000);
		scheduler = new Scheduler(rand);
		sip = new SIP(scheduler, TimeStamp.create(0));
		market = new MockMarket(scheduler, sip);
	}

	@Test
	public void nyseSimpleTest() {
		MarketDataAgent agent = new MarketDataAgent(scheduler, fundamental,
				sip, market, rand, "dataTests/nyseSimpleTest.csv");
		Iterator<OrderDatum> orderDatumItr = agent.getOrderDatumList().iterator();
		
		assertTrue("No orders", orderDatumItr.hasNext());
		OrderDatum orderDatum = orderDatumItr.next();
		OrderDatum correctOrderDatum = new OrderDatum('A', "0", "1", 'P', "SRG1",
				TimeStamp.create(88), 'O', "AARCA", new Price(3249052), 5742346, BUY);
		compareOrderDatums(orderDatum, correctOrderDatum);
	}
	
	
	@Test
	public void nyseTest() {
		MarketDataAgent agent = new MarketDataAgent(scheduler, fundamental,
				sip, market, rand, "dataTests/nyseTest.csv");
		Iterator<OrderDatum> orderDatumItr = agent.getOrderDatumList().iterator();
				
		assertTrue("Too few orders", orderDatumItr.hasNext());		
		OrderDatum orderDatum = orderDatumItr.next();
		OrderDatum correct1 = new OrderDatum('A', "1", "1", 'B', "SRG2",
				TimeStamp.create(0), 'L', "AARCA", new Price(5516081), 981477, BUY);
		compareOrderDatums(orderDatum, correct1);

		assertTrue("Too few orders", orderDatumItr.hasNext());
		orderDatum = orderDatumItr.next();
		OrderDatum correct2 = new OrderDatum('A', "4", "4", 'P', "SRG3", 
				TimeStamp.create(0), 'B', "AARCA", new Price(1177542), 1747834, SELL);
		compareOrderDatums(orderDatum, correct2);


		assertTrue("Too few orders", orderDatumItr.hasNext());
		orderDatum = orderDatumItr.next();
		OrderDatum correct3 = new OrderDatum('A', "0", "10", 'B', "SRG4", 
				TimeStamp.create(192), 'O', "AARCA", new Price(5223572), 3921581, SELL);
		compareOrderDatums(orderDatum, correct3);
		
	}
	
	//XXX - Doesnt compare several values that currently are not relevant
	void compareOrderDatums(OrderDatum x, OrderDatum y) {
		assertEquals("Incorrect message type", x.getMessageType(), y.getMessageType());
		assertEquals("Incorrect sequence number", x.getSequenceNum(), y.getSequenceNum());
		assertEquals("Incorrect order reference number", x.getOrderReferenceNum(), y.getOrderReferenceNum());
		assertEquals("Incorrect exchange code", x.getExchangeCode(), y.getExchangeCode());
		assertEquals("Incorrect symbol", x.getSymbol(), y.getSymbol());
		assertEquals("Incorrect timestamp", x.getTimeStamp(), y.getTimeStamp());
		assertEquals("Incorrect systemCode", x.getSystemCode(), y.getSystemCode());
		assertEquals("Incorrect quoteId", x.getQuoteId(), y.getQuoteId());
		assertEquals("Incorrect price", x.getPrice().intValue(), y.getPrice().intValue());
		assertEquals("Incorrect quantity", x.getQuantity(), y.getQuantity());
		assertEquals("Incorrect order type", x.getOrderType(), y.getOrderType());
	}
}
