package entity.agent;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import model.MarketModel;
import systemmanager.Keys;
import utils.RandPlus;
import activity.Activity;
import activity.SubmitOrder;
import data.EntityProperties;
import entity.infoproc.HFTIP;
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

	public LAAgent(int agentID, MarketModel model, double alpha,
			TimeStamp latency, RandPlus rand, int tickSize) {
		super(agentID, TimeStamp.ZERO, model, rand, tickSize);
		this.alpha = alpha;
		this.ips = new HashMap<Market, HFTIP>();

		for (Market market : model.getMarkets()) {
			HFTIP laip = new HFTIP(model.nextIPID(), latency, market, this);
			ips.put(market, laip);
			market.addIP(laip);
		}
	}

	public LAAgent(int agentID, MarketModel model, RandPlus rand,
			EntityProperties props) {
		this(agentID, model, props.getAsDouble(Keys.ALPHA, 0.001),
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
		return Arrays.asList(new SubmitOrder(this, bestBidMarket, midPoint,
				-1, TimeStamp.IMMEDIATE), new SubmitOrder(this, bestAskMarket,
				midPoint, 1, TimeStamp.IMMEDIATE));
	}

}
