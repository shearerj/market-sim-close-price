package entity.agent;

import static fourheap.Order.OrderType.BUY;
import static fourheap.Order.OrderType.SELL;
import static org.junit.Assert.assertEquals;
import static utils.Tests.assertNBBO;
import static utils.Tests.assertOrder;
import static utils.Tests.assertSingleTransaction;
import logger.Log;

import org.junit.Before;
import org.junit.Test;

import systemmanager.Keys.BackgroundReentryRate;
import systemmanager.Keys.PrivateValueVar;
import systemmanager.Keys.Rmax;
import systemmanager.Keys.Rmin;
import systemmanager.Keys.Withdraw;
import utils.Mock;
import utils.Rand;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

import data.FundamentalValue;
import data.Props;
import entity.market.CDAMarket;
import entity.market.Market;
import entity.market.Market.MarketView;
import entity.market.Price;
import entity.sip.MarketInfo;
import entity.sip.SIP;
import event.EventQueue;
import event.TimeStamp;

/**
 * Only testing order withdrawal, everything else is just same as ZIAgent.
 * 
 * @author ewah
 *
 */
public class ZIRAgentTest {

	private static final Rand rand = Rand.create();
	private static final Agent mockAgent = Mock.agent();
	private static final Props defaults = Props.fromPairs(
			BackgroundReentryRate.class, 0d,
			PrivateValueVar.class, 0d,
			Rmin.class, 1000,
			Rmax.class, 1000);
	
	private EventQueue timeline;
	private FundamentalValue fundamental;
	private MarketInfo sip;
	private Market nyse, nasdaq;
	private MarketView nyseView, nasdaqView;

	@Before
	public void setup() {
		timeline = EventQueue.create(Log.nullLogger(), rand);
		fundamental = Mock.fundamental(110000);
		sip = SIP.create(Mock.stats, timeline, Log.nullLogger(), rand, TimeStamp.of(50));
		nyse = CDAMarket.create(0, Mock.stats, timeline, Log.nullLogger(), rand, sip, Props.fromPairs());
		nyseView = nyse.getPrimaryView();
		nasdaq = CDAMarket.create(1, Mock.stats, timeline, Log.nullLogger(), rand, sip, Props.fromPairs());
		nasdaqView = nasdaq.getPrimaryView();
	}
	
	/**
	 * Scenario where ZIR withdrawing orders causes NBBO to grow stale and get
	 * out of sync.
	 */
	private OrderRecord withdrawRoutingScenario(ZIRAgent agent) {
		mockAgent.submitOrder(nasdaqView, SELL, Price.of(111000), 1);
		mockAgent.submitOrder(nyseView, SELL, Price.of(108000), 1);
		mockAgent.submitOrder(nasdaqView, BUY, Price.of(102000), 1);
		mockAgent.submitOrder(nyseView, BUY, Price.of(104000), 1);
		
		OrderRecord bait = mockAgent.submitOrder(nasdaqView, BUY, Price.of(105000), 1); // Will coax routing of agent's order nasdaq
		
		timeline.executeUntil(TimeStamp.of(50));
		assertNBBO(sip.getNBBO(), Price.of(105000), nasdaq, Price.of(108000), nyse);
		
		nasdaqView.withdrawOrder(bait);
		OrderRecord first = agent.submitNMSOrder(SELL, Price.of(105000), 1);
		// NBBO will be out of date
		timeline.executeUntil(TimeStamp.of(99));
		assertNBBO(sip.getNBBO(), Price.of(105000), nasdaq, Price.of(108000), nyse);
		// NBBO updated after SIP updates
		timeline.executeUntil(TimeStamp.of(100));
		assertNBBO(sip.getNBBO(), Price.of(104000), nyse, Price.of(105000), nasdaq);
		assertEquals(nasdaqView, first.getCurrentMarket());
		
		agent.agentStrategy();
		
		// Activity to actually submit order hasn't happened yet. Need to remove the first order in case it's not withdrawn
		OrderRecord order = Iterables.getOnlyElement(Sets.difference(
				Sets.newHashSet(agent.getActiveOrders()), ImmutableSet.of(first)));
		if (order.getOrderType() == SELL)
			return null; // Only valid if agent submits a buy order
		assertOrder(order, Price.of(109000), 1, TimeStamp.of(100), null);
		
		// Actually submit the order
		timeline.executeUntil(TimeStamp.of(100));
		return order;
	}

	/**
	 * Specific scenario where an out of date nbbo causes order to route where
	 * it causes an nbbo crossing.
	 */
	@Test
	public void withdrawQuoteUpdateTest() {
		OrderRecord order = null;
		while (order == null) {
			setup();
			ZIRAgent agent = zirAgent(Props.fromPairs(Withdraw.class, true));
			order = withdrawRoutingScenario(agent);
		}
		
		// Check that the order got routed
		assertEquals(nasdaqView, order.getCurrentMarket());

		/*
		 * Notice that the BID/ASK cross; if the SIP had been immediate, then
		 * the BUY order at 109 would have been routed to Nasdaq and it would
		 * have transacted immediately
		 */
		timeline.executeUntil(TimeStamp.of(150));
		assertNBBO(sip.getNBBO(), Price.of(109000), nasdaq, Price.of(108000), nyse);
	}
	
	/**
	 * Specific scenario where not withdrawing order means that the order will
	 * be routed and will transact immediately with the existing order.
	 */
	@Test
	public void noWithdrawQuoteUpdateTest() {
		OrderRecord order = null;
		while (order == null) {
			setup();
			ZIRAgent agent = zirAgent(Props.fromPairs(Withdraw.class, false));
			order = withdrawRoutingScenario(agent);
		}
		
		// Check that the order got routed
		assertEquals(nasdaqView, order.getCurrentMarket());
		
		// Check that orders transacted
		assertEquals(0, order.getQuantity());
		assertSingleTransaction(nasdaqView.getTransactions(), Price.of(105000), TimeStamp.of(100), 1);

		timeline.executeUntil(TimeStamp.of(150));
		assertNBBO(sip.getNBBO(), Price.of(104000), nyse, Price.of(108000), nyse);
	}
	
	/** Tests a bug where an order would get withdrawn before it entered the market and would result in an error being thrown */
	@Test
	public void randomTest() {
		for (int i = 0; i < 100; ++i) {
			EventQueue timeline = EventQueue.create(Log.nullLogger(), rand);
			Market market = Mock.market(timeline);			
			ZIRAgent.create(0, Mock.stats, timeline, Log.nullLogger(), rand, Mock.sip, Mock.fundamental, market, Props.fromPairs());
			timeline.executeUntil(TimeStamp.of(6000));
		}
	}

	public ZIRAgent zirAgent(Props parameters) {
		return ZIRAgent.create(0, Mock.stats, timeline, Log.nullLogger(), rand, sip, fundamental, nyse, Props.merge(defaults, parameters));
	}
	
}
