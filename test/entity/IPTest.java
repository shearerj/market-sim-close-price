package entity;

import static entity.AAAgent.AGGRESSION_KEY;
import static entity.AAAgent.DEBUG_KEY;
import static entity.AAAgent.ETA_KEY;
import static entity.AAAgent.HISTORICAL_KEY;
import static entity.AAAgent.THETAMAX_KEY;
import static entity.AAAgent.THETAMIN_KEY;
import static entity.AAAgent.THETA_KEY;
import logger.Logger;
import model.MockMarketModel;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import utils.RandPlus;
import data.DummyFundamental;
import data.EntityProperties;
import data.FundamentalValue;
import data.Keys;
import event.TimeStamp;

public class IPTest {

	private static FundamentalValue fund;
	private static RandPlus rand;
	private static EntityProperties agentProperties;
	
	private MockMarketModel model;
	private Market market;
	private int agentIndex;
	public SIP dummySIP;
	
	@BeforeClass
	public static void setupClass() {
		//Setting up the log file
		Logger.setup(3, "simulations/unit_testing", "unit_tests.txt", true);
		
		//Creating the setup properties
		rand = new RandPlus(1);
		fund = new DummyFundamental(100000);
		
		//Setting up agentProperties
		agentProperties = new EntityProperties();
		agentProperties.put(Keys.REENTRY_RATE, 0.25);
		agentProperties.put(Keys.MAX_QUANTITY, 10);
		agentProperties.put(DEBUG_KEY, false);
		agentProperties.put(ETA_KEY, 3);
		agentProperties.put(HISTORICAL_KEY, 5);
		agentProperties.put(AGGRESSION_KEY, 0);
		agentProperties.put(THETA_KEY, 0);
		agentProperties.put(THETAMAX_KEY, 4);
		agentProperties.put(THETAMIN_KEY, -4);
	}
	
	@Before
	public void setupTest() {
		//Creating the MarketModel
		model = new MockMarketModel(1);

		//XXX - Fix once SIP becomes important
		dummySIP = new SIP(1,1,new TimeStamp(0));
		
		market = new CDAMarket(1, model, 0);
		model.addMarket(market);
	}
	
	@Test
	public void initialTest() {
		assert(true);
		Logger.log(Logger.Level.DEBUG, "Testing IPs");
	}
}
