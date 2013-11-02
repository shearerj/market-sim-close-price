package entity.market;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.Math.min;
import static logger.Logger.log;
import static logger.Logger.Level.INFO;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import systemmanager.Consts.OrderType;
import utils.Iterables2;
import activity.Activity;
import activity.SendToIP;
import activity.WithdrawOrder;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multiset;

import data.TimeSeries;
import entity.Entity;
import entity.agent.Agent;
import entity.infoproc.BestBidAsk;
import entity.infoproc.IP;
import entity.infoproc.SIP;
import entity.infoproc.SMIP;
import entity.market.clearingrule.ClearingRule;
import event.TimeStamp;
import fourheap.FourHeap;
import fourheap.MatchedOrders;

/**
 * Base class for all markets. This class provides almost all market
 * functionality that one should need for creating a market.
 * 
 * A market needs two things to function appropriately. First is a latency. This
 * represents how long it takes background agents (with only one primary market)
 * to get information from the market. A latency of TimeStamp.IMMEDIATE makes
 * this immediate. The second thing they need is a ClearingRule. A ClearingRule
 * is a class that takes a set of MatchedOrders and assigns a price to them.
 * This facilitates the difference in pricing between a call market and a CDA.
 * 
 * By default the only method that schedules more activities is a Clear. A clear
 * will trigger a SendToIP for every IP this Market knows about (via the method
 * updateQuote). Every other time another action needs to happen it needs to be
 * overridden by the subclass. For example, a CDA will trigger a Clear after a
 * SubmitOrder, and sendToIP activities after a WithdrawOrder. A call market will
 * schedule another Clear after an existing one.
 * 
 * @author ewah
 */
public abstract class Market extends Entity {

	private static final long serialVersionUID = 8806298743451593261L;
	private static int nextID = 1;
	
	protected final FourHeap<OrderType, Price, MarketTime> orderbook;
	protected final ClearingRule clearingRule;
	protected final Random rand;
	protected long marketTime; // keeps track of internal market actions

	protected final SMIP ip;
	protected final SIP sip;
	protected final Collection<IP> ips; // All the information processors that get data
	
	protected Quote quote; // Current quote

	// Book keeping
	protected final Map<fourheap.Order<OrderType, Price, MarketTime>, Order> orderMapping; // How to get full orders from fourheap Orders
	protected final Multiset<Price> askPriceQuantity, bidPriceQuantity; // How many orders are at a specific price
	protected final Collection<Order> orders; // All orders ever submitted to the market
	protected final List<Transaction> allTransactions; // All successful transactions, implicitly time ordered
	
	// depths: Number of orders in the orderBook
	// spreads: Bid-ask spread value
	// midQuotes: Midpoint of bid/ask values
	protected final TimeSeries depths, spreads, midQuotes;


	/**
	 * Constructor
	 * 
	 * @param sip
	 *            the SIP for this simulation
	 * @param latency
	 *            how long it takes agents with market as a primary model get
	 *            get notified of market changes
	 * @param clearingRule
	 *            a class that dictates how the prices on matching orders are
	 *            set
	 * @param rand
	 *            the random number generator to use
	 */
	public Market(SIP sip, TimeStamp latency, ClearingRule clearingRule, Random rand) {
		super(nextID++);
		this.orderbook = FourHeap.create(OrderType.class);
		this.clearingRule = clearingRule;
		this.rand = rand;
		this.marketTime = 0;
		this.quote = new Quote(this, null, 0, null, 0, TimeStamp.ZERO);

		this.ips = Lists.newArrayList();
		this.sip = sip;
		this.ip = new SMIP(latency, this);
		ips.add(sip);
		ips.add(ip);

		this.orderMapping = Maps.newHashMap();
		this.askPriceQuantity = HashMultiset.create();
		this.bidPriceQuantity = HashMultiset.create();
		this.orders = Lists.newArrayList();
		this.allTransactions = Lists.newArrayList();

		this.depths = new TimeSeries();
		this.spreads = new TimeSeries();
		this.midQuotes = new TimeSeries();
	}

	/**
	 * Add an IP to this market that will get notified when the market quote and
	 * transactions change.
	 * 
	 * @param ip
	 *            the IP to add
	 */
	public void addIP(IP ip) {
		checkNotNull(ip, "IP");
		ips.add(ip);
	}

