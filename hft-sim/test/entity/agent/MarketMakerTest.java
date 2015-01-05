package entity.agent;

import static fourheap.Order.OrderType.BUY;
import static fourheap.Order.OrderType.SELL;
import static org.junit.Assert.assertEquals;
import static utils.Tests.assertOrderLadder;
import static utils.Tests.assertQuote;
import static utils.Tests.assertRandomOrderLadder;
import logger.Log;

import org.junit.Before;
import org.junit.Test;

import systemmanager.Keys.InitLadderMean;
import systemmanager.Keys.InitLadderRange;
import systemmanager.Keys.MarketMakerReentryRate;
import systemmanager.Keys.K;
import systemmanager.Keys.Size;
import systemmanager.Keys.TickImprovement;
import systemmanager.Keys.TickOutside;
import systemmanager.Keys.TickSize;
import systemmanager.Keys.Trunc;
import utils.Mock;
import utils.Mock.MockTimeLine;
import utils.Rand;

import com.google.common.base.Optional;
import com.google.common.collect.Range;

import data.FundamentalValue;
import data.Props;
import data.Stats;
import entity.market.CDAMarket;
import entity.market.Market;
import entity.market.Market.MarketView;
import entity.market.Price;
import entity.market.Quote;
import entity.sip.MarketInfo;
import entity.sip.SIP;
import event.EventQueue;
import event.TimeStamp;
import event.Timeline;

public class MarketMakerTest {
	private static final double eps = 1e-6;
	private static final Rand rand = Rand.create();
	private static final Agent mockAgent = Mock.agent();
	private static final FundamentalValue fundamental = Mock.fundamental(100000);
	private static final Props defaults = Props.fromPairs(
			K.class,			2,
			Size.class,			5,
			Trunc.class,	false,
			MarketMakerReentryRate.class, 0d);
	
	private MarketInfo sip;
	private Market market;
	private MarketView view, other;
	
	@Before
	public void setup() {
		sip = SIP.create(Mock.stats, Mock.timeline, Log.nullLogger(), rand, TimeStamp.ZERO);
		market = CDAMarket.create(0, Mock.stats, Mock.timeline, Log.nullLogger(), rand, sip, Props.fromPairs());
		view = market.getPrimaryView();
		other = CDAMarket.create(1, Mock.stats, Mock.timeline, Log.nullLogger(), rand, sip, Props.fromPairs()).getPrimaryView();
	}

	/** Testing when no bid/ask, does not submit any orders */
	// FIXME should MMs be able to submit rung size=0 ladders? For now, do not permit
	@Test
	public void nullBidAsk() {
		MarketMaker mm = marketMaker();
		mm.agentStrategy();
		assertQuote(view.getQuote(), null, 0, null, 0);
	}

	/** If either ladder bid or ask is null, it needs to return */
	@Test
	public void createOrderLadderNullTest() {
		MarketMaker mm = marketMaker(Props.fromPairs(
				InitLadderMean.class, 0,
				InitLadderRange.class, 0));
		mm.createOrderLadder(Optional.<Price> absent(), Optional.of(Price.of(50)));
		assertQuote(view.getQuote(), null, 0, null, 0);
	}

	@Test
	public void submitOrderLadderTest() {
		MarketMaker mm = marketMaker(Props.fromPairs(K.class, 3));

		mm.submitOrderLadder(Price.of(30), Price.of(40), Price.of(50), Price.of(60));
		assertOrderLadder(mm.getActiveOrders(),
				Price.of(30), Price.of(35), Price.of(40),
				Price.of(50), Price.of(55), Price.of(60));
	}

	@Test
	public void createOrderLadderTest() {
		MarketMaker mm = marketMaker();

		mm.createOrderLadder(Optional.of(Price.of(40)), Optional.of(Price.of(50)));
		assertOrderLadder(mm.getActiveOrders(),
				Price.of(35), Price.of(40),
				Price.of(50), Price.of(55));
	}


	@Test
	public void tickImprovement() {
		MarketMaker mm = marketMaker(Props.fromPairs(
				TickImprovement.class, true,
				TickOutside.class, true));

		setQuote(view, Price.of(40), Price.of(50));

		mm.createOrderLadder(Optional.of(Price.of(40)), Optional.of(Price.of(50)));
		assertOrderLadder(mm.getActiveOrders(),
				Price.of(39), Price.of(34),
				Price.of(51), Price.of(56));
	}
	
