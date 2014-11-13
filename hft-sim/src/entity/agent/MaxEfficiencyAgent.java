package entity.agent;

import static com.google.common.base.Preconditions.checkArgument;
import static fourheap.Order.OrderType.BUY;
import static fourheap.Order.OrderType.SELL;
import logger.Log;
import systemmanager.Keys.ArrivalRate;
import systemmanager.Keys.ReentryRate;
import utils.Rand;
import data.FundamentalValue;
import data.Props;
import data.Stats;
import entity.market.CallMarket;
import entity.market.Market;
import entity.market.Price;
import entity.sip.MarketInfo;
import event.Timeline;

/**
 * Created solely for purpose of measuring maximum allocative efficiency in 
 * a market. 
 * 
 * Arrives at time 0. 
 * 
 * @author ewah
 *
 */
public class MaxEfficiencyAgent extends BackgroundAgent {

	protected MaxEfficiencyAgent(int id, Stats stats, Timeline timeline, Log log, Rand rand, MarketInfo sip, FundamentalValue fundamental,
			Market market, Props props) {
		super(id, stats, timeline, log, rand, sip, fundamental, market, Props.withDefaults(props,
				ReentryRate.class, 0d,
				ArrivalRate.class, Double.POSITIVE_INFINITY));
		checkArgument(market instanceof CallMarket, "MaxEfficiency Agent can only enter call markets");
	}
	
	public static MaxEfficiencyAgent create(int id, Stats stats, Timeline timeline, Log log, Rand rand, MarketInfo sip, FundamentalValue fundamental,
			Market market, Props props) {
		return new MaxEfficiencyAgent(id, stats, timeline, log, rand, sip, fundamental, market, props);
	}

	@Override
	protected void agentStrategy() {
		// submit 1-unit limit orders for all values in its private vector

		for (int qty = -getMaxAbsPosition() + 1; qty <= 0; qty++)
			submitOrder(SELL, getHypotheticalPrivateValue(qty, 1, SELL), 1);
		for (int qty = 0; qty < getMaxAbsPosition(); qty++)
			submitOrder(BUY, getHypotheticalPrivateValue(qty, 1, BUY), 1);
	}

	@Override
	public void liquidateAtPrice(Price price) {
		postStat(Stats.MAX_EFF_POSITION + getPosition(), 1);
		super.liquidateAtPrice(price);
	}

	private static final long serialVersionUID = -8915874536659571239L;
}
