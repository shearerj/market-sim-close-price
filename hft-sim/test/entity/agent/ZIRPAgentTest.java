package entity.agent;

import static fourheap.Order.OrderType.BUY;
import static fourheap.Order.OrderType.SELL;
import static org.junit.Assert.assertEquals;
import static utils.Tests.checkSingleOrder;

import java.io.IOException;
import java.util.Random;

import logger.Log;

import org.junit.Before;
import org.junit.Test;

import systemmanager.Keys.AcceptableProfitFrac;
import systemmanager.Keys.BidRangeMax;
import systemmanager.Keys.BidRangeMin;
import systemmanager.Keys.FundamentalKappa;
import systemmanager.Keys.FundamentalMean;
import systemmanager.Keys.FundamentalShockVar;
import systemmanager.Keys.MaxQty;
import systemmanager.Keys.PrivateValueVar;
import systemmanager.Keys.ReentryRate;
import systemmanager.Keys.SimLength;
import systemmanager.Keys.WithdrawOrders;
import systemmanager.MockSim;

import com.google.common.collect.Iterables;

import data.Props;
import entity.agent.position.PrivateValues;
import entity.market.Market;
import entity.market.Market.MarketView;
import entity.market.Price;
import event.TimeStamp;
import fourheap.Order.OrderType;

public class ZIRPAgentTest {
	private static Random rand = new Random();
	private static Props defaults = Props.builder()
			.put(ReentryRate.class, 0d)
			.put(MaxQty.class, 2)
			.put(PrivateValueVar.class, 100d)
			.put(BidRangeMin.class, 10000)
			.put(BidRangeMax.class, 10000)
			.put(SimLength.class, 60000)
			.put(FundamentalKappa.class, 0.05)
			.put(FundamentalMean.class, 100000)
			.put(FundamentalShockVar.class, 0d)
			.put(WithdrawOrders.class, true)
			.put(AcceptableProfitFrac.class, 0.75)
			.build();
	
	private MockSim sim;
	private Market market;
	private MarketView view;
	private Agent mockAgent;
	
	@Before
	public void setup() throws IOException {
		sim = MockSim.createCDA(getClass(), Log.Level.NO_LOGGING, 1);
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

	public ZIRPAgent zirpAgent() {
		return ZIRPAgent.create(sim, TimeStamp.ZERO, market, rand, defaults);
	}
	
	private Agent mockAgent() {
		return new Agent(sim, PrivateValues.zero(), TimeStamp.ZERO, rand, Props.fromPairs()) {
			private static final long serialVersionUID = 1L;
			@Override public void agentStrategy() { }
			@Override public String toString() { return "TestAgent " + id; }
		};
	}
}
