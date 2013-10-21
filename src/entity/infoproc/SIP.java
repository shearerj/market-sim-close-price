package entity.infoproc;

import static logger.Logger.Level.INFO;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import data.TimeSeries;

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

	protected final TimeSeries nbboSpreads;

	public SIP(TimeStamp latency) {
		super(latency);
		this.marketQuotes = Maps.newHashMap();
		this.quoteTimes = Maps.newHashMap();
		this.transactions = Lists.newArrayList();
		this.nbbo = new BestBidAsk(null, null, null, null);

		this.nbboSpreads = new TimeSeries();
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
		Market bestBidMkt = null, bestAskMkt = null;

		for (Entry<Market, Quote> marketQuote : marketQuotes.entrySet()) {
			Quote q = marketQuote.getValue();
			if (q.getAskPrice() != null && q.getAskPrice().lessThan(bestAsk)) {
				bestAsk = q.getAskPrice();
				bestAskMkt = marketQuote.getKey();
			}
			if (q.getBidPrice() != null && q.getBidPrice().greaterThan(bestBid)) {
				bestBid = q.getBidPrice();
				bestBidMkt = marketQuote.getKey();
			}
		}

		// NBBO Fix XXX should figure out best way to handle price discrepancies
		if (bestBid != null && bestAsk != null && bestBid.greaterThan(bestAsk)) {
			int mid = (bestBid.intValue() + bestAsk.intValue()) / 2;
			// Removed the tick increment from old fix, mainly for ease of use. What would the
			// appropriate tick size for the SIP be anyways, in terms of this fix? Seems like
			// just the midpoint will work fine.
			bestBid = new Price(mid);
			bestAsk = new Price(mid);
		}

		nbbo = new BestBidAsk(bestBidMkt, bestBid, bestAskMkt, bestAsk);
		nbboSpreads.add((int) currentTime.getInTicks(), nbbo.getSpread());
		return ImmutableList.of();
	}

	public BestBidAsk getNBBO() {
		return nbbo;
	}
	
	public List<Transaction> getTransactions() {
		return ImmutableList.copyOf(transactions);
	}

	public TimeSeries getNBBOSpreads() {
		return nbboSpreads;
	}

	public String toString() {
		return "SIP";
	}
}