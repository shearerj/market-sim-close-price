package entity.agent;

import static fourheap.Order.OrderType.BUY;
import static fourheap.Order.OrderType.SELL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static utils.Tests.checkOrderLadder;
import static utils.Tests.checkRandomOrderLadder;
import static utils.Tests.checkSingleTransaction;
import static utils.Tests.j;

import java.io.IOException;
import java.util.Random;

import logger.Log;

import org.junit.Before;
import org.junit.Test;

import systemmanager.Consts.MarketType;
import systemmanager.Keys;
import systemmanager.MockSim;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Range;

import data.Props;
import entity.market.Market;
import entity.market.Market.MarketView;
import entity.market.Price;
import event.TimeStamp;
import fourheap.Order.OrderType;

public class BasicMarketMakerTest {
	private static final Random rand = new Random();
	private static final Props defaults = Props.fromPairs(
			Keys.REENTRY_RATE, 0,
			Keys.NUM_RUNGS, 3,
			Keys.RUNG_SIZE, 5,
			Keys.TRUNCATE_LADDER, true,
			Keys.TICK_IMPROVEMENT, false);
	
	private MockSim sim;
	private Market actualMarket;
	private MarketView market;
	private Agent mockAgent;

	@Before
	public void setup() throws IOException {
		sim = MockSim.create(getClass(),
				Log.Level.NO_LOGGING, Keys.FUNDAMENTAL_MEAN,
				100000, Keys.FUNDAMENTAL_SHOCK_VAR,
				0, MarketType.CDA, j.join(Keys.NUM_MARKETS, 1));
		actualMarket = Iterables.getOnlyElement(sim.getMarkets());
		market = actualMarket.getPrimaryView();
		mockAgent = mockAgent();
	}

	@Test
	public void nullBidAsk() {
		BasicMarketMaker mm = basicMarketMaker(
				Keys.TRUNCATE_LADDER, false,
				Keys.INITIAL_LADDER_MEAN, 0,
				Keys.INITIAL_LADDER_RANGE, 0);
		mm.agentStrategy();
		assertTrue(mm.activeOrders.isEmpty());
	}

	/** Was defined, then the market maker should not do anything. */
	@Test
	public void quoteUndefined() {
		BasicMarketMaker mm = basicMarketMaker(
				Keys.TRUNCATE_LADDER, false,
				Keys.INITIAL_LADDER_MEAN, 0,
				Keys.INITIAL_LADDER_RANGE, 0);
		mm.lastAsk = Optional.of(Price.of(55));
		mm.lastBid = Optional.of(Price.of(45));

		// Both sides undefined
		mm.agentStrategy();
		assertTrue(mm.activeOrders.isEmpty());

		// One side undefined
		submitOrder(mockAgent, BUY, Price.of(40));

		mm.lastAsk = Optional.of(Price.of(55));
		mm.lastBid = Optional.of(Price.of(45));
		mm.agentStrategy();
		assertTrue(mm.activeOrders.isEmpty());
	}


	@Test
	public void basicLadderTest() {
		BasicMarketMaker mm = basicMarketMaker(
				Keys.NUM_RUNGS, 2,
				Keys.RUNG_SIZE, 10,
				Keys.TRUNCATE_LADDER, false);

		setQuote(Price.of(40), Price.of(50));

		mm.agentStrategy();
		sim.executeImmediate();
		assertTrue(market.getTransactions().isEmpty());
		checkOrderLadder(mm.activeOrders,
				Price.of(30), Price.of(40),
				Price.of(50), Price.of(60));
	}

	/** Check when quote changes in between reentries */
	@Test
	public void quoteChangeTest() {
		BasicMarketMaker marketmaker = basicMarketMaker(
				Keys.NUM_RUNGS, 2,
				Keys.RUNG_SIZE, 10,
				Keys.TRUNCATE_LADDER, false);

		setQuote(Price.of(40), Price.of(50));

		// Initial MM strategy
		marketmaker.agentStrategy();

		// Quote change
		setQuote(Price.of(42), Price.of(48));

		marketmaker.agentStrategy();
		checkOrderLadder(marketmaker.activeOrders,
				Price.of(32), Price.of(42),
				Price.of(48), Price.of(58));
	}

	@Test
	public void withdrawLadderTest() {
		BasicMarketMaker marketmaker = basicMarketMaker();
		
		setQuote(Price.of(40), Price.of(50));

		// Initial MM strategy; submits ladder with numRungs=3
		marketmaker.agentStrategy();
		sim.executeImmediate();
		assertEquals("Incorrect number of orders", 6, marketmaker.activeOrders.size());

		setQuote(Price.of(42), Price.of(49));

		// Verify that it withdraws ladder entirely & submits new ladder
		marketmaker.agentStrategy();
		assertTrue(marketmaker.lastBid.isPresent());
		assertTrue(marketmaker.lastAsk.isPresent());
		
		checkOrderLadder(marketmaker.activeOrders,
				Price.of(32), Price.of(37), Price.of(42),
				Price.of(49), Price.of(54), Price.of(59));
	}

