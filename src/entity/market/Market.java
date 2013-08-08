package entity.market;

import static logger.Logger.log;
import static logger.Logger.Level.INFO;
import static java.lang.Integer.signum;
import static java.lang.Math.min;
import static java.lang.Math.max;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Iterator;

import clearingrule.ClearingRule;

import model.MarketModel;
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
import event.TimeStamp;
import fourheap.FourHeap;

/**
 * Base class for all markets.
 * 
 * @author ewah
 */
public abstract class Market extends Entity {

	protected final MarketModel model;
	protected final FourHeap<Price, TimeStamp> orderbook;
	protected final ClearingRule clearingRule;

	protected final SMIP ip;
	protected final SIP sip;
	protected final Collection<IP> ips;
	
	protected Quote quote;

	// Book keeping
	protected final Map<fourheap.Order<Price, TimeStamp>, Order> orderMapping;
	protected final Collection<Order> orders; // All orders ever submitted to the market
	
	// depths: Number of orders in the orderBook
	// spreads: Bid-ask spread value
	// midQuotes: Midpoint of bid/ask values
	protected final TimeSeries depths, spreads, midQuotes;


	public Market(int marketID, MarketModel model, ClearingRule clearingRule, TimeStamp latency) {
		super(marketID);
		this.model = model;
		this.orderbook = new FourHeap<Price, TimeStamp>();
		this.clearingRule = clearingRule;

		this.ips = new ArrayList<IP>();
		this.sip = model.getSIP();
		this.ip = new SMIP(model.nextIPID(), latency, this);
		ips.add(sip);
		ips.add(ip);

		this.orderMapping = new HashMap<fourheap.Order<Price, TimeStamp>, Order>();
		this.orders = new ArrayList<Order>();

		this.depths = new TimeSeries();
		this.spreads = new TimeSeries();
		this.midQuotes = new TimeSeries();
	}

	public void addIP(IP ip) {
		ips.add(ip);
	}

	public SMIP getSMIP() {
		return this.ip;
	}

	/**
	 * @return All bids submitted to this market
	 */
	public Collection<Order> getAllOrders() {
		return Collections.unmodifiableCollection(orders);
	}

	public Collection<? extends Activity> withdrawOrder(Order order,
			TimeStamp currentTime) {
		return withdrawOrder(order, order.getQuantity(), currentTime);
	}

	/**
	 * This will work even if quantity has "decreased" after order was submitted. Trying to cancel a
	 * higher quantity than the order has to offer will simply result in the entire order being
	 * cancelled.
	 * 
	 * @param order
	 * @param quantity
	 * @param currentTime
	 * @return
	 */
	public Collection<? extends Activity> withdrawOrder(Order order, int quantity, TimeStamp currentTime) {
		if (order.getQuantity() == 0) return Collections.emptySet();
		if (quantity == 0 || signum(order.getQuantity()) != signum(quantity))
			throw new IllegalArgumentException("Improper quantity");
		quantity = quantity < 0 ? max(quantity, order.getQuantity()) : min(quantity, order.getQuantity());
		orderbook.withdrawOrder(order.order, quantity);
		checkOrder(order);
		return Collections.emptySet();
	}

	/**
	 * Clears the order book.
	 */
	public Collection<? extends Activity> clear(TimeStamp currentTime) {

		List<fourheap.Transaction<Price, TimeStamp>> ftransactions = orderbook.clear();
		List<Price> prices = clearingRule.pricing(ftransactions);

		List<Transaction> transactions = new ArrayList<Transaction>(
				ftransactions.size());
		Iterator<Price> pit = prices.iterator();
		for (fourheap.Transaction<Price, TimeStamp> ftrans : ftransactions) {

			Order buy = orderMapping.get(ftrans.getBuy());
			Order sell = orderMapping.get(ftrans.getSell());
			Transaction trans = new Transaction(buy.getAgent(),
					sell.getAgent(), this, buy, sell, ftrans.getQuantity(),
					pit.next(), currentTime);
			checkOrder(buy);
			checkOrder(sell);
			transactions.add(trans);
			model.addTrans(trans);
			// TODO add delay to this
			buy.getAgent().addTransaction(trans);
			if (!buy.getAgent().equals(sell.getAgent())) // In case buyer == seller
				sell.getAgent().addTransaction(trans);
		}

		return updateQuote(Collections.unmodifiableList(transactions), currentTime);
	}