	public SMIP getSMIP() {
		return this.ip;
	}

	/**
	 * Get all orders ever submitted to the market
	 * 
	 * @return All orders ever submitted to this market
	 */
	public Collection<Order> getAllOrders() {
		return ImmutableList.copyOf(orders);
	}
	
	/**
	 * Convenience method to fully remove an order.
	 * 
	 * NOTE: This should only be called by the corresponding activity
	 * 
	 * @param order
	 *            order to remove
	 * @param currentTime
	 *            the current time
	 * @return any side effect activities (base case none)
	 */
	public Iterable<? extends Activity> withdrawOrder(Order order,
			TimeStamp currentTime) {
		return withdrawOrder(order, order.getQuantity(), currentTime);
	}

	/**
	 * Method to withdraw specific quantity from an order.
	 * 
	 * This will work even if quantity has decreased after order was
	 * submitted. Trying to cancel a higher quantity than the order has to offer
	 * will simply result in the entire order being cancelled.
	 * 
	 * NOTE: This should only be called by the corresponding activity
	 * 
	 * @param order
	 *            order to withdraw quantity from
	 * @param quantity
	 *            quantity to withdraw, always positive
	 * @param currentTime
	 *            the current time
	 * @return any side effect activities (base case none)
	 */
	public Iterable<? extends Activity> withdrawOrder(Order order, int quantity, 
			TimeStamp currentTime) {
		marketTime++;
		checkArgument(quantity >= 0, "Quantiy can't be negative");
		if (order.getQuantity() == 0) return ImmutableList.of();
		quantity = min(quantity, order.getQuantity());
		
		Multiset<Price> priceQuant = order.getOrderType().equals(OrderType.SELL) ? askPriceQuantity : bidPriceQuantity;
		priceQuant.remove(order.getPrice(), quantity);
		
		orderbook.withdrawOrder(order.order, quantity);
		
		if (order.getQuantity() == 0) {
			order.agent.removeOrder(order);
			orderMapping.remove(order.order);
		}
		
		return ImmutableList.of();
	}

	/**
	 * Clears the order book.
	 * 
	 * NOTE: This should only be called by the corresponding activity
	 * 
	 * @param currentTime
	 *            the current time
	 * @return any side effect activities (base case SendToIP activities)
	 */
	public Iterable<? extends Activity> clear(TimeStamp currentTime) {
		marketTime++;
		List<MatchedOrders<OrderType, Price, MarketTime>> matchedOrders = orderbook.clear();
		Builder<Transaction> transactions = ImmutableList.builder();
		for (Entry<MatchedOrders<OrderType, Price, MarketTime>, Price> e : clearingRule.pricing(matchedOrders).entrySet()) {

			Order buy = orderMapping.get(e.getKey().getBuy());
			Order sell = orderMapping.get(e.getKey().getSell());

			Transaction trans = new Transaction(buy.getAgent(),
					sell.getAgent(), this, buy, sell, e.getKey().getQuantity(),
					e.getValue(), currentTime);
			
			askPriceQuantity.remove(sell.getPrice(), trans.getQuantity());
			bidPriceQuantity.remove(buy.getPrice(), trans.getQuantity());
			
			transactions.add(trans);
			allTransactions.add(trans);
			// TODO add delay to agent facing actions...
			buy.getAgent().addTransaction(trans);
			if (buy.getQuantity() == 0)
				buy.agent.removeOrder(buy);
			sell.getAgent().addTransaction(trans);
			if (sell.getQuantity() == 0)
				sell.agent.removeOrder(sell);
		}
		
		// Remove fully transacted orders
		for (MatchedOrders<OrderType, Price, MarketTime> match : matchedOrders) {
			if (match.getBuy().getQuantity() == 0)
				orderMapping.remove(match.getBuy());
			if (match.getSell().getQuantity() == 0)
				orderMapping.remove(match.getSell());
		}

		return updateQuote(transactions.build(), currentTime);
	}

