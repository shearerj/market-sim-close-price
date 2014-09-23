package entity.infoproc;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import systemmanager.Simulation;
import utils.Orderings;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.Iterables;
import com.google.common.collect.Ordering;

import entity.Entity;
import entity.market.Market;
import entity.market.Market.MarketView;
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
public class SIP extends Entity {

	private static final long serialVersionUID = -4600049787044894823L;
	protected static final Ordering<Optional<Price>> bidOrder = Orderings.optionalOrdering(Ordering.<Price> natural());
	protected static final Ordering<Optional<Price>> askOrder = Orderings.optionalOrdering(Ordering.<Price> natural().reverse());
	
	protected final Collection<MarketView> views;

	protected SIP(Simulation sim, TimeStamp latency, Iterable<Market> markets) {
		super(0, sim);
		checkNotNull(latency);
		Builder<MarketView> builder = ImmutableList.builder();
		for (Market market : markets)
			builder.add(market.getView(latency));
		this.views = builder.build();
	}
	
	public static SIP create(Simulation sim, TimeStamp latency, Iterable<Market> markets) {
		return new SIP(sim, latency, markets);
	}

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
	
	public Iterable<Transaction> getTransactions() {
		return Iterables.unmodifiableIterable(Iterables.mergeSorted(Collections2.transform(views, new Function<MarketView, List<Transaction>>() {
			@Override public List<Transaction> apply(MarketView marketView) { return marketView.getTransactions(); }
		}), new Comparator<Transaction>() {
			@Override public int compare(Transaction arg0, Transaction arg1) {
				// FIXME may be reverse
				return arg0.getExecTime().compareTo(arg1.getExecTime()); }
		}));
	}

	@Override
	public String toString() {
		return "SIP";
	}

}