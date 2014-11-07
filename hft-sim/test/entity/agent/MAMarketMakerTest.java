package entity.agent;

import static fourheap.Order.OrderType.BUY;
import static fourheap.Order.OrderType.SELL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static utils.Tests.assertOrderLadder;

import java.util.Iterator;
import java.util.List;

import logger.Log;

import org.junit.Before;
import org.junit.Test;

import systemmanager.Keys.InitLadderMean;
import systemmanager.Keys.InitLadderRange;
import systemmanager.Keys.NumHistorical;
import systemmanager.Keys.NumRungs;
import systemmanager.Keys.ReentryRate;
import systemmanager.Keys.RungSize;
import systemmanager.Keys.TickImprovement;
import systemmanager.Keys.TruncateLadder;
import utils.Mock;
import utils.Rand;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

import data.FundamentalValue;
import data.Props;
import entity.market.Market;
import entity.market.Market.MarketView;
import entity.market.Price;
import fourheap.Order.OrderType;

// FIXME MM orders are withdrawn manually due to lack of "proper" instantaneous withdraw orders implementation

public class MAMarketMakerTest {
	private static final Rand rand = Rand.create();
	private static final FundamentalValue fundamental = Mock.fundamental(100000);
	private static final Props defaults = Props.builder()
			.put(ReentryRate.class, 0d)
			.put(TickImprovement.class, false)
			.put(NumHistorical.class, 5)
			.put(NumRungs.class, 1)
			.put(RungSize.class, 10)
			.put(TruncateLadder.class, false)
			.build();
	
	private MAMarketMaker mm;
	private Market market;
	private MarketView view;
	private Agent mockAgent;

	@Before
	public void setup() {
		market = Mock.market();
		view = market.getPrimaryView();
		mockAgent = Mock.agent();
	}
	
	/**
	 * Check that bid and ask queues are updating correctly, and moving average
	 * is computed correctly
	 */
	@Test
	public void computeMovingAverage() {
		mm = maMarketMaker();

		// Add quotes & execute agent strategy in between to add quotes to queue
		List<Price> bids = ImmutableList.of(Price.of(50), Price.of(52), Price.of(54), Price.of(56), Price.of(58));
		List<Price> asks = ImmutableList.of(Price.of(60), Price.of(64), Price.of(68), Price.of(72), Price.of(76));
		
		for (Iterator<Price> buys = bids.iterator(), sells = asks.iterator(); buys.hasNext() && sells.hasNext();) {
			setQuote(buys.next(), sells.next());
			mm.agentStrategy();
		}

		assertEquals(Optional.of(Iterables.getLast(bids)), mm.lastBid);
		assertEquals(Optional.of(Iterables.getLast(asks)), mm.lastAsk);

		// check queues
		// TODO These two very implementation dependent. This test should just test results.
		assertEquals(bids, ImmutableList.copyOf(mm.bidQueue));
		assertEquals(asks, ImmutableList.copyOf(mm.askQueue));

		// check submitted orders
		assertOrderLadder(mm.getActiveOrders(), Price.of(54), Price.of(68));
	}

	/**
	 * Verify that bids/asks are being evicted correctly
	 */
	@Test
	public void evictingQueueTest() {
		mm = maMarketMaker(Props.fromPairs(
				NumRungs.class, 2,
				NumHistorical.class, 3));

		// Add quotes & execute agent strategy in between to add quotes to queue
		List<Price> bids = ImmutableList.of(Price.of(50), Price.of(52), Price.of(53), Price.of(53), Price.of(56));
		List<Price> asks = ImmutableList.of(Price.of(60), Price.of(64), Price.of(69), Price.of(75), Price.of(81));

		int i = 0;
		for (Iterator<Price> buys = bids.iterator(), sells = asks.iterator(); buys.hasNext() && sells.hasNext();) {
			setQuote(buys.next(), sells.next());
			mm.agentStrategy();
			
			// check queues
			// TODO Very implementation dependent
			int back = Math.max(0, i-2);
			assertEquals(bids.get(back), mm.bidQueue.peek());
			assertEquals(asks.get(back), mm.askQueue.peek());
			assertEquals(bids.subList(back, i+1), ImmutableList.copyOf(mm.bidQueue));
			assertEquals(asks.subList(back, i+1), ImmutableList.copyOf(mm.askQueue));
			
			++i;
		}
	}

