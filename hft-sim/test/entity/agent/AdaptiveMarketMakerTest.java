package entity.agent;

import static com.google.common.base.Preconditions.checkArgument;
import static fourheap.Order.OrderType.BUY;
import static fourheap.Order.OrderType.SELL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static utils.Tests.assertOrderLadder;
import static utils.Tests.assertRandomOrderLadder;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import logger.Log;

import org.junit.Before;
import org.junit.Test;

import systemmanager.Keys.FastLearning;
import systemmanager.Keys.InitLadderMean;
import systemmanager.Keys.InitLadderRange;
import systemmanager.Keys.MarketMakerReentryRate;
import systemmanager.Keys.MovingAveragePrice;
import systemmanager.Keys.N;
import systemmanager.Keys.K;
import systemmanager.Keys.Size;
import systemmanager.Keys.Strats;
import systemmanager.Keys.TickImprovement;
import systemmanager.Keys.TickOutside;
import systemmanager.Keys.Trunc;
import systemmanager.Keys.UseLastPrice;
import systemmanager.Keys.UseMedianSpread;
import utils.Mock;
import utils.Rand;

import com.google.common.base.Optional;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.collect.Range;
import com.google.common.primitives.Ints;

import data.FundamentalValue;
import data.Props;
import entity.agent.AdaptiveMarketMaker.TransactionResult;
import entity.market.Market;
import entity.market.Market.MarketView;
import entity.market.Price;
import entity.market.Quote;
import fourheap.Order.OrderType;

// TODO Weights should be moved out to an expert object and tested separately.

public class AdaptiveMarketMakerTest {
	private static final Rand rand = Rand.create();
	private static final Agent mockAgent = Mock.agent();
	private static final FundamentalValue fundamental = Mock.fundamental(100000);
	private static final Props defaults = Props.builder()
			.put(K.class, 2)
			.put(Size.class, 10)
			.put(MarketMakerReentryRate.class, 0d)
			.put(Strats.class, Ints.asList(2, 4, 6, 8))
			.put(Trunc.class, false)
			.put(TickImprovement.class, false)
			.put(InitLadderRange.class, 0)
			.put(InitLadderMean.class, 0)
			.build();
	
	private Market market;
	private MarketView view;

	@Before
	public void setup() {
		market = Mock.market();
		view = market.getPrimaryView();
	}

	@Test
	public void nullBidAsk() {
		AdaptiveMarketMaker mm = aMarketMaker();
		mm.agentStrategy();
		assertTrue(mm.getActiveOrders().isEmpty());
	}

	/** Was defined, then the market maker should not do anything. */
	@Test
	public void quoteUndefined() {
		AdaptiveMarketMaker mm = aMarketMaker(Props.fromPairs(
				Trunc.class, false,
				InitLadderMean.class, 0,
				InitLadderRange.class, 0));
		mm.lastAsk = Optional.of(Price.of(55));
		mm.lastBid = Optional.of(Price.of(45));

		// Both sides undefined
		mm.agentStrategy();
		assertTrue(mm.getActiveOrders().isEmpty());

		// One side undefined
		submitOrder(mockAgent, BUY, Price.of(40));

		mm.lastAsk = Optional.of(Price.of(55));
		mm.lastBid = Optional.of(Price.of(45));
		mm.agentStrategy();
		assertTrue(mm.getActiveOrders().isEmpty());
	}

	@Test
	public void basicLadderTest() {
		AdaptiveMarketMaker mm = aMarketMaker(Props.fromPairs(
				K.class, 2,
				Size.class, 10,
				Trunc.class, false,
				Strats.class, Ints.asList(10)));

		setQuote(Price.of(40), Price.of(50));

		mm.agentStrategy();
		assertTrue(view.getTransactions().isEmpty());
		assertOrderLadder(mm.getActiveOrders(),
				Price.of(30), Price.of(40),
				Price.of(50), Price.of(60));
	}

	/** Check when quote changes in between reentries */
	@Test
	public void quoteChangeTest() {
		AdaptiveMarketMaker marketmaker = aMarketMaker(Props.fromPairs(
				Strats.class, Ints.asList(2),
				K.class, 2,
				Size.class, 10,
				Trunc.class, false));

		setQuote(Price.of(40), Price.of(50));

		// Initial MM strategy
		marketmaker.agentStrategy();

		// Quote change
		setQuote(Price.of(42), Price.of(48));

		marketmaker.agentStrategy();
		assertOrderLadder(marketmaker.getActiveOrders(),
				Price.of(34), Price.of(44),
				Price.of(46), Price.of(56));
	}
	@Test
	public void withdrawLadderTest() {
		AdaptiveMarketMaker marketmaker = aMarketMaker(Props.fromPairs(
				K.class, 3,
				Size.class, 5,
				Trunc.class, true,
				Strats.class, Ints.asList(2)));
		
		setQuote(Price.of(40), Price.of(50));
		
		// Initial MM strategy; submits ladder with numRungs=3
		marketmaker.agentStrategy();
		assertEquals("Incorrect number of orders", 6, marketmaker.getActiveOrders().size());

		setQuote(Price.of(42), Price.of(49));

		// Verify that it withdraws ladder entirely & submits new ladder
		marketmaker.agentStrategy();
		assertTrue(marketmaker.lastBid.isPresent());
		assertTrue(marketmaker.lastAsk.isPresent());
		
		assertOrderLadder(marketmaker.getActiveOrders(),
				Price.of(34), Price.of(39), Price.of(44),
				Price.of(46), Price.of(51), Price.of(56));
	}

