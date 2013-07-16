package entity;

import static logger.Logger.log;
import static logger.Logger.Level.INFO;
import static systemmanager.Consts.INF_TIME;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import market.BestBidAsk;
import market.Bid;
import market.PQBid;
import market.PQOrderBook;
import market.Price;
import market.Quote;
import market.Transaction;
import model.MarketModel;
import activity.Activity;
import activity.SendToSIP;
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
	protected final PQOrderBook orderbook;
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
		this.orderbook = new PQOrderBook(this);
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
		this.quote = new Quote(this, null, 0, null, 0, TimeStamp.ZERO);
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
	public abstract Map<Agent, Bid> getBids();

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
	public Collection<? extends Activity> removeBid(Agent agent,
			TimeStamp currentTime) {
		orderbook.removeBid(agent.getID()); // TODO Change to Agent
		recordDepth(currentTime);
		return Collections.emptySet();
	}

	/**
	 * Method to get the type of the market.
	 * 
	 * @return
	 */
	@Deprecated
	public String getType() {
		return getClass().getSimpleName();
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

		Collection<Transaction> transes = orderbook.earliestPriceClear(currentTime);

		// Different logging if no transactions
		if (transes.isEmpty()) {
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

		for (Transaction trans : transes) {
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
		updateQuote(currentTime);
		return Collections.singleton(new SendToSIP(this, INF_TIME));
	}

	/**
	 * Send market's bid/ask to the Security Information Processor to be processed at some time
	 * (determined by latency) in the future.
	 */
	public Collection<? extends Activity> sendToSIP(TimeStamp currentTime) {
		Price bid = quote.getBidPrice();
		Price ask = quote.getAskPrice();
		log(INFO, currentTime + " | " + this + " SendToSIP(" + quote + ")");

		Collection<Activity> activities = new ArrayList<Activity>();
		for (IP ip : ips) {
			activities.add(ip.scheduleProcessQuote(this, bid, ask, currentTime));
		}
		return activities;
	}

	/**
	 * Updates the Markets current quote TODO This should be subsumed by sendToIPs. sendToIP should
	 * compute this quote and update things appropriately.
	 */
	protected void updateQuote(TimeStamp currentTime) {
		// TODO This first part should be done a lot differently

		PQBid ask = ((PQBid) orderbook.getAskQuote());
		PQBid bid = ((PQBid) orderbook.getBidQuote());
		Price askPrice = ask.bidTreeSet.last().getPrice();
		Price bidPrice = bid.bidTreeSet.first().getPrice();
		int askQuantity = ask.bidTreeSet.last().getQuantity();
		int bidQuantity = bid.bidTreeSet.first().getQuantity();

		quote = new Quote(this, askPrice, askQuantity, bidPrice, bidQuantity,
				currentTime);

		recordDepth(currentTime);
		addSpread(currentTime);
		addMidQuote(currentTime);
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
	public Collection<? extends Activity> submitBid(Agent agent, Price price,
			int quantity, TimeStamp currentTime) {
		return submitBid(agent, price, quantity, currentTime,
				TimeStamp.IMMEDIATE);
	}

	public Collection<? extends Activity> submitBid(Agent agent, Price price,
			int quantity, TimeStamp currentTime, TimeStamp duration) {
		if (quantity == 0) return Collections.emptySet();

		log(INFO, currentTime + " | " + this + " " + getType()
				+ "::submitBid: +(" + price + ", " + quantity + ") from "
				+ agent);

		PQBid pqBid = new PQBid(agent, this, currentTime);
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

	public Collection<? extends Activity> submitNMSBid(Agent agent,
			Price price, int quantity, TimeStamp currentTime) {
		return submitNMSBid(agent, price, quantity, currentTime,
				TimeStamp.IMMEDIATE);
	}

	public Collection<? extends Activity> submitNMSBid(Agent agent,
			Price price, int quantity, TimeStamp currentTime, TimeStamp duration) {
		BestBidAsk nbbo = sip.getBBOQuote();
		Market bestMarket;

		if (quantity > 0) { // buy
			boolean nbboBetter = nbbo.getBestAsk() != null
					&& nbbo.getBestAsk().lessThan(quote.getAskPrice());
			boolean willTransact = price.greaterThan(nbbo.getBestAsk());
			bestMarket = nbboBetter && willTransact ? nbbo.getBestAskMarket()
					: this;
		} else { // sell
			boolean nbboBetter = nbbo.getBestBid() != null
					&& nbbo.getBestBid().greaterThan(quote.getBidPrice());
			boolean willTransact = price.lessThan(nbbo.getBestBid());
			bestMarket = nbboBetter && willTransact ? nbbo.getBestBidMarket()
					: this;
		}

		if (!bestMarket.equals(this))
			log(INFO, currentTime + " | " + agent + " " + getType()
					+ "::submitNMSBid: " + "NBBO" + nbbo + " better than "
					+ this + " Quote" + quote);

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
