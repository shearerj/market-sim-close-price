package entity.sip;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;

import logger.Log;
import utils.Orderings;
import utils.Rand;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;

import data.Stats;
import entity.Entity;
import entity.market.Market;
import entity.market.Market.MarketView;
import entity.market.Price;
import entity.market.Quote;
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
	
	// FIXME Log spread, and figure out how... LA style?

	private static final Ordering<Optional<Price>> bidOrder = Orderings.optionalOrdering(Ordering.<Price> natural());
	private static final Ordering<Optional<Price>> askOrder = Orderings.optionalOrdering(Ordering.<Price> natural().reverse());
	
	private final TimeStamp latency;
	private final Collection<MarketView> views;

	protected SIP(Stats stats, Timeline timeline, Log log, Rand rand, TimeStamp latency) {
		super(0, stats, timeline, log, rand);
		this.latency = checkNotNull(latency);
		this.views = Lists.newArrayList();
	}
	
	public static SIP create(Stats stats, Timeline timeline, Log log, Rand rand, TimeStamp latency) {
		return new SIP(stats, timeline, log, rand, latency);
	}

	@Override
	public BestBidAsk getNBBO() {
		Optional<Market> bestBidMarket = Optional.absent(), bestAskMarket = Optional.absent();
		Optional<Price> bestBidPrice = Optional.absent(), bestAskPrice = Optional.absent();
		int bestAskQuantity = 0, bestBidQuantity = 0;
		for (MarketView market : views) {
			Quote quote = market.getQuote();
			if (bidOrder.compare(quote.getBidPrice(), bestBidPrice) > 0) {
				bestBidPrice = quote.getBidPrice();
				bestBidMarket = Optional.of(quote.getMarket());
				bestBidQuantity = quote.getBidQuantity();
			}
			if (askOrder.compare(quote.getAskPrice(), bestAskPrice) > 0) {
				bestAskPrice = quote.getAskPrice();
				bestAskMarket = Optional.of(quote.getMarket());
				bestAskQuantity = quote.getAskQuantity();
			}
		}
		return BestBidAsk.create(bestBidMarket, bestBidPrice, bestBidQuantity, bestAskMarket, bestAskPrice, bestAskQuantity);
	}
	
	@Override
	public void processMarket(Market market) {
		views.add(market.getView(latency));
	}

	@Override
	public String toString() {
		return "SIP";
	}
	
	private static final long serialVersionUID = -4600049787044894823L;

}