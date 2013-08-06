package entity;

import static logger.Logger.log;
import static logger.Logger.Level.INFO;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import market.BestBidAsk;
import market.Bid;
import market.OrderBook;
import market.Price;
import market.Quote;
import market.Transaction;
import model.MarketModel;
import activity.Activity;
import activity.SendToIP;
import activity.WithdrawBid;
import data.TimeSeries;
import event.TimeStamp;

/**
 * Base class for all markets.
 * 
 * @author ewah
 */
public abstract class Market extends Entity {

	protected final MarketModel model;
	protected final OrderBook orderbook;
	protected final SMIP ip;
	protected final SIP sip; // For REG NMS
	protected final Collection<IP> ips;

	// Statistics
	protected final Collection<Bid> bids; // All bids ever submitted to the market
	protected final TimeSeries depths, spreads, midQuotes;
	// depths: Number of orders in the orderBook
	// spreads: Bid-ask spread value
	// modQuotes: Midpoint of bid/ask values

	// Market information
	protected TimeStamp lastClearTime, lastBidTime;
	protected Price lastClearPrice;
	protected Quote quote;

	public Market(int marketID, MarketModel model) {
		super(marketID);
		this.model = model;
		this.bids = new ArrayList<Bid>();
		this.orderbook = new OrderBook(this);
		this.depths = new TimeSeries();
		this.spreads = new TimeSeries();
		this.midQuotes = new TimeSeries();

		// FIXME Add latency properly
		this.ips = new ArrayList<IP>();
		this.sip = model.getSIP();
		this.ip = new SMIP(model.nextIPID(), new TimeStamp(0), this);
		ips.add(sip);
		ips.add(ip);

		this.lastClearTime = TimeStamp.ZERO;
		this.lastClearPrice = null;
		this.quote = new Quote(this, null, null, TimeStamp.ZERO);
	}

	// TODO Rename
	public SMIP getIPSM() {
		return this.ip;
	}

	public Quote getQuote() {
		return quote;
	}

	/**
	 * @return Map of current bids
	 */
	// TODO Remove, this is too powerful
	public Map<Agent, Bid> getBids() {
		return orderbook.getActiveBids();
	}

	/**
	 * @return All bids submitted to this market
	 */
	public Collection<Bid> getAllBids() {
		return bids;
	}

	public void addIP(IP ip) {
		ips.add(ip);
	}

	/**
	 * Remove bid for given agent from the market.
	 */
	public Collection<? extends Activity> withdrawBid(Agent agent,
			TimeStamp currentTime) {
		orderbook.removeBid(agent.getID()); // TODO Change to Agent
		recordDepth(currentTime);
		return Collections.emptySet();
	}

	/**
	 * Clears the order book.
	 */
	public Collection<? extends Activity> clear(TimeStamp currentTime) {
		lastClearTime = currentTime;

		// Log prior quote
		log(INFO, currentTime + " | " + this + " Prior-clear Quote" + quote);

		// Update the orderbook
		orderbook.logActiveBids(currentTime);
		orderbook.logFourHeap(currentTime);

		List<Transaction> transactions = clearPricing(currentTime);

		// Different logging if no transactions
		if (transactions.isEmpty()) {
			log(INFO, currentTime + " | ....." + this + " "
					+ getClass().getSimpleName()
					+ "::clear: No change. Post-clear Quote" + quote);
		} else {
			orderbook.logActiveBids(currentTime);
			orderbook.logClearedBids(currentTime);
			orderbook.logFourHeap(currentTime);
			log(INFO, currentTime + " | ....." + this + " "
					+ getClass().getSimpleName()
					+ "::clear: Order book cleared: " + "Post-clear Quote"
					+ quote);
		}

		for (Transaction trans : transactions) {
			model.addTrans(trans);
			trans.getBuyer().addTransaction(trans, currentTime);
			trans.getSeller().addTransaction(trans, currentTime);
			lastClearPrice = trans.getPrice();
		}

		// FIXME SendToSIP(s) should probably happen after a quote update,
		// not after a clear. In general a clear should always cause a quote
		// update, but this is important to note. Also, if there's a clear,
		// but no transactions, it's probably not worth a SendToSIP, unless
		// you're a call market.
		return updateQuote(Collections.unmodifiableList(transactions),
				currentTime);
	}

	/**
	 * This method provides the abstraction for choosing what pricing rule the market clears at.
	 * This method should actually do the order book clear. Essentially this should be
	 * <code>orderbook.uniformPriceClear</code> or <code>orderbook.earliestPriceClear</code>
	 */
	// TODO There's probably a better way to do this. Maybe passing an object all of the way into
	// the order book, that given some bids, determines where to price them.
	protected abstract List<Transaction> clearPricing(TimeStamp currentTime);