	@Test
	public void tickOutside() {
		MarketMaker mm = marketMaker(Props.fromPairs(
				TickImprovement.class, true,
				TickOutside.class, false));

		setQuote(view, Price.of(40), Price.of(50));
		
		mm.createOrderLadder(Optional.of(Price.of(40)), Optional.of(Price.of(50)));
		assertOrderLadder(mm.getActiveOrders(),
				Price.of(36), Price.of(41),
				Price.of(49), Price.of(54));
	}
	
	@Test
	public void truncateBidTest() {
		MarketMaker marketmaker = marketMaker(Props.fromPairs(
				K.class, 3,
				Trunc.class, true,
				TickImprovement.class, false));

		setQuote(other, Price.of(90), Price.of(100));
		setQuote(view, Price.of(102), Price.of(105));

		marketmaker.createOrderLadder(Optional.of(Price.of(102)), Optional.of(Price.of(105)));
		assertOrderLadder(marketmaker.getActiveOrders(), 2,
				Price.of(92), Price.of(97),
				Price.of(105), Price.of(110), Price.of(115));
	}

	public void truncateLadder() {
		MarketMaker mm = marketMaker(Props.fromPairs(
				Trunc.class, true,
				TickImprovement.class, true,
				TickOutside.class, true));

		setQuote(view, Price.of(40), Price.of(50));
		setQuote(other, Price.of(30), Price.of(38));

		mm.createOrderLadder(Optional.of(Price.of(40)), Optional.of(Price.of(50)));
		assertOrderLadder(mm.getActiveOrders(), 1,
				Price.of(35),
				Price.of(51), Price.of(56));
	}
	
	@Test
	public void truncateLadderOnBuyRungFix() {
		MarketMaker mm = marketMaker(Props.fromPairs(
				Trunc.class, true,
				TickImprovement.class, true,
				TickOutside.class, false));

		setQuote(view, Price.of(40), Price.of(50));
		setQuote(other, Price.of(30), Price.of(35));

		mm.createOrderLadder(Optional.of(Price.of(40)), Optional.of(Price.of(50)));
		assertOrderLadder(mm.getActiveOrders(), 0,
				Price.of(49), Price.of(54));
	}
	
	@Test
	public void truncateLadderOnSellRungFix() {
		MarketMaker mm = marketMaker(Props.fromPairs(
				Trunc.class, true,
				TickImprovement.class, true,
				TickOutside.class, false));

		setQuote(view, Price.of(40), Price.of(50));
		setQuote(other, Price.of(55), Price.of(60));
		
		mm.createOrderLadder(Optional.of(Price.of(40)), Optional.of(Price.of(50)));
		assertOrderLadder(mm.getActiveOrders(), 2,
				Price.of(36), Price.of(41));
	}

	@Test
	public void truncateAskTest() {
		MarketMaker marketmaker = marketMaker(Props.fromPairs(
				K.class, 3,
				Trunc.class, true,
				TickImprovement.class, false));
		
		setQuote(other, Price.of(90), Price.of(100));
		setQuote(view, Price.of(70), Price.of(89));
		marketmaker.createOrderLadder(Optional.of(Price.of(70)), Optional.of(Price.of(89)));
		assertOrderLadder(marketmaker.getActiveOrders(), 3,
				Price.of(60), Price.of(65), Price.of(70),
				Price.of(94), Price.of(99));
	}
	
	/** Verify quantization happening */
	@Test
	public void tickSizeTest() {
		MarketMaker marketmaker = marketMaker(Props.fromPairs(
				K.class, 3,
				Size.class, 12,
				TickSize.class, 5));
	
		marketmaker.createOrderLadder(Optional.of(Price.of(40)), Optional.of(Price.of(50)));
		assertOrderLadder(marketmaker.getActiveOrders(),
				Price.of(20), Price.of(30), Price.of(40),
				Price.of(50), Price.of(60), Price.of(70));
	}
	
	@Test
	public void tickInside() {
		MarketMaker mm = marketMaker(Props.fromPairs(
				TickImprovement.class, true,
				TickOutside.class, false));
		
		setQuote(view, Price.of(40), Price.of(50));
		
		mm.createOrderLadder(Optional.of(Price.of(40)), Optional.of(Price.of(50)));
		assertOrderLadder(mm.getActiveOrders(),
				Price.of(36), Price.of(41),
				Price.of(49), Price.of(54));
	}
	
	
	/**
	 * Creating ladder without bid/ask quote
	 */
	@Test
	public void initRandLadder() {
		MarketMaker mm = marketMaker(Props.fromPairs(
				Trunc.class, true,
				TickImprovement.class, true,
				TickOutside.class, false,
				InitLadderMean.class, 100,
				InitLadderRange.class, 10));
		
		Quote quote = view.getQuote();
		mm.createOrderLadder(quote.getBidPrice(), quote.getAskPrice());
		assertRandomOrderLadder(mm.getActiveOrders(), 4, Range.closed(Price.of(95), Price.of(105)), 5);
	}

