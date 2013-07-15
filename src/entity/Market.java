package entity;

import static logger.Logger.log;
import static logger.Logger.Level.INFO;
import static systemmanager.Consts.INF_TIME;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import market.Bid;
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

	// Statistics
	protected final Collection<Bid> bids; // All bids ever submitted to the
											// market
	protected final TimeSeries depths; // Number of orders in the orderBook
	protected final TimeSeries spreads; // Bid-ask spread value
	protected final TimeSeries midQuotes; // Midpoint of bid/ask values
	// end reorg

	// Model information
	// TODO equals method...
	protected int modelID; // ID of associated model
	protected int marketID;

	// Agent information
	protected ArrayList<Integer> buyers;
	protected ArrayList<Integer> sellers;
	protected ArrayList<Integer> agentIDs;

	// Market information
	public TimeStamp nextQuoteTime;
	public TimeStamp nextClearTime; // Most important for call markets
	public TimeStamp lastQuoteTime;
	public TimeStamp lastClearTime;
	public TimeStamp lastBidTime;
	public Price lastClearPrice;
	public Price lastAskPrice;
	public Price lastBidPrice;
	public int lastAskQuantity;
	public int lastBidQuantity;
	protected IPSM ip_SM;
	public String marketType;
	protected final Collection<AbstractIP> ips;

	/*
	 * public Market(int marketID, MarketModel model, int ipID) {
	 * super(marketID);
	 * 
	 * this.model = model; }
	 */// not sure which constructor to use...

	public Market(int marketID, MarketModel model, int ipID) {
		super(marketID);
		agentIDs = new ArrayList<Integer>();
		buyers = new ArrayList<Integer>();
		sellers = new ArrayList<Integer>();
		this.marketID = marketID;
		this.ips = new ArrayList<AbstractIP>();

		lastQuoteTime = new TimeStamp(-1);
		lastClearTime = new TimeStamp(-1);
		nextQuoteTime = new TimeStamp(-1);
		nextClearTime = new TimeStamp(-1);

		lastClearPrice = new Price(-1);
		lastAskPrice = new Price(-1);
		lastBidPrice = new Price(-1);

		this.ip_SM = setupIPSM();

		// reorg
		this.model = model;

		this.bids = new ArrayList<Bid>();
		this.orderbook = new PQOrderBook(this);
		this.depths = new TimeSeries();
		this.spreads = new TimeSeries();
		this.midQuotes = new TimeSeries();
		addIP(model.sip);
	}

	public void addIP(AbstractIP ip) {
		ips.add(ip);
	}

	protected IPSM setupIPSM() {
		// TODO should have global variable of SM Latency
		IPSM ip_SM = new IPSM(0, marketID, new TimeStamp(0), this);
		addIP(ip_SM);
		return ip_SM;
	}

	/**
	 * @return IP_SM
	 */
	public IPSM getIPSM() {
		return this.ip_SM;
	}

	/**
	 * @return bid (highest buy offer)
	 */
	public abstract Bid getBidQuote();

	/**
	 * @return ask (lowest sell offer)
	 */
	public abstract Bid getAskQuote();

	/**
	 * @return bid price (highest)
	 */
	public abstract Price getBidPrice();

	/**
	 * @return ask price (lowest
	 */
	public abstract Price getAskPrice();

	/**
	 * Publish quotes.
	 * 
	 * @param quoteTime
	 */
	public abstract Quote quote(TimeStamp quoteTime);

	/**
	 * Clears the order book.
	 * 
	 * @param currentTime
	 * @return
	 */
	public Collection<? extends Activity> clear(TimeStamp currentTime) {
		// Log prior quote
		log(INFO,
				currentTime + " | " + this + " Prior-clear Quote"
						+ this.quote(currentTime));

		// Update the orderbook
		orderbook.logActiveBids(currentTime);
		orderbook.logFourHeap(currentTime);

		Collection<Transaction> transes = orderbook.earliestPriceClear(currentTime);
		lastClearTime = currentTime;
		addDepth(currentTime, orderbook.getDepth()); // Updating Depth

		// Different logging if no transactions
		if (transes.isEmpty()) {
			log(INFO, currentTime + " | ....." + this + " "
					+ getClass().getSimpleName()
					+ "::clear: No change. Post-clear Quote"
					+ quote(currentTime));
		} else {
			orderbook.logActiveBids(currentTime);
			orderbook.logClearedBids(currentTime);
			orderbook.logFourHeap(currentTime);
			log(INFO, currentTime + " | ....." + this + " "
					+ getClass().getSimpleName()
					+ "::clear: Order book cleared: " + "Post-clear Quote"
					+ quote(currentTime));
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
		return Collections.singleton(new SendToSIP(this, INF_TIME));
	}

	/**
	 * @return map of bids (hashed by agent ID)
	 */
	public abstract Map<Agent, Bid> getBids();

	/**
	 * Returns all bids submitted to this market
	 * 
	 * @return bids
	 */
	public Collection<Bid> getAllBids() {
		return bids;
	}

	/**
	 * Add bid to the market.
	 * 
	 * @param b
	 * @param currentTime
	 *            TimeStamp of bid addition
	 * @return Collection<Activity> of further activities to add, if any
	 */
	public abstract Collection<? extends Activity> addBid(Bid b,
			TimeStamp currentTime);

	/**
	 * Remove bid for given agent from the market.
	 * 
	 * @param agentID
	 * @param currentTime
	 *            TimeStamp of bid removal
	 * @return Collection<Activity> (unused for now)
	 */
	public abstract Collection<? extends Activity> removeBid(Agent agent,
			TimeStamp currentTime);

	/**
	 * Method to get the type of the market.
	 * 
	 * @return
	 */
	@Deprecated
	public String getType() {
		return marketType;
	}

	/**
	 * Send market's bid/ask to the Security Information Processor to be
	 * processed at some time (determined by latency) in the future.
	 * 
	 * @param currentTime
	 * @return
	 */
	public Collection<? extends Activity> sendToSIP(TimeStamp currentTime) {
		// TODO switch to Quote object
		int bid = this.getBidPrice().getPrice();
		int ask = this.getAskPrice().getPrice();
		log(INFO, currentTime + " | " + this + " SendToSIP(" + bid + ", " + ask
				+ ")");

		Collection<Activity> activities = new ArrayList<Activity>();
		for (AbstractIP ip : ips) {
			activities.add(ip.scheduleProcessQuote(this, bid, ask, currentTime));
		}
		return activities;
	}

	public boolean defined() {
		return lastAskPrice != null && lastBidPrice != null;
	}

	public Price getLastClearPrice() {
		return lastClearPrice;
	}

	public TimeStamp getNextClearTime() {
		return nextClearTime;
	}

	public TimeStamp getLastClearTime() {
		return lastClearTime;
	}

	public TimeStamp getNextQuoteTime() {
		return nextQuoteTime;
	}

	public TimeStamp getLastQuoteTime() {
		return lastQuoteTime;
	}

	public Collection<Transaction> getModelTrans() {
		return this.model.getTrans();
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

	protected void addDepth(TimeStamp ts, int point) {
		this.depths.add(ts, (double) point);
	}

	protected void addSpread(TimeStamp ts, int spread) {
		this.spreads.add(ts, spread);
	}

	protected void addMidQuote(TimeStamp ts, Price bid, Price ask) {
		double midQuote = Double.NaN;
		if (!bid.equals(Price.INF) && !ask.equals(Price.INF)) {
			midQuote = (bid.getPrice() + ask.getPrice()) / 2;
		}
		this.midQuotes.add(ts, midQuote);
	}

	@Deprecated
	public TreeMap<Integer, TimeStamp> getSubmissionTimes() {
		TreeMap<Integer, TimeStamp> map = new TreeMap<Integer, TimeStamp>();
		for (Bid b : this.bids)
			map.put(b.getBidID(), b.getSubmitTime());
		return map;
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
