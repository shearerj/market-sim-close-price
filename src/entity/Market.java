package entity;

import static logger.Logger.log;
import static logger.Logger.Level.INFO;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
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
	protected Collection<Bid> bids;			//All bids ever submitted to the market
	protected TimeSeries depths;		//Number of orders in the orderBook
	protected TimeSeries spreads;		//Bid-ask spread value
	protected TimeSeries midQuotes;		//Midpoint of bid/ask values
	// end reorg

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
		
		this.bids = new ArrayList<Bid>();
		this.orderbook = new PQOrderBook(this);
		this.depths = new TimeSeries();
		this.spreads = new TimeSeries();
		this.midQuotes = new TimeSeries();
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
		log(INFO, clearTime + " | " + this + " Prior-clear Quote" + 
				this.quote(clearTime));
		
		//Update the orderbook
		orderbook.logActiveBids(clearTime);
		orderbook.logFourHeap(clearTime);
		
		ArrayList<Transaction> trans = orderbook.earliestPriceClear(clearTime);
		lastClearTime = clearTime;
		
		//If there are no new transactions
		if(trans == null) {
			this.addDepth(clearTime, orderbook.getDepth());
			
			log(INFO, clearTime + " | ....." + this + " " + 
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
			lastClearPrice = tr.getPrice();			
		}
		
		//Orderbook logging
		orderbook.logActiveBids(clearTime);
		orderbook.logClearedBids(clearTime);
		orderbook.logFourHeap(clearTime);
		//Updating Depth
		this.addDepth(clearTime, orderbook.getDepth());
		Logger.log(Logger.Level.INFO, clearTime + " | ....." + toString() + " " + 
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
	public Collection<Bid> getAllBids() {
		return bids;
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
		log(INFO, ts + " | " + this + " SendToSIP(" + bid + ", "
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
		if (bid != Consts.INF_PRICE && ask != Consts.INF_PRICE) {
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
