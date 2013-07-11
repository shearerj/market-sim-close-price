package entity;

import static logger.Logger.log;
import static logger.Logger.Level.INFO;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import logger.Logger;
import market.Bid;
import market.PQOrderBook;
import market.Price;
import market.Quote;
import market.Transaction;
import model.MarketModel;
import systemmanager.Consts;
import activity.Activity;
import activity.ProcessQuote;
import activity.SendToSIP;
import activity.UpdateNBBO;
import data.EntityProperties;
import data.SystemData;
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
	public IP_SM ip_SM;
	public String marketType;
	Collection<AbstractIP> ips;
	
/*
	public Market(int marketID, MarketModel model, int ipID) {
		super(marketID);
		
		this.model = model;
	}*/  // not sure which constructor to use... 

	public Market(int marketID, MarketModel model, int ipID) {
		super(marketID);
		agentIDs = new ArrayList<Integer>();
		buyers = new ArrayList<Integer>();
		sellers = new ArrayList<Integer>();
		this.marketID = marketID;

		lastQuoteTime = new TimeStamp(-1);
		lastClearTime = new TimeStamp(-1);
		nextQuoteTime = new TimeStamp(-1);
		nextClearTime = new TimeStamp(-1);

		lastClearPrice = new Price(-1);
		lastAskPrice = new Price(-1);
		lastBidPrice = new Price(-1);
		
		setupIPSM();
		
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
	
	protected void setupIPSM() {
		// TODO should have global variable of SM Latency
		IP_SM ip_SM = new IP_SM(0, marketID, new TimeStamp(0), this);
		addIP(ip_SM);
	} 
	
	/**
	 * @return IP_SM
	 */
	public IP_SM getIPSM() {
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
	 * @return last bid quantity
	 */
	public int getBidQuantity() {
		return lastBidQuantity;
	}

	/**
	 * @return last ask quantity
	 */
	public int getAskQuantity() {
		return lastAskQuantity;
	}

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

		ArrayList<Transaction> trans = orderbook.earliestPriceClear(currentTime);
		lastClearTime = currentTime;

		// If there are no new transactions
		if (trans == null) {
			this.addDepth(currentTime, orderbook.getDepth());

			log(INFO,
					currentTime + " | ....." + this + " " + this.getName()
							+ "::clear: No change. Post-clear Quote"
							+ this.quote(currentTime));

			// FIXME SendToSIP(s) should probably happen after a quote update,
			// not after a clear. In general a clear should always cause a quote
			// update, but this is important to note. Also, if there's a clear,
			// but no transactions, it's probably not worth a SendToSIP, unless
			// you're a call market.
			return Collections.singleton(new SendToSIP(this, currentTime));
		}
		// Otherwise, update and log Transactions
		for (Transaction tr : trans) {
			model.addTrans(tr);
			// update and log transactions
			tr.getBuyer().updateTransactions(currentTime);
			tr.getBuyer().logTransactions(currentTime);
			tr.getSeller().updateTransactions(currentTime);
			tr.getSeller().logTransactions(currentTime);
			lastClearPrice = tr.getPrice();
		}

		// Orderbook logging
		orderbook.logActiveBids(currentTime);
		orderbook.logClearedBids(currentTime);
		orderbook.logFourHeap(currentTime);

		// Updating Depth
		this.addDepth(currentTime, orderbook.getDepth());
		Logger.log(Logger.Level.INFO, currentTime + " | ....." + toString()
				+ " " + this.getName() + "::clear: Order book cleared: "
				+ "Post-clear Quote" + this.quote(currentTime));

		return Collections.singleton(new SendToSIP(this, currentTime));
	}

	/**
	 * @return map of bids (hashed by agent ID)
	 */
	public abstract Map<Integer, Bid> getBids();

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

		Collection<Activity> actMap = new ArrayList<Activity>();
		
		for (AbstractIP ip : ips) {
			actMap.add(ip.scheduleProcessQuote(this, bid, ask, currentTime));
		}
		return actMap;
	}

	/**
	 * @return true if both BID & ASK are defined (!= -1)
	 */
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

	public MarketModel getMarketModel() {
		return this.model;
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
		if (obj == null || !(obj instanceof Market))
			return false;
		Market market = (Market) obj;
		return super.equals(market) && model.equals(market.model);
	}

	@Override
	public String toString() {
		return new String("[" + id + "]");
	}
}
