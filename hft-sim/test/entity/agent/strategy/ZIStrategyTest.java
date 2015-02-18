package entity.agent.strategy;

import static fourheap.Order.OrderType.BUY;
import static fourheap.Order.OrderType.SELL;
import static org.junit.Assert.assertEquals;
import static utils.Tests.assertRange;
import logger.Log;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

import utils.Mock;
import utils.Rand;
import data.FundamentalValue;
import data.Props;
import entity.agent.BackgroundAgent;
import entity.agent.OrderRecord;
import entity.agent.position.ListPrivateValue;
import entity.market.Market;
import entity.market.Price;
import event.TimeStamp;

public class ZIStrategyTest {

	private static final Rand rand = Rand.create();
	
	private Market market;
	private FundamentalValue fundamental;
	
	@Before
	public void setup() {
		market = Mock.market();
		fundamental = Mock.fundamental(12345);
	}
	
	@Test
	public void randZIBuyTest() {
		int min_shade = rand.nextInt(5000); 		//[$0.00, $5.00];
		int max_shade = 5000 + rand.nextInt(5000);	//[$5.00, $10.00];
		
		ListPrivateValue value = ListPrivateValue.createRandomly(1, 1e6, rand);
		BackgroundAgent agent = backgroundAgent(value);
		BackgroundStrategy zi = ZIStrategy.create(Mock.timeline, market.getPrimaryView(),
				NaiveLimitPriceEstimator.create(agent, fundamental.getView(TimeStamp.ZERO)),
				min_shade, max_shade, rand);

		//Execute strategy
		OrderRecord order = zi.getOrder(BUY, 1);

		// Calculate Price Range
		Price pv = value.getValue(0, BUY);
		
		assertRange(order.getPrice(),
				Price.of(12345 + pv.intValue() - max_shade),
				Price.of(12345 + pv.intValue() - min_shade));
		assertEquals(1, order.getQuantity());
	}
	
	@Test
	public void randZISellTest() {
		int min_shade = rand.nextInt(5000); 		//[$0.00, $5.00];
		int max_shade = 5000 + rand.nextInt(5000);	//[$5.00, $10.00];

		ListPrivateValue value = ListPrivateValue.createRandomly(1, 1e6, rand);
		BackgroundAgent agent = backgroundAgent(value);
		BackgroundStrategy zi = ZIStrategy.create(Mock.timeline, market.getPrimaryView(),
				NaiveLimitPriceEstimator.create(agent, fundamental.getView(TimeStamp.ZERO)),
				min_shade, max_shade, rand);
		//Execute strategy
		OrderRecord order = zi.getOrder(SELL, 1);

		// Calculate Price Range
		Price pv = value.getValue(0, SELL);
		
		assertRange(order.getPrice(),
				Price.of(12345 + pv.intValue() + min_shade),
				Price.of(12345 + pv.intValue() + max_shade));
		assertEquals(1, order.getQuantity());

	}
	
	@Test
	public void initialPriceZIBuyTest() {
		fundamental = FundamentalValue.create(Mock.stats, Mock.timeline, 0, 100000, 1e8, 1.0, rand);
		BackgroundAgent agent = backgroundAgent(ListPrivateValue.createRandomly(1, 0, rand));
		BackgroundStrategy zi = ZIStrategy.create(Mock.timeline, market.getPrimaryView(),
				NaiveLimitPriceEstimator.create(agent, fundamental.getView(TimeStamp.ZERO)),
				0, 0, rand);
		
		OrderRecord order = zi.getOrder(BUY, 1);
		/*
		 * Fundamental = 100000 ($100.00), Stdev = sqrt(100000000) = 10000
		 * ($10.000) Bid Range = 0, 0 ($0.00, $1.00) 99.993% of bids should
		 * fall between 100000 +/- (4*10000) = 60000, 140000
		 */
		assertRange(order.getPrice(), Price.of(60000), Price.of(140000));
		assertEquals(1, order.getQuantity());
	}
	
	@Test
	public void initialPriceZISellTest() {
		fundamental = FundamentalValue.create(Mock.stats, Mock.timeline, 0, 100000, 1e8, 1.0, rand);
		BackgroundAgent agent = backgroundAgent(ListPrivateValue.createRandomly(1, 0, rand));
		BackgroundStrategy zi = ZIStrategy.create(Mock.timeline, market.getPrimaryView(),
				NaiveLimitPriceEstimator.create(agent, fundamental.getView(TimeStamp.ZERO)),
				0, 0, rand);
		
		OrderRecord order = zi.getOrder(SELL, 1);
		/*
		 * Fundamental = 100000 ($100.00), Stdev = sqrt(100000000) = 10000
		 * ($10.000) Bid Range = 0, 0 ($0.00, $1.00) 99.993% of bids should
		 * fall between 100000 +/- (4*10000) = 60000, 140000
		 */
		assertRange(order.getPrice(), Price.of(60000), Price.of(140000));
		assertEquals(1, order.getQuantity());
	}
	
	@Test
	public void ziPrivateValueBuyTest() {
		fundamental = Mock.fundamental(100000);
		BackgroundAgent agent = backgroundAgent(ListPrivateValue.create(Price.of(10000), Price.of(-10000)));
		BackgroundStrategy zi = ZIStrategy.create(Mock.timeline, market.getPrimaryView(),
				NaiveLimitPriceEstimator.create(agent, fundamental.getView(TimeStamp.ZERO)),
				0, 1000, rand);
		
		OrderRecord order = zi.getOrder(BUY, 1);
		
		// Buyers always buy at price lower than valuation ($100 + buy PV = $90)
		assertRange(order.getPrice(), Price.of(89000), Price.of(90000));
		assertEquals(1, order.getQuantity());
	}
	
	@Test
	public void ziPrivateValueSellTest() {
		fundamental = Mock.fundamental(100000);
		BackgroundAgent agent = backgroundAgent(ListPrivateValue.create(Price.of(10000), Price.of(-10000)));
		BackgroundStrategy zi = ZIStrategy.create(Mock.timeline, market.getPrimaryView(),
				NaiveLimitPriceEstimator.create(agent, fundamental.getView(TimeStamp.ZERO)),
				0, 1000, rand);

		OrderRecord order = zi.getOrder(SELL, 1);

		// Sellers always sell at price higher than valuation ($100 + sell PV = $110)
		assertRange(order.getPrice(), Price.of(110000), Price.of(111000));
		assertEquals(1, order.getQuantity());
	}
	
	@Test
	public void randomTest() {
		for (int i = 0; i < 100; ++i) {
			setup();
			randZIBuyTest();
			setup();
			randZISellTest();
			setup();
			initialPriceZIBuyTest();
			setup();
			initialPriceZISellTest();
			setup();
			ziPrivateValueBuyTest();
			setup();
			ziPrivateValueSellTest();
		}
	}
	
	private BackgroundAgent backgroundAgent(ListPrivateValue privateValue) {
		return new BackgroundAgent(0, Mock.stats, Mock.timeline, Log.nullLogger(), rand, Mock.sip, fundamental,
				ImmutableList.<TimeStamp> of().iterator(), privateValue, market, Props.fromPairs()) {
			private static final long serialVersionUID = 1L;
		};
	}

}
