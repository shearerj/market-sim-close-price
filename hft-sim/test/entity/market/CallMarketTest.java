package entity.market;

import static fourheap.Order.OrderType.BUY;
import static fourheap.Order.OrderType.SELL;
import static org.junit.Assert.assertEquals;
import static utils.Tests.checkQuote;
import static utils.Tests.checkSingleTransaction;

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

/**
 * Note that CallMarket initial Clear activities are inserted by the
 * SystemManager.executeEvents method.
 * 
 * @author ewah
 */
public class CallMarketTest {

	private static final Random rand = new Random();
	private MockSim sim;
	private Market market;
	private MarketView info;
	
	@Before
	public void setup() throws IOException {
		sim = MockSim.create(getClass(),
				Log.Level.NO_LOGGING, MarketType.CALL,
				Keys.NUM_MARKETS + '_' + 1, Keys.CLEAR_FREQ,
				100, Keys.PRICING_POLICY, 1);
		market = Iterables.getOnlyElement(sim.getMarkets());
		info = market.getPrimaryView();
		sim.executeImmediate(); // First clear
	}

	/** Test modified pricing policy */
	@Test
	public void pricingPolicyTest() {
		Agent agent1 = mockAgent();
		Agent agent2 = mockAgent();
		
		// Creating and adding bids
		submitOrder(agent1, BUY, Price.of(200), 1);
		submitOrder(agent2, SELL, Price.of(100), 1);
		
		// Testing market for the correct transaction
		clear();
		
		checkSingleTransaction(info.getTransactions(), Price.of(200), TimeStamp.ZERO, 1);
		checkQuote(info.getQuote(), null, 0, null, 0);
	}
	
	/** Test that market clears at intervals */
	@Test
	public void clearActivityInsertion() {
		Agent agent1 = mockAgent();
		Agent agent2 = mockAgent();
			
		// Test that before time 100 quotes do not change
		checkQuote(info.getQuote(), null, 0, null, 0);
		
		// Quote still undefined before clear
		submitOrder(agent1, BUY, Price.of(100),  1);
		OrderRecord sell = submitOrder(agent1, SELL, Price.of(110), 1);
		sim.executeUntil(TimeStamp.of(99));
		checkQuote(info.getQuote(), null, 0, null, 0);
		
		// Now quote should be updated
		sim.executeUntil(TimeStamp.of(100));
		checkQuote(info.getQuote(), Price.of(100), 1, Price.of(110), 1);
		
		// Now check that transactions are correct as well as quotes
		submitOrder(agent2, SELL, Price.of(150), 1);
		OrderRecord buy = submitOrder(agent2, BUY, Price.of(120), 1);
		// Before second clear interval ends, quote remains the same
		sim.executeUntil(TimeStamp.of(199));
		checkQuote(info.getQuote(), Price.of(100), 1, Price.of(110), 1);
		
		// Once clear interval ends, orders match and clear, and the quote updates
		sim.executeUntil(TimeStamp.of(200));
		
		checkQuote(info.getQuote(), Price.of(100), 1, Price.of(150), 1);
		checkSingleTransaction(info.getTransactions(), Price.of(120), TimeStamp.of(200), 1);
		assertEquals(0, buy.getQuantity());
		assertEquals(0, sell.getQuantity());
	}
	
	private void clear() {
		market.clear();
		sim.executeImmediate();
	}
	
	private Agent mockAgent() {
		return new Agent(sim, TimeStamp.ZERO, rand, Props.fromPairs()) {
			private static final long serialVersionUID = 1L;
			@Override public void agentStrategy() { }
			@Override public String toString() { return "TestAgent " + id; }
		};
	}
	
	private OrderRecord submitOrder(Agent agent, OrderType buyOrSell, Price price, int quantity) {
		OrderRecord order = OrderRecord.create(info, sim.getCurrentTime(), buyOrSell, price, quantity);
		market.submitOrder(info, agent.getView(info.getLatency()), order);
		return order;
	}
	
}
