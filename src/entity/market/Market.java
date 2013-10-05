package entity.market;

import static logger.Logger.log;
import static logger.Logger.Level.INFO;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.Integer.signum;
import static java.lang.Math.min;
import static java.lang.Math.max;
import static java.lang.Math.abs;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import utils.Iterables2;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multiset;

import activity.Activity;
import activity.SendToIP;
import activity.WithdrawOrder;
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

/**
 * Base class for all markets.
 * 
 * @author ewah
 */
/**
 * @author ewah
 *
 */
public abstract class Market extends Entity {

	private static final long serialVersionUID = 8806298743451593261L;
	private static int nextID = 1;
	
	protected final FourHeap<Price, TimeStamp> orderbook;
	protected final ClearingRule clearingRule;
	protected final Random rand;

	protected final SMIP ip;
	protected final SIP sip;
	protected final Collection<IP> ips;
	
	protected Quote quote;

	// Book keeping
	protected final Map<fourheap.Order<Price, TimeStamp>, Order> orderMapping;
	protected final Multiset<Price> askPriceQuantity, bidPriceQuantity;
	protected final Collection<Order> orders; // All orders ever submitted to the market
	protected final List<Transaction> allTransactions; // All successful transactions
	
	// depths: Number of orders in the orderBook
	// spreads: Bid-ask spread value
	// midQuotes: Midpoint of bid/ask values
	protected final TimeSeries depths, spreads, midQuotes;


