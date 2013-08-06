package entity.market;

import static logger.Logger.log;
import static logger.Logger.Level.INFO;

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
import entity.Agent;
import entity.Entity;
import entity.IP;
import entity.SIP;
import entity.SMIP;
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

	// Book keeping
	protected final Map<fourheap.Order<Price, TimeStamp>, Order> orderMapping;

	// Statistics
	protected final Collection<Order> orders; // All bids ever submitted to the market
	// depths: Number of orders in the orderBook
	// spreads: Bid-ask spread value
	// modQuotes: Midpoint of bid/ask values
	protected final TimeSeries depths, spreads, midQuotes;

	// Market information - Move to IP
	protected TimeStamp lastClearTime, lastBidTime;
	protected Price lastClearPrice;

	public Market(int marketID, MarketModel model, ClearingRule clearingRule) {
		super(marketID);
		this.model = model;
		this.orderbook = new FourHeap<Price, TimeStamp>();
		this.clearingRule = clearingRule;

		// FIXME Add latency properly, change null to this
		this.ips = new ArrayList<IP>();
		this.sip = model.getSIP();
		this.ip = new SMIP(model.nextIPID(), new TimeStamp(0), null);
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
		// FIXME Log something
		orderbook.withdrawOrder(order.order);
		order.agent.removeOrder(order);
		return Collections.emptySet();
	}

	// FIXME withdraw quantity of an order

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
			model.addTrans(trans);
			buy.getAgent().addTransaction(trans, currentTime);
			// FIXME Account for same buyer and seller
			// FIXME Set last clear price / do it in update quote
		}

		List<Transaction> tempTrans = new ArrayList<Transaction>(transactions); // FIXME remove
		return updateQuote(Collections.unmodifiableList(tempTrans), currentTime);
	}

	/**
	 * Updates the Markets current quote TODO This should be subsumed by sendToIPs. sendToIP should
	 * compute this quote and update things appropriately.
	 */
	protected Collection<? extends Activity> updateQuote(
			List<Transaction> transactions, TimeStamp currentTime) {
		Price ask = orderbook.askQuote();
		Price bid = orderbook.bidQuote();

		Quote quote = new Quote(this, ask, bid, currentTime);

		log(INFO, currentTime + " | " + this + " SendToSIP" + quote);

		Collection<Activity> activities = new ArrayList<Activity>();
		for (IP ip : ips)
			// FIXME Change null to this
			activities.add(new SendToIP(this, quote, transactions, ip,
					TimeStamp.IMMEDIATE));

		depths.add(currentTime, orderbook.size());
		spreads.add(currentTime, quote.getSpread());
		midQuotes.add(currentTime, quote.getMidquote());
		return activities;
	}

	// TODO Add bid type that gets withdrawn after next clear

	// TODO switch to Bid Object
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

		log(INFO, currentTime + " | " + this + " " + getName()
				+ "::submitBid: +(" + price + ", " + quantity + ") from "
				+ agent);

		fourheap.Order<Price, TimeStamp> nativeOrder = new fourheap.Order<Price, TimeStamp>(
				price, quantity, currentTime);
		Order order = new Order(agent, this, nativeOrder);

		orderbook.insertOrder(nativeOrder);
		orderMapping.put(nativeOrder, order);
		orders.add(order);
		agent.addOrder(order);
		depths.add(currentTime, orderbook.size());

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

	// FIXME How should call markets handle Reg NMS. Can't route to call market to get immediate
	// execution.
	// FIXME orderbook.(bid/ask)Quote() will return the current quote, not the published one
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
			log(INFO, currentTime + " | " + agent + " " + getName()
					+ "::submitNMSBid: " + "NBBO" + nbbo + " better than "
					+ this + " Quote" + null);

		return bestMarket.submitOrder(agent, price, quantity, currentTime,
				duration);
	}

	public Price getLastClearPrice() {
		return lastClearPrice;
	}

	public TimeStamp getLastClearTime() {
		return lastClearTime;
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
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof Market)) return false;
		Market market = (Market) obj;
		return super.equals(market) && model.equals(market.model);
	}

	@Override
	public String toString() {
		return new String("[" + id + "]");
	}
}
