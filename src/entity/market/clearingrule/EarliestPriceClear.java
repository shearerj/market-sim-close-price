package entity.market.clearingrule;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

import entity.market.MarketTime;
import entity.market.Price;
import fourheap.MatchedOrders;

public class EarliestPriceClear implements ClearingRule {

	private static final long serialVersionUID = -6417178198266057261L;

	protected final int tickSize;
	
	public EarliestPriceClear(int tickSize) {
		this.tickSize = tickSize;
	}
	
	@Override
	public Map<MatchedOrders<Price, MarketTime>, Price> pricing(
			Iterable<MatchedOrders<Price, MarketTime>> matchedOrders) {
		Builder<MatchedOrders<Price, MarketTime>, Price> prices = ImmutableMap.builder();
		for (MatchedOrders<Price, MarketTime> match : matchedOrders)
			prices.put(match, match.getBuy().getSubmitTime().compareTo(match.getSell().getSubmitTime()) < 0
					? match.getBuy().getPrice().quantize(tickSize)
					: match.getSell().getPrice().quantize(tickSize));
		return prices.build();
	}

}
