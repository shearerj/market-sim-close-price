package market;

import java.util.List;

import event.TimeStamp;
import fourheap.Transaction;

public interface ClearingRule {

	public List<Price> pricing(List<Transaction<Price, TimeStamp>> transactions);
	
}
