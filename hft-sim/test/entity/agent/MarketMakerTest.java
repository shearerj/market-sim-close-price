package entity.agent;

import static fourheap.Order.OrderType.BUY;
import static fourheap.Order.OrderType.SELL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static utils.Tests.checkOrderLadder;
import static utils.Tests.checkRandomOrderLadder;
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

import com.google.common.base.Optional;
import com.google.common.collect.Range;

import data.Props;
import data.Stats;
import entity.market.Market;
import entity.market.Market.MarketView;
import entity.market.Price;
import entity.market.Quote;
import event.TimeStamp;
import fourheap.Order.OrderType;

public class MarketMakerTest {
	private static final double eps = 1e-6;
	private static final Random rand = new Random();
	private static final Props defaults = Props.fromPairs(
			Keys.NUM_RUNGS, 2,
			Keys.RUNG_SIZE, 5,
			Keys.TRUNCATE_LADDER, false,
			Keys.REENTRY_RATE, 0);
	
	private MockSim sim;
	private Market actualMarket;
	private MarketView market, other;
	private Agent mockAgent;

	@Before
	public void setup() throws IOException {
		sim = MockSim.create(getClass(),
				Log.Level.NO_LOGGING, Keys.FUNDAMENTAL_MEAN,
				100000, Keys.FUNDAMENTAL_SHOCK_VAR,
				0, MarketType.CDA, j.join(Keys.NUM_MARKETS, 2));
		
		Iterator<Market> markets = sim.getMarkets().iterator();
		actualMarket = markets.next();
		market = actualMarket.getPrimaryView();
		other = markets.next().getPrimaryView();
		assertFalse(markets.hasNext());
		
		mockAgent = mockAgent();
	}

	/** Testing when no bid/ask, does not submit any orders */
	@Test
	public void nullBidAsk() {
		MarketMaker mm = marketMaker();

		mm.agentStrategy();
		assertTrue(mm.activeOrders.isEmpty());
	}

	/** If either ladder bid or ask is null, it needs to return */
	@Test
	public void createOrderLadderNullTest() {
		MarketMaker mm = marketMaker(
				Keys.INITIAL_LADDER_MEAN, 0,
				Keys.INITIAL_LADDER_RANGE, 0);
		mm.createOrderLadder(Optional.<Price> absent(), Optional.of(Price.of(50)));
		assertTrue(mm.activeOrders.isEmpty());
	}

	@Test
	public void submitOrderLadderTest() {
		MarketMaker mm = marketMaker(Keys.NUM_RUNGS, 3);

		mm.submitOrderLadder(Price.of(30), Price.of(40), Price.of(50), Price.of(60));
		checkOrderLadder(mm.activeOrders,
				Price.of(30), Price.of(35), Price.of(40),
				Price.of(50), Price.of(55), Price.of(60));
	}

	@Test
	public void createOrderLadderTest() {
		MarketMaker mm = marketMaker();

		mm.createOrderLadder(Optional.of(Price.of(40)), Optional.of(Price.of(50)));
		checkOrderLadder(mm.activeOrders,
				Price.of(35), Price.of(40),
				Price.of(50), Price.of(55));
	}


	@Test
	public void tickImprovement() {
		MarketMaker mm = marketMaker(
				Keys.TICK_IMPROVEMENT, true,
				Keys.TICK_OUTSIDE, true);
		assertEquals(5, mm.stepSize);

		setQuote(market, Price.of(40), Price.of(50));

		mm.createOrderLadder(Optional.of(Price.of(40)), Optional.of(Price.of(50)));
		checkOrderLadder(mm.activeOrders,
				Price.of(39), Price.of(34),
				Price.of(51), Price.of(56));
	}

	@Test
	public void truncateLadderTickImprovement() throws IOException {
		MarketMaker mm = marketMaker(
				Keys.TRUNCATE_LADDER, true,
				Keys.TICK_IMPROVEMENT, true,
				Keys.TICK_OUTSIDE, true);

		// Updating NBBO quote (place orders and advance time)
		setQuote(other, Price.of(30), Price.of(38));
		setQuote(market, Price.of(40), Price.of(50));

		mm.createOrderLadder(Optional.of(Price.of(40)), Optional.of(Price.of(50)));
		mm.createOrderLadder(Optional.of(Price.of(40)), Optional.of(Price.of(50)));
		checkOrderLadder(mm.activeOrders, 1,
				Price.of(34),
				Price.of(51), Price.of(56));
	}
	
