package entity.agent;

import java.io.File;

import logger.Logger;

import org.junit.Before;
import org.junit.BeforeClass;

import systemmanager.Consts;
import data.DummyFundamental;
import data.FundamentalValue;
import entity.infoproc.SIP;
import entity.market.Market;
import entity.market.MockMarket;
import event.TimeStamp;

public class BackgroundAgentTest {

	private FundamentalValue fundamental = new DummyFundamental(100000);
	private Market market;
	private Agent agent;
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
	
	// TODO test private value computation
	
	// TODO test generating fundamental (get valuation)
	
	// TODO test executing ZI strategy (very basic)
}
