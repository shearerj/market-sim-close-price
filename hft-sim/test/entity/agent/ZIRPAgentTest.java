package entity.agent;

import static fourheap.Order.OrderType.BUY;
import static fourheap.Order.OrderType.SELL;
import static org.junit.Assert.assertEquals;
import static utils.Tests.checkSingleOrder;
import static utils.Tests.j;

import java.io.IOException;
import java.util.Random;

import logger.Log;

import org.junit.Before;
import org.junit.Test;

import systemmanager.Consts.MarketType;
import systemmanager.Keys;
import systemmanager.MockSim;

import com.google.common.collect.Iterables;

import data.Props;
import entity.market.Market;
import entity.market.Market.MarketView;
import entity.market.Price;
import event.TimeStamp;
import fourheap.Order.OrderType;

public class ZIRPAgentTest {
	private static Random rand = new Random();
	private static Props defaults = Props.fromPairs(
		Keys.REENTRY_RATE, 0,
		Keys.MAX_QUANTITY, 2,
		Keys.PRIVATE_VALUE_VAR, 100,
		Keys.BID_RANGE_MIN, 10000,
		Keys.BID_RANGE_MAX, 10000,
		Keys.SIMULATION_LENGTH, 60000,
		Keys.FUNDAMENTAL_KAPPA, 0.05,
		Keys.FUNDAMENTAL_MEAN, 100000,
		Keys.FUNDAMENTAL_SHOCK_VAR, 0,
		Keys.WITHDRAW_ORDERS, true,
		Keys.ACCEPTABLE_PROFIT_FRACTION, 0.75);
	
	private MockSim sim;
	private Market market;
	private MarketView view;
	private Agent mockAgent;
	
	@Before
	public void setup() throws IOException {
		sim = MockSim.create(getClass(), Log.Level.NO_LOGGING, MarketType.CDA, j.join(Keys.NUM_MARKETS, 1));
		market = Iterables.getOnlyElement(sim.getMarkets());
		view = market.getPrimaryView();
		mockAgent = mockAgent();
	}
	
	/** Verify that agentStrategy actually follows ZIRP strategy */
	@Test
	public void zirpBasicBuyerTest() throws IOException {
		ZIRPAgent zirp = zirpAgent();
		setQuote(Price.of(120000), Price.of(130000));
		
		Price val = zirp.getEstimatedValuation(BUY);
		zirp.rand.setSeed(0); // Set to get a BUY order
		zirp.agentStrategy();
		
		assertEquals("Didn't submit appropriate order type", BUY, Iterables.getOnlyElement(zirp.activeOrders).getOrderType());
		sim.executeImmediate();

		 // Verify that agent does shade since 10000 * 0.75 > val - 130000
		
		checkSingleOrder(zirp.activeOrders, Price.of(val.intValue() - 10000), 1, TimeStamp.ZERO, TimeStamp.ZERO);
	}
	
	private void setQuote(Price bid, Price ask) {
		submitOrder(mockAgent, BUY, bid);
		submitOrder(mockAgent, SELL, ask);
	}

	private OrderRecord submitOrder(Agent agent, OrderType buyOrSell, Price price) {
		OrderRecord order = agent.submitOrder(view, buyOrSell, price, 1);
		sim.executeImmediate();
		return order;
	}

	public ZIRPAgent zirpAgent(Object... parameters) {
		return ZIRPAgent.create(sim, TimeStamp.ZERO, market, rand, Props.withDefaults(defaults, parameters));
	}
	
	private Agent mockAgent() {
		return new Agent(sim, TimeStamp.ZERO, rand, Props.fromPairs()) {
			private static final long serialVersionUID = 1L;
			@Override public void agentStrategy() { }
			@Override public String toString() { return "TestAgent " + id; }
		};
	}
}