	@Test
	public void tickOutside() {
		MarketMaker mm = marketMaker(
				Keys.TICK_IMPROVEMENT, true,
				Keys.TICK_OUTSIDE, false);

		setQuote(market, Price.of(40), Price.of(50));
		
		mm.createOrderLadder(Optional.of(Price.of(40)), Optional.of(Price.of(50)));
		checkOrderLadder(mm.activeOrders,
				Price.of(36), Price.of(41),
				Price.of(49), Price.of(54));
	}
	
	@Test
	public void truncateLadderTickImprovementOutside() throws IOException {
		MarketMaker mm = marketMaker(
				Keys.TRUNCATE_LADDER, true,
				Keys.TICK_IMPROVEMENT, true,
				Keys.TICK_OUTSIDE, false);
		
		setQuote(other, Price.of(30), Price.of(38)); // Set NBBO
		setQuote(market, Price.of(40), Price.of(50));

		mm.createOrderLadder(Optional.of(Price.of(40)), Optional.of(Price.of(50)));
		checkOrderLadder(mm.activeOrders, 1,
				Price.of(36),
				Price.of(49), Price.of(54));
	}
	
	@Test
	public void truncateBidTest() {
		MarketMaker marketmaker = marketMaker(
				Keys.NUM_RUNGS, 3,
				Keys.TRUNCATE_LADDER, true,
				Keys.TICK_IMPROVEMENT, false);

		setQuote(other, Price.of(90), Price.of(100));
		setQuote(market, Price.of(102), Price.of(105));

		marketmaker.createOrderLadder(Optional.of(Price.of(102)), Optional.of(Price.of(105)));
		checkOrderLadder(marketmaker.activeOrders, 2,
				Price.of(92), Price.of(97),
				Price.of(105), Price.of(110), Price.of(115));
	}

	@Test
	public void truncateAskTest() {
		MarketMaker marketmaker = marketMaker(
				Keys.NUM_RUNGS, 3,
				Keys.TRUNCATE_LADDER, true,
				Keys.TICK_IMPROVEMENT, false);
		
		setQuote(other, Price.of(90), Price.of(100));
		setQuote(market, Price.of(70), Price.of(89));

		marketmaker.createOrderLadder(Optional.of(Price.of(70)), Optional.of(Price.of(89)));
		checkOrderLadder(marketmaker.activeOrders, 3,
				Price.of(60), Price.of(65), Price.of(70),
				Price.of(94), Price.of(99));
	}
	
	/** Verify quantization happening */
	@Test
	public void tickSizeTest() {
		MarketMaker marketmaker = marketMaker(
				Keys.NUM_RUNGS, 3,
				Keys.RUNG_SIZE, 12,
				Keys.TICK_SIZE, 5);
	
		marketmaker.createOrderLadder(Optional.of(Price.of(40)), Optional.of(Price.of(50)));
		checkOrderLadder(marketmaker.activeOrders,
				Price.of(20), Price.of(30), Price.of(40),
				Price.of(50), Price.of(60), Price.of(70));
	}

	/** Creating ladder without bid/ask quote */
	@Test
	public void initRandLadder() {
		MarketMaker mm = marketMaker(
				Keys.TRUNCATE_LADDER, true,
				Keys.TICK_IMPROVEMENT, true,
				Keys.TICK_OUTSIDE, false,
				Keys.INITIAL_LADDER_MEAN, 100,
				Keys.INITIAL_LADDER_RANGE, 10);
		
		Quote quote = market.getQuote();
		mm.createOrderLadder(quote.getBidPrice(), quote.getAskPrice());
		checkRandomOrderLadder(mm.activeOrders, 4, Range.closed(Price.of(90), Price.of(100)), 5);
	}

