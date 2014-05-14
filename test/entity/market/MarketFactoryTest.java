package entity.market;

import static org.junit.Assert.*;

import java.util.Random;

import org.junit.*;

import data.MarketProperties;
import entity.infoproc.SIP;
import event.TimeStamp;
import systemmanager.Consts.MarketType;
import systemmanager.Keys;
import systemmanager.Scheduler;

public class MarketFactoryTest {
	
	private Scheduler scheduler;
	private SIP sip;
	private MarketFactory factory;
	
	@Before
	public void setup() {
		this.scheduler = new Scheduler(new Random());
		this.factory = new MarketFactory(scheduler, sip, new Random());
	}
	
	@Test
	public void createCallMarket() {
		MarketProperties props = MarketProperties.empty(MarketType.CALL);
		props.put(Keys.CLEAR_FREQ, 100);
		Market mkt = factory.createMarket(props);
		assertTrue(mkt instanceof CallMarket);
		assertEquals(TimeStamp.create(100), ((CallMarket) mkt).clearFreq);
	}
	
	@Test
	public void createZeroLatencyCallMarket() {
		MarketProperties props = MarketProperties.empty(MarketType.CALL);
		props.put(Keys.CLEAR_FREQ, 0);
		Market mkt = factory.createMarket(props);
		assertTrue(mkt instanceof CDAMarket);
	}
}
