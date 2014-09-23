package entity.agent;

import static com.google.common.base.Preconditions.checkArgument;
import static fourheap.Order.OrderType.BUY;
import static fourheap.Order.OrderType.SELL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static utils.Tests.checkOrderLadder;
import static utils.Tests.checkRandomOrderLadder;
import static utils.Tests.j;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import logger.Log;

import org.junit.Before;
import org.junit.Test;

import systemmanager.Consts.MarketType;
import systemmanager.Keys;
import systemmanager.MockSim;

import com.google.common.base.Optional;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.collect.Range;

import data.Props;
import entity.agent.AdaptiveMarketMaker.TransactionResult;
import entity.market.Market;
import entity.market.Market.MarketView;
import entity.market.Price;
import entity.market.Quote;
import event.TimeStamp;
import fourheap.Order.OrderType;

// TODO Weights should be moved out to an expert object and tested separately.

public class AdaptiveMarketMakerTest {
	private static final Random rand = new Random();
	private static final Props defaults = Props.fromPairs(
			Keys.NUM_RUNGS, 2,
			Keys.RUNG_SIZE, 10,
			Keys.REENTRY_RATE, 0,
			Keys.SPREADS, "2-4-6-8",
			Keys.TRUNCATE_LADDER, false,
			Keys.TICK_IMPROVEMENT, false,
			Keys.INITIAL_LADDER_RANGE, 0,
			Keys.INITIAL_LADDER_MEAN, 0);
	
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
		AdaptiveMarketMaker mm = aMarketMaker();
		mm.agentStrategy();
		assertTrue(mm.activeOrders.isEmpty());
	}

	/** Was defined, then the market maker should not do anything. */
	@Test
	public void quoteUndefined() {
		AdaptiveMarketMaker mm = aMarketMaker(
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
		AdaptiveMarketMaker mm = aMarketMaker(
				Keys.NUM_RUNGS, 2,
				Keys.RUNG_SIZE, 10,
				Keys.TRUNCATE_LADDER, false,
				Keys.SPREADS, 10);

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
		AdaptiveMarketMaker marketmaker = aMarketMaker(
				Keys.SPREADS, 2,
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
				Price.of(34), Price.of(44),
				Price.of(46), Price.of(56));
	}
	@Test
	public void withdrawLadderTest() {
		AdaptiveMarketMaker marketmaker = aMarketMaker(
				Keys.NUM_RUNGS, 3,
				Keys.RUNG_SIZE, 5,
				Keys.TRUNCATE_LADDER, true,
				Keys.SPREADS, 2);
		
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
				Price.of(34), Price.of(39), Price.of(44),
				Price.of(46), Price.of(51), Price.of(56));
	}

	/**
	 * Case where withdrawing the ladder causes the quote to become undefined
	 * (as well as the last NBBO quote)
	 */
	// FIXME This fails because the market maker is not waiting for the quote to update after withdrawing the orders...
	@Test
	public void withdrawUndefinedTest() {
		AdaptiveMarketMaker marketmaker = aMarketMaker(
				Keys.NUM_RUNGS, 3,
				Keys.RUNG_SIZE, 5,
				Keys.TRUNCATE_LADDER, true,
				Keys.SPREADS, "2");
		
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
				Price.of(34), Price.of(39), Price.of(44),
				Price.of(46), Price.of(51), Price.of(56));
	}

	@Test
	public void nullBidAskLadder() {
		AdaptiveMarketMaker marketmaker = aMarketMaker(
				Keys.NUM_RUNGS, 3,
				Keys.RUNG_SIZE, 5,
				Keys.TRUNCATE_LADDER, true,
				Keys.SPREADS, "2",
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
	public void chooseMedianWeight() {
		MarketMaker marketmaker = aMarketMaker(
				Keys.NUM_RUNGS, 3,
				Keys.RUNG_SIZE, 5,
				Keys.TRUNCATE_LADDER, true,
				Keys.SPREADS, "2-4-6",
				Keys.USE_MEDIAN_SPREAD, true);

		setQuote(Price.of(40), Price.of(50));

		marketmaker.agentStrategy();

		//Check orders submitted using median spread of 4
		checkOrderLadder(marketmaker.activeOrders,
				Price.of(33), Price.of(38), Price.of(43),
				Price.of(47), Price.of(52), Price.of(57));
	}

	@Test
	public void recalculateWeights(){
		AdaptiveMarketMaker marketmaker = aMarketMaker(
				Keys.NUM_RUNGS, 3,
				Keys.RUNG_SIZE, 5,
				Keys.TRUNCATE_LADDER, true,
				Keys.SPREADS, "2-40-50",
				Keys.NUM_HISTORICAL, 1,
				Keys.MOVING_AVERAGE_PRICE, false,
				Keys.USE_LAST_PRICE, true,
				Keys.FAST_LEARNING, true,
				Keys.USE_MEDIAN_SPREAD, true);

		setQuote(Price.of(80), Price.of(120));

		// Run agent strategy; check that lastPrice updates
		assertFalse(marketmaker.lastPrice.isPresent());
		marketmaker.agentStrategy();
		assertEquals(Optional.of(Price.of(100)), marketmaker.lastPrice);

		// Check weights initially equal FIXME Implementation dependent
		checkEqualWeights(marketmaker.weights);

		//Check orders submitted using median spread of 40
		assertEquals(40, marketmaker.getSpread());
		checkOrderLadder(marketmaker.activeOrders,
				Price.of(70), Price.of(75), Price.of(80),
				Price.of(120), Price.of(125), Price.of(130));

		//Make a trade that yields profit for one of the spreads(2) and is useless for the other two(40,50)
		submitOrder(mockAgent, BUY, Price.of(118));
		assertEquals(0, mockAgent.positionBalance);
		
		Quote quote = market.getQuote();
		assertEquals(Optional.of(Price.of(120)), quote.getAskPrice());
		assertEquals(Optional.of(Price.of(118)), quote.getBidPrice());

		//check that spread = 2 is now weighted slightly higher
		marketmaker.agentStrategy();
		checkHigherWeight(marketmaker.weights, 2);
	}

	@Test
	public void movingAverage() {
		AdaptiveMarketMaker marketmaker = aMarketMaker(
				Keys.NUM_RUNGS, 3,
				Keys.RUNG_SIZE, 5,
				Keys.TRUNCATE_LADDER, true,
				Keys.SPREADS, "2-30-50",
				Keys.NUM_HISTORICAL, 5,
				Keys.MOVING_AVERAGE_PRICE, false,
				Keys.USE_LAST_PRICE, true,
				Keys.FAST_LEARNING, true,
				Keys.USE_MEDIAN_SPREAD, true);

		setQuote(Price.of(80), Price.of(120));
		
		//Run agent strategy;
		marketmaker.agentStrategy();
		assertEquals(Optional.of(Price.of(100)), marketmaker.lastPrice);
		checkEqualWeights(marketmaker.weights);

		//Check orders submitted using median spread of 30
		assertEquals(30, marketmaker.getSpread());
		checkOrderLadder(marketmaker.activeOrders,
				Price.of(75), Price.of(80), Price.of(85),
				Price.of(115), Price.of(120), Price.of(125));

		//Make a trade that yields profit for one of the spreads(2) and is useless for the other two(30,40)
		sim.executeImmediate();
		mockAgent.withdrawAllOrders();
		sim.executeImmediate();
		submitOrder(mockAgent, BUY, Price.of(116), 2);
		
		assertEquals(1, mockAgent.positionBalance);
		Quote quote = market.getQuote();
		// $115 traded with agent1 buying at $116, so ASK now 120
		assertEquals(Optional.of(Price.of(120)), quote.getAskPrice());
		assertEquals(Optional.of(Price.of(116)), quote.getBidPrice());

		// Check that spread = 2 is now weighted slightly higher
		// check that spread = 30 also now has more weight
		marketmaker.agentStrategy();
		assertEquals(2, marketmaker.getSpread());
		checkOrderLadder(marketmaker.activeOrders,
				Price.of(98), Price.of(103), Price.of(108),
				Price.of(110), Price.of(115), Price.of(120));

		checkOrderedWeights(marketmaker.weights, ImmutableList.of(50, 30, 2));

		// check moving average price stored as last price
		assertEquals(Optional.of(Price.of(109)), marketmaker.lastPrice);
		
		double weight2 = marketmaker.weights.get(2);
		double weight30 = marketmaker.weights.get(30);


		// Third test: only spread = 2 should get more weight
		// $116 from agent1 executes with $110 MM
		sim.executeImmediate();
		submitOrder(mockAgent, BUY, Price.of(118), 2);
		
		quote = market.getQuote();
		assertEquals(Optional.of(Price.of(120)), quote.getAskPrice());
		assertEquals(Optional.of(Price.of(118)), quote.getBidPrice()); 

		// check that spread = 2 is now weighted slightly higher than before
		// check that spread = 30 is less weight than prior iteration, but still > weight for 50
		marketmaker.agentStrategy();
		assertEquals(1, marketmaker.weights.get(2) + marketmaker.weights.get(30) + marketmaker.weights.get(50), 0.001);
		assertTrue(marketmaker.weights.get(2) > marketmaker.weights.get(30));
		assertTrue(marketmaker.weights.get(2) > weight2);
		assertTrue(marketmaker.weights.get(2) > marketmaker.weights.get(50));
		assertTrue(marketmaker.weights.get(30) > marketmaker.weights.get(50));
		assertTrue(marketmaker.weights.get(30) < weight30);

		assertEquals(Optional.of(Price.of(112)), marketmaker.lastPrice);
	}

	@Test
	public void lastTransactionResult() {
		AdaptiveMarketMaker marketmaker = aMarketMaker(
				Keys.NUM_RUNGS, 3,
				Keys.RUNG_SIZE, 5,
				Keys.TRUNCATE_LADDER, true,
				Keys.SPREADS, "2-40-50",
				Keys.USE_MEDIAN_SPREAD, true);

		marketmaker.lastAsk = Optional.of(Price.of(100));
		marketmaker.lastBid = Optional.of(Price.of(80));
		marketmaker.lastPrice = Optional.of(Price.of(90));

		TransactionResult test = marketmaker.lastTransactionResult(10, Price.of(81), Price.of(91));
		// rungs generated should be SELL(95,100,105), BUY(85,80,75)
		// nothing should trade in this scenario
		assertEquals(0, (int) test.getCashChange());
		assertEquals(0, (int) test.getHoldingsChange());

		test = marketmaker.lastTransactionResult(2, Price.of(81), Price.of(83));
		// rungs generated should be SELL(91,96,101), BUY(89,84,81)
		// two orders should trade, a buy rung at price 89 & one at 84 
		assertEquals(2, (int) test.getHoldingsChange());
		assertEquals(-(90-1)-(90-6), (int) test.getCashChange());

		test = marketmaker.lastTransactionResult(2, Price.of(97), Price.of(99));
		// rungs generated should be SELL(91,96,101), BUY(89,84,81)
		// two orders should trade, a sell rung at price 91 & one at 96 
		assertEquals(-2, (int) test.getHoldingsChange());
		assertEquals((90+1)+(90+6), (int) test.getCashChange());

		marketmaker.lastPrice = Optional.of(Price.of(83));
		test = marketmaker.lastTransactionResult(2, Price.of(81), Price.of(83));
		// rungs generated should be SELL(91,96,101), BUY(89,84,81)
		// two orders should trade, a buy rung at price 89 & one at 84 
		assertEquals(0, (int) test.getHoldingsChange());
		assertEquals(0, (int) test.getCashChange());

		marketmaker.lastPrice = Optional.of(Price.of(95));
		test = marketmaker.lastTransactionResult(2, Price.of(97), Price.of(99));
		// rungs generated should be SELL(91,96,101), BUY(89,84,81)
		// two orders should trade, a sell rung at price 91 & one at 96 
		assertEquals(-1, (int) test.getHoldingsChange());
		assertEquals((95+1), (int) test.getCashChange());
	}
	
	/** Check that all weights are the same and ad to 1 */
	private static double checkEqualWeights(Map<?, Double> weights) {
		double weight = Iterables.getFirst(weights.values(), null);
		double total = 0;
		for (double w : weights.values()) {
			assertEquals(weight, w, 0.0001);
			total += w;
		}
		assertEquals(1, total, 0.0001);
		return weight;
	}
	
	/** Check that higher is higher than the other weights, and other weights are equal */
	private static <K> void checkHigherWeight(Map<K, Double> weights, K higher) {
		double higherWeight = weights.get(higher);
		double otherWeight = Iterables.getFirst(Iterables.filter(weights.values(), Predicates.not(Predicates.equalTo(higherWeight))), null);
		double total = 0;
		for (Entry<K, Double> e : weights.entrySet()) {
			if (!e.getKey().equals(higher)) {
				assertEquals(otherWeight, e.getValue(), 0.0001);
				assertTrue(higherWeight > e.getValue());
			}
			total += e.getValue();
		}
		assertEquals(1, total, 0.0001);
	}
	
	/** Checks that all of the weights are ordered by the keys */
	private static <K> void checkOrderedWeights(Map<K, Double> weights, List<K> order) {
		checkArgument(weights.size() == order.size(), "Order must be the same size as weights");
		List<Double> orderedWeights = Lists.newArrayListWithCapacity(order.size());
		double total = 0;
		for (K key : order) {
			double w = weights.get(key);
			orderedWeights.add(w);
			total += w;
		}
		assertTrue("Weights were not ordered by ordering", Ordering.natural().isOrdered(orderedWeights));
		assertEquals(1, total, 0.0001);
	}
	

	private AdaptiveMarketMaker aMarketMaker(Object... parameters) {
		return AdaptiveMarketMaker.create(sim, actualMarket, rand, Props.withDefaults(defaults, parameters));
	}

	private void setQuote(Price bid, Price ask) {
		submitOrder(mockAgent, BUY, bid);
		submitOrder(mockAgent, SELL, ask);
	}
	
	private OrderRecord submitOrder(Agent agent, OrderType buyOrSell, Price price) {
		return submitOrder(agent, buyOrSell, price, 1);
	}

	private OrderRecord submitOrder(Agent agent, OrderType buyOrSell, Price price, int quantity) {
		OrderRecord order = agent.submitOrder(market, buyOrSell, price, quantity);
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
