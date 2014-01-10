package entity.agent;

import java.io.File;

import logger.Logger;

import org.junit.Before;
import org.junit.BeforeClass;

import systemmanager.Consts;
import systemmanager.Keys;
import data.EntityProperties;
import data.FundamentalValue;
import data.MockFundamental;
import entity.infoproc.SIP;
import entity.market.MockMarket;
import event.TimeStamp;

public class WMAMarketMakerTest {

	private MockMarket market;
	private SIP sip;
	private FundamentalValue fundamental = new MockFundamental(100000);

	@BeforeClass
	public static void setupClass() {
		Logger.setup(3, new File(Consts.TEST_OUTPUT_DIR + "WMAMarketMakerTest.log"));
	}

	@Before
	public void setup() {
		sip = new SIP(TimeStamp.IMMEDIATE);
		market = new MockMarket(sip);
	}

	private EntityProperties setupProperties(int numRungs, int rungSize, 
			boolean truncateLadder, int tickSize, int windowLength,
			int weightFactor) {
		EntityProperties agentProperties = new EntityProperties();
		agentProperties.put(Keys.NUM_RUNGS, numRungs);
		agentProperties.put(Keys.RUNG_SIZE, rungSize);
		agentProperties.put(Keys.TRUNCATE_LADDER, truncateLadder);
		agentProperties.put(Keys.TICK_SIZE, tickSize);
		agentProperties.put(Keys.REENTRY_RATE, 0.000001);
		agentProperties.put(Keys.WINDOW_LENGTH, windowLength);
		agentProperties.put(Keys.WEIGHT_FACTOR, weightFactor);
		return agentProperties;
	}
}
