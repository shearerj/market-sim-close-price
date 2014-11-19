package entity.agent;

import static org.junit.Assert.assertTrue;
import logger.Log;

import org.junit.Before;
import org.junit.Test;

import systemmanager.Keys.ClearInterval;
import systemmanager.Keys.MaxPosition;
import systemmanager.Keys.PrivateValueVar;
import utils.Mock;
import utils.Rand;
import data.Props;
import entity.market.CDAMarket;
import entity.market.CallMarket;
import entity.market.Market;
import entity.market.Market.MarketView;
import event.EventQueue;
import event.TimeStamp;

public class MaxEfficiencyAgentTest {
	
	private static Rand rand = Rand.create();
	
	private EventQueue timeline;
	private Market market;
	private MarketView view;
	
	// FIXME Test that orders and negative and we get the efficient outcome
	// FIXME Bug when doing multiple simulations that observations aren't accounted for for missing observations
	// FIXME There seems to be a bug in this, in that agents rarely get to -10 position. This may be a result of background agents preventing bids
	// FIXME Tests are likely all broken because agent's have to bypass standard order submission
	
	@Before
	public void setup(){
		timeline = EventQueue.create(Log.nullLogger(), rand);
		market = CallMarket.create(0, Mock.stats, timeline, Log.nullLogger(), rand, Mock.sip, Props.fromPairs(
				ClearInterval.class, TimeStamp.of(1)));
		view = market.getPrimaryView();
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void callMarket() {
		market = CDAMarket.create(0, Mock.stats, Mock.timeline, Log.nullLogger(), rand, Mock.sip, Props.fromPairs());
		MaxEfficiencyAgent.create(0, Mock.stats, Mock.timeline, Log.nullLogger(), rand, Mock.sip, Mock.fundamental, market, Props.fromPairs());
	}
	
	@Test
	public void numOrdersTest() {
		Agent agent = MaxEfficiencyAgent.create(0, Mock.stats, Mock.timeline, Log.nullLogger(), rand, Mock.sip, Mock.fundamental, market, Props.fromPairs( 
				MaxPosition.class, 10,
				PrivateValueVar.class, 1000d));
		
		agent.agentStrategy();
		timeline.executeUntil(TimeStamp.of(1));
		
		// FIXME Somehow test that there are 20 orders, and that the number of buys and sells is equals
		assertTrue(view.getQuote().getAskPrice().isPresent());
		assertTrue(view.getQuote().getBidPrice().isPresent());
	}
	
	@Test
	public void basicTest() {
		Agent agent = MaxEfficiencyAgent.create(0, Mock.stats, Mock.timeline, Log.nullLogger(), rand, Mock.sip, Mock.fundamental, market, Props.fromPairs( 
				MaxPosition.class, 1,
				PrivateValueVar.class, 1e7));
		
		agent.agentStrategy();
		timeline.executeUntil(TimeStamp.of(1));
		
		// XXX Can't test number of active orders, because the bypass order of max eff agent doesn't add orders to active orders.
		assertTrue(view.getQuote().getAskPrice().isPresent());
		assertTrue(view.getQuote().getBidPrice().isPresent());
	}
	
	// FIXME Test that position at end of simulation is correctly stored in stats
	
	@Test
	public void extraTest() {
		for (int i = 0; i < 100; i++) {
			setup();
			basicTest();
			setup();
			numOrdersTest();
		}
	}
	
}