	/**
	 * Updates the Markets current quote and returns a set of SendToIP
	 * activities for every IP the market "knows" about
	 * 
	 * @param transactions
	 *            the transactions that are new since the last quote update
	 * @param currentTime
	 *            the current time
	 * @return the SendToIP activities required to propagate information
	 */
	protected Iterable<? extends Activity> updateQuote(
			List<Transaction> transactions, TimeStamp currentTime) {
		Price ask = orderbook.askQuote();
		Price bid = orderbook.bidQuote();
		int quantityAsk = askPriceQuantity.count(ask);
		int quantityBid = bidPriceQuantity.count(bid);
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
		quote = new Quote(this, bid, quantityBid, ask, quantityAsk, currentTime);

		log(INFO, this + " " + quote);

		depths.add((int) currentTime.getInTicks(), orderbook.size());
		spreads.add((int) currentTime.getInTicks(), quote.getSpread());
		midQuotes.add((int) currentTime.getInTicks(), quote.getMidquote());
		
		MarketTime quoteTime = new MarketTime(currentTime, marketTime);
		Builder<Activity> acts = ImmutableList.builder();
		for (IP ip : Iterables2.randomOrder(ips, rand))
			acts.add(new SendToIP(this, quoteTime, quote, transactions, ip, TimeStamp.IMMEDIATE));
		return acts.build();
	}

	/*
	 * TODO Add IOC / Fill or Kill Order (potentially change current syntax so
	 * that a withdraw without a duration never expires, and one with duration
	 * IMMEDIATE is a Fill or Kill. Not sure if this will work / make sense.
	 */

	/**
	 * Submit an order that doesn't expire
	 * 
	 * NOTE: This should only be called by the corresponding activity
	 * 
	 * @param agent
	 *            The agent that's submitting the order
	 * @param type TODO
	 * @param price
	 *            The price of the order
	 * @param quantity
	 *            The quantity of the order (negative for sell orders)
	 * @param currentTime
	 *            The current time
	 * @return any side effect activities (base case none)
	 */
	public Iterable<? extends Activity> submitOrder(Agent agent, OrderType type,
			Price price, int quantity, TimeStamp currentTime) {
		return submitOrder(agent, type, price, quantity, currentTime, TimeStamp.IMMEDIATE);
	}

	/**
	 * Submit an order that will "expire." An order expires by returning a
	 * withdrawOrder activity scheduled at currentTime + duration in the future.
	 * 
	 * NOTE: This should only be called by the corresponding activity
	 * 
	 * @param agent
	 *            The agent submitting the order
	 * @param type
	 * 			  The type of the order (BUY/SELL)
	 * @param price
	 *            The price of the order
	 * @param quantity
	 *            The quantity of the order, negative for sell orders
	 * @param currentTime
	 *            The current time
	 * @param duration
	 *            The amount of time to wait before canceling the order. Use
	 *            TimeStamp.IMMEDIATE for an order that doesn't expire.
	 * @return The side effect activities (base case potentially a WithdrawOrder for this
	 *         method)
	 */
	public Iterable<? extends Activity> submitOrder(Agent agent, OrderType type, Price price,
			int quantity, TimeStamp currentTime, TimeStamp duration) {
		checkNotNull(type, "Order type");
		checkArgument(quantity > 0, "Quantity must be positive");
		marketTime++;
		
		log(INFO, agent + " " + type + "(" + quantity + "@" + price + ") -> " + this);

		fourheap.Order<OrderType, Price, MarketTime> nativeOrder = fourheap.Order.create(
				type, price, quantity, new MarketTime(currentTime, marketTime));
		Order order = new Order(agent, this, nativeOrder);

		Multiset<Price> priceQuant = order.getOrderType().equals(OrderType.SELL) ? askPriceQuantity : bidPriceQuantity;
		priceQuant.add(order.getPrice(), quantity);
		
		orderbook.insertOrder(nativeOrder);
		orderMapping.put(nativeOrder, order);
		orders.add(order);
		agent.addOrder(order);
		depths.add((int) currentTime.getInTicks(), orderbook.size());
		
		if (!duration.equals(TimeStamp.IMMEDIATE))
			return ImmutableList.of(new WithdrawOrder(order, currentTime.plus(duration)));
		return ImmutableList.of();
	}

