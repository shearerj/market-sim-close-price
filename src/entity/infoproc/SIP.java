package entity.infoproc;

import static logger.Logger.Level.INFO;
import static data.Observations.BUS;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import data.Observations.NBBOStatistic;

import logger.Logger;
import activity.Activity;
import entity.market.Market;
import entity.market.MarketTime;
import entity.market.Price;
import entity.market.Quote;
import entity.market.Transaction;
import event.TimeStamp;

/**
 * Class that updates pertinent information for the system. Generally used for creating NBBO update
 * events. Serves the purpose of the Security Information Processor in Regulation NMS. Is the NBBO
 * for one market model
 * 
 * @author ewah
 */
public class SIP extends IP {

	private static final long serialVersionUID = -4600049787044894823L;
	
	protected final Map<Market, Quote> marketQuotes;
	protected final Map<Market, MarketTime> quoteTimes;
	protected final List<Transaction> transactions;
	protected BestBidAsk nbbo;

	public SIP(TimeStamp latency) {
		super(latency);
		this.marketQuotes = Maps.newHashMap();
		this.quoteTimes = Maps.newHashMap();
		this.transactions = Lists.newArrayList();
		this.nbbo = new BestBidAsk(null, null, 0, null, null, 0);
	}

	public Iterable<Activity> processQuote(Market market, MarketTime quoteTime, Quote quote,
			List<Transaction> newTransactions, TimeStamp currentTime) {
		MarketTime lastTime = quoteTimes.get(market);
		// If we get a stale quote, ignore it.
		if (lastTime != null && lastTime.compareTo(quoteTime) > 0)
			return ImmutableList.of();

		marketQuotes.put(market, quote);
		quoteTimes.put(market, quoteTime);
		transactions.addAll(newTransactions);
		
		Logger.log(INFO, market + " -> " + this + " quote " + quote);

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

	public BestBidAsk getNBBO() {
		return nbbo;
	}
	
	public List<Transaction> getTransactions() {
		return ImmutableList.copyOf(transactions);
	}

	public String toString() {
		return "SIP";
	}
}