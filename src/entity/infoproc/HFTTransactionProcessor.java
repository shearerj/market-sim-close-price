package entity.infoproc;

import entity.agent.HFTAgent;
import entity.market.Market;
import event.TimeStamp;

public class HFTTransactionProcessor extends SMTransactionProcessor {

	private static final long serialVersionUID = 2897824399529496851L;
	
	protected final HFTAgent hftAgent;

	public HFTTransactionProcessor(TimeStamp latency, Market mkt, HFTAgent hftAgent) {
		super(latency, mkt);
		this.hftAgent = hftAgent;
	}

	/*
	 * XXX may need to have HFT agent who acts immediately upon getting new
	 * transaction info. I think we should just mandate / have mandated that if
	 * Transactions are created, the quote will be updated. Thus, we only need
	 * to execute strategy during quote update. With the separation of
	 * transaction and quote processors, it is very clear that transactions are
	 * processed first, and then quotes.
	 */
//	@Override
//	public Iterable<? extends Activity> processTransactions(Market market, 
//			List<Transaction> newTransactions, TimeStamp currentTime) {
//		return super.processTransactions(market, newTransactions, currentTime);
//	}

	@Override
	public String toString() {
		return "(HFTTransactionProcessor " + id + " in " + associatedMarket + " for " + hftAgent + ")"; 
	}

}
