package entity.agent.strategy;

import static com.google.common.base.Preconditions.checkArgument;
import static fourheap.Order.OrderType.BUY;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.Range;

import entity.agent.OrderRecord;
import entity.market.Price;
import entity.market.Quote;

/**
 * 
 * XXX If profit "shading" profit is greater than 1 e.g. the current quote would
 * already transact with the price, this will not change the price in case the
 * quote is out of date.
 * 
 * XXX on multi-quantity orders, if the quote is not for the total quantity
 * requested, then this will not opportunistically shade.
 * 
 * @author erik
 * 
 */
public class GreedyShader implements Function<OrderRecord, OrderRecord> {

	private final Range<Double> shadeRange;
	private final LimitPriceEstimator estimator;
	
	private GreedyShader(LimitPriceEstimator estimator, double acceptableProfitThreshold) {
		checkArgument(Range.closed(0d, 1d).contains(acceptableProfitThreshold));
		this.estimator = estimator;
		this.shadeRange = Range.closed(acceptableProfitThreshold, 1d);
	}
	
	public static GreedyShader create(LimitPriceEstimator estimator, double acceptableProfitThreshold) {
		return new GreedyShader(estimator, acceptableProfitThreshold);
	}
	
	@Override
	public OrderRecord apply(OrderRecord order) {
		Quote quote = order.getCurrentMarket().getQuote();
		Optional<Price> tradingAgainst = order.getOrderType() == BUY ? quote.getAskPrice() : quote.getBidPrice();
		int tradingQuantity = order.getOrderType() == BUY ? quote.getAskQuantity() : quote.getBidQuantity();
		
		if (tradingQuantity < order.getQuantity() || !tradingAgainst.isPresent()) // The second check should be redundant, but just to be safe
			return order; // Nothing or not enough to transact against
		
		Price limit = estimator.getLimitPrice(order.getOrderType(), order.getQuantity());
		checkArgument((limit.doubleValue() - order.getPrice().doubleValue()) * order.getOrderType().sign() >= 0, "Can't submit an order that would incur a loss");
				
		double profitFraction = (limit.doubleValue() - order.getPrice().doubleValue()) / (limit.doubleValue() - tradingAgainst.get().doubleValue());
		if (shadeRange.contains(profitFraction))
			return OrderRecord.create(order.getCurrentMarket(), order.getCreatedTime(), order.getOrderType(), tradingAgainst.get(), order.getQuantity());
		else return order;
		// FIXME postStat(Stats.ZIRP_GREEDY, 0 or 1);
	}

}
