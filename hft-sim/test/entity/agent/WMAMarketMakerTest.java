package entity.agent;

import static fourheap.Order.OrderType.BUY;
import static fourheap.Order.OrderType.SELL;
import static utils.Tests.checkOrderLadder;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import logger.Log;

import org.junit.Before;
import org.junit.Test;

import systemmanager.Keys.FundamentalMean;
import systemmanager.Keys.FundamentalShockVar;
import systemmanager.Keys.NumHistorical;
import systemmanager.Keys.NumRungs;
import systemmanager.Keys.ReentryRate;
import systemmanager.Keys.RungSize;
import systemmanager.Keys.TickImprovement;
import systemmanager.Keys.TruncateLadder;
import systemmanager.Keys.WeightFactor;
import systemmanager.MockSim;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

import data.Props;
import entity.agent.position.PrivateValues;
import entity.market.Market;
import entity.market.Market.MarketView;
import entity.market.Price;
import event.TimeStamp;
import fourheap.Order.OrderType;

//FIXME MM orders are withdrawn manually due to lack of "proper" instantaneous withdraw orders implementation

public class WMAMarketMakerTest {
	private static final Random rand = new Random();
	private static final Props defaults = Props.builder()
			.put(ReentryRate.class, 0d)
			.put(TickImprovement.class, false)
			.put(NumRungs.class, 1)
			.put(RungSize.class, 10)
			.put(TruncateLadder.class, false)
			.put(NumHistorical.class, 5)
			.build();
	
	private MockSim sim;
	private Market actualMarket;
	private MarketView market;
	private WMAMarketMaker mm;
	private Agent mockAgent;

	@Before
	public void setup() throws IOException {
		sim = MockSim.createCDA(getClass(), Log.Level.NO_LOGGING, 1,
				Props.fromPairs(FundamentalMean.class, 100000, FundamentalShockVar.class, 0d));
		actualMarket = Iterables.getOnlyElement(sim.getMarkets());
		market = actualMarket.getPrimaryView();
		mockAgent = mockAgent();
	}

	/**
	 * Check that bid and ask queues are updating correctly, and moving average
	 * is computed correctly
	 */
	@Test
	public void computeLinearWeightedMovingAverage() {
		mm = wmaMarketMaker(Props.fromPairs(WeightFactor.class, 0d));
		
		// Add quotes & execute agent strategy in between
		List<Price> bids = ImmutableList.of(Price.of(50), Price.of(52), Price.of(54), Price.of(56), Price.of(58));
		List<Price> asks = ImmutableList.of(Price.of(60), Price.of(64), Price.of(68), Price.of(72), Price.of(76));
		
		for (Iterator<Price> buys = bids.iterator(), sells = asks.iterator(); buys.hasNext() && sells.hasNext();) {
			setQuote(buys.next(), sells.next());
			mm.agentStrategy();
			sim.executeImmediate();
		}

		checkOrderLadder(mm.activeOrders, Price.of(55), Price.of(71));
	}
	
	@Test
	public void computeExpWeightedMovingAverage() {
		mm = wmaMarketMaker(Props.fromPairs(WeightFactor.class, 0.5));
		
		// Add quotes & execute agent strategy in between
		List<Price> bids = ImmutableList.of(Price.of(50), Price.of(52), Price.of(54), Price.of(56), Price.of(58));
		List<Price> asks = ImmutableList.of(Price.of(60), Price.of(64), Price.of(68), Price.of(72), Price.of(76));

		for (Iterator<Price> buys = bids.iterator(), sells = asks.iterator(); buys.hasNext() && sells.hasNext();) {
			setQuote(buys.next(), sells.next());
			mm.agentStrategy();
			sim.executeImmediate();
		}
		
		checkOrderLadder(mm.activeOrders, Price.of(56), Price.of(73));
	}

	/**
	 * When bid/ask queues are not full yet, test the computation of the
	 * moving average
	 */
	@Test
	public void partialQueueExponentialLadderTest() {
		mm = wmaMarketMaker(Props.fromPairs(WeightFactor.class, 0.9));

		// Add quotes & execute agent strategy in between
		List<Price> bids = ImmutableList.of(Price.of(50000), Price.of(52000));
		List<Price> asks = ImmutableList.of(Price.of(60000), Price.of(64000));

		for (Iterator<Price> buys = bids.iterator(), sells = asks.iterator(); buys.hasNext() && sells.hasNext();) {
			setQuote(buys.next(), sells.next());
			mm.agentStrategy();
			sim.executeImmediate();
		}
		
		checkOrderLadder(mm.activeOrders, Price.of(51818), Price.of(63636));
	}
	
	/**
	 * When bid/ask queues are not full yet, test the computation of the
	 * moving average
	 */
	@Test
	public void partialQueueLienarLadderTest() {
		mm = wmaMarketMaker(Props.fromPairs(WeightFactor.class, 0d));

		// Add quotes & execute agent strategy in between
		List<Price> bids = ImmutableList.of(Price.of(50000), Price.of(52000));
		List<Price> asks = ImmutableList.of(Price.of(60000), Price.of(64000));

		for (Iterator<Price> buys = bids.iterator(), sells = asks.iterator(); buys.hasNext() && sells.hasNext();) {
			setQuote(buys.next(), sells.next());
			mm.agentStrategy();
			sim.executeImmediate();
		}
		
		checkOrderLadder(mm.activeOrders, Price.of(51333), Price.of(62667));
	}

	private WMAMarketMaker wmaMarketMaker(Props parameters) {
		return WMAMarketMaker.create(sim, actualMarket, rand, Props.merge(defaults, parameters));
	}
	
	private void setQuote(Price bid, Price ask) {
		mockAgent.withdrawAllOrders();
		sim.executeImmediate();
		mm.withdrawAllOrders();
		sim.executeImmediate();
		submitOrder(mockAgent, BUY, bid);
		submitOrder(mockAgent, SELL, ask);
	}

	private OrderRecord submitOrder(Agent agent, OrderType buyOrSell, Price price) {
		OrderRecord order = agent.submitOrder(market, buyOrSell, price, 1);
		sim.executeImmediate();
		return order;
	}
	
	private Agent mockAgent() {
		return new Agent(sim, PrivateValues.zero(), TimeStamp.ZERO, rand, Props.fromPairs()) {
			private static final long serialVersionUID = 1L;
			@Override public void agentStrategy() { }
			@Override public String toString() { return "TestAgent " + id; }
		};
	}
	
}
