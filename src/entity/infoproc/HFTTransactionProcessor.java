package entity.infoproc;

import java.util.List;

import activity.Activity;
import activity.AgentStrategy;

import com.google.common.collect.ImmutableList;

import entity.agent.HFTAgent;
import entity.market.Market;
import entity.market.Transaction;
import event.TimeStamp;

public class HFTTransactionProcessor extends TransactionProcessor {

	private static final long serialVersionUID = 2897824399529496851L;
	
	protected final HFTAgent hftAgent;

	public HFTTransactionProcessor(TimeStamp latency, Market mkt, HFTAgent hftAgent) {
		super(latency, mkt);
		this.hftAgent = hftAgent;
	}

	@Override
	public Iterable<? extends Activity> processTransaction(Market market, 
			List<Transaction> newTransactions, TimeStamp currentTime) {
		return ImmutableList.<Activity> builder().addAll(
				super.processTransaction(market, newTransactions, currentTime)).add(
				new AgentStrategy(hftAgent, TimeStamp.IMMEDIATE)).build();
	}

	@Override
	public String toString() {
		return "(HFTIP " + id + " in " + associatedMarket + " for " + hftAgent + ")"; 
	}

}
