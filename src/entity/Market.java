package entity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import com.sun.tools.javac.util.List;

import systemmanager.Consts;

import logger.Logger;
import market.Bid;
import market.PQOrderBook;
import market.Price;
import market.Quote;
import market.Transaction;
import model.MarketModel;
import activity.Activity;
import activity.ProcessQuote;
import activity.SendToSIP;
import data.ObjectProperties;
import data.SystemData;
import data.TimeSeries;
import event.TimeStamp;

/**
 * Base class for all markets.
 * 
 * @author ewah
 */
public abstract class Market extends Entity {
	
	// XXX There should probably be more model specific stuff here...
	// reorg
	protected final MarketModel model;
	protected PQOrderBook orderbook;
	//Statistics
	protected List<Bid> bids;			//All bids ever submitted to the market
	protected TimeSeries depths;		//Number of orders in the orderBook
	protected TimeSeries spreads;		//Bid-ask spread value
	protected TimeSeries midQuotes;		//Midpoint of bid/ask values
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
		this.orderbook = new PQOrderBook(this);
		this.depths = new TimeSeries();
		this.spreads = new TimeSeries();
		this.midQuotes = new TimeSeries();
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
	public Collection<Activity> clear(TimeStamp clearTime) {
		Collection<Activity> actList = new ArrayList<Activity>();
		
		//Log prior quote
		Logger.log(Logger.INFO, clearTime + " | " + this + " Prior-clear Quote" + 
				this.quote(clearTime));
		
		//Update the orderbook
		orderbook.logActiveBids(clearTime);
		orderbook.logFourHeap(clearTime);
		
		ArrayList<Transaction> trans = orderbook.earliestPriceClear(clearTime);
		lastClearTime = clearTime;
		
		//If there are no new transactions
		if(trans == null) {
			this.addDepth(clearTime, orderbook.getDepth());
			
			Logger.log(Logger.INFO, clearTime + " | ....." + this + " " + 
					this.getName() + "::clear: No change. Post-clear Quote" +  
					this.quote(clearTime));
			
			actList.add(new SendToSIP(this, clearTime));
			return actList;
		}
		//Otherwise, update and log Transactions
		for(Transaction tr : trans) {
			model.addTrans(tr);
			//update and log transactions
			tr.getBuyer().updateTransactions(clearTime);
			tr.getBuyer().logTransactions(clearTime);
			tr.getSeller().updateTransactions(clearTime);
			tr.getSeller().logTransactions(clearTime);
			lastClearPrice = tr.price;			
		}
		
		//Orderbook logging
		orderbook.logActiveBids(clearTime);
		orderbook.logClearedBids(clearTime);
		orderbook.logFourHeap(clearTime);
		//Updating Depth
		this.addDepth(clearTime, orderbook.getDepth());
		Logger.log(Logger.INFO, clearTime + " | ....." + toString() + " " + 
				this.getName() + "::clear: Order book cleared: " +
				"Post-clear Quote" + this.quote(clearTime));
		actList.add(new SendToSIP(this, clearTime));
		return actList;
	}

	/**
	 * @return map of bids (hashed by agent ID)
	 */
	public abstract HashMap<Integer, Bid> getBids();

	/**
	 * Returns all bids submitted to this market
	 * 
	 * @return bids
	 */
	public List<Bid> getAllBids() {
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
	
	protected void addSpread(TimeStamp ts, int point) {
		this.spreads.add(ts, (double) point);
	}
	
	protected void addMidQuote(TimeStamp ts, int bid, int ask) {
		double midQuote = Double.NaN;
		if (bid != Consts.INF_PRICE && ask != Consts.INF_PRICE) {
			midQuote = (bid + ask) / 2;
		}
		this.midQuotes.add(ts, midQuote);
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

	@Deprecated
	public TreeMap<Integer, TimeStamp> getSubmissionTimes() {
		TreeMap<Integer, TimeStamp> map = new TreeMap<Integer, TimeStamp>();
		for (Bid b : this.bids)
			map.put(b.getBidID(), b.getSubmitTime());
		return map;
	}
}
