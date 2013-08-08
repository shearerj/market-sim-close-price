package clearingrule;

import static utils.Compare.min;
import static utils.Compare.max;

import java.util.ArrayList;
import java.util.List;


import entity.market.Price;
import event.TimeStamp;
import fourheap.Transaction;

public class UniformPriceClear implements ClearingRule {

	protected final double ratio;
	protected final int tickSize;
	
	public UniformPriceClear(double ratio, int tickSize) {
		this.ratio = ratio;
		this.tickSize = tickSize;
	}

	@Override
	public List<Price> pricing(List<Transaction<Price, TimeStamp>> transactions) {
		Price minBuy = null, maxSell = null;
		for (Transaction<Price, TimeStamp> trans : transactions) {
			minBuy = min(minBuy, trans.getBuy().getPrice());
			maxSell = max(maxSell, trans.getSell().getPrice());
		}
		Price clearPrice = new Price(
				(int) (minBuy.getInTicks() * ratio + maxSell.getInTicks()
						* (1 - ratio))).quantize(tickSize);
		List<Price> prices = new ArrayList<Price>(transactions.size());
		for (int i = 0; i < transactions.size(); i++)
			prices.add(clearPrice);
		return prices;
	}

}
