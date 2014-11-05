package entity.market;

import static com.google.common.base.Preconditions.checkArgument;
import static fourheap.Order.OrderType.BUY;
import static fourheap.Order.OrderType.SELL;
import static logger.Log.Level.INFO;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import logger.Log;
import systemmanager.Keys.MarketLatency;
import utils.Iterables2;
import utils.Rand;

import com.google.common.base.Optional;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multiset;

import data.Props;
import data.Stats;
import entity.Entity;
import entity.View;
import entity.agent.Agent;
import entity.agent.Agent.AgentView;
import entity.agent.HFTAgent;
import entity.agent.HFTAgent.HFTAgentView;
import entity.agent.OrderRecord;
import entity.market.clearingrule.ClearingRule;
import entity.sip.BestBidAsk;
import entity.sip.MarketInfo;
import event.Activity;
import event.TimeStamp;
import event.Timeline;
import fourheap.FourHeap;
import fourheap.MatchedOrders;

/**
 * Base class for all markets. This class provides almost all market
 * functionality that one should need for creating a market.
 * 
 * A market needs two things to function appropriately. First is a latency. This
 * represents how long it takes background agents (with only one primary market)
 * to talk to the market. A latency of TimeStamp.IMMEDIATE makes this immediate
 * in terms of scheduling. The second thing they need is a ClearingRule. A
 * ClearingRule is a class that takes a set of MatchedOrders and assigns a price
 * to them. This facilitates the difference in pricing between a call market and
 * a CDA.
 * 
 * By default the only methods that schedules more activities is a Clear and a
 * quoteUpdate.
 * 
 * @author ewah
 * @author ebrink
 */
public abstract class Market extends Entity {
	
	private final MarketInfo sip;
	private final FourHeap<Price, MarketTime, Order> orderbook;
	private final ClearingRule clearingRule;
	private long marketTime; // keeps track of internal market actions

	private final Map<HFTAgentView, MarketView> notified;
	private final Collection<MarketView> views;
	private final MarketView primaryView; // Default View
	
	private Quote quote; // Current quote

	// Book keeping
	private final Map<OrderRecord, MarketView> routedOrders; // Maps all routed orders to the appropriate market
	private final Map<OrderRecord, Order> orderMapping; // Maps active records to their order object
	private final Multiset<Price> askPriceQuantity, bidPriceQuantity; // How many orders are at a specific price
	
	protected Market(int id, Stats stats, Timeline timeline, Log log, Rand rand, MarketInfo sip, ClearingRule clearingRule, Props props) {
		super(id, stats, timeline, log, rand);
		this.orderbook = FourHeap.<Price, MarketTime, Order> create();
		this.clearingRule = clearingRule;
		this.marketTime = 0;
		this.quote = new Quote(this, Optional.<Price> absent(), 0, Optional.<Price> absent(), 0, TimeStamp.ZERO);
		
		this.notified = Maps.newHashMap();
		this.views = Lists.newArrayList();
		this.primaryView = getView(props.get(MarketLatency.class));
		
		this.orderMapping = Maps.newHashMap();
		this.routedOrders = Maps.newHashMap();
		this.askPriceQuantity = HashMultiset.create();
		this.bidPriceQuantity = HashMultiset.create();
		
		this.sip = sip;
		sip.processMarket(this);
	}
	
	// This is only intended to be called by a market view
	protected void submitOrder(MarketView thisView, AgentView agent, OrderRecord orderRecord) {
		marketTime++;
		final Order order = Order.create(agent, orderRecord, new MarketTime(getCurrentTime(), marketTime));
		log(INFO, "%s", order);
	
		Multiset<Price> priceQuant = order.getOrderType() == BUY ? bidPriceQuantity : askPriceQuantity;
		priceQuant.add(orderRecord.getPrice(), orderRecord.getQuantity());
		
		orderbook.add(order);
		orderMapping.put(orderRecord, order);
		agent.orderSubmitted(orderRecord, thisView, getCurrentTime());
	}

