package entity.infoproc;

import static logger.Logger.log;
import static logger.Logger.Level.INFO;
import static data.Observations.BUS;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import data.Observations.NBBOStatistic;
import activity.Activity;
import activity.ProcessQuote;
import activity.ProcessTransactions;
import entity.Entity;
import entity.market.Market;
import entity.market.MarketTime;
import entity.market.Price;
import entity.market.Quote;
import entity.market.Transaction;
import event.TimeStamp;

/**
 * Class that updates pertinent information for the system. Generally used for creating NBBO update
 * events. Serves the purpose of the Security Information Processor in Regulation NMS. Is the NBBO
 * for one market model. Also returns transactions, at the same latency
 * as the NBBO updates.
 * 
 * @author ewah
 */
public class SIP extends Entity implements QuoteProcessor, TransactionProcessor {

	private static final long serialVersionUID = -4600049787044894823L;
	
	protected TimeStamp latency;
	protected final Map<Market, Quote> marketQuotes;
	protected final Map<Market, MarketTime> quoteTimes;
	protected final List<Transaction> transactions;
	protected BestBidAsk nbbo;

	public SIP(TimeStamp latency) {
		super(0);
		this.latency = checkNotNull(latency);
		this.marketQuotes = Maps.newHashMap();
		this.quoteTimes = Maps.newHashMap();
		this.transactions = Lists.newArrayList();
		this.nbbo = new BestBidAsk(null, null, 0, null, null, 0);
	}

	public BestBidAsk getNBBO() {
		return nbbo;
	}
	
	public List<Transaction> getTransactions() {
		return Collections.unmodifiableList(transactions);
	}

	@Override
	public String toString() {
		return "SIP";
	}

	@Override
	public Iterable<? extends Activity> sendToTransactionProcessor(Market market,
			List<Transaction> newTransactions, TimeStamp currentTime) {
		if (latency.equals(TimeStamp.IMMEDIATE)) {
			return ImmutableList.<Activity> builder().addAll(
					processTransactions(market, newTransactions, currentTime)).build();
		}
//		TimeStamp nextTime = latency.equals(TimeStamp.IMMEDIATE) ? TimeStamp.IMMEDIATE : currentTime.plus(latency); 
		return ImmutableList.of(new ProcessTransactions(this, market, 
				newTransactions, currentTime.plus(latency)));
	}

	@Override
	public Iterable<? extends Activity> processTransactions(Market market,
			List<Transaction> newTransactions, TimeStamp currentTime) {
		transactions.addAll(newTransactions);
		return ImmutableList.of();
	}

	// TODO This check shouldn't be necessary if we allow agents to immediately execute activities.
	@Override
	public Iterable<? extends Activity> sendToQuoteProcessor(Market market,
			MarketTime quoteTime, Quote quote, TimeStamp currentTime) {
//		TimeStamp nextTime = latency.equals(TimeStamp.IMMEDIATE) ? 
//		TimeStamp.IMMEDIATE : currentTime.plus(latency);
		if (latency.equals(TimeStamp.IMMEDIATE))
			return processQuote(market, quoteTime, quote, currentTime);
		else
			return ImmutableList.of(new ProcessQuote(this, market, quoteTime, quote, 
					currentTime.plus(latency)));
	}

	@Override
	public Iterable<? extends Activity> processQuote(Market market,
			MarketTime quoteTime, Quote quote, TimeStamp currentTime) {
		MarketTime lastTime = quoteTimes.get(market);
		// If we get a stale quote, ignore it.
		if (lastTime != null && lastTime.compareTo(quoteTime) > 0)
			return ImmutableList.of();

		marketQuotes.put(market, quote);
		quoteTimes.put(market, quoteTime);
		
		log(INFO, market + " -> " + this + " quote " + quote);

		Price bestBid = null, bestAsk = null;
		int bestBidQuantity = 0, bestAskQuantity = 0;
		Market bestBidMkt = null, bestAskMkt = null;

		for (Entry<Market, Quote> marketQuote : marketQuotes.entrySet()) {
			Quote q = marketQuote.getValue();
			if (q.getAskPrice() != null && q.getAskPrice().lessThan(bestAsk)) {
				bestAsk = q.getAskPrice();
				bestAskQuantity = q.getAskQuantity();
				bestAskMkt = marketQuote.getKey();
			}
			if (q.getBidPrice() != null && q.getBidPrice().greaterThan(bestBid)) {
				bestBid = q.getBidPrice();
				bestBidQuantity = q.getBidQuantity();
				bestBidMkt = marketQuote.getKey();
			}
		}

		nbbo = new BestBidAsk(bestBidMkt, bestBid, bestBidQuantity, 
				bestAskMkt, bestAsk, bestAskQuantity);
		BUS.post(new NBBOStatistic(nbbo.getSpread(), currentTime));
		return ImmutableList.of();
	}

	@Override
	public TimeStamp getLatency() {
		return latency;
	}

}