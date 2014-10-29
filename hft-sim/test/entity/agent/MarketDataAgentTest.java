package entity.agent;

import static org.junit.Assert.assertTrue;
import static utils.Tests.assertQuote;

import java.io.IOException;
import java.io.StringReader;
import java.util.Iterator;
import java.util.Random;

import logger.Log;

import org.junit.Before;
import org.junit.Test;

import utils.Mock;

import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;

import data.Props;
import entity.agent.MarketDataParser.MarketAction;
import entity.market.CDAMarket;
import entity.market.Market;
import entity.market.Market.MarketView;
import entity.market.Price;
import event.EventQueue;
import event.TimeStamp;

// TODO More complicated tests

public class MarketDataAgentTest {
	private static final Random rand = new Random();

	private EventQueue timeline;
	private Market market;
	private MarketView view;

	@Before
	public void setupTest() throws IOException {
		timeline = EventQueue.create(Log.nullLogger(), rand);
		market = CDAMarket.create(0, Mock.stats, timeline, Log.nullLogger(), rand, Mock.sip, Props.fromPairs());
		view = market.getPrimaryView();
	}

	@Test
	public void nyseSimpleTest() {
		Iterator<MarketAction> actions = MarketDataParser.parseNYSE(new StringReader("A,0,1,P,B,5742346,SRG1,3249052,0,88,O,AARCA,\n"));
		marketDataAgent(actions);
		
		timeline.executeUntil(TimeStamp.of(87));
		assertQuote(view.getQuote(), null, 0, null, 0);
		
		timeline.executeUntil(TimeStamp.of(88));
		assertQuote(view.getQuote(), Price.of(3249052), 5742346, null, 0);
	}
	
	
	
	@Test
	public void nyseDeleteTest() {
		Iterator<MarketAction> actions = MarketDataParser.parseNYSE(new StringReader(
				"A,0,1,B,B,981477,SRG2,5516081,0,0,L,AARCA,\n" +
				"D,0,1,2,0,SRG2,B,L,AARCA,B,\n"));
		MarketDataAgent agent = marketDataAgent(actions);
				
		timeline.executeUntil(TimeStamp.ZERO);
		assertQuote(view.getQuote(), Price.of(5516081), 981477, null, 0);
		
		timeline.executeUntil(TimeStamp.of(1999));
		assertQuote(view.getQuote(), Price.of(5516081), 981477, null, 0);
		
		timeline.executeUntil(TimeStamp.of(2000));
		assertQuote(view.getQuote(), null, 0, null, 0);
		assertTrue(agent.refNumbers.isEmpty()); // Implementation dependent, but no way to tell other than checking
	}
	
	@Test
	public void nasdaqAddTest() {
		Iterator<MarketAction> actions = MarketDataParser.parseNasdaq(new StringReader(
				"T,0\n" +
				"A,16382751,1,B,3748742,SRG1,5630815\n"));
		marketDataAgent(actions);
		
		timeline.executeUntil(TimeStamp.of(15));
		assertQuote(view.getQuote(), null, 0, null, 0);

		timeline.executeUntil(TimeStamp.of(16));
		assertQuote(view.getQuote(), Price.of(5630815), 3748742, null, 0);
	}
	
	@Test
	public void nasdaqDeleteTest() {
		Iterator<MarketAction> actions = MarketDataParser.parseNasdaq(new StringReader(
				"T,0\n" +
				"A,16382751,1,B,3748742,SRG1,5630815\n" +
				"D,20000000,1\n"));
		MarketDataAgent agent = marketDataAgent(actions);
		
		timeline.executeUntil(TimeStamp.of(15));
		assertQuote(view.getQuote(), null, 0, null, 0);
		
		timeline.executeUntil(TimeStamp.of(16));
		assertQuote(view.getQuote(), Price.of(5630815), 3748742, null, 0);
		
		timeline.executeUntil(TimeStamp.of(19));
		assertQuote(view.getQuote(), Price.of(5630815), 3748742, null, 0);

		timeline.executeUntil(TimeStamp.of(20));
		assertQuote(view.getQuote(), null, 0, null, 0);
		assertTrue(agent.refNumbers.isEmpty()); // Implementation dependent, but no way to tell other than checking
		
	}
	
	private MarketDataAgent marketDataAgent(Iterator<MarketAction> actions) {
		PeekingIterator<MarketAction> peekable = Iterators.peekingIterator(actions);
		assertTrue("For this purpose, the iterator must have a market action", actions.hasNext());
		TimeStamp arrivalTime = peekable.peek().getScheduledTime();
		return new MarketDataAgent(0, Mock.stats, timeline, Log.nullLogger(), rand, Mock.sip, Mock.fundamental, arrivalTime, market,
				peekable, Props.fromPairs());
	}
}

