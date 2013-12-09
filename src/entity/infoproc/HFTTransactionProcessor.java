package entity.infoproc;

import entity.agent.HFTAgent;
import entity.market.Market;
import event.TimeStamp;

public class HFTTransactionProcessor extends TransactionProcessor {

	private static final long serialVersionUID = 2897824399529496851L;
	
	protected final HFTAgent hftAgent;

	public HFTTransactionProcessor(TimeStamp latency, Market mkt, HFTAgent hftAgent) {
		super(latency, mkt);
		this.hftAgent = hftAgent;
	}

	// XXX may need to have HFT agent who acts immediately upon getting new transaction info
//	@Override
//	public Iterable<? extends Activity> processTransaction(Market market, 
//			List<Transaction> newTransactions, TimeStamp currentTime) {
//		return ImmutableList.<Activity> builder().addAll(
//				super.processTransaction(market, newTransactions, currentTime)).add(
//				new AgentStrategy(hftAgent, TimeStamp.IMMEDIATE)).build();
//	}

	@Override
	public String toString() {
		return "(HFTTransactionProcessor " + id + " in " + associatedMarket + " for " + hftAgent + ")"; 
	}

}
