package entity.agent;

import static fourheap.Order.OrderType.BUY;
import static fourheap.Order.OrderType.SELL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static utils.Tests.assertOrderLadder;
import static utils.Tests.assertRandomOrderLadder;
import static utils.Tests.assertSingleTransaction;

import java.io.IOException;
import java.util.Random;

import logger.Log;

import org.junit.Before;
import org.junit.Test;

import systemmanager.Keys.InitLadderMean;
import systemmanager.Keys.InitLadderRange;
import systemmanager.Keys.MarketMakerReentryRate;
import systemmanager.Keys.NumRungs;
import systemmanager.Keys.RungSize;
import systemmanager.Keys.TickImprovement;
import systemmanager.Keys.TickOutside;
import systemmanager.Keys.TruncateLadder;
import utils.Mock;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Range;

import data.FundamentalValue;
import data.Props;
import entity.market.Market;
import entity.market.Market.MarketView;
import entity.market.Price;
import event.TimeStamp;

public class BasicMarketMakerTest {
	private static final Random rand = new Random();
	private static final FundamentalValue fundamental = Mock.fundamental(100000);
	private static final Props defaults = Props.fromPairs(
			MarketMakerReentryRate.class, 0d,
			NumRungs.class, 3,
			RungSize.class, 5,
			TruncateLadder.class, true,
			TickImprovement.class, false);
	
	private Market market;
	private MarketView view;
	private Agent mockAgent;

	@Before
	public void setup() throws IOException {
		market = Mock.market();
		view = market.getPrimaryView();
		mockAgent = Mock.agent();
	}

	@Test
	public void nullBidAsk() {
		BasicMarketMaker mm = basicMarketMaker(Props.fromPairs(
				TruncateLadder.class, false,
				InitLadderMean.class, 0,
				InitLadderRange.class, 0));
		mm.agentStrategy();
		assertTrue(mm.getActiveOrders().isEmpty());
	}

	/** Was defined, then the market maker should not do anything. */
	@Test
	public void quoteUndefined() {
		BasicMarketMaker mm = basicMarketMaker(Props.fromPairs(
				TruncateLadder.class, false,
				InitLadderMean.class, 0,
				InitLadderRange.class, 0));
		mm.lastAsk = Optional.of(Price.of(55));
		mm.lastBid = Optional.of(Price.of(45));

		// Both sides undefined
		mm.agentStrategy();
		assertTrue(mm.getActiveOrders().isEmpty());

		// One side undefined
		mockAgent.submitOrder(view, BUY, Price.of(40), 1);

		mm.lastAsk = Optional.of(Price.of(55));
		mm.lastBid = Optional.of(Price.of(45));
		mm.agentStrategy();
		assertTrue(mm.getActiveOrders().isEmpty());
	}


	@Test
	public void basicLadderTest() {
		BasicMarketMaker mm = basicMarketMaker(Props.fromPairs(
				NumRungs.class, 2,
				RungSize.class, 10,
				TruncateLadder.class, false));

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
		BasicMarketMaker marketmaker = basicMarketMaker(Props.fromPairs(
				NumRungs.class, 2,
				RungSize.class, 10,
				TruncateLadder.class, false));

		setQuote(Price.of(40), Price.of(50));

		// Initial MM strategy
		marketmaker.agentStrategy();

		// Quote change
		setQuote(Price.of(42), Price.of(48));

		marketmaker.agentStrategy();
		assertOrderLadder(marketmaker.getActiveOrders(),
				Price.of(32), Price.of(42),
				Price.of(48), Price.of(58));
	}

	@Test
	public void withdrawLadderTest() {
		BasicMarketMaker marketmaker = basicMarketMaker();
		
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
		assertEquals("Incorrect number of orders", 6, marketmaker.getActiveOrders().size());

		for (OrderRecord order : ImmutableList.copyOf(mockAgent.getActiveOrders()))
			if (order.getOrderType() == BUY)
				mockAgent.withdrawOrder(order);
		
		marketmaker.lastBid = Optional.of(Price.of(42)); // FIXME necessary? to make sure MM will withdraw its orders

		// Verify that it withdraws ladder entirely
		// Note that now the quote is undefined, after it withdraws its ladder
		// so it will submit a ladder with the lastBid
		marketmaker.agentStrategy();

		assertTrue(marketmaker.lastBid.isPresent());
		assertTrue(marketmaker.lastAsk.isPresent());
		assertOrderLadder(marketmaker.getActiveOrders(),
				Price.of(32), Price.of(37), Price.of(42),
				Price.of(50), Price.of(55), Price.of(60));
	}

	@Test
	public void nullBidAskLadder() {
		BasicMarketMaker marketmaker = basicMarketMaker(Props.fromPairs(
				TickImprovement.class, true,
				TickOutside.class, true,
				InitLadderMean.class, 50,
				InitLadderRange.class, 10));
		
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
	public void oneBackgroundBuyer() {
		BasicMarketMaker marketmaker = basicMarketMaker(Props.fromPairs(
				TickImprovement.class, true,
				TickOutside.class, false,
				InitLadderMean.class, 50,
				InitLadderRange.class, 10));
		
		mockAgent.submitOrder(view, BUY, Price.of(40), 1);

		marketmaker.agentStrategy();
		assertRandomOrderLadder(marketmaker.getActiveOrders(), 6,
				Range.singleton(Price.of(41)), Range.singleton(Price.of(50)), 5);
		assertTrue(view.getTransactions().isEmpty());
		
		// Verify that single background trader will transact with the MM
		mockAgent.submitOrder(view, BUY, Price.of(80), 1);
		assertSingleTransaction(view.getTransactions(), Price.of(50), TimeStamp.ZERO, 1);
	}
	
	@Test
	public void oneBackgroundSeller() {
		BasicMarketMaker marketmaker = basicMarketMaker(Props.fromPairs(
				TickImprovement.class, true,
				TickOutside.class, false,
				InitLadderMean.class, 50,
				InitLadderRange.class, 10));
		
		mockAgent.submitOrder(view, SELL, Price.of(60), 1);

		marketmaker.agentStrategy();
		assertRandomOrderLadder(marketmaker.getActiveOrders(), 6,
				Range.singleton(Price.of(50)), Range.singleton(Price.of(59)), 5);
		assertTrue(view.getTransactions().isEmpty());

		// Verify that single background trader will transact with the MM
		mockAgent.submitOrder(view, SELL, Price.of(30), 1);
		assertSingleTransaction(view.getTransactions(), Price.of(50), TimeStamp.ZERO, 1);
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
		mockAgent.submitOrder(view, BUY, bid, 1);
		mockAgent.submitOrder(view, SELL, ask, 1);
	}
	
	private BasicMarketMaker basicMarketMaker(Props parameters) {
		Mock.timeline.ignoreNext();
		return BasicMarketMaker.create(0, Mock.stats, Mock.timeline, Log.nullLogger(), rand, Mock.sip, fundamental, market,
				Props.merge(defaults, parameters));
	}
	
	private BasicMarketMaker basicMarketMaker() {
		return basicMarketMaker(Props.fromPairs());
	}
	
}