	/** One side of ladder is undefined */
	@Test
	public void oneSidedLadderBuy() {
		MarketMaker mm = marketMaker(Props.fromPairs(
				Trunc.class, true,
				TickImprovement.class, true,
				TickOutside.class, false,
				InitLadderMean.class, 100,
				InitLadderRange.class, 10));
		
		mockAgent.submitOrder(view, BUY, Price.of(40), 1);
		
		Quote quote = view.getQuote();
		mm.createOrderLadder(quote.getBidPrice(), quote.getAskPrice());
		assertRandomOrderLadder(mm.getActiveOrders(), 4, Range.singleton(Price.of(41)), Range.closed(Price.of(45), Price.of(50)), 5);
	}
	
	/** One side of ladder is undefined */
	@Test
	public void oneSidedLadderSell() {
		MarketMaker mm = marketMaker(Props.fromPairs(
				Trunc.class, true,
				TickImprovement.class, true,
				TickOutside.class, false,
				InitLadderMean.class, 100,
				InitLadderRange.class, 10));
		
		mockAgent.submitOrder(view, SELL, Price.of(50), 1);
		
		Quote quote = view.getQuote();
		mm.createOrderLadder(quote.getBidPrice(), quote.getAskPrice());
		assertRandomOrderLadder(mm.getActiveOrders(), 4, Range.closed(Price.of(40), Price.of(45)), Range.singleton(Price.of(49)), 5);
	}
	
	@Test
	public void marketMakerExecutionTimePostTest() {
		EventQueue timeline = EventQueue.create(Log.nullLogger(), rand);
		Stats stats = Stats.create();
		market = CDAMarket.create(0, Mock.stats, timeline, Log.nullLogger(), rand, Mock.sip, Props.fromPairs());
		view = market.getPrimaryView();
		MarketMaker mm = marketMaker(stats, timeline, Props.fromPairs());
		
		mm.submitOrder(view, BUY, Price.of(50), 1);
		timeline.executeUntil(TimeStamp.of(2));
		mockAgent.submitOrder(view, SELL, Price.of(50), 1); // 2 execution time
		timeline.executeUntil(TimeStamp.of(2));
		
		mockAgent.submitOrder(view, BUY, Price.of(100), 2);
		mm.submitOrder(view, SELL, Price.of(100), 2); // 0 execution time
		timeline.executeUntil(TimeStamp.of(2));
		
		// XXX Not this does not account for quantity unlike background agent execution times
		assertEquals(1, stats.getSummaryStats().get(Stats.MARKET_MAKER_EXECTUION_TIME + mm).mean(), eps);
	}
	
	@Test
	public void marketMakerSpreadLadderPostTest() {
		Stats stats = Stats.create();
		MarketMaker mm = marketMaker(stats, Mock.timeline, Props.fromPairs());
		mm.createOrderLadder(Optional.of(Price.of(50)), Optional.of(Price.of(70)));
		mm.createOrderLadder(Optional.of(Price.of(60)), Optional.of(Price.of(100)));
		
		assertEquals(30, stats.getSummaryStats().get(Stats.MARKET_MAKER_SPREAD + mm).mean(), eps);
		assertEquals(2, stats.getSummaryStats().get(Stats.MARKET_MAKER_SPREAD + mm).n());
		
		assertEquals(70, stats.getSummaryStats().get(Stats.MARKET_MAKER_LADDER + mm).mean(), eps);
		assertEquals(2, stats.getSummaryStats().get(Stats.MARKET_MAKER_LADDER + mm).n());
	}
	
	// TODO test truncation when quote latency (or using NBBO?)
	
	@Test
	public void extraTest() {
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
		mockAgent.submitOrder(view, BUY, bid, 1);
		mockAgent.submitOrder(view, SELL, ask, 1);
	}
	
	private MarketMaker marketMaker(Stats stats, Timeline timeline, Props parameters) {
		if (timeline instanceof MockTimeLine) // If initialized with a mocktimeline, things get executed out of order and so some fields aren't initialized
			((MockTimeLine) timeline).ignoreNext();
		return new MarketMaker(0, stats, timeline, Log.nullLogger(), rand, sip, fundamental, market, Props.merge(defaults, parameters)) {
			private static final long serialVersionUID = 1L;
		};
	}
	
	private MarketMaker marketMaker(Props parameters) {
		return marketMaker(Mock.stats, Mock.timeline, parameters);
	}
	
	private MarketMaker marketMaker() {
		return marketMaker(Props.fromPairs());
	}
	
}
