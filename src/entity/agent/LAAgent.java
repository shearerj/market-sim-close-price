package entity.agent;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;

import systemmanager.Keys;
import utils.RandPlus;
import activity.Activity;
import activity.SubmitOrder;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;

import data.EntityProperties;
import data.FundamentalValue;
import entity.infoproc.HFTIP;
import entity.infoproc.SIP;
import entity.market.Market;
import entity.market.Price;
import entity.market.Quote;
import event.TimeStamp;

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
			SIP sip, double alpha, TimeStamp latency, RandPlus rand,
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
			SIP sip, RandPlus rand, EntityProperties props) {
		this(markets, fundamental, sip, props.getAsDouble(Keys.ALPHA, 0.001),
				new TimeStamp(props.getAsLong(Keys.LA_LATENCY, -1)), rand,
				props.getAsInt(Keys.TICK_SIZE, 1000));
	}

	@Override
	// TODO Extend to multiple quantities / all possible arbitrage opportunities
	// TODO Strategy for orders that don't execute
	public Collection<? extends Activity> agentStrategy(TimeStamp ts) {
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

}
