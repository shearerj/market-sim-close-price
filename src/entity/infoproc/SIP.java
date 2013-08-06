package entity.infoproc;

import static logger.Logger.Level.INFO;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import data.TimeSeries;

import logger.Logger;
import activity.Activity;
import entity.market.Market;
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

	protected final int modelID; // for logging only
	protected final Map<Market, Quote> marketQuotes; // hashed by market
	protected final List<Transaction> transactions;
	protected BestBidAsk nbbo;

	protected final TimeSeries nbboSpreads;

	public SIP(int ID, int modelID, TimeStamp latency) {
		super(ID, latency);
		this.modelID = modelID;
		this.marketQuotes = new HashMap<Market, Quote>();
		this.transactions = new ArrayList<Transaction>();
		this.nbbo = new BestBidAsk(null, null, null, null);

		this.nbboSpreads = new TimeSeries();
	}

	public Collection<Activity> processQuote(Market mkt, Quote quote,
			List<Transaction> newTransactions, TimeStamp currentTime) {
		marketQuotes.put(mkt, quote);
		transactions.addAll(newTransactions);
		
		Logger.log(INFO, currentTime + " | " + this + " | " + mkt + " "
				+ "ProcessQuote: " + quote);

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

		// NBBO Fix FIXME should figure out best way to handle price discrepancies
		if (bestBid != null && bestAsk != null && bestBid.greaterThan(bestAsk)) {
			int mid = (bestBid.getPrice() + bestAsk.getPrice()) / 2;
			// Removed the tick increment from old fix, mainly for ease of use. What would the
			// appropriate tick size for the SIP be anyways, in terms of this fix? Seems like
			// just the midpoint will work fine.
			bestBid = new Price(mid);
			bestAsk = new Price(mid);
		}

		nbbo = new BestBidAsk(bestBidMkt, bestBid, bestAskMkt, bestAsk);
		nbboSpreads.add(currentTime, nbbo.getSpread());
		return Collections.emptySet();
	}

	public BestBidAsk getNBBO() {
		return nbbo;
	}
	
	public List<Transaction> getTransactions() {
		return Collections.unmodifiableList(transactions);
	}

	public TimeSeries getNBBOSpreads() {
		return nbboSpreads;
	}

	public String toString() {
		return "SIP number " + id + ", model number " + modelID;
	}
}