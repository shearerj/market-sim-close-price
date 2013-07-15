package entity;

import static logger.Logger.log;
import static logger.Logger.Level.INFO;
import static systemmanager.Consts.INF_TIME;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import market.Bid;
import market.PQBid;
import market.PQOrderBook;
import market.Price;
import market.Quote;
import market.Transaction;
import model.MarketModel;
import activity.Activity;
import activity.SendToSIP;
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
	protected final Collection<IP> ips;

	// Statistics
	protected final Collection<Bid> bids; // All bids ever submitted to the market
	protected final TimeSeries depths; // Number of orders in the orderBook
	protected final TimeSeries spreads; // Bid-ask spread value
	protected final TimeSeries midQuotes; // Midpoint of bid/ask values

	// Market information
	protected TimeStamp lastClearTime, lastBidTime;
	protected Price lastClearPrice;
	protected Quote quote;

	public Market(int marketID, MarketModel model, int ipID) {
		super(marketID);
		this.model = model;
		this.bids = new ArrayList<Bid>();
		this.orderbook = new PQOrderBook(this);
		this.depths = new TimeSeries();
		this.spreads = new TimeSeries();
		this.midQuotes = new TimeSeries();
		this.ips = new ArrayList<IP>();

		// FIXME Add latency properly
		this.ip = new SMIP(0, new TimeStamp(0), this);
		ips.add(model.getSip());
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
	 * Add bid to the market.
	 */
	public Collection<? extends Activity> addBid(Bid bid, TimeStamp currentTime) {
		// FIXME This is bad. Add bid should enforce PQBid if this is the case,
		// or the PQBid interface should be pushed to Bid
		orderbook.insertBid((PQBid) bid);
		bids.add(bid);
		recordDepth(currentTime);
		return Collections.emptySet();
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
		recordDepth(currentTime); // Updating Depth

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
		
		Price askPrice = ((PQBid) orderbook.getAskQuote()).bidTreeSet.last().getPrice();
		Price bidPrice = ((PQBid) orderbook.getBidQuote()).bidTreeSet.first().getPrice();
		int askQuantity = ((PQBid) orderbook.getAskQuote()).bidTreeSet.last().getQuantity();
		int bidQuantity = ((PQBid) orderbook.getBidQuote()).bidTreeSet.first().getQuantity();
		
		quote = new Quote(this, askPrice, askQuantity, bidPrice, bidQuantity, currentTime);

		addSpread(currentTime);
		addMidQuote(currentTime);
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