	/**
	 * Updates the Markets current quote
	 */
	protected Collection<? extends Activity> updateQuote(
			List<Transaction> transactions, TimeStamp currentTime) {
		Price ask = orderbook.askQuote();
		Price bid = orderbook.bidQuote();

		Quote quote = new Quote(this, ask, bid, currentTime);

		log(INFO, this + " SendToSIP" + quote);

		Collection<Activity> activities = new ArrayList<Activity>();
		for (IP ip : ips)
			activities.add(new SendToIP(this, quote, transactions, ip,
					TimeStamp.IMMEDIATE));

		depths.add((int) currentTime.getInTicks(), orderbook.size());
		spreads.add((int) currentTime.getInTicks(), quote.getSpread());
		midQuotes.add((int) currentTime.getInTicks(), quote.getMidquote());
		return activities;
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

	// TODO Add bid type that gets withdrawn after next clear

	/**
	 * Bid doesn't expire
	 */
	public Collection<? extends Activity> submitOrder(Agent agent, Price price,
			int quantity, TimeStamp currentTime) {
		return submitOrder(agent, price, quantity, currentTime,
				TimeStamp.IMMEDIATE);
	}

	public Collection<? extends Activity> submitOrder(Agent agent, Price price,
			int quantity, TimeStamp currentTime, TimeStamp duration) {
		if (quantity == 0) return Collections.emptySet();

		log(INFO, this + " " + getName() + "::submitBid: +(" + price + ", "
				+ quantity + ") from " + agent);

		fourheap.Order<Price, TimeStamp> nativeOrder = new fourheap.Order<Price, TimeStamp>(
				price, quantity, currentTime);
		Order order = new Order(agent, this, nativeOrder);

		orderbook.insertOrder(nativeOrder);
		orderMapping.put(nativeOrder, order);
		orders.add(order);
		agent.addOrder(order);
		depths.add((int) currentTime.getInTicks(), orderbook.size());

		if (!duration.equals(TimeStamp.IMMEDIATE))
			return Collections.singleton(new WithdrawOrder(order, currentTime.plus(duration)));
		return Collections.emptySet();
	}

	/**
	 * Bid doesn't expire
	 */
	public Collection<? extends Activity> submitNMSOrder(Agent agent,
			Price price, int quantity, TimeStamp currentTime) {
		return submitNMSOrder(agent, price, quantity, currentTime,
				TimeStamp.IMMEDIATE);
	}

	// TODO How should call markets handle Reg NMS. Can't route to call market to get immediate
	// execution.
	// TODO orderbook.(bid/ask)Quote() will return the current quote, not the published one
	// TODO NMSOrder will not route properly for a call market if there is another market in the
	// model
	public Collection<? extends Activity> submitNMSOrder(Agent agent,
			Price price, int quantity, TimeStamp currentTime, TimeStamp duration) {
		BestBidAsk nbbo = sip.getNBBO();
		Market bestMarket = this;

		if (quantity > 0) { // buy
			boolean nbboBetter = nbbo.getBestAsk() != null
					&& nbbo.getBestAsk().lessThan(orderbook.askQuote());
			boolean willTransact = price.greaterThan(nbbo.getBestAsk());
			if (nbboBetter && willTransact)
				bestMarket = nbbo.getBestAskMarket();
		} else { // sell
			boolean nbboBetter = nbbo.getBestBid() != null
					&& nbbo.getBestBid().greaterThan(orderbook.bidQuote());
			boolean willTransact = price.lessThan(nbbo.getBestBid());
			if (nbboBetter && willTransact)
				bestMarket = nbbo.getBestBidMarket();
		}

		if (!bestMarket.equals(this))
			log(INFO, agent + " " + getName() + "::submitNMSBid: " + "NBBO"
					+ nbbo + " better than " + this + " Quote" + null);

		return bestMarket.submitOrder(agent, price, quantity, currentTime,
				duration);
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

	public MarketModel getModel() {
		return this.model;
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ model.hashCode();
	}

	@Override
	public String toString() {
		return new String("[" + id + "]");
	}
}
