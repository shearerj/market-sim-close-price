package entity.market;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Random;

import logger.Log;

import org.junit.Before;
import org.junit.Test;

import systemmanager.Consts.MarketType;
import systemmanager.Keys;
import systemmanager.MockSim;
import data.Props;

public class MarketFactoryTest {

	private static final Random rand = new Random();
	private MockSim sim;
	private MarketFactory factory;

	@Before
	public void setup() throws IOException {
		this.sim = MockSim.create(getClass(), Log.Level.NO_LOGGING);
		this.factory = MarketFactory.create(sim, rand);
	}

	@Test
	public void createMarkets() {
		Market mkt;

		mkt = factory.createMarket(MarketType.CALL, Props.fromPairs(Keys.CLEAR_FREQ, 100));
		assertTrue(mkt instanceof CallMarket);
		mkt = factory.createMarket(MarketType.CALL, Props.fromPairs(Keys.CLEAR_FREQ, 0));
		assertTrue(mkt instanceof CDAMarket);
		mkt = factory.createMarket(MarketType.CDA, Props.fromPairs());
		assertTrue(mkt instanceof CDAMarket);
	}
}
