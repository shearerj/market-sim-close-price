package market;

import static logger.Logger.log;
import static logger.Logger.Level.INFO;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import model.MarketModel;
import activity.Activity;
import activity.SendToIP;
import activity.WithdrawBid;
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
public abstract class Market2 extends Entity {

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

	public Market2(int marketID, MarketModel model, ClearingRule clearingRule) {
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

	// TODO Rename
	public SMIP getSMIP() {
		return this.ip;
	}

	/**
	 * @return All bids submitted to this market
	 */
	public Collection<Order> getAllOrders() {
		return Collections.unmodifiableCollection(orders);
	}

	public void addIP(IP ip) {
		ips.add(ip);
	}

	// FIXME Withdraw order method

	/**
	 * Clears the order book.
	 */
	public Collection<? extends Activity> clear(TimeStamp currentTime) {

		// FIXME Log prior quote
		log(INFO, currentTime + " | " + this + " Prior-clear Quote" + null);

		List<fourheap.Transaction<Price, TimeStamp>> ftransactions = orderbook.clear();
		List<Price> prices = clearingRule.pricing(ftransactions);

		// FIXME log postclear quote

		List<Transaction> transactions = new ArrayList<Transaction>(ftransactions.size());
		for (fourheap.Transaction<Price, TimeStamp> trans : ftransactions) {
			// FIXME Construct new Transaction
			// FIXME Add transaction to model
			// FIXME Add Transaction to Buyer and Seller account for same buyer and seller
			// FIXME Set last clear price
		}

		// FIXME SendToSIP(s) should probably happen after a quote update,
		// not after a clear. In general a clear should always cause a quote
		// update, but this is important to note. Also, if there's a clear,
		// but no transactions, it's probably not worth a SendToSIP, unless
		// you're a call market.
		return updateQuote(Collections.unmodifiableList(transactions), currentTime);
	}

	/**
	 * Updates the Markets current quote TODO This should be subsumed by sendToIPs. sendToIP should
	 * compute this quote and update things appropriately.
	 */
	protected Collection<? extends Activity> updateQuote(
			List<Transaction> transactions, TimeStamp currentTime) {
		// TODO This first part should be done a lot differently

		Price ask = orderbook.askQuote();
		Price bid = orderbook.bidQuote();

		// FIXME Change the way quote is
		Quote quote = new Quote(null, ask, bid, currentTime);

		log(INFO, currentTime + " | " + this + " SendToSIP" + quote);

		Collection<Activity> activities = new ArrayList<Activity>();
		for (IP ip : ips)
			// FIXME Change null to this
			activities.add(new SendToIP(null, quote, transactions, ip,
					TimeStamp.IMMEDIATE));

		// FIXME Change way this is organized
		recordDepth(currentTime);
		addSpread(currentTime);
		addMidQuote(currentTime);
		return activities;
	}

	protected void recordDepth(TimeStamp ts) {
		depths.add(ts, orderbook.size());
	}

	protected void addSpread(TimeStamp ts) {
		// FIXME Add spread calculation to quote
//		spreads.add(ts, quote.getSpread());
	}

	protected void addMidQuote(TimeStamp ts) {
		// FIXME Add midquote method to quote
//		double midQuote = quote.isDefined() ? Double.NaN
//				: (quote.getBidPrice().getPrice() + quote.getAskPrice().getPrice()) / 2d;
//		midQuotes.add(ts, midQuote);
	}

	// TODO Add bid type that gets withdrawn after next clear
	
	// TODO switch to Bid Object
	/**
	 * Bid doesn't expire
	 */
	public Collection<? extends Activity> submitBid(Agent agent, Price price,
			int quantity, TimeStamp currentTime) {
		return submitBid(agent, price, quantity, currentTime,
				TimeStamp.IMMEDIATE);
	}

	public Collection<? extends Activity> submitBid(Agent agent, Price price,
			int quantity, TimeStamp currentTime, TimeStamp duration) {
		if (quantity == 0) return Collections.emptySet();

		log(INFO, currentTime + " | " + this + " " + getName()
				+ "::submitBid: +(" + price + ", " + quantity + ") from "
				+ agent);

//		Bid pqBid = new Bid(agent, this, currentTime);
//		pqBid.addPoint(quantity, price);
//
//		orderbook.insertBid(pqBid);
//		bids.add(pqBid);
		recordDepth(currentTime);

		if (duration.equals(TimeStamp.IMMEDIATE))
			return Collections.emptySet();
		else
			return Collections.singleton(new WithdrawBid(agent, null,
					currentTime.plus(duration)));
	}
	
	/**
	 * Bid doesn't expire
	 */
	public Collection<? extends Activity> submitNMSBid(Agent agent,
			Price price, int quantity, TimeStamp currentTime) {
		return submitNMSBid(agent, price, quantity, currentTime,
				TimeStamp.IMMEDIATE);
	}

	public Collection<? extends Activity> submitNMSBid(Agent agent,
			Price price, int quantity, TimeStamp currentTime, TimeStamp duration) {
		BestBidAsk nbbo = sip.getNBBO();
		Market2 bestMarket = this;

//		if (quantity > 0) { // buy
//			boolean nbboBetter = nbbo.getBestAsk() != null
//					&& nbbo.getBestAsk().lessThan(quote.getAskPrice());
//			boolean willTransact = price.greaterThan(nbbo.getBestAsk());
//			if (nbboBetter && willTransact) bestMarket = nbbo.getBestAskMarket();
//		} else { // sell
//			boolean nbboBetter = nbbo.getBestBid() != null
//					&& nbbo.getBestBid().greaterThan(quote.getBidPrice());
//			boolean willTransact = price.lessThan(nbbo.getBestBid());
//			if (nbboBetter && willTransact) bestMarket = nbbo.getBestBidMarket();
//		}

		if (!bestMarket.equals(this))
			log(INFO, currentTime + " | " + agent + " " + getName()
					+ "::submitNMSBid: " + "NBBO" + nbbo + " better than "
					+ this + " Quote" + null);

		// FIXME Right now this causes an issue, because the agent won't know where its bid actually
		// got submitted. I think to fix this agent's should have a bid object, and that would have
		// the market it's in. If an agent submit's a bid to one market, but it gets routed, the
		// bid's market will get updated
		return bestMarket.submitBid(agent, price, quantity, currentTime,
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
		if (obj == null || !(obj instanceof Market2)) return false;
		Market2 market = (Market2) obj;
		return super.equals(market) && model.equals(market.model);
	}

	@Override
	public String toString() {
		return new String("[" + id + "]");
	}
}
