package entity.agent;

import static fourheap.Order.OrderType.BUY;
import static fourheap.Order.OrderType.SELL;
import static org.junit.Assert.assertTrue;
import static utils.Tests.assertOrder;
import logger.Log;

import org.junit.Before;
import org.junit.Test;

import systemmanager.Keys.AcceptableProfitThreshold;
import systemmanager.Keys.ArrivalRate;
import systemmanager.Keys.FundamentalKappa;
import systemmanager.Keys.FundamentalMean;
import systemmanager.Keys.FundamentalShockVar;
import systemmanager.Keys.MaxPosition;
import systemmanager.Keys.PrivateValueVar;
import systemmanager.Keys.RMax;
import systemmanager.Keys.RMin;
import systemmanager.Keys.SimLength;
import systemmanager.Keys.Withdraw;
import utils.Mock;
import utils.Rand;

import com.google.common.collect.Iterables;

import data.FundamentalValue;
import data.Props;
import entity.agent.strategy.OptimalLimitPriceEstimator;
import entity.market.Market;
import entity.market.Market.MarketView;
import entity.market.Price;
import event.TimeStamp;
import fourheap.Order.OrderType;

public class ZIRPAgentTest {
	private static final Rand rand = Rand.create();
	private static final Agent mockAgent = Mock.agent();
	private static final FundamentalValue fundamental = Mock.fundamental(63152);
	private static final Props defaults = Props.builder()
			.put(ArrivalRate.class,			0d)
			.put(MaxPosition.class,				2)
			.put(PrivateValueVar.class,		100d)
			.put(RMin.class,				10000)
			.put(RMax.class,				10000)
			.put(SimLength.class,			60000)
			.put(FundamentalKappa.class,	0.05)
			.put(FundamentalMean.class,		100000)
			.put(FundamentalShockVar.class,	0d)
			.put(Withdraw.class,			true)
			.put(AcceptableProfitThreshold.class, 0.75)
			.build();
	
	private Market market;
	private MarketView view;
	
	
	@Before
	public void setup() {
		market = Mock.market();
		view = market.getPrimaryView();
	}
	
	/** Verify that agentStrategy actually follows ZIRP strategy */
	@Test
	public void zirpBasicBuyerTest() {
		OrderType type = SELL;
		OrderRecord order = null;
		Price val = null;
		BackgroundAgent zirp;
		// Loop runs until agent submits a buy order
		while (type == SELL) {
			setup();
			zirp = zirpAgent();
			setQuote(Price.of(120000), Price.of(130000));
			
			val = OptimalLimitPriceEstimator.create(zirp, fundamental.getView(TimeStamp.ZERO), Mock.timeline, defaults).getLimitPrice(BUY, 1);
			zirp.agentStrategy();
			
			// Sometimes an order will transact, so we need a default in that case
			order = Iterables.getOnlyElement(zirp.getActiveOrders(), OrderRecord.create(view, TimeStamp.ZERO, SELL, Price.ZERO, 1));
			// If an order transacted, we need to verify that it wasn't a buy order
			assertTrue("Submitted a BUY order that transacted", view.getQuote().getAskPrice().isPresent());
			
			type = order.getOrderType();
		}

		// Verify that agent does shade since 10000 * 0.75 > val - 130000
		assertOrder(order, Price.of(val.intValue() - 10000), 1, TimeStamp.ZERO, TimeStamp.ZERO);
	}
	
	@Test
	public void randomTest() {
		for (int i = 0; i < 100; ++i) {
			setup();
			zirpBasicBuyerTest();
		}
	}
	
	private void setQuote(Price bid, Price ask) {
		mockAgent.submitOrder(view, BUY, bid, 1);
		mockAgent.submitOrder(view, SELL, ask, 1);
	}

	public ZIRPAgent zirpAgent() {
		Mock.timeline.ignoreNext();
		return ZIRPAgent.create(0, Mock.stats, Mock.timeline, Log.nullLogger(), rand, Mock.sip, fundamental, market, defaults);
	}
	
}
