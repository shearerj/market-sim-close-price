package entity.agent.strategy;

import systemmanager.Keys.RMax;
import systemmanager.Keys.RMin;
import utils.Rand;
import data.Props;
import entity.agent.OrderRecord;
import entity.market.Market.MarketView;
import entity.market.Price;
import event.Timeline;
import fourheap.Order.OrderType;

public class ZIStrategy implements BackgroundStrategy {
	
	private final Timeline timeline;
	private final MarketView market;
	private final LimitPriceEstimator estimator;
	private final int bidRangeMin, bidRangeMax; // Shading
	private final Rand rand;

	/*
	 * XXX Could make this class take a fundamental estimator and a private
	 * value instead of an agent. Then plugging in a different fundamental
	 * estimate gives a different strategy
	 */
	private ZIStrategy(Timeline timeline, MarketView market, LimitPriceEstimator estimator, int bidRangeMin, int bidRangeMax, Rand rand) {
		this.timeline = timeline;
		this.market = market;
		this.estimator = estimator;
		this.bidRangeMin = bidRangeMin;
		this.bidRangeMax = bidRangeMax;
		this.rand = rand;
	}
	
	public static ZIStrategy create(Timeline timeline, MarketView market, LimitPriceEstimator estimator, int bidRangeMin, int bidRangeMax, Rand rand) {
		return new ZIStrategy(timeline, market, estimator, bidRangeMin, bidRangeMax, rand);
	}
	
	public static ZIStrategy create(Timeline timeline, MarketView market, LimitPriceEstimator estimator, Props props, Rand rand) {
		return create(timeline, market, estimator, props.get(RMin.class), props.get(RMax.class), rand);
	}

	@Override
	public OrderRecord getOrder(OrderType buyOrSell, int quantity) {
		Price limit = estimator.getLimitPrice(buyOrSell, quantity);
		if (limit.lessThan(Price.ZERO))
			return null; // Does not want to submit an order for this position / quantity
		Price price = Price.of((limit.doubleValue() - buyOrSell.sign() * rand.nextUniform(bidRangeMin, bidRangeMax))).nonnegative();
		return OrderRecord.create(market, timeline.getCurrentTime(), buyOrSell, price, quantity);
	}

}
