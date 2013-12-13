package entity.infoproc;

import java.util.List;

import activity.Activity;
import entity.agent.HFTAgent;
import entity.market.Market;
import entity.market.Order;
import entity.market.Transaction;
import event.TimeStamp;

public class MarketTransactionProcessor extends AbstractTransactionProcessor {

	private static final long serialVersionUID = 4550103178485854572L;
	
	public MarketTransactionProcessor(TimeStamp latency, Market market) {
		super(latency, market);
	}

	@Override
	public Iterable<? extends Activity> processTransactions(Market market,
			List<Transaction> newTransactions, TimeStamp currentTime) {
		Iterable<? extends Activity> superActs = super.processTransactions(market, newTransactions, currentTime);
		
		for (Transaction trans : newTransactions) {
			Order buy = trans.getBuyBid(), sell = trans.getSellBid();
			// XXX Right now every agent but HFT's should function this way
			// (e.g. Market makers). Maybe this will change
			if (!(buy.getAgent() instanceof HFTAgent))
				updateAgent(buy.getAgent(), buy, trans);
			if (!(sell.getAgent() instanceof HFTAgent))
				updateAgent(sell.getAgent(), sell, trans);
		}
		return superActs;
	}

	public String toString() {
		return "(TransactionProcessor " + id + " in " + associatedMarket + ")";
	}

}
