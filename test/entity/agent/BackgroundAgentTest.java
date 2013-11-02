package entity.agent;

import static org.junit.Assert.*;

import java.io.File;

import logger.Logger;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import systemmanager.Consts;
import systemmanager.Consts.OrderType;
import data.DummyFundamental;
import data.FundamentalValue;
import entity.agent.BackgroundAgent;
import entity.infoproc.SIP;
import entity.market.Market;
import entity.market.MockMarket;
import entity.market.Price;
import event.TimeStamp;

public class BackgroundAgentTest {

	private FundamentalValue fundamental = new DummyFundamental(100000);
	private Market market;
	private BackgroundAgent agent;
	private SIP sip;
	
	@BeforeClass
	public static void setupClass() {
		Logger.setup(3, new File(Consts.TEST_OUTPUT_DIR + "BackgroundAgentTest.log"));
	}
	
	@Before
	public void setup() {
		sip = new SIP(TimeStamp.IMMEDIATE);
		market = new MockMarket(sip);
		agent = new MockBackgroundAgent(fundamental, sip, market);
	}
	
	@Test
	public void getValuation() {
		TimeStamp time = TimeStamp.ZERO;
		Price val = agent.getValuation(OrderType.BUY, time);
		assertTrue(val.greaterThanEqual(fundamental.getValueAt(time)));
		
		// TODO need more here. pv right is zero...
		
	}
	
	// TODO test executing ZI strategy (very basic)
}
