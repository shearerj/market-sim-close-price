package entity.market;

import static org.junit.Assert.assertTrue;

import java.util.Random;

import logger.Log;

import org.junit.Test;

import systemmanager.Consts.MarketType;
import systemmanager.Keys.ClearFrequency;
import utils.Mock;
import data.Props;
import event.TimeStamp;

public class MarketFactoryTest {

	@Test
	public void createMarkets() {
		Random rand = new Random();
		MarketFactory factory = MarketFactory.create(Mock.stats, Mock.timeline, Log.nullLogger(), rand, Mock.sip);
		Market market;

		market = factory.createMarket(MarketType.CALL, Props.fromPairs(ClearFrequency.class, TimeStamp.of(100)));
		assertTrue(market instanceof CallMarket);
		market = factory.createMarket(MarketType.CALL, Props.fromPairs(ClearFrequency.class, TimeStamp.ZERO));
		assertTrue(market instanceof CDAMarket);
		market = factory.createMarket(MarketType.CDA, Props.fromPairs());
		assertTrue(market instanceof CDAMarket);
	}
}
