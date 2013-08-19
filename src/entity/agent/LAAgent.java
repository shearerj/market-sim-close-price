package entity.agent;

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
import entity.infoproc.HFTIP;
import entity.infoproc.SIP;
import entity.market.Market;
import entity.market.Price;
import entity.market.Quote;
import event.TimeStamp;
import fourheap.FourHeap;
import fourheap.Order;
import fourheap.Transaction;

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
	protected final Map<Market, HFTIP> ips;

	public LAAgent(Collection<Market> markets, FundamentalValue fundamental,
			SIP sip, double alpha, TimeStamp latency, Random rand,
			int tickSize) {
		super(TimeStamp.ZERO, markets, fundamental, sip, rand, tickSize);
		this.alpha = alpha;
		this.ips = Maps.newHashMap();

		for (Market market : markets) {
			HFTIP laip = new HFTIP(latency, market, this);
			ips.put(market, laip);
			market.addIP(laip);
		}
	}

	public LAAgent(Collection<Market> markets, FundamentalValue fundamental,
			SIP sip, Random rand, EntityProperties props) {
		this(markets, fundamental, sip, props.getAsDouble(Keys.ALPHA, 0.001),
				new TimeStamp(props.getAsLong(Keys.LA_LATENCY, -1)), rand,
				props.getAsInt(Keys.TICK_SIZE, 1000));
	}

	@Override
	// TODO Strategy for orders that don't execute
	public Iterable<? extends Activity> agentStrategy(TimeStamp ts) {
		Price bestBid = null, bestAsk = null;
		Market bestBidMarket = null, bestAskMarket = null;

		for (Entry<Market, HFTIP> ipEntry : ips.entrySet()) {
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
				|| bestAsk.getInTicks() * (1 + alpha) > bestBid.getInTicks())
			return Collections.emptySet();

		Price midPoint = bestBid.plus(bestAsk).times(0.5).quantize(tickSize);
		return ImmutableList.of(new SubmitOrder(this, bestBidMarket, midPoint,
				-1, TimeStamp.IMMEDIATE), new SubmitOrder(this, bestAskMarket,
				midPoint, 1, TimeStamp.IMMEDIATE));
	}
	
	public Iterable<? extends Activity> agentStrategy2(TimeStamp ts) {
		FourHeap<Price, Integer> fh = FourHeap.create();
		Map<Order<Price, Integer>, Market> orderMap = Maps.newHashMap();
		
		for (Entry<Market, HFTIP> ipEntry : ips.entrySet()) {
			Quote q = ipEntry.getValue().getQuote();
			if (q.getBidPrice() != null && q.getBidQuantity() > 0) {
				Order<Price, Integer> order = Order.create(q.getBidPrice(), q.getBidQuantity(), 0);
				orderMap.put(order, ipEntry.getKey());
				fh.insertOrder(order);
			}
			if (q.getAskPrice() != null && q.getAskQuantity() > 0) {
				Order<Price, Integer> order = Order.create(q.getAskPrice(), -q.getAskQuantity(), 0);
				orderMap.put(order, ipEntry.getKey());
				fh.insertOrder(order);
			}
		}
		
		List<Transaction<Price, Integer>> transactions = fh.clear();
		Builder<Activity> acts = ImmutableList.builder();
		for (Transaction<Price, Integer> trans : transactions) {
			Order<Price, Integer> buy = trans.getBuy(), sell = trans.getSell();
			if (sell.getPrice().getInTicks() * (1 + alpha) > buy.getPrice().getInTicks())
				continue;
			double midPoint = .5 * (buy.getPrice().getInTicks() + sell.getPrice().getInTicks()); 
			Price midPrice = new Price(midPoint).quantize(tickSize);
			
			acts.add(new SubmitOrder(this, orderMap.get(sell), midPrice, trans.getQuantity(), TimeStamp.IMMEDIATE));
			acts.add(new SubmitOrder(this, orderMap.get(buy), midPrice, -trans.getQuantity(), TimeStamp.IMMEDIATE));
		}
		return acts.build();
	}

}
