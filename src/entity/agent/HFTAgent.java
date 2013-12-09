package entity.agent;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;

import data.FundamentalValue;
import entity.infoproc.HFTQuoteProcessor;
import entity.infoproc.HFTTransactionProcessor;
import entity.infoproc.SIP;
import entity.market.Market;
import entity.market.Quote;
import entity.market.Transaction;
import event.TimeStamp;

/**
 * Abstract class for high-frequency traders. Creates the necessary information
 * processors and links them to the appropriate markets.
 * 
 * @author ewah
 */
public abstract class HFTAgent extends MMAgent {

	private static final long serialVersionUID = -1483633963238206201L;
	
	protected final Map<Market, HFTQuoteProcessor> quoteProcessors;
	protected final Map<Market, HFTTransactionProcessor> transactionProcessors;

	public HFTAgent(TimeStamp latency, TimeStamp arrivalTime,
			FundamentalValue fundamental, SIP sip, Collection<Market> markets, 
			Random rand, int tickSize) {
		this(latency, latency, arrivalTime, fundamental, sip, markets, rand, tickSize);
	}
	
	public HFTAgent(TimeStamp quoteLatency, TimeStamp transactionLatency, 
			TimeStamp arrivalTime, FundamentalValue fundamental, SIP sip, 
			Collection<Market> markets,	Random rand, int tickSize) {
		super(arrivalTime, fundamental, sip, markets, rand, tickSize);
		this.quoteProcessors = Maps.newHashMap();
		this.transactionProcessors = Maps.newHashMap();
		
		for (Market market : markets) {
			HFTTransactionProcessor tp = new HFTTransactionProcessor(transactionLatency,
					market, this);
			transactionProcessors.put(market, tp);
			market.addIP(tp);
			
			HFTQuoteProcessor qp = new HFTQuoteProcessor(quoteLatency, market, this);
			quoteProcessors.put(market, qp);
			market.addIP(qp);
		}
	}
	
	/**
	 * Get quote in the specified market.
	 * @param market
	 * @return
	 */
	public Quote getQuote(Market market) {
		checkNotNull(market);
		if (quoteProcessors.containsKey(market))
			return quoteProcessors.get(market).getQuote();
		return new Quote(null, null, 0, null, 0, TimeStamp.ZERO);
	}
	
	/**
	 * Get transactions in the specified market.
	 * @param market
	 * @return
	 */
	public List<Transaction> getTransactions(Market market) {
		checkNotNull(market);
		if (quoteProcessors.containsKey(market))
			return transactionProcessors.get(market).getTransactions();
		return ImmutableList.<Transaction> of();
	}
}
