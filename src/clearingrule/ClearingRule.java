package clearingrule;

import java.util.List;


import entity.market.Price;
import event.TimeStamp;
import fourheap.Transaction;

public interface ClearingRule {

	public List<Price> pricing(List<Transaction<Price, TimeStamp>> transactions);
	
}
