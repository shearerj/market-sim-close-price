package entity.market;

import static fourheap.Order.OrderType.BUY;
import static fourheap.Order.OrderType.SELL;
import static org.junit.Assert.assertEquals;
import static utils.Tests.checkQuote;

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
import entity.agent.Agent;
import entity.agent.OrderRecord;
import entity.market.Market.MarketView;
import event.TimeStamp;
import fourheap.Order.OrderType;

public class CDAMarketTest {

	private static final Random rand = new Random();
	private MockSim sim;
	private Market market;
	private MarketView info;

	@Before
	public void setup() throws IOException {
		sim = MockSim.create(getClass(),
				Log.Level.NO_LOGGING, MarketType.CDA, Keys.NUM_MARKETS + '_' + 1);
		market = Iterables.getOnlyElement(sim.getMarkets());
		info = market.getPrimaryView();
	}
	
	@Test
	public void submitClearTest() {
		Agent agent1 = mockAgent();
		Agent agent2 = mockAgent();
		
		submitOrder(agent1, BUY, Price.of(100), 1);
		submitOrder(agent2, SELL, Price.of(100), 1);
		
		checkQuote(info.getQuote(), null, 0, null, 0);
		assertEquals(1, info.getTransactions().size());
	}
	
	@Test
	public void withdrawUpdateTest() {
		Agent agent = mockAgent();
		
		OrderRecord order = submitOrder(agent, BUY, Price.of(100), 1);
		
		checkQuote(info.getQuote(), Price.of(100), 1, null, 0);
		
		withdrawOrder(order);
		
		checkQuote(info.getQuote(), null, 0, null, 0);
	}
	
	private void withdrawOrder(OrderRecord order) {
		market.withdrawOrder(order, order.getQuantity());
		sim.executeImmediate();
	}
	
	private OrderRecord submitOrder(Agent agent, OrderType buyOrSell, Price price, int quantity) {
		OrderRecord order = OrderRecord.create(info, sim.getCurrentTime(), buyOrSell, price, quantity);
		market.submitOrder(info, agent.getView(info.getLatency()), order);
		sim.executeImmediate();
		return order;
	}
	
	private Agent mockAgent() {
		return new Agent(sim, TimeStamp.ZERO, rand, Props.fromPairs()) {
			private static final long serialVersionUID = 1L;
			@Override public void agentStrategy() { }
			@Override public String toString() { return "TestAgent " + id; }
		};
	}
	
}