	/**
	 * The placed bid will be routed to the Market that appears to offer the
	 * best execution according to the NBBO and this market's current up to date
	 * quote.
	 * 
	 * If the quote is routed, the market and agent will communicated with the
	 * other markets primary latency.
	 * @param thisView TODO
	 */
	// This is only intended to be called by a market view
	// This needs the appropriate view if it goes unrouted, and the agent if it does route 
	protected void submitNMSOrder(MarketView thisView, Agent agent, AgentView view, OrderRecord order) {
		marketTime++;
		
		BestBidAsk nbbo = sip.getNBBO();
		Market bestMarket = this;
	
		if (order.getOrderType() == BUY) {
			boolean nbboBetter = nbbo.getBestAsk().or(Price.INF).lessThan(quote.getAskPrice().or(Price.INF));
			boolean willTransact = order.getPrice().greaterThanEqual(nbbo.getBestAsk().or(Price.INF));
			if (nbboBetter && willTransact)
				bestMarket = nbbo.getBestAskMarket().get();
		} else {
			boolean nbboBetter = nbbo.getBestBid().or(Price.NEG_INF).greaterThan(quote.getBidPrice().or(Price.NEG_INF));
			boolean willTransact = order.getPrice().lessThanEqual(nbbo.getBestBid().or(Price.NEG_INF));
			if (nbboBetter && willTransact)
				bestMarket = nbbo.getBestBidMarket().get();
		}
	
		if (bestMarket.equals(this)) {
			submitOrder(thisView, view, order);
		} else {
			log(INFO, "Routing %s %s %d @ %s from %s %s to NBBO %s %s",
					agent, order.getOrderType(), order.getQuantity(), order.getPrice(), this, quote, bestMarket, nbbo);
			MarketView routedTo = bestMarket.getPrimaryView();
			routedOrders.put(order, routedTo);
			routedTo.submitOrder(agent, order);
		}
	}

	protected void withdrawOrder(OrderRecord orderRecord, int quantity) {
		marketTime++;
		
		// Check if we routed it due to NMS
		MarketView routedTo = routedOrders.get(orderRecord);
		if (routedTo != null ) {
			routedTo.withdrawOrder(orderRecord, quantity);
			return;
		}
		
		// Find the appropriate order object
		Order order = orderMapping.get(orderRecord);
		if (order == null)
			return;
		
		checkArgument(quantity > 0, "Quantity must be positive");
		quantity = Math.min(quantity, order.getQuantity());

		Multiset<Price> priceQuant = order.getOrderType() == SELL ? askPriceQuantity : bidPriceQuantity;
		priceQuant.remove(order.getPrice(), quantity);

		orderbook.remove(order, quantity);
		if (order.getQuantity() == 0)
			orderMapping.remove(orderRecord);
	}

	protected void clear() {
		marketTime++;
		MarketTime transactionTime = new MarketTime(getCurrentTime(), marketTime);
		
		Collection<MatchedOrders<Price, MarketTime, Order>> matchedOrders = orderbook.marketClear();
		for (Entry<MatchedOrders<Price, MarketTime, Order>, Price> e : clearingRule.pricing(matchedOrders).entrySet()) {

			Order buy = e.getKey().getBuy();
			Order sell = e.getKey().getSell();

			Transaction transaction = new Transaction(e.getKey().getQuantity(), e.getValue(), transactionTime);
			log(INFO, "%s", transaction);
			
			askPriceQuantity.remove(sell.getPrice(), transaction.getQuantity());
			bidPriceQuantity.remove(buy.getPrice(), transaction.getQuantity());
			
			// Views
			for (MarketView view : views)
				view.addTransaction(transaction);
			buy.getAgent().orderTransacted(buy.getOrderRecord(), transaction.getQuantity());
			buy.getAgent().processTransaction(buy.getSubmitTime(), BUY, transaction);
			sell.getAgent().orderTransacted(sell.getOrderRecord(), transaction.getQuantity());
			sell.getAgent().processTransaction(sell.getSubmitTime(), SELL, transaction);
			
			// Statistics
			postStat(Stats.PRICE, e.getValue().doubleValue()); // XXX Not robust to quantity?
			postTimedStat(Stats.TRANSACTION_PRICE, e.getValue().doubleValue());
		}
		updateQuote();
	}

