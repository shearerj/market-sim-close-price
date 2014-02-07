package entity.agent;

import static fourheap.Order.OrderType.BUY;
import static fourheap.Order.OrderType.SELL;
import static logger.Logger.logger;
import static logger.Logger.Level.INFO;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import systemmanager.Keys;
import activity.Activity;
import activity.SubmitOrder;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.Maps;

import data.EntityProperties;
import data.FundamentalValue;
import entity.infoproc.HFTQuoteProcessor;
import entity.infoproc.SIP;
import entity.market.Market;
import entity.market.Price;
import entity.market.Quote;
import event.TimeStamp;
import fourheap.FourHeap;
import fourheap.MatchedOrders;
import fourheap.Order;

/**
 * LAAGENT
 * 
 * High-frequency trader employing latency arbitrage strategy.
 * 
 * @author ewah
 */
public class LAAgent extends HFTAgent {

	private static final long serialVersionUID = 1479379512311568959L;
	
	protected final double alpha; // LA profit gap

	public LAAgent(TimeStamp latency, FundamentalValue fundamental,
			SIP sip, Collection<Market> markets, Random rand, int tickSize,
			double alpha) {
		super(latency, TimeStamp.ZERO, fundamental, sip, markets, rand, tickSize);
		
		this.alpha = alpha;
	}

	public LAAgent(FundamentalValue fundamental, SIP sip,
			Collection<Market> markets, Random rand, EntityProperties props) {
		this(new TimeStamp(props.getAsLong(Keys.LA_LATENCY, -1)), fundamental, sip, markets,
				rand, props.getAsInt(Keys.TICK_SIZE, 1),
				props.getAsDouble(Keys.ALPHA, 0.001));
	}

	@Override
	// TODO Need strategy for orders that don't execute
	public Iterable<? extends Activity> agentStrategy(TimeStamp ts) {
		if (positionBalance != 0 && activeOrders.isEmpty())
			// Have pending submit order activities
			return ImmutableList.of();
		
		Price bestBid = null, bestAsk = null;
		Market bestBidMarket = null, bestAskMarket = null;

		for (Entry<Market, HFTQuoteProcessor> ipEntry : quoteProcessors.entrySet()) {
			Quote q = ipEntry.getValue().getQuote();
			if (q.getAskPrice() != null && q.getAskPrice().lessThan(bestAsk)) {
				bestAsk = q.getAskPrice();
				bestAskMarket = ipEntry.getKey();
			}
			if (q.getBidPrice() != null && q.getBidPrice().greaterThan(bestBid)) {
				bestBid = q.getBidPrice();
				bestBidMarket = ipEntry.getKey();
			}
		}

		if (bestBid == null || bestAsk == null
				|| bestAsk.doubleValue() * (1 + alpha) > bestBid.doubleValue())
			return Collections.emptySet();

		logger.log(INFO, "%s detected arbirage between %s %s and %s %s", this, 
				bestBidMarket, quoteProcessors.get(bestBidMarket).getQuote(),
				bestAskMarket, quoteProcessors.get(bestAskMarket).getQuote());
		Price midPoint = new Price((bestBid.doubleValue() + bestAsk.doubleValue()) * .5).quantize(tickSize);
		return ImmutableList.of(
				new SubmitOrder(this, bestBidMarket, SELL, midPoint, 1, TimeStamp.IMMEDIATE),
				new SubmitOrder(this, bestAskMarket, BUY, midPoint, 1, TimeStamp.IMMEDIATE));
	}

	/*
	 * This should be a natural extension of the arbitrage strategy extended to
	 * multi quantities, which should be necessary if the arbitrageur has a
	 * latency. FIXME For some reason this is not the same as the above
	 * strategy, sometimes making more profit, sometimes less, and I'm unsure
	 * why.
	 */
	public Iterable<? extends Activity> agentStrategy2(TimeStamp ts) {
		FourHeap<Price, Integer, Order<Price, Integer>> fh = FourHeap.<Price, Integer, Order<Price, Integer>> create();
		Map<Order<Price, Integer>, Market> orderMap = Maps.newHashMap();
		
		for (Entry<Market, HFTQuoteProcessor> ipEntry : quoteProcessors.entrySet()) {
			Quote q = ipEntry.getValue().getQuote();
			if (q.getBidPrice() != null && q.getBidQuantity() > 0) {
				Order<Price, Integer> order = Order.create(BUY, q.getBidPrice(), q.getBidQuantity(), 0);
				orderMap.put(order, ipEntry.getKey());
				fh.insertOrder(order);
			}
			if (q.getAskPrice() != null && q.getAskQuantity() > 0) {
				Order<Price, Integer> order = Order.create(SELL, q.getAskPrice(), q.getAskQuantity(), 0);
				orderMap.put(order, ipEntry.getKey());
				fh.insertOrder(order);
			}
		}
		
		List<MatchedOrders<Price, Integer, Order<Price, Integer>>> transactions = fh.clear();
		Builder<Activity> acts = ImmutableList.builder();
		for (MatchedOrders<Price, Integer, Order<Price, Integer>> trans : transactions) {
			Order<Price, Integer> buy = trans.getBuy(), sell = trans.getSell();
			if (sell.getPrice().doubleValue() * (1 + alpha) > buy.getPrice().doubleValue())
				continue;
			double midPoint = .5 * (buy.getPrice().doubleValue() + sell.getPrice().doubleValue()); 
			Price midPrice = new Price(midPoint).quantize(tickSize);
			
			acts.add(new SubmitOrder(this, orderMap.get(sell), BUY, midPrice, trans.getQuantity(), TimeStamp.IMMEDIATE));
			acts.add(new SubmitOrder(this, orderMap.get(buy), SELL, midPrice, trans.getQuantity(), TimeStamp.IMMEDIATE));
		}
		return acts.build();
	}

	@Override
	public String toString() {
		return "LA " + super.toString();
	}

}
