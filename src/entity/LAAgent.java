package entity;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import market.Price;
import market.Quote;
import model.MarketModel;
import systemmanager.Consts;
import utils.RandPlus;
import activity.Activity;
import activity.SubmitBid;
import data.EntityProperties;
import data.Keys;
import event.TimeStamp;

/**
 * LAAGENT
 * 
 * High-frequency trader employing latency arbitrage strategy.
 * 
 * @author ewah
 */
public class LAAgent extends HFTAgent {

	protected final double alpha; // LA profit gap
	protected final Map<Market, LAIP> ips;

	public LAAgent(int agentID, MarketModel model, double alpha,
			TimeStamp latency, RandPlus rand, int tickSize) {
		super(agentID, Consts.START_TIME, model, rand, tickSize);
		this.alpha = alpha;
		this.ips = new HashMap<Market, LAIP>();

		for (Market market : model.getMarkets()) {
			LAIP laip = new LAIP(model.nextIPID(), latency, market, this);
			ips.put(market, laip);
			market.addIP(laip);
		}
	}

	public LAAgent(int agentID, MarketModel model, RandPlus rand,
			EntityProperties props) {
		this(agentID, model, props.getAsDouble(Keys.ALPHA, 0.001), null, rand,
				props.getAsInt(Keys.TICK_SIZE, 1000));
	}

	@Override
	public Collection<? extends Activity> agentStrategy(TimeStamp ts) {
		Price bestBid = null, bestAsk = null;
		Market bestBidMarket = null, bestAskMarket = null;
		int bestBidQuantity = 0, bestAskQuantity = 0;

		for (Entry<Market, LAIP> ipEntry : ips.entrySet()) {
			Quote q = ipEntry.getValue().getQuote();
			if (q.getAskPrice() != null && q.getAskPrice().lessThan(bestAsk)) {
				bestAsk = q.getAskPrice();
				bestAskMarket = ipEntry.getKey();
//				bestAskQuantity = -q.getAskQuantity(); FIXME quantity no longer in quote
				// This shouldn't be a problem because quote shouldn't have quantity. LAAgent should
				// have to go to order book to get this info
			}
			if (q.getBidPrice() != null && q.getBidPrice().greaterThan(bestBid)) {
				bestBid = q.getBidPrice();
				bestBidMarket = ipEntry.getKey();
//				bestBidQuantity = q.getBidQuantity();
			}
		}

		if (bestBid == null || bestAsk == null
				|| bestAsk.getPrice() < (1 + alpha) * bestBid.getPrice())
			return Collections.emptySet();

		Price midPoint = bestBid.plus(bestAsk).times(0.5).quantize(tickSize);
		int quantity = Math.min(bestBidQuantity, bestAskQuantity);
		return Arrays.asList(new SubmitBid(this, bestBidMarket, midPoint,
				-quantity, TimeStamp.IMMEDIATE), new SubmitBid(this, bestAskMarket,
				midPoint, quantity, TimeStamp.IMMEDIATE));
	}

}