	/**
	 * Case where withdrawing the ladder causes the quote to become undefined
	 * (as well as the last NBBO quote)
	 */
	@Test
	public void withdrawUndefinedTest() {
		AdaptiveMarketMaker marketmaker = aMarketMaker(Props.fromPairs(
				K.class, 3,
				Size.class, 5,
				Trunc.class, true,
				Strats.class, Ints.asList(2)));
		
		setQuote(Price.of(40), Price.of(50));

		// Submits ladder with numRungs=3
		marketmaker.agentStrategy();
		assertEquals("Incorrect number of orders", 6, marketmaker.getActiveOrders().size());

		for (OrderRecord order : ImmutableList.copyOf(mockAgent.getActiveOrders()))
			if (order.getOrderType() == BUY)
				mockAgent.withdrawOrder(order);
		
		marketmaker.lastBid = Optional.of(Price.of(42));

		// Verify that it withdraws ladder entirely
		// Note that now the quote is undefined, after it withdraws its ladder
		// so it will submit a ladder with the lastBid
		marketmaker.agentStrategy();

		assertTrue(marketmaker.lastBid.isPresent());
		assertTrue(marketmaker.lastAsk.isPresent());
		assertOrderLadder(marketmaker.getActiveOrders(),
				Price.of(34), Price.of(39), Price.of(44),
				Price.of(46), Price.of(51), Price.of(56));
	}

	@Test
	public void nullBidAskLadder() {
		AdaptiveMarketMaker marketmaker = aMarketMaker(Props.builder()
				.put(K.class, 3)
				.put(Size.class, 5)
				.put(Trunc.class, true)
				.put(Strats.class, Ints.asList(2))
				.put(TickImprovement.class, true)
				.put(TickOutside.class, true)
				.put(InitLadderMean.class, 50)
				.put(InitLadderRange.class, 10)
				.build());
		
		setQuote(Price.of(40), Price.of(50));

		marketmaker.agentStrategy();

		assertEquals("Incorrect number of orders", 6, marketmaker.getActiveOrders().size());
		
		// Quote change
		// Withdraw other orders
		// Note that now the quote is undefined, after it withdraws its ladder
		mockAgent.withdrawAllOrders();
		assertTrue(marketmaker.lastAsk.isPresent());
		assertTrue(marketmaker.lastBid.isPresent());
		
		marketmaker.agentStrategy();
		assertRandomOrderLadder(marketmaker.getActiveOrders(), 6, Range.closed(Price.of(40), Price.of(60)), 5);
	}
	
	@Test
	public void chooseMedianWeight() {
		MarketMaker marketmaker = aMarketMaker(Props.fromPairs(
				K.class, 3,
				Size.class, 5,
				Trunc.class, true,
				Strats.class, Ints.asList(2, 4, 5),
				UseMedianSpread.class, true));

		setQuote(Price.of(40), Price.of(50));

		marketmaker.agentStrategy();

		//Check orders submitted using median spread of 4
		assertOrderLadder(marketmaker.getActiveOrders(),
				Price.of(33), Price.of(38), Price.of(43),
				Price.of(47), Price.of(52), Price.of(57));
	}

	@Test
	public void recalculateWeights(){
		AdaptiveMarketMaker marketmaker = aMarketMaker(Props.builder()
				.put(K.class, 3)
				.put(Size.class, 5)
				.put(Trunc.class, true)
				.put(Strats.class, Ints.asList(2, 40, 50))
				.put(N.class, 1)
				.put(MovingAveragePrice.class, false)
				.put(UseLastPrice.class, true)
				.put(FastLearning.class, true)
				.put(UseMedianSpread.class, true)
				.build());

		setQuote(Price.of(80), Price.of(120));

		// Run agent strategy; check that lastPrice updates
		assertFalse(marketmaker.lastPrice.isPresent());
		marketmaker.agentStrategy();
		assertEquals(Optional.of(Price.of(100)), marketmaker.lastPrice);

		// Check weights initially equal TODO Implementation dependent
		checkEqualWeights(marketmaker.weights);

		//Check orders submitted using median spread of 40
		assertEquals(40, marketmaker.getSpread());
		assertOrderLadder(marketmaker.getActiveOrders(),
				Price.of(70), Price.of(75), Price.of(80),
				Price.of(120), Price.of(125), Price.of(130));

		//Make a trade that yields profit for one of the spreads(2) and is useless for the other two(40,50)
		submitOrder(mockAgent, BUY, Price.of(118));
		assertEquals(0, mockAgent.getPosition());
		
		Quote quote = view.getQuote();
		assertEquals(Optional.of(Price.of(120)), quote.getAskPrice());
		assertEquals(Optional.of(Price.of(118)), quote.getBidPrice());

		//check that spread = 2 is now weighted slightly higher
		marketmaker.agentStrategy();
		checkHigherWeight(marketmaker.weights, 2);
	}

