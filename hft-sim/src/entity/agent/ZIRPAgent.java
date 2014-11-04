package entity.agent;

import static fourheap.Order.OrderType.BUY;
import static fourheap.Order.OrderType.SELL;
import static logger.Log.Level.INFO;

import java.util.Random;

import logger.Log;
import data.FundamentalValue;
import data.Props;
import data.Stats;
import entity.market.Market;
import entity.sip.MarketInfo;
import event.Timeline;
import fourheap.Order.OrderType;

public final class ZIRPAgent extends BackgroundAgent {
	
	private static final long serialVersionUID = -8805640643365079141L;
	
	protected ZIRPAgent(int id, Stats stats, Timeline timeline, Log log, Random rand, MarketInfo sip, FundamentalValue fundamental,
			Market market, Props props) {
		super(id, stats, timeline, log, rand, sip, fundamental, market, props);
	}
	
	public static ZIRPAgent create(int id, Stats stats, Timeline timeline, Log log, Random rand, MarketInfo sip, FundamentalValue fundamental,
			Market market, Props props) {
		return new ZIRPAgent(id, stats, timeline, log, rand, sip, fundamental, market, props);
	}

	@Override
	protected void agentStrategy() {
		super.agentStrategy();
		
		// 50% chance of being either long or short
		OrderType orderType = rand.nextBoolean() ? BUY : SELL;
		log(INFO, "%s Submit %s order", this, orderType);
		executeZIRPStrategy(orderType, 1);
	}
}
