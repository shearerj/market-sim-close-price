package entity.sip;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;
import java.util.Map.Entry;

import logger.Log;
import utils.Maps2;
import utils.Rand;

import com.google.common.base.Optional;
import com.google.common.base.Suppliers;
import com.google.common.collect.Maps;

import data.Stats;
import entity.Entity;
import entity.market.Market;
import entity.market.Price;
import entity.market.Quote;
import event.Activity;
import event.TimeStamp;
import event.Timeline;

/**
 * Class that updates pertinent information for the system. Generally used for creating NBBO update
 * events. Serves the purpose of the Security Information Processor in Regulation NMS. Is the NBBO
 * for one market model. Also returns transactions, at the same latency
 * as the NBBO updates.
 * 
 * @author ewah
 */
public class SIP extends Entity implements MarketInfo {
	
	private final TimeStamp latency;
	private final Map<Market, Quote> quotes;
	private BestBidAsk nbbo;

	private SIP(Stats stats, Timeline timeline, Log log, Rand rand, TimeStamp latency) {
		super(0, stats, timeline, log, rand);
		this.latency = checkNotNull(latency);
		this.quotes = Maps2.addDefault(Maps.<Market, Quote> newHashMap(), Suppliers.ofInstance(Quote.empty()));
		this.nbbo = BestBidAsk.empty();
		postTimedStat(Stats.NBBO_SPREAD, nbbo.getSpread());
	}
	
	public static SIP create(Stats stats, Timeline timeline, Log log, Rand rand, TimeStamp latency) {
		return new SIP(stats, timeline, log, rand, latency);
	}

	@Override
	public BestBidAsk getNBBO() {
		return nbbo;
	}
	
	/** Actually update NBBO with delayed information */
	private void quoteSubmitted(Market market, Quote quote) {
		if (quote.getQuoteTime().compareTo(quotes.get(market).getQuoteTime()) < 0)
			return; // Out of date;
		quotes.put(market, quote);
		
		Optional<Market> bidMarket = nbbo.getBestBidMarket(), askMarket = nbbo.getBestAskMarket();
		Optional<Price> bid = nbbo.getBestBid(), ask = nbbo.getBestAsk();
		int bidQuantity = nbbo.getBestBidQuantity(), askQuantity = nbbo.getBestAskQuantity();
		
		if (quote.getAskPrice().isPresent() && quote.getAskPrice().get().lessThanEqual(nbbo.getBestAsk().or(Price.INF))) {
			// Price improved, so can do quick update
			ask = quote.getAskPrice();
			askMarket = Optional.of(market);
			askQuantity = quote.getAskQuantity();
		} else if (nbbo.getBestAskMarket().asSet().contains(market)) {
			// Quote got worse, need to do full scan of markets
			ask = Optional.absent();
			for (Entry<Market, Quote> e : quotes.entrySet()) {
				if (e.getValue().getAskPrice().or(Price.INF).lessThan(ask.or(Price.INF))) {
					ask = e.getValue().getAskPrice();
					askMarket = Optional.of(e.getKey());
					askQuantity = e.getValue().getAskQuantity();
				}
			}
		} // A worse market got worse, and so nothing changes
		
		if (quote.getBidPrice().isPresent() && quote.getBidPrice().get().greaterThanEqual(nbbo.getBestBid().or(Price.NEG_INF))) {
			// Price improved, so can do quick update
			bid = quote.getBidPrice();
			bidMarket = Optional.of(market);
			bidQuantity = quote.getBidQuantity();
		} else if (nbbo.getBestBidMarket().asSet().contains(market)) {
			// Quote got worse, need to do full scan of markets
			bid = Optional.absent();
			for (Entry<Market, Quote> e : quotes.entrySet()) {
				if (e.getValue().getBidPrice().or(Price.NEG_INF).greaterThan(bid.or(Price.NEG_INF))) {
					bid = e.getValue().getBidPrice();
					bidMarket = Optional.of(e.getKey());
					bidQuantity = e.getValue().getBidQuantity();
				}
			}
		} // A worse market got worse, and so nothing changes
		
		nbbo = BestBidAsk.create(bidMarket, bid, bidQuantity, askMarket, ask, askQuantity);
		postTimedStat(Stats.NBBO_SPREAD, nbbo.getSpread());
	}
	
	@Override
	public void quoteSubmit(final Market market, final Quote quote) {
		scheduleActivityIn(latency, new Activity() {
			@Override public void execute() { quoteSubmitted(market, quote); }
		});
	}

	@Override
	public String toString() {
		return "SIP";
	}
	
	@Override
	public TimeStamp getLatency() {
		return latency;
	}

	private static final long serialVersionUID = -4600049787044894823L;

}