	@Test
	public void movingAverage() {
		AdaptiveMarketMaker marketmaker = aMarketMaker(Props.builder()
				.put(K.class, 3)
				.put(Size.class, 5)
				.put(Trunc.class, true)
				.put(Strats.class, Ints.asList(2, 30, 50))
				.put(N.class, 5)
				.put(MovingAveragePrice.class, false)
				.put(UseLastPrice.class, true)
				.put(FastLearning.class, true)
				.put(UseMedianSpread.class, true)
				.build());

		setQuote(Price.of(80), Price.of(120));

		//Run agent strategy;
		marketmaker.agentStrategy();
		assertEquals(Optional.of(Price.of(100)), marketmaker.lastPrice);
		checkEqualWeights(marketmaker.weights);

		//Check orders submitted using median spread of 30
		assertEquals(30, marketmaker.getSpread());
		assertOrderLadder(marketmaker.getActiveOrders(),
				Price.of(75), Price.of(80), Price.of(85),
				Price.of(115), Price.of(120), Price.of(125));

		//Make a trade that yields profit for one of the spreads(2) and is useless for the other two(30,40)
		mockAgent.withdrawAllOrders();
		submitOrder(mockAgent, BUY, Price.of(116), 2);
		
		assertEquals(1, mockAgent.getPosition());
		Quote quote = view.getQuote();
		// $115 traded with agent1 buying at $116, so ASK now 120
		assertEquals(Optional.of(Price.of(120)), quote.getAskPrice());
		assertEquals(Optional.of(Price.of(116)), quote.getBidPrice());

		// Check that spread = 2 is now weighted slightly higher
		// check that spread = 30 also now has more weight
		marketmaker.agentStrategy();
		assertEquals(2, marketmaker.getSpread());
		assertOrderLadder(marketmaker.getActiveOrders(),
				Price.of(98), Price.of(103), Price.of(108),
				Price.of(110), Price.of(115), Price.of(120));

		checkOrderedWeights(marketmaker.weights, Ints.asList(50, 30, 2));

		// check moving average price stored as last price
		assertEquals(Optional.of(Price.of(109)), marketmaker.lastPrice);
		
		double weight2 = marketmaker.weights.get(2);
		double weight30 = marketmaker.weights.get(30);


		// Third test: only spread = 2 should get more weight
		// $116 from agent1 executes with $110 MM
		submitOrder(mockAgent, BUY, Price.of(118), 2);
		
		quote = view.getQuote();
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
		AdaptiveMarketMaker marketmaker = aMarketMaker(Props.fromPairs(
				K.class, 3,
				Size.class, 5,
				Trunc.class, true,
				Strats.class, Ints.asList(2, 40, 50),
				UseMedianSpread.class, true));

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
	private static <T> void checkHigherWeight(Map<T, Double> weights, T higher) {
		double higherWeight = weights.get(higher);
		double otherWeight = Iterables.getFirst(Iterables.filter(weights.values(), Predicates.not(Predicates.equalTo(higherWeight))), null);
		double total = 0;
		for (Entry<T, Double> e : weights.entrySet()) {
			if (!e.getKey().equals(higher)) {
				assertEquals(otherWeight, e.getValue(), 0.0001);
				assertTrue(higherWeight > e.getValue());
			}
			total += e.getValue();
		}
		assertEquals(1, total, 0.0001);
	}
	
	/** Checks that all of the weights are ordered by the keys */
	private static <T> void checkOrderedWeights(Map<T, Double> weights, List<T> order) {
		checkArgument(weights.size() == order.size(), "Order must be the same size as weights");
		List<Double> orderedWeights = Lists.newArrayListWithCapacity(order.size());
		double total = 0;
		for (T key : order) {
			double w = weights.get(key);
			orderedWeights.add(w);
			total += w;
		}
		assertTrue("Weights were not ordered by ordering", Ordering.natural().isOrdered(orderedWeights));
		assertEquals(1, total, 0.0001);
	}
	
	private AdaptiveMarketMaker aMarketMaker() {
		return aMarketMaker(Props.fromPairs());
	}

	private AdaptiveMarketMaker aMarketMaker(Props parameters) {
		Mock.timeline.ignoreNext();
		return AdaptiveMarketMaker.create(0, Mock.stats, Mock.timeline, Log.nullLogger(), rand, Mock.sip, fundamental, market,
				Props.merge(defaults, parameters));
	}

	private void setQuote(Price bid, Price ask) {
		submitOrder(mockAgent, BUY, bid);
		submitOrder(mockAgent, SELL, ask);
	}
	
	private OrderRecord submitOrder(Agent agent, OrderType buyOrSell, Price price) {
		return submitOrder(agent, buyOrSell, price, 1);
	}

	private OrderRecord submitOrder(Agent agent, OrderType buyOrSell, Price price, int quantity) {
		return agent.submitOrder(view, buyOrSell, price, quantity);
	}

}
