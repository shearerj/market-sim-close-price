package entity.agent;

import static fourheap.Order.OrderType.BUY;
import static fourheap.Order.OrderType.SELL;
import static org.junit.Assert.assertFalse;
import static utils.Tests.assertOrderLadder;
import logger.Log;

import org.junit.Before;
import org.junit.Test;

import systemmanager.Keys.FundEstimate;
import systemmanager.Keys.FundamentalMean;
import systemmanager.Keys.FundamentalShockVar;
import systemmanager.Keys.InitLadderMean;
import systemmanager.Keys.InitLadderRange;
import systemmanager.Keys.K;
import systemmanager.Keys.ReentryRate;
import systemmanager.Keys.SimLength;
import systemmanager.Keys.Size;
import systemmanager.Keys.Spread;
import systemmanager.Keys.TickImprovement;
import systemmanager.Keys.TickOutside;
import systemmanager.Keys.Trunc;
import utils.Mock;
import utils.Rand;
import data.FundamentalValue;
import data.Props;
import entity.market.Market;
import entity.market.Market.MarketView;
import entity.market.Price;
import fourheap.Order.OrderType;

/**
 * @author ewah
 *
 */
public class FundamentalMarketMakerTest {
	
	// FIXME Test that when constant fundamental estimate is not made, that fundamental that agent uses updates

	private static final Rand rand = Rand.create();
	private static final Props defaults = Props.builder()
			.put(ReentryRate.class, 0d)
			.put(Trunc.class, true)
			.put(TickImprovement.class, true)
			.put(TickOutside.class, true)
			.put(InitLadderMean.class, 0)
			.put(InitLadderRange.class, 0)
			.put(SimLength.class, 1000l)
			.put(FundamentalMean.class, 146724)
			.put(FundamentalShockVar.class, 0d)
			.put(K.class, 3)
			.put(Size.class, 5)
			.build();
	private static final FundamentalValue fundamental = Mock.fundamental(defaults.get(FundamentalMean.class));
	
	private Market market;
	private MarketView view;
	private Agent mockAgent;

	@Before
	public void setup() {
		market = Mock.market();
		view = market.getPrimaryView();
		mockAgent = Mock.agent();
	}
	
	// FIXME Test math of fundamental estimate
	// TODO test const spread < 0, = 0 (same)
	// TODO test computation of spread with no const spread and null bid / ask

	
	@Test
	public void estimatedFundamentalTest() {
		FundamentalMarketMaker mm = fundamentalMM(Props.fromPairs(
				K.class, 1,
				Trunc.class, false,
				TickImprovement.class, false,
				TickOutside.class, false));

		setQuote(Price.of(146724 - 1000), Price.of(146724 + 1000));
		
		mm.agentStrategy();

		assertOrderLadder(mm.getActiveOrders(), Price.of(146724 - 1000), Price.of(146724 + 1000));
	}
	
	@Test
	public void constantSpreadTest() {
		FundamentalMarketMaker mm = fundamentalMM(Props.fromPairs(
				K.class, 1,
				Trunc.class, false,
				TickImprovement.class, false,
				TickOutside.class, false,
				Spread.class, Price.of(1000)));

		setQuote(Price.of(146724 - 1000), Price.of(146724 + 1000));

		mm.agentStrategy();
		
		assertOrderLadder(mm.getActiveOrders(), Price.of(146724 - 500), Price.of(146724 + 500));
	}
	
	/** testing when no bid/ask, still submits orders */
	@Test
	public void nullBidAsk() {
		MarketMaker mm = fundamentalMM(Props.fromPairs(
				Trunc.class, false));

		mm.agentStrategy();
		assertFalse(mm.getActiveOrders().isEmpty());
	}

	@Test
	public void basicLadderTest() {
		MarketMaker mm = fundamentalMM(Props.fromPairs(
				K.class, 2,
				Trunc.class, false,
				TickImprovement.class, false,
				TickOutside.class, false,
				FundEstimate.class, Price.of(45)));

		setQuote(Price.of(40), Price.of(50));

		mm.agentStrategy();

		assertOrderLadder(mm.getActiveOrders(),
				Price.of(35), Price.of(40),
				Price.of(50), Price.of(55));
	}

	/**
	 * Check when quote changes in between reentries
	 */
	@Test
	public void quoteChangeTest() {
		MarketMaker marketmaker = fundamentalMM(Props.fromPairs(
				K.class, 2,
				Size.class, 10,
				Trunc.class, false,
				TickOutside.class, false,
				FundEstimate.class, Price.of(45)));

		setQuote(Price.of(40), Price.of(50));

		marketmaker.agentStrategy();
		
		// Quote change
		setQuote(Price.of(42), Price.of(48));

		marketmaker.agentStrategy();
		
		assertOrderLadder(marketmaker.getActiveOrders(),
				Price.of(33), Price.of(43),
				Price.of(47), Price.of(57));
	}

	private void setQuote(Price bid, Price ask) {
		mockAgent.withdrawAllOrders();
		submitOrder(mockAgent, BUY, bid);
		submitOrder(mockAgent, SELL, ask);
	}

	private OrderRecord submitOrder(Agent agent, OrderType buyOrSell, Price price) {
		return agent.submitOrder(view, buyOrSell, price, 1);
	}
	
	private FundamentalMarketMaker fundamentalMM(Props props) {
		Mock.timeline.ignoreNext();
		return FundamentalMarketMaker.create(0, Mock.stats, Mock.timeline, Log.nullLogger(), rand, Mock.sip, fundamental, market,
				Props.builder().putAll(defaults).putAll(props).build());
	}

}
