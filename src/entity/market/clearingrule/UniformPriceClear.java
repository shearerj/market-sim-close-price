package entity.market.clearingrule;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.Iterables;
import com.google.common.collect.Ordering;

import entity.market.Price;
import event.TimeStamp;
import fourheap.MatchedOrders;

public class UniformPriceClear implements ClearingRule {

	private static final long serialVersionUID = -342335313880137387L;
	protected static final Ordering<Price> ord = Ordering.natural();
	
	protected final double ratio;
	protected final int tickSize;
	
	public UniformPriceClear(double ratio, int tickSize) {
		this.ratio = ratio;
		this.tickSize = tickSize;
	}

	@Override
	public Map<MatchedOrders<Price, TimeStamp>, Price> pricing(
			Iterable<MatchedOrders<Price, TimeStamp>> transactions) {
		if (Iterables.isEmpty(transactions)) return ImmutableMap.of();

		Price minBuy = Iterables.getFirst(transactions, null).getBuy().getPrice();
		Price maxSell = Iterables.getFirst(transactions, null).getSell().getPrice();
		for (MatchedOrders<Price, TimeStamp> trans : transactions) {
			minBuy = ord.min(minBuy, trans.getBuy().getPrice());
			maxSell = ord.max(maxSell, trans.getSell().getPrice());
		}
		
		Price clearPrice = new Price(
				(int) (minBuy.getInTicks() * ratio + maxSell.getInTicks()
						* (1 - ratio))).quantize(tickSize);
		
		Builder<MatchedOrders<Price, TimeStamp>, Price> prices = ImmutableMap.builder();
		for (MatchedOrders<Price, TimeStamp> trans : transactions)
			prices.put(trans, clearPrice);
		return prices.build();
	}

}
