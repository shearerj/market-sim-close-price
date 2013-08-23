package entity.market.clearingrule;

import java.io.Serializable;
import java.util.Map;


import entity.market.Price;
import event.TimeStamp;
import fourheap.Transaction;

public interface ClearingRule extends Serializable {

	public Map<Transaction<Price, TimeStamp>, Price> pricing(Iterable<Transaction<Price, TimeStamp>> transactions);
	
}
