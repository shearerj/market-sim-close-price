package entity.agent;

import static fourheap.Order.OrderType.BUY;
import static fourheap.Order.OrderType.SELL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static utils.Tests.checkNBBO;
import static utils.Tests.checkOrder;
import static utils.Tests.checkSingleTransaction;
import static utils.Tests.j;

import java.io.IOException;
import java.util.Iterator;
import java.util.Random;

import logger.Log;

import org.junit.Before;
import org.junit.Test;

import systemmanager.Consts.MarketType;
import systemmanager.Keys;
import systemmanager.MockSim;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

import data.Props;
import entity.market.Market;
import entity.market.Market.MarketView;
import entity.market.Price;
import event.TimeStamp;

/**
 * Only testing order withdrawal, everything else is just same as ZIAgent.
 * 
 * @author ewah
 *
 */
public class ZIRAgentTest {

	private static final Random rand = new Random();
	private static final Props defaults = Props.fromPairs(
			Keys.REENTRY_RATE, 0,
			Keys.MAX_QUANTITY, 1,
			Keys.PRIVATE_VALUE_VAR, 0,
			Keys.BID_RANGE_MIN, 1000,
			Keys.BID_RANGE_MAX, 1000);
	
	private MockSim sim;
	private Market market, other;
	private MarketView view, otherView;

	@Before
	public void setup() throws IOException{
		sim = MockSim.create(getClass(),
				Log.Level.NO_LOGGING, Keys.FUNDAMENTAL_MEAN,
				110000, Keys.FUNDAMENTAL_SHOCK_VAR,
				0, Keys.NBBO_LATENCY,
				50, MarketType.CDA, j.join(Keys.NUM_MARKETS, 2));
		Iterator<Market> markets = sim.getMarkets().iterator();
		market = markets.next();
		view = market.getPrimaryView();
		other = markets.next();
		otherView = other.getPrimaryView();
		assertFalse(markets.hasNext());
	}
	
	/**
	 * Scenario where ZIR withdrawing orders causes NBBO to grow stale and get
	 * out of sync.
	 * 
	 * FIXME Elaine please check that this is correct
	 */
	private OrderRecord withdrawRoutingScenario(ZIRAgent agent) {
		Agent seller = mockAgent();
		Agent buyer = mockAgent();
		
		seller.submitOrder(otherView, SELL, Price.of(111000), 1);
		seller.submitOrder(view, SELL, Price.of(108000), 1);
		buyer.submitOrder(otherView, BUY, Price.of(102000), 1);
		buyer.submitOrder(view, BUY, Price.of(104000), 1);
		
		sim.executeUntil(TimeStamp.of(50));
		checkNBBO(sim.getSIP().getNBBO(), Price.of(104000), market, Price.of(108000), market);
		
		OrderRecord first = agent.submitOrder(otherView, SELL, Price.of(105000), 1);
		sim.executeUntil(TimeStamp.of(100));
		checkNBBO(sim.getSIP().getNBBO(), Price.of(104000), market, Price.of(105000), other);
		
		agent.rand.setSeed(4); // Selected so next order is a BUY
		agent.agentStrategy();
		
		// Activity to actually submit order hasn't happened yet. Need to remove the first order in case it's not withdrawn
		OrderRecord order = Iterables.getOnlyElement(Sets.difference(
				Sets.newHashSet(agent.activeOrders), ImmutableSet.of(first)));
		checkOrder(order, Price.of(109000), 1, TimeStamp.of(100), null);
		
		// Actually submit the order
		sim.executeImmediate();
		return order;
	}

	/**
	 * Specific scenario where an out of date nbbo causes order to route where
	 * it causes an nbbo crossing.
	 */
	@Test
	public void withdrawQuoteUpdateTest() throws IOException {
		ZIRAgent agent = zirAgent(Keys.WITHDRAW_ORDERS, true);
		OrderRecord order = withdrawRoutingScenario(agent);
		
		// Check that the order got routed
		assertEquals(otherView, order.getCurrentMarket());

		/*
		 * Notice that the BID/ASK cross; if the SIP had been immediate, then
		 * the BUY order at 109 would have been routed to Nasdaq and it would
		 * have transacted immediately
		 */
		sim.executeUntil(TimeStamp.of(150));
		checkNBBO(sim.getSIP().getNBBO(), Price.of(109000), other, Price.of(108000), market);
	}
	
	/**
	 * Specific scenario where not withdrawing order means that the order will
	 * be routed and will transact immediately with the existing order.
	 */
	@Test
	public void noWithdrawQuoteUpdateTest() throws IOException {
		ZIRAgent agent = zirAgent(Keys.WITHDRAW_ORDERS, false);
		OrderRecord order = withdrawRoutingScenario(agent);
		
		// Check that the order got routed
		assertEquals(otherView, order.getCurrentMarket());
		
		// Check that orders transacted
		assertTrue(agent.activeOrders.isEmpty());
		assertEquals(0, order.getQuantity());
		checkSingleTransaction(otherView.getTransactions(), Price.of(105000), TimeStamp.of(100), 1);

		sim.executeUntil(TimeStamp.of(150));
		checkNBBO(sim.getSIP().getNBBO(), Price.of(104000), market, Price.of(108000), market);
	}

	public ZIRAgent zirAgent(Object... parameters) {
		return ZIRAgent.create(sim, TimeStamp.ZERO, market, rand, Props.withDefaults(defaults, parameters));
	}
	
	private Agent mockAgent() {
		return new Agent(sim, TimeStamp.ZERO, rand, Props.fromPairs()) {
			private static final long serialVersionUID = 1L;
			@Override public void agentStrategy() { }
			@Override public String toString() { return "TestAgent " + id; }
		};
	}
}
