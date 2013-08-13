package clearingrule;

import java.io.Serializable;
import java.util.List;


import entity.market.Price;
import event.TimeStamp;
import fourheap.Transaction;

public interface ClearingRule extends Serializable {

	public List<Price> pricing(List<Transaction<Price, TimeStamp>> transactions);
	
}
