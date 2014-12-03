package entity.agent;

import static com.google.common.base.Preconditions.checkArgument;
import static fourheap.Order.OrderType.BUY;
import static fourheap.Order.OrderType.SELL;
import logger.Log;
import systemmanager.Keys.MaxPosition;
import systemmanager.Keys.PrivateValueVar;
import utils.Rand;

import com.google.common.collect.Iterators;

import data.FundamentalValue;
import data.Props;
import data.Stats;
import entity.agent.position.ListPrivateValue;
import entity.market.CallMarket;
import entity.market.Market;
import entity.market.Price;
import entity.sip.MarketInfo;
import event.TimeStamp;
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
		super(id, stats, timeline, log, rand, sip, fundamental,
				Iterators.singletonIterator(TimeStamp.ZERO), // Arrive once at time zero
				ListPrivateValue.createRandomly(props.get(MaxPosition.class), props.get(PrivateValueVar.class), rand),
				market, props);
		checkArgument(market instanceof CallMarket, "MaxEfficiency Agent can only enter call markets");
		for (int i = -getMaxAbsPosition(); i <= getMaxAbsPosition(); ++i)
			postStat(Stats.MAX_EFF_POSITION + i, 0);
	}
	
	public static MaxEfficiencyAgent create(int id, Stats stats, Timeline timeline, Log log, Rand rand, MarketInfo sip, FundamentalValue fundamental,
			Market market, Props props) {
		return new MaxEfficiencyAgent(id, stats, timeline, log, rand, sip, fundamental, market, props);
	}

	@Override
	protected void agentStrategy() {
		// submit 1-unit limit orders for all values in its private vector
		// FIXME All orders are offset by maxint / 2, but they could still be truncated at either end. There is no guard for this...
		for (int qty = -getMaxAbsPosition() + 1; qty <= 0; qty++)
			submitOrder(primaryMarket, SELL, Price.of(getHypotheticalPrivateValue(qty, 1, SELL).intValue() + Integer.MAX_VALUE / 2), 1);
		for (int qty = 0; qty < getMaxAbsPosition(); qty++)
			submitOrder(primaryMarket, BUY, Price.of(getHypotheticalPrivateValue(qty, 1, BUY).intValue() + Integer.MAX_VALUE / 2), 1);
	}

	@Override
	public void liquidateAtPrice(Price price) {
		postStat(Stats.MAX_EFF_POSITION + getPosition(), 1);
		super.liquidateAtPrice(price);
	}

	private static final long serialVersionUID = -8915874536659571239L;
}
