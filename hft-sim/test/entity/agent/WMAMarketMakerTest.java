package entity.agent;

import static fourheap.Order.OrderType.BUY;
import static fourheap.Order.OrderType.SELL;
import static utils.Tests.assertOrderLadder;

import java.util.Iterator;
import java.util.List;

import logger.Log;

import org.junit.Before;
import org.junit.Test;

import systemmanager.Keys.N;
import systemmanager.Keys.K;
import systemmanager.Keys.ReentryRate;
import systemmanager.Keys.Size;
import systemmanager.Keys.TickImprovement;
import systemmanager.Keys.Trunc;
import systemmanager.Keys.W;
import utils.Mock;
import utils.Rand;

import com.google.common.collect.ImmutableList;

import data.FundamentalValue;
import data.Props;
import entity.market.Market;
import entity.market.Market.MarketView;
import entity.market.Price;
import fourheap.Order.OrderType;

//FIXME MM orders are withdrawn manually due to lack of "proper" instantaneous withdraw orders implementation

public class WMAMarketMakerTest {
	private static final Rand rand = Rand.create();
	private static final Agent mockAgent = Mock.agent();
	private static final FundamentalValue fundamental = Mock.fundamental(100000);
	private static final Props defaults = Props.builder()
			.put(ReentryRate.class, 0d)
			.put(TickImprovement.class, false)
			.put(K.class, 1)
			.put(Size.class, 10)
			.put(Trunc.class, false)
			.put(N.class, 5)
			.build();
	
	private Market market;
	private MarketView view;
	private WMAMarketMaker mm;

	@Before
	public void setup() {
		market = Mock.market();
		view = market.getPrimaryView();
	}

	/**
	 * Check that bid and ask queues are updating correctly, and moving average
	 * is computed correctly
	 */
	@Test
	public void computeLinearWeightedMovingAverage() {
		mm = wmaMarketMaker(Props.fromPairs(W.class, 0d));
		
		// Add quotes & execute agent strategy in between
		List<Price> bids = ImmutableList.of(Price.of(50), Price.of(52), Price.of(54), Price.of(56), Price.of(58));
		List<Price> asks = ImmutableList.of(Price.of(60), Price.of(64), Price.of(68), Price.of(72), Price.of(76));
		
		for (Iterator<Price> buys = bids.iterator(), sells = asks.iterator(); buys.hasNext() && sells.hasNext();) {
			setQuote(buys.next(), sells.next());
			mm.agentStrategy();
		}

		assertOrderLadder(mm.getActiveOrders(), Price.of(55), Price.of(71));
	}
	
	@Test
	public void computeExpWeightedMovingAverage() {
		mm = wmaMarketMaker(Props.fromPairs(W.class, 0.5));
		
		// Add quotes & execute agent strategy in between
		List<Price> bids = ImmutableList.of(Price.of(50), Price.of(52), Price.of(54), Price.of(56), Price.of(58));
		List<Price> asks = ImmutableList.of(Price.of(60), Price.of(64), Price.of(68), Price.of(72), Price.of(76));

		for (Iterator<Price> buys = bids.iterator(), sells = asks.iterator(); buys.hasNext() && sells.hasNext();) {
			setQuote(buys.next(), sells.next());
			mm.agentStrategy();
		}
		
		assertOrderLadder(mm.getActiveOrders(), Price.of(56), Price.of(73));
	}

	/**
	 * When bid/ask queues are not full yet, test the computation of the
	 * moving average
	 */
	@Test
	public void partialQueueExponentialLadderTest() {
		mm = wmaMarketMaker(Props.fromPairs(W.class, 0.9));

		// Add quotes & execute agent strategy in between
		List<Price> bids = ImmutableList.of(Price.of(50000), Price.of(52000));
		List<Price> asks = ImmutableList.of(Price.of(60000), Price.of(64000));

		for (Iterator<Price> buys = bids.iterator(), sells = asks.iterator(); buys.hasNext() && sells.hasNext();) {
			setQuote(buys.next(), sells.next());
			mm.agentStrategy();
		}
		
		assertOrderLadder(mm.getActiveOrders(), Price.of(51818), Price.of(63636));
	}
	
	/**
	 * When bid/ask queues are not full yet, test the computation of the
	 * moving average
	 */
	@Test
	public void partialQueueLienarLadderTest() {
		mm = wmaMarketMaker(Props.fromPairs(W.class, 0d));
		// Add quotes & execute agent strategy in between
		List<Price> bids = ImmutableList.of(Price.of(50000), Price.of(52000));
		List<Price> asks = ImmutableList.of(Price.of(60000), Price.of(64000));

		for (Iterator<Price> buys = bids.iterator(), sells = asks.iterator(); buys.hasNext() && sells.hasNext();) {
			setQuote(buys.next(), sells.next());
			mm.agentStrategy();
		}
		
		assertOrderLadder(mm.getActiveOrders(), Price.of(51333), Price.of(62667));
	}

	private WMAMarketMaker wmaMarketMaker(Props parameters) {
		Mock.timeline.ignoreNext();
		return WMAMarketMaker.create(0, Mock.stats, Mock.timeline, Log.nullLogger(), rand, Mock.sip, fundamental, market,
				Props.merge(defaults, parameters));
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
	
	// TODO add test to check adding to EvictingQ when bid/ask is null
	// TODO add test to check that won't compute MA when bid/ask Qs are empty
}
