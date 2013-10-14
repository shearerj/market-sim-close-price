package entity.market.clearingrule;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

import entity.market.Price;
import event.TimeStamp;
import fourheap.MatchedOrders;

public class EarliestPriceClear implements ClearingRule {

	private static final long serialVersionUID = -6417178198266057261L;

	protected final int tickSize;
	
	public EarliestPriceClear(int tickSize) {
		this.tickSize = tickSize;
	}
	
	@Override
	public Map<MatchedOrders<Price, TimeStamp>, Price> pricing(
			Iterable<MatchedOrders<Price, TimeStamp>> matchedOrders) {
		Builder<MatchedOrders<Price, TimeStamp>, Price> prices = ImmutableMap.builder();
		for (MatchedOrders<Price, TimeStamp> match : matchedOrders)
			prices.put(match, match.getBuy().getSubmitTime().before(match.getSell().getSubmitTime())
					? match.getBuy().getPrice().quantize(tickSize)
					: match.getSell().getPrice().quantize(tickSize));
		// TODO will always pick seller's price when at the same time, is this correct? Probably not?
		return prices.build();
	}

}
