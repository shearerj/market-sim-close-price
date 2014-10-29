package entity.agent;

import static fourheap.Order.OrderType.BUY;
import static fourheap.Order.OrderType.SELL;
import static utils.Tests.*;

import java.io.IOException;
import java.util.Random;

import logger.Log;

import org.junit.Before;
import org.junit.Test;

import systemmanager.Keys.AcceptableProfitFrac;
import systemmanager.Keys.ArrivalRate;
import systemmanager.Keys.BidRangeMax;
import systemmanager.Keys.BidRangeMin;
import systemmanager.Keys.FundamentalKappa;
import systemmanager.Keys.FundamentalMean;
import systemmanager.Keys.FundamentalShockVar;
import systemmanager.Keys.MaxQty;
import systemmanager.Keys.PrivateValueVar;
import systemmanager.Keys.SimLength;
import systemmanager.Keys.WithdrawOrders;
import utils.Mock;

import com.google.common.collect.Iterables;

import data.Props;
import entity.market.Market;
import entity.market.Market.MarketView;
import entity.market.Price;
import event.TimeStamp;
import fourheap.Order.OrderType;

public class ZIRPAgentTest {
	private static final Random rand = new Random();
	private static final Agent mockAgent = Mock.agent();
	private static final Props defaults = Props.builder()
			.put(ArrivalRate.class,		0d)
			.put(MaxQty.class,			2)
			.put(PrivateValueVar.class,	100d)
			.put(BidRangeMin.class,		10000)
			.put(BidRangeMax.class,		10000)
			.put(SimLength.class,		60000)
			.put(FundamentalKappa.class, 0.05)
			.put(FundamentalMean.class,	100000)
			.put(FundamentalShockVar.class, 0d)
			.put(WithdrawOrders.class,	true)
			.put(AcceptableProfitFrac.class, 0.75)
			.build();
	
	private Market market;
	private MarketView view;
	
	
	@Before
	public void setup() throws IOException {
		market = Mock.market();
		view = market.getPrimaryView();
	}
	
	/** Verify that agentStrategy actually follows ZIRP strategy */
	@Test
	public void zirpBasicBuyerTest() throws IOException {
		OrderType type = SELL;
		OrderRecord order = null;
		Price val = null;
		BackgroundAgent zirp;
		while (type == SELL) { // Must submit buy order
			setup();
			zirp = zirpAgent();
			setQuote(Price.of(120000), Price.of(130000));

			val = zirp.getEstimatedValuation(BUY);
			zirp.agentStrategy();
			order = Iterables.getOnlyElement(zirp.getActiveOrders());
			type = order.getOrderType();
		}

		 // Verify that agent does shade since 10000 * 0.75 > val - 130000
		assertOrder(order, Price.of(val.intValue() - 10000), 1, TimeStamp.ZERO, TimeStamp.ZERO);
	}
	
	private void setQuote(Price bid, Price ask) {
		mockAgent.submitOrder(view, BUY, bid, 1);
		mockAgent.submitOrder(view, SELL, ask, 1);
	}

	public ZIRPAgent zirpAgent() {
		return ZIRPAgent.create(0, Mock.stats, Mock.timeline, Log.nullLogger(), rand, Mock.sip, Mock.fundamental, market, defaults);
	}
	
}
