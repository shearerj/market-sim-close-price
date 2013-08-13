package clearingrule;

import java.util.ArrayList;
import java.util.List;


import entity.market.Price;
import event.TimeStamp;
import fourheap.Transaction;

public class EarliestPriceClear implements ClearingRule {

	private static final long serialVersionUID = -6417178198266057261L;

	@Override
	public List<Price> pricing(List<Transaction<Price, TimeStamp>> transactions) {
		List<Price> prices = new ArrayList<Price>(transactions.size());
		for (Transaction<Price, TimeStamp> trans : transactions)
			prices.add(trans.getBuy().getSubmitTime().before(trans.getSell().getSubmitTime())
					? trans.getBuy().getPrice()
					: trans.getSell().getPrice());
		return prices;
	}

}
