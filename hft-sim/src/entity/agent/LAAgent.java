package entity.agent;

import static com.google.common.base.Preconditions.checkState;
import static entity.agent.LAAgent.Status.OUTDATED;
import static entity.agent.LAAgent.Status.READY;
import static entity.agent.LAAgent.Status.SUBMITTED;
import static fourheap.Order.OrderType.BUY;
import static fourheap.Order.OrderType.SELL;
import static logger.Log.Level.INFO;

import java.util.Collection;
import java.util.Map;
import java.util.Random;

import logger.Log;
import systemmanager.Keys.Alpha;
import systemmanager.Keys.LaLatency;

import com.google.common.base.Functions;
import com.google.common.base.Optional;
import com.google.common.collect.Maps;

import data.FundamentalValue;
import data.Props;
import data.Stats;
import entity.market.Market;
import entity.market.Market.MarketView;
import entity.market.Price;
import entity.market.Quote;
import entity.sip.MarketInfo;
import event.TimeLine;
import event.TimeStamp;
import fourheap.Order.OrderType;

/**
 * LAAGENT
 * 
 * High-frequency trader employing latency arbitrage strategy.
 * 
 * @author ewah
 */
public class LAAgent extends HFTAgent {

	private static final long serialVersionUID = 1479379512311568959L;
	protected enum Status { READY, SUBMITTED, OUTDATED};
	
	protected final double alpha; // LA profit gap
	// To "lock" certain markets until agent knows it's current quote information reflects placed bids
	protected final Map<MarketView, Status> buyStatus, sellStatus;
	
	protected LAAgent(int id, Stats stats, TimeLine timeline, Log log, Random rand, MarketInfo sip, FundamentalValue fundamental,
			Collection<Market> markets, Props props) {
		super(id, stats, timeline, log, rand, sip, fundamental, TimeStamp.ZERO,
				Maps.toMap(markets, Functions.constant(props.get(LaLatency.class))),
				props);

		this.alpha = props.get(Alpha.class);
		this.buyStatus = Maps.newHashMapWithExpectedSize(this.markets.size());
		this.sellStatus = Maps.newHashMapWithExpectedSize(this.markets.size());
		for (MarketView market : this.markets) {
			buyStatus.put(market, READY);
			sellStatus.put(market, READY);
		}
	}

	public static LAAgent create(int id, Stats stats, TimeLine timeline, Log log, Random rand, MarketInfo sip, FundamentalValue fundamental
			, Collection<Market> markets, Props props) {
		return new LAAgent(id, stats, timeline, log, rand, sip, fundamental, markets, props);
	}

	@Override
	// TODO Need strategy for orders that don't execute
	protected void agentStrategy() {
		Optional<Price> bestBid = Optional.absent(), bestAsk = Optional.absent();
		MarketView bestBidMarket = null, bestAskMarket = null;
		int bestBidQuantity = 0, bestAskQuantity = 0;
		
		for (MarketView market : markets) {
			Quote q = market.getQuote();
			
			if (buyStatus.get(market) == READY && q.getAskPrice().or(Price.INF).lessThan(bestAsk.or(Price.INF))) {
				bestAsk = q.getAskPrice();
				bestAskMarket = market;
				bestAskQuantity = q.getAskQuantity();
			}
			if (sellStatus.get(market) == READY && q.getBidPrice().or(Price.NEG_INF).greaterThan(bestBid.or(Price.NEG_INF))) {
				bestBid = q.getBidPrice();
				bestBidMarket = market;
				bestBidQuantity = q.getBidQuantity();
			}
		}

		if (!bestBid.isPresent() || !bestAsk.isPresent() || bestAsk.get().doubleValue() * (1 + alpha) > bestBid.get().doubleValue())
			return;
		
		log(INFO, "%s detected arbitrage between %s %s and %s %s", this, 
				bestBidMarket, bestBidMarket.getQuote(),
				bestAskMarket, bestAskMarket.getQuote());
		Price midPoint = Price.of((bestBid.get().doubleValue() + bestAsk.get().doubleValue()) * .5);
		int quantity = Math.min(bestBidQuantity, bestAskQuantity);
		
		submitOrder(bestBidMarket, SELL, midPoint, quantity);
		submitOrder(bestAskMarket, BUY, midPoint, quantity);
	}

	@Override
	protected OrderRecord submitOrder(MarketView market, OrderType type, Price price, int quantity) {
		checkState((type == BUY ? buyStatus : sellStatus).put(market, SUBMITTED) == READY,
				"Submitted an order when the market wasn't ready");
		return super.submitOrder(market, type, price, quantity);
	}

	// After the order is submitted, we wait for a quote update from the market
	@Override
	protected void orderSubmitted(OrderRecord order, MarketView market, TimeStamp submittedTime) {
		checkState((order.getOrderType() == BUY ? buyStatus : sellStatus).put(market, OUTDATED) == SUBMITTED,
				"Notified of a submitted order, but market wasn't marked as submitted");
		super.orderSubmitted(order, market, submittedTime);
	}

	// Once we update the quote of markets that were outdated, we're ready to arbitrage again
	@Override
	protected void quoteUpdate(MarketView market) {
		if (buyStatus.get(market) == OUTDATED)
			buyStatus.put(market, READY);
		if (sellStatus.get(market) == OUTDATED)
			sellStatus.put(market, READY);
		super.quoteUpdate(market);
	}

}
