package edu.umich.srg.marketsim.market;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.function.Function;

import com.google.common.collect.Maps;

import edu.umich.srg.egtaonline.spec.Spec;
import edu.umich.srg.fourheap.MatchedOrders;
import edu.umich.srg.fourheap.Order;
import edu.umich.srg.fourheap.Order.OrderType;
import edu.umich.srg.marketsim.Price;
import edu.umich.srg.marketsim.Sim;

public class CDAMarket extends AbstractMarket {
	
	private static final Function<Collection<MatchedOrders<Price>>, Iterable<Entry<MatchedOrders<Price>, Price>>> cdaPricing = matches -> {
		Iterator<MatchedOrders<Price>> it = matches.iterator();
		return () -> new  Iterator<Entry<MatchedOrders<Price>, Price>>() {

			@Override
			public boolean hasNext() {
				return it.hasNext();
			}

			@Override
			public Entry<MatchedOrders<Price>, Price> next() {
				MatchedOrders<Price> match = it.next();
				// less than is okay here, because two orders will never have the same time
				return Maps.immutableEntry(match, match.getBuy().getSubmitTime() < match.getSell().getSubmitTime()
						? match.getBuy().getPrice() : match.getSell().getPrice());
			}
			
		};
	};

	private CDAMarket(Sim sim) {
		super(sim, cdaPricing);
	}
	
	public static CDAMarket create(Sim sim) {
		return new CDAMarket(sim);
	}
	
	public static CDAMarket createFromSpec(Sim sim, Spec spec) {
		return new CDAMarket(sim);
	}

	@Override
	Order<Price> submitOrder(AbstractMarketView submitter, OrderType buyOrSell, Price price, int quantity) {
		Order<Price> order = super.submitOrder(submitter, buyOrSell, price, quantity);
		clear();
		return order;
	}

	@Override
	void withdrawOrder(Order<Price> order, int quantity) {
		super.withdrawOrder(order, quantity);
		updateQuote();
	}

	@Override
	void clear() {
		super.clear();
		updateQuote();
	}

	private static final long serialVersionUID = 2207946707270663436L;

}
