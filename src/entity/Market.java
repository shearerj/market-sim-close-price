package entity;

import event.*;
import model.*;
import market.*;
import activity.*;
import systemmanager.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

/**
 * Base class for all markets.
 * 
 * @author ewah
 */
public abstract class Market extends Entity {

	// Model information
	protected int modelID;				// ID of associated model
	
	// Agent information
	protected ArrayList<Integer> buyers;
	protected ArrayList<Integer> sellers;
	protected ArrayList<Integer> agentIDs;

	// Market information
	public TimeStamp nextQuoteTime;
	public TimeStamp nextClearTime;		// Most important for call markets
	public TimeStamp lastQuoteTime;
	public TimeStamp lastClearTime;
	public TimeStamp lastBidTime;
	public Price lastClearPrice;
	public Price lastAskPrice;
	public Price lastBidPrice;
	public int lastAskQuantity;
	public int lastBidQuantity;
	public String marketType;
	

	/**
	 * @param marketID
	 * @param d
	 * @param p
	 * @param l
	 */
	public Market(int marketID, SystemData d, ObjectProperties p, Log l) {
		super(marketID, d, p, l);

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
	 * Add bid to the market.
	 * 
	 * @param b
	 * @param ts TimeStamp of bid addition
	 * @return Collection<Activity> of further activities to add, if any
	 */
	public abstract Collection<Activity> addBid(Bid b, TimeStamp ts);

	/**
	 * Remove bid for given agent from the market.
	 * 
	 * @param agentID
	 * @param ts TimeStamp of bid removal
	 * @return Collection<Activity> (unused for now)
	 */
	public abstract Collection<Activity> removeBid(int agentID, TimeStamp ts);
	
	
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
 	 * Send market's bid/ask to the Security Information Processor to be processed at some time
 	 * (determined by latency) in the future.
 	 *
 	 * @param ts
 	 * @return
 	 */
	public Collection<Activity> sendToSIP(TimeStamp ts) {
                int bid = this.getBidPrice().getPrice();
                int ask = this.getAskPrice().getPrice();
		log.log(Log.INFO, ts + " | " + this + " SendToSIP(" + bid + ", " + ask + ")");

		Collection<Activity> actMap = new ArrayList<Activity>();
		MarketModel model = data.getModelByMarketID(this.getID());
		SIP sip = data.getSIP();
		if (data.nbboLatency.longValue() == 0) {
			sip.processQuote(this, bid, ask, ts);
			sip.updateNBBO(model, ts);
		} else {
			TimeStamp tsNew = ts.sum(data.nbboLatency);
			actMap.add(new ProcessQuote(sip, this, bid, ask, tsNew));
			actMap.add(new UpdateNBBO(sip, model, tsNew));
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
	
	/* (non-Javadoc)
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

	
	/**
	 * Quantizes the given integer based on the given granularity. Formula from
	 * Wikipedia (http://en.wikipedia.org/wiki/Quantization_signal_processing)
	 * 
	 * @param num integer to quantize
	 * @param n granularity (e.g. tick size)
	 * @return
	 */
	public static int quantize(int num, int n) {
		double tmp = 0.5 + Math.abs((double) num) / ((double) n);
		return Integer.signum(num) * n * (int)Math.floor(tmp);
	}

}