	protected void updateQuote() {
		Optional<Price> ask = Optional.fromNullable(orderbook.askQuote());
		Optional<Price> bid = Optional.fromNullable(orderbook.bidQuote());
		int quantityAsk = askPriceQuantity.count(ask.orNull());
		int quantityBid = bidPriceQuantity.count(bid.orNull());
		/*
		 * TODO In certain circumstances, there will be no orders with the
		 * current ask or bid price in the market. This is a result of the ask
		 * price being set as part of a matched order. The correct "quantity" is
		 * 0, but it doesn't tell how many orders are available at that price.
		 * This is possible to do a number of ways. One could use a sorted map
		 * which has log time, one could scan the heap which would be worst case
		 * linear, or probably the best thing would be to have the heap have a
		 * hashmap to check the quantity based off of wither matched or
		 * unmatched order... I think this may be fine the way it is, only
		 * because if quantity is 0, but price isn't null, then you'll have to
		 * bid over instead of equal to the quote in order to execute. Also,
		 * this will only happen when there are matched orders when a quote is
		 * generated, which is currently never possible
		 */
		MarketTime quoteTime = new MarketTime(getCurrentTime(), marketTime);
		quote = new Quote(this, bid, quantityBid, ask, quantityAsk, quoteTime);

		log(INFO, "%s %s", this, quote);

		postTimedStat(Stats.MIDQUOTE + this, quote.getMidquote());
		postTimedStat(Stats.SPREAD + this, quote.getSpread());

		// Update quite happens first so hfts will have access to fully updated quote
		for (MarketView view : views)
			view.updateQuote(quote);
		for (Entry<HFTAgentView, MarketView> hft : Iterables2.shuffle(notified.entrySet(), rand))
			hft.getKey().quoteUpdate(hft.getValue());
	}

	@Override
	protected String name() {
		String oldName = super.name();
		return oldName.endsWith("Market") ? oldName.substring(0, oldName.length() - 6) : oldName;
	}
	
	/**
	 * Base Market just returns the id in []. Subclasses should override to also
	 * add information about the type of market.
	 */
	@Override
	public String toString() {
		return name() + "[" + getID() + "]";
	}

	public MarketView getView(TimeStamp latency) {
		MarketView view = new MarketView(latency);
		views.add(view);
		return view;
	}
	
	public MarketView getPrimaryView() {
		return primaryView;
	}

	// FIXME Have market keep transactions, and just return sublist...
	public class MarketView implements View {
		protected final TimeStamp latency;
		
		protected final List<Transaction> transactions;
		protected Quote quote;
		
		protected MarketView(TimeStamp latency) {
			this.latency = latency;
			this.quote = Quote.create(Market.this, Optional.<Price> absent(), 0, Optional.<Price> absent(), 0, TimeStamp.ZERO);
			this.transactions = Lists.newArrayList();
		}
		
		protected void addTransaction(final Transaction transaction) {
			scheduleActivityIn(latency, new Activity() {
				@Override public void execute() {
					MarketView.this.transactions.add(transaction);
				}
				@Override public String toString() { return "Add Transaction"; }
			});
		}
		
		protected void updateQuote(final Quote quote) {
			scheduleActivityIn(latency, new Activity() {
				@Override public void execute() {
					if (MarketView.this.quote.getQuoteTime().compareTo(quote.getQuoteTime()) <= 0)
						MarketView.this.quote = quote;
				}
				@Override public String toString() { return "Update Quote"; }
			});
		}
		
		public List<Transaction> getTransactions() {
			return Collections.unmodifiableList(Lists.reverse(transactions));
		}
		
		public Quote getQuote() {
			return quote;
		}

		public OrderRecord submitNMSOrder(final Agent agent, final OrderRecord order) {
			final AgentView view = agent.getView(latency);
			scheduleActivityIn(latency, new Activity() {
				@Override public void execute() { Market.this.submitNMSOrder(MarketView.this, agent, view, order); }
				@Override public String toString() { return "Submit NMS Order"; }
			});
			return order;
		}

		public void submitOrder(final Agent agent, final OrderRecord order) {
			final AgentView view = agent.getView(latency);
			scheduleActivityIn(latency, new Activity() {
				@Override public void execute() { Market.this.submitOrder(MarketView.this, view, order); }
				@Override public String toString() { return "Submit Order"; }
			});
		}
		
		public void withdrawOrder(final OrderRecord order, final int quantity) {
			scheduleActivityIn(latency, new Activity() {
				@Override public void execute() { Market.this.withdrawOrder(order, quantity); }
				@Override public String toString() { return "Withdraw Order"; }
			});
		}
		
		public void withdrawOrder(OrderRecord order) {
			withdrawOrder(order, order.getQuantity());
		}
		
		public void notify(HFTAgent agent) {
			Market.this.notified.put(agent.getView(latency), this);
		}

		@Override
		public TimeStamp getLatency() {
			return latency;
		}

		@Override
		public String toString() {
			return Market.this.toString() + '*';
		}
		
	}

	private static final long serialVersionUID = 8806298743451593261L;
	
}
