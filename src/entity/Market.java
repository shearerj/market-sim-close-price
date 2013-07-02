package entity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.TreeMap;

import systemmanager.Consts;

import logger.Logger;
import market.Bid;
import market.Price;
import market.Quote;
import market.Transaction;
import model.MarketModel;
import activity.Activity;
import activity.ProcessQuote;
import data.ObjectProperties;
import data.SystemData;
import event.TimeStamp;

/**
 * Base class for all markets.
 * 
 * @author ewah
 */
public abstract class Market extends Entity {

	// reorg
	protected final MarketModel model;
	protected ArrayList<Bid> bids;
	// XXX There should probably be more model specific stuff here...
	protected TreeMap<Integer, TimeStamp> executionTimes; // Hashed by bidID
	// end reorg

	// Model information
	// TODO equals method...
	protected int modelID; // ID of associated model

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
	public String marketType;

	public Market(int marketID, MarketModel model) {
		super(marketID);
		this.model = model;
	}

	public Market(int marketID, SystemData d, ObjectProperties p,
			MarketModel model) {
		super(marketID, d, p);

		agentIDs = new ArrayList<Integer>();
		buyers = new ArrayList<Integer>();
		sellers = new ArrayList<Integer>();

		lastQuoteTime = new TimeStamp(-1);
		lastClearTime = new TimeStamp(-1);
		nextQuoteTime = new TimeStamp(-1);
		nextClearTime = new TimeStamp(-1);

		lastClearPrice = new Price(-1);
		lastAskPrice = new Price(-1);
		lastBidPrice = new Price(-1);

		// reorg
		this.model = model;
	}

	/**
	 * Set the model ID for the market.
	 * 
	 * @param id
	 */
	public void linkModel(int id) {
		this.modelID = id;
	}

	/**
	 * @return model ID
	 */
	public int getModelID() {
		return this.modelID;
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
	 * @param clearTime
	 * @return
	 */
	public abstract Collection<Activity> clear(TimeStamp clearTime);

	/**
	 * @return map of bids (hashed by agent ID)
	 */
	public abstract HashMap<Integer, Bid> getBids();

	/**
	 * Returns all bids submitted to this market
	 * 
	 * @return bids
	 */
	public ArrayList<Bid> getAllBids() {
		return this.bids;
	}

	/**
	 * Add bid to the market.
	 * 
	 * @param b
	 * @param ts
	 *            TimeStamp of bid addition
	 * @return Collection<Activity> of further activities to add, if any
	 */
	public abstract Collection<? extends Activity> addBid(Bid b, TimeStamp ts);

	/**
	 * Remove bid for given agent from the market.
	 * 
	 * @param agentID
	 * @param ts
	 *            TimeStamp of bid removal
	 * @return Collection<Activity> (unused for now)
	 */
	public abstract Collection<? extends Activity> removeBid(int agentID,
			TimeStamp ts);

	/**
	 * Clears all the market's data structures.
	 */
	protected void clearAll() {
		buyers.clear();
		sellers.clear();
		agentIDs.clear();
	}

	/**
	 * Method to get the type of the market.
	 * 
	 * @return
	 */
	public String getType() {
		return marketType;
	}

	/**
	 * Send market's bid/ask to the Security Information Processor to be
	 * processed at some time (determined by latency) in the future.
	 * 
	 * @param ts
	 * @return
	 */
	public Collection<Activity> sendToSIP(TimeStamp ts) {
		int bid = this.getBidPrice().getPrice();
		int ask = this.getAskPrice().getPrice();
		Logger.log(Logger.INFO, ts + " | " + this + " SendToSIP(" + bid + ", "
				+ ask + ")");

		Collection<Activity> actMap = new ArrayList<Activity>();
		SIP sip = data.getSIP();
		if (data.nbboLatency.longValue() == 0) {
			actMap.add(new ProcessQuote(sip, this, bid, ask, Consts.INF_TIME));
		} else {
			actMap.add(new ProcessQuote(sip, this, bid, ask,
					ts.sum(data.nbboLatency)));
		}
		return actMap;
	}

	@Deprecated
	public void addExecutionTime(int bidID, TimeStamp ts) {
		// check if submission time contains it (if not, there is an error)
		for (Bid b : bids) {
			if (b.getBidID() == bidID) {
				executionTimes.put(bidID, ts.diff(b.getSubmissionTime()));
				return;
			}
		}
		System.err.println(this.getClass().getSimpleName()
				+ ":: submission time does not contain bidID " + bidID);
	}

	/**
	 * @return true if both BID & ASK are defined (!= -1)
	 */
	public boolean defined() {
		if (lastAskPrice.getPrice() == -1 || lastBidPrice.getPrice() == -1) {
			return false;
		}
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return new String("[" + this.getID() + "]");
	}

	/**
	 * Returns true if agent can sell in this market.
	 * 
	 * @param agentID
	 * @return boolean
	 */
	public boolean canSell(int agentID) {
		return sellers.contains(agentID);
	}

	/**
	 * Returns true if agent can buy in this market.
	 * 
	 * @param agentID
	 * @return boolean
	 */
	public boolean canBuy(int agentID) {
		return buyers.contains(agentID);
	}

	/**
	 * @return number of agents active in the market
	 */
	public int getNumAgents() {
		return agentIDs.size();
	}

	/**
	 * @return number of buyers in the market
	 */
	public int getNumBuyers() {
		return buyers.size();
	}

	/**
	 * @return number of sellers in the market
	 */
	public int getNumSellers() {
		return sellers.size();
	}

	/**
	 * @return array of agentIDs
	 */
	public ArrayList<Integer> getAgentIDs() {
		return agentIDs;
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
		return model.getTrans();
	}

	public MarketModel getMarketModel() {
		return model;
	}

	/**
	 * Quantizes the given integer based on the given granularity. Formula from
	 * Wikipedia (http://en.wikipedia.org/wiki/Quantization_signal_processing)
	 * 
	 * @param num
	 *            integer to quantize
	 * @param n
	 *            granularity (e.g. tick size)
	 * @return
	 */
	public static int quantize(int num, int n) {
		double tmp = 0.5 + Math.abs((double) num) / ((double) n);
		return Integer.signum(num) * n * (int) Math.floor(tmp);
	}

	public TreeMap<Integer, TimeStamp> getExecutionTimes() {
		return executionTimes;
	}

	@Deprecated
	public TreeMap<Integer, TimeStamp> getSubmissionTimes() {
		TreeMap<Integer, TimeStamp> map = new TreeMap<Integer, TimeStamp>();
		for (Bid b : this.bids)
			map.put(b.getBidID(), b.getSubmissionTime());
		return map;
	}

}
