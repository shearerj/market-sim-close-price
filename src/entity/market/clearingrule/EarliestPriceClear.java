package entity.market.clearingrule;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

import entity.market.Price;
import event.TimeStamp;
import fourheap.Transaction;

public class EarliestPriceClear implements ClearingRule {

	private static final long serialVersionUID = -6417178198266057261L;

	@Override
	public Map<Transaction<Price, TimeStamp>, Price> pricing(
			Iterable<Transaction<Price, TimeStamp>> transactions) {
		Builder<Transaction<Price, TimeStamp>, Price> prices = ImmutableMap.builder();
		for (Transaction<Price, TimeStamp> trans : transactions)
			prices.put(trans, trans.getBuy().getSubmitTime().before(trans.getSell().getSubmitTime())
					? trans.getBuy().getPrice()
					: trans.getSell().getPrice());
		return prices.build();
	}

}