	/** One side of ladder is undefined */
	@Test
	public void oneSidedLadderBuy() {
		MarketMaker mm = marketMaker(
				Keys.TRUNCATE_LADDER, true,
				Keys.TICK_IMPROVEMENT, true,
				Keys.TICK_OUTSIDE, false,
				Keys.INITIAL_LADDER_MEAN, 100,
				Keys.INITIAL_LADDER_RANGE, 10);
		
		submitOrder(market, mockAgent, BUY, Price.of(40));
		
		Quote quote = market.getQuote();
		mm.createOrderLadder(quote.getBidPrice(), quote.getAskPrice());
		checkRandomOrderLadder(mm.activeOrders, 4, Range.singleton(Price.of(41)), Range.closed(Price.of(45), Price.of(50)), 5);
	}
	
	/** One side of ladder is undefined */
	@Test
	public void oneSidedLadderSell() {
		MarketMaker mm = marketMaker(
				Keys.TRUNCATE_LADDER, true,
				Keys.TICK_IMPROVEMENT, true,
				Keys.TICK_OUTSIDE, false,
				Keys.INITIAL_LADDER_MEAN, 100,
				Keys.INITIAL_LADDER_RANGE, 10);
		
		submitOrder(market, mockAgent, SELL, Price.of(50));
		
		Quote quote = market.getQuote();
		mm.createOrderLadder(quote.getBidPrice(), quote.getAskPrice());
		checkRandomOrderLadder(mm.activeOrders, 4, Range.closed(Price.of(40), Price.of(45)), Range.singleton(Price.of(49)), 5);
	}
	
	@Test
	public void marketMakerExecutionTimePostTest() {
		MarketMaker mm = marketMaker();
		
		submitOrder(market, mm, BUY, Price.of(50));
		sim.executeUntil(TimeStamp.of(2));
		submitOrder(market, mockAgent, SELL, Price.of(50)); // 2 execution time
		
		submitOrder(market, mockAgent, BUY, Price.of(100), 2);
		submitOrder(market, mm, SELL, Price.of(100), 2); // 0 execution time
		
		// XXX Not this does not account for quantity unlike background agent execution times
		assertEquals(1, sim.getStats().getSummaryStats().get(Stats.MARKET_MAKER_EXECTUION_TIME + mm).mean(), eps);
	}
	
	@Test
	public void marketMakerSpreadLadderPostTest() {
		MarketMaker mm = marketMaker();
		mm.createOrderLadder(Optional.of(Price.of(50)), Optional.of(Price.of(70)));
		mm.createOrderLadder(Optional.of(Price.of(60)), Optional.of(Price.of(100)));
		
		assertEquals(30, sim.getStats().getSummaryStats().get(Stats.MARKET_MAKER_SPREAD + mm).mean(), eps);
		assertEquals(2, sim.getStats().getSummaryStats().get(Stats.MARKET_MAKER_SPREAD + mm).n());
		
		assertEquals(70, sim.getStats().getSummaryStats().get(Stats.MARKET_MAKER_LADDER + mm).mean(), eps);
		assertEquals(2, sim.getStats().getSummaryStats().get(Stats.MARKET_MAKER_LADDER + mm).n());
	}
	
	@Test
	public void extraTest() throws IOException {
		for (int i = 0; i < 100; i++) {
			setup();
			initRandLadder();
			setup();
			oneSidedLadderBuy();
			setup();
			oneSidedLadderSell();
		}
	}
	
	private void setQuote(MarketView view, Price bid, Price ask) {
		submitOrder(view, mockAgent, BUY, bid);
		submitOrder(view, mockAgent, SELL, ask);
	}
	
	private OrderRecord submitOrder(MarketView view, Agent agent, OrderType buyOrSell, Price price) {
		return submitOrder(view, agent, buyOrSell, price, 1);
	}

	private OrderRecord submitOrder(MarketView view, Agent agent, OrderType buyOrSell, Price price, int quantity) {
		OrderRecord order = agent.submitOrder(view, buyOrSell, price, quantity);
		sim.executeImmediate();
		return order;
	}
	
	private MarketMaker marketMaker(Object... parameters) {
		return new MarketMaker(sim, actualMarket, rand, Props.withDefaults(defaults, parameters)) {
			private static final long serialVersionUID = 1L;
			@Override public String toString() { return "TestMM " + id; }
		};
	}
	
	private Agent mockAgent() {
		return new Agent(sim, TimeStamp.ZERO, rand, Props.fromPairs()) {
			private static final long serialVersionUID = 1L;
			@Override public void agentStrategy() { }
			@Override public String toString() { return "TestAgent " + id; }
		};
	}
}
