package clearingrule;

import static utils.Compare.min;
import static utils.Compare.max;

import java.util.ArrayList;
import java.util.List;


import entity.market.Price;
import event.TimeStamp;
import fourheap.Transaction;

public class UniformPriceClear implements ClearingRule {

	double ratio;
	
	public UniformPriceClear(double ratio) {
		this.ratio = ratio;
	}

	@Override
	// FIXME ticksize!
	public List<Price> pricing(List<Transaction<Price, TimeStamp>> transactions) {
		Price minBuy = null, maxSell = null;
		for (Transaction<Price, TimeStamp> trans : transactions) {
			minBuy = min(minBuy, trans.getBuy().getPrice());
			maxSell = max(maxSell, trans.getSell().getPrice());
		}
		Price clearPrice = new Price((int) (minBuy.getPrice() * ratio + maxSell.getPrice() * (1 - ratio)));
		List<Price> prices = new ArrayList<Price>(transactions.size());
		for (int i = 0; i < transactions.size(); i++)
			prices.add(clearPrice);
		return prices;
	}

}