	/**
	 * Submit a routed order that doesn't expire. The placed bid will be routed
	 * to the Market that appears to offer the best execution according to the
	 * NBBO and this market's current up to date quote.
	 * 
	 * NOTE: This should only be called by the corresponding activity
	 * 
	 * @param agent
	 *            The agent that's submitting the order
	 * @param type
	 * 			  The type of the order (BUY/SELL)
	 * @param price
	 *            The price of the order
	 * @param quantity
	 *            The quantity of the order, negative for sell orders
	 * @param currentTime
	 *            The currentTime
	 * @return Any side effect activities (none in base case)
	 */
	public Iterable<? extends Activity> submitNMSOrder(Agent agent,
			OrderType type, Price price, int quantity, TimeStamp currentTime) {
		return submitNMSOrder(agent, type, price, quantity, currentTime, TimeStamp.IMMEDIATE);
	}

	/**
	 * Submit a routed order that "expires" after duration. The placed bid will
	 * be routed to the Market that appears to offer the best execution
	 * according to the NBBO and this market's current up to date quote. If
	 * duration is TimeStamp.IMMEDIATE than the order will not expire.
	 * 
	 * NOTE: This should only be called by the corresponding activity
	 * 
	 * @param agent
	 *            The agent submitting the order
	 * @param type
	 * 			  The type of the order (BUY/SELL)
	 * @param price
	 *            The price of the order
	 * @param quantity
	 *            The quantity of the order, negative for sell orders
	 * @param currentTime
	 *            The current time
	 * @param duration
	 *            The duration before the bid should expire. If duration =
	 *            TimeStamp(1), the bid will expire after 1 millisecond. To make
	 *            an order that doesn't expire use TimeStamp.IMMEDIATE.
	 * @return Any side effect activities (base case possibly a withdraw order)
	 */
	/*
	 * TODO How should call markets handle Reg NMS. Can't route to call market
	 * to get immediate execution. NMSOrder will not route properly for a call
	 * market if there is another market in the model
	 */
	public Iterable<? extends Activity> submitNMSOrder(Agent agent, OrderType type,
			Price price, int quantity, TimeStamp currentTime, TimeStamp duration) {
		checkNotNull(type, "Order type");
		checkArgument(quantity > 0, "Quantity must be positive");
		
		BestBidAsk nbbo = sip.getNBBO();
		Market bestMarket = this;

		if (quantity > 0) { // buy
			boolean nbboBetter = nbbo.getBestAsk() != null
					&& nbbo.getBestAsk().lessThan(quote.getAskPrice());
			boolean willTransact = price.greaterThan(nbbo.getBestAsk());
			if (nbboBetter && willTransact)
				bestMarket = nbbo.getBestAskMarket();
		} else { // sell
			boolean nbboBetter = nbbo.getBestBid() != null
					&& nbbo.getBestBid().greaterThan(quote.getBidPrice());
			boolean willTransact = price.lessThan(nbbo.getBestBid());
			if (nbboBetter && willTransact)
				bestMarket = nbbo.getBestBidMarket();
		}

		if (!bestMarket.equals(this))
			log(INFO, "Routing " + agent + " " + type + "(" +
					+ quantity + "@" + price + ") -> " 
					+ this + " " + quote + " to NBBO " + nbbo);

		return bestMarket.submitOrder(agent, type, price, quantity, currentTime, duration);
	}
	
	/**
	 * Get a list of all transactions in the market. This should NOT be used by
	 * agents to get transaction lists.
	 * 
	 * @return An immutable list of Transactions.
	 */
	public List<Transaction> getTransactions() {
		return ImmutableList.copyOf(allTransactions);
	}

	/**
	 * Get the depths. Used for observations.
	 * 
	 * @return The depths of the Market
	 */
	public TimeSeries getDepth() {
		return this.depths;
	}

	/**
	 * Get the spreads. Used for observations.
	 * 
	 * @return The spreads of the Market
	 */
	public TimeSeries getSpread() {
		return this.spreads;
	}

	/**
	 * Get the mid quotes. Used for observations.
	 * 
	 * @return The mid quotes of the Market
	 */
	public TimeSeries getMidQuotes() {
		return this.midQuotes;
	}

	/**
	 * Base Market just returns the id in []. Subclasses should override to also
	 * add information about the type of market.
	 */
	@Override
	public String toString() {
		return new String("[" + id + "]");
	}
	
}