	/**
	 * Case where withdrawing the ladder causes the quote to become undefined
	 * (as well as the last NBBO quote)
	 */
	// FIXME This fails because the market maker is not waiting for the quote to update after withdrawing the orders...
	@Test
	public void withdrawUndefinedTest() {
		BasicMarketMaker marketmaker = basicMarketMaker();
		
		setQuote(Price.of(40), Price.of(50));

		// Submits ladder with numRungs=3
		marketmaker.agentStrategy();
		sim.executeImmediate();
		assertEquals("Incorrect number of orders", 6, marketmaker.activeOrders.size());

		for (OrderRecord order : ImmutableList.copyOf(mockAgent.activeOrders))
			if (order.getOrderType() == BUY)
				mockAgent.withdrawOrder(order);
		sim.executeImmediate();
		
		marketmaker.lastBid = Optional.of(Price.of(42)); // FIXME necessary? to make sure MM will withdraw its orders

		// Verify that it withdraws ladder entirely
		// Note that now the quote is undefined, after it withdraws its ladder
		// so it will submit a ladder with the lastBid
		marketmaker.agentStrategy();

		assertTrue(marketmaker.lastBid.isPresent());
		assertTrue(marketmaker.lastAsk.isPresent());
		checkOrderLadder(marketmaker.activeOrders,
				Price.of(32), Price.of(37), Price.of(42),
				Price.of(50), Price.of(55), Price.of(60));
	}

	@Test
	public void nullBidAskLadder() {
		BasicMarketMaker marketmaker = basicMarketMaker(
				Keys.TICK_IMPROVEMENT, true,
				Keys.TICK_OUTSIDE, true,
				Keys.INITIAL_LADDER_MEAN, 50,
				Keys.INITIAL_LADDER_RANGE, 10);
		
		setQuote(Price.of(40), Price.of(50));

		marketmaker.agentStrategy();

		assertEquals("Incorrect number of orders", 6, marketmaker.activeOrders.size());
		
		// Quote change
		// Withdraw other orders
		// Note that now the quote is undefined, after it withdraws its ladder
		mockAgent.withdrawAllOrders();
		sim.executeImmediate();
		assertTrue(marketmaker.lastAsk.isPresent());
		assertTrue(marketmaker.lastBid.isPresent());
		
		marketmaker.agentStrategy();
		checkRandomOrderLadder(marketmaker.activeOrders, 6, Range.closed(Price.of(40), Price.of(60)), 5);
	}
	
	@Test
	public void oneBackgroundBuyer() {
		BasicMarketMaker marketmaker = basicMarketMaker(
				Keys.TICK_IMPROVEMENT, true,
				Keys.TICK_OUTSIDE, false,
				Keys.INITIAL_LADDER_MEAN, 50,
				Keys.INITIAL_LADDER_RANGE, 10);
		
		submitOrder(mockAgent, BUY, Price.of(40));

		marketmaker.agentStrategy();
		sim.executeImmediate();
		checkRandomOrderLadder(marketmaker.activeOrders, 6,
				Range.singleton(Price.of(41)), Range.singleton(Price.of(50)), 5);
		assertTrue(market.getTransactions().isEmpty());
		
		// Verify that single background trader will transact with the MM
		submitOrder(mockAgent, BUY, Price.of(80));
		checkSingleTransaction(market.getTransactions(), Price.of(50), TimeStamp.ZERO, 1);
	}
	
	@Test
	public void oneBackgroundSeller() {
		BasicMarketMaker marketmaker = basicMarketMaker(
				Keys.TICK_IMPROVEMENT, true,
				Keys.TICK_OUTSIDE, false,
				Keys.INITIAL_LADDER_MEAN, 50,
				Keys.INITIAL_LADDER_RANGE, 10);
		
		submitOrder(mockAgent, SELL, Price.of(60));

		marketmaker.agentStrategy();
		sim.executeImmediate();
		checkRandomOrderLadder(marketmaker.activeOrders, 6,
				Range.singleton(Price.of(50)), Range.singleton(Price.of(59)), 5);
		assertTrue(market.getTransactions().isEmpty());

		// Verify that single background trader will transact with the MM
		submitOrder(mockAgent, SELL, Price.of(30));
		checkSingleTransaction(market.getTransactions(), Price.of(50), TimeStamp.ZERO, 1);
	}

	@Test
	public void extraTest() throws IOException {
		for (int i = 0; i < 100; i++) {
			setup();
			nullBidAskLadder();
			setup();
			oneBackgroundBuyer();
			setup();
			oneBackgroundSeller();
		}
	}

	private void setQuote(Price bid, Price ask) {
		submitOrder(mockAgent, BUY, bid);
		submitOrder(mockAgent, SELL, ask);
	}

	private OrderRecord submitOrder(Agent agent, OrderType buyOrSell, Price price) {
		OrderRecord order = agent.submitOrder(market, buyOrSell, price, 1);
		sim.executeImmediate();
		return order;
	}
	
	private BasicMarketMaker basicMarketMaker(Object... parameters) {
		return BasicMarketMaker.create(sim, actualMarket, rand, Props.withDefaults(defaults, parameters));
	}
	
	private Agent mockAgent() {
		return new Agent(sim, TimeStamp.ZERO, rand, Props.fromPairs()) {
			private static final long serialVersionUID = 1L;
			@Override public void agentStrategy() { }
			@Override public String toString() { return "TestAgent " + id; }
		};
	}
	
}
