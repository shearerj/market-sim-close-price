package entity.agent;

import static logger.Log.Level.INFO;
import logger.Log;
import systemmanager.Keys.Thresh;
import utils.Rand;
import data.FundamentalValue;
import data.Props;
import data.Stats;
import entity.agent.strategy.BackgroundStrategy;
import entity.agent.strategy.GreedyShader;
import entity.agent.strategy.LimitPriceEstimator;
import entity.agent.strategy.OptimalLimitPriceEstimator;
import entity.agent.strategy.ZIStrategy;
import entity.market.Market;
import entity.sip.MarketInfo;
import event.Timeline;
import fourheap.Order.OrderType;

public final class ZIRPAgent extends BackgroundAgent {
	
	private final BackgroundStrategy strategy;
	private final GreedyShader shader;

	protected ZIRPAgent(int id, Stats stats, Timeline timeline, Log log, Rand rand, MarketInfo sip, FundamentalValue fundamental,
			Market market, Props props) {
		super(id, stats, timeline, log, rand, sip, fundamental, market, props);
		LimitPriceEstimator estimator = OptimalLimitPriceEstimator.create(this, getFundamentalValueView(), timeline, props);
		strategy = ZIStrategy.create(timeline, primaryMarket, estimator, props, rand);
		shader = GreedyShader.create(estimator, props.get(Thresh.class));
	}
	
	public static ZIRPAgent create(int id, Stats stats, Timeline timeline, Log log, Rand rand, MarketInfo sip, FundamentalValue fundamental,
			Market market, Props props) {
		return new ZIRPAgent(id, stats, timeline, log, rand, sip, fundamental, market, props);
	}

	@Override
	protected void agentStrategy() {
		super.agentStrategy();
		
		// 50% chance of being either long or short
		OrderType orderType = rand.nextElement(OrderType.values());
		log(INFO, "%s Submit %s order", this, orderType);
		
		OrderRecord original = strategy.getOrder(orderType, 1);
		if (original == null)
			return; // No order
		
		OrderRecord shaded = shader.apply(original);
		postStat(Stats.ZIRP_GREEDY, shaded.equals(original) ? 0 : 1); // Determine if shader changed order
		
		submitNMSOrder(shaded);
	}

	private static final long serialVersionUID = -8805640643365079141L;
}