	/**
	 * When bid/ask queues are not full yet, test the computation of the
	 * moving average
	 */
	@Test
	public void partialQueueLadderTest() {
		mm = maMarketMaker();

		// Add quotes & execute agent strategy in between to add quotes to queue
		List<Price> bids = ImmutableList.of(Price.of(50), Price.of(52), Price.of(54));
		List<Price> asks = ImmutableList.of(Price.of(60), Price.of(64), Price.of(68));

		for (Iterator<Price> buys = bids.iterator(), sells = asks.iterator(); buys.hasNext() && sells.hasNext();) {
			setQuote(buys.next(), sells.next());
			mm.agentStrategy();
		}

		assertEquals(Optional.of(Price.of(54)), mm.lastBid);
		assertEquals(Optional.of(Price.of(68)), mm.lastAsk);

		// check queues TODO Implementation dependent
		assertEquals(Price.of(50), mm.bidQueue.peek());
		assertEquals(Price.of(60), mm.askQueue.peek());
		assertEquals(bids, ImmutableList.copyOf(mm.bidQueue));
		assertEquals(asks, ImmutableList.copyOf(mm.askQueue));

		// check submitted orders
		assertOrderLadder(mm.getActiveOrders(), Price.of(52), Price.of(64));
	}
	
	/**
	 * Verify queue updates when quotes are undefined.
	 */
	@Test
	public void nullBidAskInQueue() {
		mm = maMarketMaker(Props.fromPairs(
				InitLadderMean.class, 50,
				InitLadderRange.class, 7));

		// Add quotes & execute agent strategy in between
		// Verify that if quote undefined, nothing is added to the queues
		mockAgent.submitOrder(view, BUY, Price.of(50), 1);
		mm.agentStrategy();
		assertFalse(mm.lastAsk.isPresent());
		assertEquals(Optional.of(Price.of(50)), mm.lastBid);
		assertTrue(mm.bidQueue.isEmpty());
		assertTrue(mm.askQueue.isEmpty());
		assertEquals("Incorrect number of orders", 2, mm.getActiveOrders().size());
		
		// Remove old quote and set new one
		mm.withdrawAllOrders();
		submitOrder(mockAgent, SELL, Price.of(50));
		submitOrder(mockAgent, SELL, Price.of(60));
		mm.agentStrategy();
		assertEquals(Optional.absent(), mm.lastBid);
		assertEquals(Optional.of(Price.of(60)), mm.lastAsk);
		assertTrue(mm.bidQueue.isEmpty());
		assertTrue(mm.askQueue.isEmpty());
		
		// Adding a new quote
		setQuote(Price.of(52), Price.of(64));
		mm.agentStrategy();
		assertEquals(Optional.of(Price.of(52)), mm.lastBid);
		assertEquals(Optional.of(Price.of(64)), mm.lastAsk);
		setQuote(Price.of(54), Price.of(68));
		mm.agentStrategy();
		
		// check queues
		assertEquals(Price.of(52), mm.bidQueue.peek());
		assertEquals(Price.of(64), mm.askQueue.peek());
		assertEquals(ImmutableList.of(Price.of(52), Price.of(54)), ImmutableList.copyOf(mm.bidQueue));
		assertEquals(ImmutableList.of(Price.of(64), Price.of(68)), ImmutableList.copyOf(mm.askQueue));
		
		// check submitted orders
		assertOrderLadder(mm.getActiveOrders(), Price.of(53), Price.of(66));
	}
	
	private void setQuote(Price bid, Price ask) {
		mockAgent.withdrawAllOrders();
		mm.withdrawAllOrders();
		submitOrder(mockAgent, BUY, bid);
		submitOrder(mockAgent, SELL, ask);
	}

	private OrderRecord submitOrder(Agent agent, OrderType buyOrSell, Price price) {
		return agent.submitOrder(view, buyOrSell, price, 1);
	}
	
	private MAMarketMaker maMarketMaker(Props parameters) {
		Mock.timeline.ignoreNext();
		return MAMarketMaker.create(0, Mock.stats, Mock.timeline, Log.nullLogger(), rand, Mock.sip, fundamental, market,
				Props.merge(defaults, parameters));
	}
	
	private MAMarketMaker maMarketMaker() {
		return maMarketMaker(Props.fromPairs());
	}

}
