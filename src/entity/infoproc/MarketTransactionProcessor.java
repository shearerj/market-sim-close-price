package entity.infoproc;

import entity.market.Market;
import event.TimeStamp;

public class MarketTransactionProcessor extends AbstractTransactionProcessor {

	private static final long serialVersionUID = 4550103178485854572L;
	
	public MarketTransactionProcessor(TimeStamp latency, Market market) {
		super(latency, market);
		// TODO Add transactions to backgroudn agents
	}

	public String toString() {
		return "(TransactionProcessor " + id + " in " + associatedMarket + ")";
	}

}