	/**
	 * Updates the Markets current quote TODO This should be subsumed by sendToIPs. sendToIP should
	 * compute this quote and update things appropriately.
	 */
	protected Collection<? extends Activity> updateQuote(
			List<Transaction> transactions, TimeStamp currentTime) {
		// TODO This first part should be done a lot differently

		Bid ask = orderbook.getAskQuote();
		Bid bid = orderbook.getBidQuote();
		Price askPrice = ask.getPoints().last().getPrice();
		Price bidPrice = bid.getPoints().first().getPrice();

		quote = new Quote(this, askPrice, bidPrice, currentTime);

		log(INFO, currentTime + " | " + this + " SendToSIP" + quote);

		Collection<Activity> activities = new ArrayList<Activity>();
		for (IP ip : ips) {
			activities.add(new SendToIP(this, quote, transactions, ip,
					TimeStamp.IMMEDIATE));
		}

		recordDepth(currentTime);
		addSpread(currentTime);
		addMidQuote(currentTime);
		return activities;
	}

	protected void recordDepth(TimeStamp ts) {
		depths.add(ts, orderbook.getDepth());
	}

	protected void addSpread(TimeStamp ts) {
		spreads.add(ts, quote.getSpread());
	}

	protected void addMidQuote(TimeStamp ts) {
		double midQuote = quote.isDefined() ? Double.NaN
				: (quote.getBidPrice().getPrice() + quote.getAskPrice().getPrice()) / 2d;
		midQuotes.add(ts, midQuote);
	}

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

		Bid pqBid = new Bid(agent, this, currentTime);
		pqBid.addPoint(quantity, price);

		orderbook.insertBid(pqBid);
		bids.add(pqBid);
		recordDepth(currentTime);

		if (duration.equals(TimeStamp.IMMEDIATE))
			return Collections.emptySet();
		else
			return Collections.singleton(new WithdrawBid(agent, this,
					currentTime.plus(duration)));
	}

	public Collection<? extends Activity> submitMultiPointBid(Agent agent,
			Map<Price, Integer> priceQuantityMap, TimeStamp currentTime) {
		return submitMultiPointBid(agent, priceQuantityMap, currentTime,
				TimeStamp.IMMEDIATE);
	}

	public Collection<? extends Activity> submitMultiPointBid(Agent agent,
			Map<Price, Integer> priceQuantityMap, TimeStamp currentTime,
			TimeStamp duration) {
		log(INFO, currentTime + " | " + this + " " + agent + ": +"
				+ priceQuantityMap);

		Bid pqBid = new Bid(agent, this, currentTime);
		for (Entry<Price, Integer> priceQuant : priceQuantityMap.entrySet()) {
			int quantity = priceQuant.getValue();
			Price price = priceQuant.getKey();
			if (quantity == 0) continue; // TODO add check in PQPoint instead
			pqBid.addPoint(quantity, price); // TODO Handle quantization
		}

		orderbook.insertBid(pqBid);
		bids.add(pqBid);
		recordDepth(currentTime);

		if (duration.equals(TimeStamp.IMMEDIATE))
			return Collections.emptySet();
		else
			return Collections.singleton(new WithdrawBid(agent, this,
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
		Market bestMarket = this;

		if (quantity > 0) { // buy
			boolean nbboBetter = nbbo.getBestAsk() != null
					&& nbbo.getBestAsk().lessThan(quote.getAskPrice());
			boolean willTransact = price.greaterThan(nbbo.getBestAsk());
			if (nbboBetter && willTransact) bestMarket = nbbo.getBestAskMarket();
		} else { // sell
			boolean nbboBetter = nbbo.getBestBid() != null
					&& nbbo.getBestBid().greaterThan(quote.getBidPrice());
			boolean willTransact = price.lessThan(nbbo.getBestBid());
			if (nbboBetter && willTransact) bestMarket = nbbo.getBestBidMarket();
		}

		if (!bestMarket.equals(this))
			log(INFO, currentTime + " | " + agent + " " + getName()
					+ "::submitNMSBid: " + "NBBO" + nbbo + " better than "
					+ this + " Quote" + quote);

		// TODO Right now this causes an issue, because the agent won't know where its bid actually
		// got submitted. I think to fix this agent's should have a bid object, and that would have
		// the market it's in. If an agent submit's a bid to one market, but it gets routed, the
		// bid's market will get updated
		return bestMarket.submitBid(agent, price, quantity, currentTime,
				duration);
	}

	public boolean isDefined() {
		return quote.isDefined();
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