	public Market(SIP sip, TimeStamp latency, ClearingRule clearingRule, Random rand) {
		super(nextID++);
		this.orderbook = FourHeap.<Price, TimeStamp>create();
		this.clearingRule = clearingRule;
		this.rand = rand;
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

	public void addIP(IP ip) {
		checkNotNull(ip, "IP");
		ips.add(ip);
	}

	public SMIP getSMIP() {
		return this.ip;
	}

	/**
	 * @return All bids submitted to this market
	 */
	public Collection<Order> getAllOrders() {
		return ImmutableList.copyOf(orders);
	}

	public Iterable<? extends Activity> withdrawOrder(Order order,
			TimeStamp currentTime) {
		return withdrawOrder(order, order.getQuantity(), currentTime);
	}

	/**
	 * This will work even if quantity has "decreased" after order was submitted. Trying to cancel a
	 * higher quantity than the order has to offer will simply result in the entire order being
	 * cancelled.
	 * 
	 * NOTE: quantity can be negative. If withdrawing part of sell order, quantity should be < 0
	 */
	public Iterable<? extends Activity> withdrawOrder(Order order, int quantity, TimeStamp currentTime) {
		if (order.getQuantity() == 0) return ImmutableList.of();
		checkArgument(quantity != 0 && signum(order.getQuantity()) == signum(quantity),
				"Improper quantity");
		quantity = quantity < 0 ? max(quantity, order.getQuantity()) : 
								  min(quantity, order.getQuantity());
		
		Multiset<Price> priceQuant = order.getQuantity() < 0 ? askPriceQuantity : bidPriceQuantity;
		priceQuant.remove(order.getPrice(), abs(quantity));
		
		orderbook.withdrawOrder(order.order, quantity);
		checkOrder(order);
		return ImmutableList.of();
	}

	/**
	 * Clears the order book.
	 */
	public Iterable<? extends Activity> clear(TimeStamp currentTime) {
		List<fourheap.MatchedOrders<Price, TimeStamp>> ftransactions = orderbook.clear();
		Builder<Transaction> transactions = ImmutableList.builder();
		for (Entry<fourheap.MatchedOrders<Price, TimeStamp>, Price> e : clearingRule.pricing(ftransactions).entrySet()) {

			Order buy = orderMapping.get(e.getKey().getBuy());
			Order sell = orderMapping.get(e.getKey().getSell());
			
			if (!buy.getAgent().equals(sell.getAgent())) { // In case buyer == seller
				Transaction trans = new Transaction(buy.getAgent(),
						sell.getAgent(), this, buy, sell, e.getKey().getQuantity(),
						e.getValue(), currentTime);
				
				askPriceQuantity.remove(sell.getPrice(), trans.getQuantity());
				bidPriceQuantity.remove(buy.getPrice(), trans.getQuantity());
			
				checkOrder(buy);
				checkOrder(sell);
				transactions.add(trans);
				allTransactions.add(trans);
				// TODO add delay to this
				buy.getAgent().addTransaction(trans);
				sell.getAgent().addTransaction(trans);
			}
		}

		return updateQuote(transactions.build(), currentTime);
	}

	/**
	 * Updates the Markets current quote
	 */
	protected Iterable<? extends Activity> updateQuote(
			List<Transaction> transactions, TimeStamp currentTime) {
		Price ask = orderbook.askQuote();
		Price bid = orderbook.bidQuote();
		int quantityAsk = askPriceQuantity.count(ask);
		int quantityBid = bidPriceQuantity.count(bid);
		// TODO In certain circumstances, there will be no orders with the current ask or bid price
		// in the market. This is a result of the ask price being set as part of a matched order.
		// The correct "quantity" is 0, but it doesn't tell how many orders are available at that
		// price. This is possible to do a number of ways. One could use a sorted map which has log
		// time, one could scan the heap which would be worst case linear, or probably the best
		// thing would be to have the heap have a hashmap to check the quantity based off of wither
		// matched or unmatched order... I think this may be fine the way it is, only because if
		// quantity is 0, but price isn't null, then you'll have to bid over instead of equal to the
		// quote in order to execute. Also, this will only happen when there are matched orders when
		// a quote is generated, which is currently never possible
		quote = new Quote(this, ask, quantityAsk, bid, quantityBid, currentTime);

		log(INFO, this + " " + quote);

		depths.add((int) currentTime.getInTicks(), orderbook.size());
		spreads.add((int) currentTime.getInTicks(), quote.getSpread());
		midQuotes.add((int) currentTime.getInTicks(), quote.getMidquote());
		
		Builder<Activity> acts = ImmutableList.builder();
		for (IP ip : Iterables2.randomOrder(ips, rand))
			acts.add(new SendToIP(this, quote, transactions, ip, TimeStamp.IMMEDIATE));
		return acts.build();
	}
	
	/**
	 * Removes order from appropriate data structures if it's fully transacted
	 * @order
	 */
	protected void checkOrder(Order order) {
		if (order.getQuantity() == 0) {
			order.agent.removeOrder(order);
			orderMapping.remove(order.order);
		}
	}

	// TODO Add IOC / Fill or Kill Order

	/**
	 * Bid doesn't expire
	 */
	public Iterable<? extends Activity> submitOrder(Agent agent, Price price,
			int quantity, TimeStamp currentTime) {
		return submitOrder(agent, price, quantity, currentTime, TimeStamp.IMMEDIATE);
	}

	/**
	 * @param agent
	 * @param price
	 * @param quantity
	 * @param currentTime
	 * @param duration
	 * @return
	 */
	public Iterable<? extends Activity> submitOrder(Agent agent, Price price,
			int quantity, TimeStamp currentTime, TimeStamp duration) {
		checkArgument(quantity != 0, "Can't submit a 0 quantity order");

		log(INFO, agent + " (" + price + ", " + quantity + ") -> " + this);

		fourheap.Order<Price, TimeStamp> nativeOrder = fourheap.Order.create(
				price, quantity, currentTime);
		Order order = new Order(agent, this, nativeOrder);

		Multiset<Price> priceQuant = order.getQuantity() < 0 ? askPriceQuantity : bidPriceQuantity;
		priceQuant.add(order.getPrice(), abs(quantity));
		
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
	 * Bid doesn't expire
	 */
	public Iterable<? extends Activity> submitNMSOrder(Agent agent,
			Price price, int quantity, TimeStamp currentTime) {
		return submitNMSOrder(agent, price, quantity, currentTime, TimeStamp.IMMEDIATE);
	}

	// TODO How should call markets handle Reg NMS. Can't route to call market to get immediate
	// execution. NMSOrder will not route properly for a call market if there is another market in
	// the model
	public Iterable<? extends Activity> submitNMSOrder(Agent agent,
			Price price, int quantity, TimeStamp currentTime, TimeStamp duration) {
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
			log(INFO, "Routing " + agent + " (" + price + ", " + quantity
					+ ") -> " + this + " " + quote + " to NBBO " + nbbo);

		return bestMarket.submitOrder(agent, price, quantity, currentTime, duration);
	}
	
	public List<Transaction> getTransactions() {
		return ImmutableList.copyOf(allTransactions);
	}

	public TimeSeries getDepth() {
		return this.depths;
	}

	public TimeSeries getSpread() {
		return this.spreads;
	}

	public TimeSeries getMidQuotes() {
		return this.midQuotes;
	}

	@Override
	public String toString() {
		return new String("[" + id + "]");
	}
	
}
