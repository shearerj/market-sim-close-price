package entity.agent;

import static org.junit.Assert.*;
import static fourheap.Order.OrderType.*;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import logger.Logger;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.Iterables;

import activity.Activity;
import activity.SubmitNMSOrder;
import systemmanager.Consts;
import data.DummyFundamental;
import data.FundamentalValue;
import entity.agent.BackgroundAgent;
import entity.infoproc.SIP;
import entity.market.Market;
import entity.market.MockMarket;
import entity.market.Price;
import event.TimeStamp;

public class BackgroundAgentTest {

	private Market market;
	private SIP sip;
	
	@BeforeClass
	public static void setupClass() {
		Logger.setup(3, new File(Consts.TEST_OUTPUT_DIR + "BackgroundAgentTest.log"));
	}
	
	@Before
	public void setup() {
		sip = new SIP(TimeStamp.IMMEDIATE);
		market = new MockMarket(sip);
	}
	
	@Test
	public void getValuationBasic() {
		TimeStamp time = TimeStamp.ZERO;
		FundamentalValue randFundamental = new FundamentalValue(0.2, 100000, 10000, new Random());
		
		BackgroundAgent agent = new MockBackgroundAgent(randFundamental, sip, market);
		
		// Verify valuation (where PV = 0)
		Price val = agent.getValuation(BUY, time);
		assertEquals(randFundamental.getValueAt(time), val);
		val = agent.getValuation(SELL, time);
		assertEquals(randFundamental.getValueAt(time), val);
	}
	
	@Test
	public void getValuationConstPV() {
		TimeStamp time = TimeStamp.ZERO;
		List<Price> values = Arrays.asList(new Price(100), new Price(10));
		PrivateValue pv = new DummyPrivateValue(1, values);
		FundamentalValue fundamental = new DummyFundamental(100000);
		
		BackgroundAgent agent = new MockBackgroundAgent(fundamental, sip, market, pv, 0, 1000);
		
		// Verify valuation (current position of 0)
		Price fund = fundamental.getValueAt(time);
		Price val = agent.getValuation(BUY, time);
		assertEquals(fund.intValue() + 10, val.intValue());
		val = agent.getValuation(SELL, time);
		assertEquals(fund.intValue() + 100, val.intValue());
	}
	
	@Test
	public void getValuationRand() {
		// Testing with randomized values
		TimeStamp time = TimeStamp.ZERO;
		FundamentalValue randFundamental = new FundamentalValue(0.2, 100000, 10000, new Random());
		PrivateValue pv = new PrivateValue(5, 1000000, new Random());
		
		BackgroundAgent agent = new MockBackgroundAgent(randFundamental, sip, market, pv, 0, 1000);
		
		// Verify valuation for various positionBalances
		int pv0 = pv.values.get(0).intValue();
		int pv1 = pv.values.get(1).intValue();
		int pv2 = pv.values.get(2).intValue();
		int pv3 = pv.values.get(3).intValue();
		int pv4 = pv.values.get(4).intValue();
		int pv5 = pv.values.get(5).intValue();
		int pv6 = pv.values.get(6).intValue();
		int pv7 = pv.values.get(7).intValue();
		int pv8 = pv.values.get(8).intValue();
		int pv9 = pv.values.get(9).intValue();
		
		agent.positionBalance = 3;
		Price fund = randFundamental.getValueAt(time);
		Price val = agent.getValuation(BUY, time);
		assertEquals(fund.intValue() + pv8, val.intValue());
		assertEquals(fund.intValue()*2 + pv9 + pv8, 
				agent.getValuation(BUY, 2, time).intValue());
		val = agent.getValuation(SELL, time);
		assertEquals(fund.intValue() + pv7, val.intValue());
		assertEquals(fund.intValue()*2 + pv7 + pv6, 
				agent.getValuation(SELL, 2, time).intValue());
		
		agent.positionBalance = -2;
		fund = randFundamental.getValueAt(time);
		val = agent.getValuation(BUY, time);
		assertEquals(fund.intValue() + pv3, val.intValue());
		assertEquals(fund.intValue()*3 + pv3 + pv4 + pv5, 
				agent.getValuation(BUY, 3, time).intValue());
		val = agent.getValuation(SELL, time);
		assertEquals(fund.intValue() + pv2, val.intValue());
		assertEquals(fund.intValue()*3 + pv2 + pv1 + pv0, 
				agent.getValuation(SELL, 3, time).intValue());
	}
	
	@Test
	public void extraTest() {
		setup();
		getValuationRand();
	}
	
	
	@Test
	public void testZIStrat() {
		TimeStamp time = TimeStamp.ZERO;
		List<Price> values = Arrays.asList(new Price(100), new Price(10));
		PrivateValue pv = new DummyPrivateValue(1, values);
		FundamentalValue fundamental = new DummyFundamental(100000);
		
		BackgroundAgent agent = new MockBackgroundAgent(fundamental, sip, market, pv, 0, 1000);
		
		// Test that returns empty if exceed max position
		Iterable<? extends Activity> acts = agent.executeZIStrategy(BUY, 5, time);
		assertEquals(0, Iterables.size(acts));
		acts = agent.executeZIStrategy(SELL, 5, time);
		assertEquals(0, Iterables.size(acts));
		
		// Test ZI strategy
		acts = agent.executeZIStrategy(BUY, 1, time);
		assertTrue(Iterables.getOnlyElement(acts) instanceof SubmitNMSOrder);
		
		// XXX much of this is tested within ZIAgentTest, may want to move it here
	}
}
