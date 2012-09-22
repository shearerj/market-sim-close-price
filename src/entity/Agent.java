package entity;

import event.*;
import market.*;
import activity.*;
import systemmanager.*;

import java.util.Iterator;
import java.util.Random;
import java.util.Vector;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.TreeSet;

/**
 * Base class for all agents.
 * 
 * @author ewah
 */
/**
 * @author ewah
 *
 */
public abstract class Agent extends Entity {

	protected Random rand;
	
	// Market information (all hashed by market ID, as ID may be negative)
	protected ArrayList<Integer> marketIDs;
	protected HashMap<Integer,Price> bidPrice;
	protected HashMap<Integer,Price> askPrice;
	protected HashMap<Integer,Bid> currentBid;
	protected HashMap<Integer,Vector<Quote>> quotes;
	protected HashMap<Integer,Integer> initBid;
	protected HashMap<Integer,Integer> initAsk;
	protected HashMap<Integer,Integer> prevBid;
	protected HashMap<Integer,Integer> prevAsk;
	protected HashMap<Integer,TimeStamp> lastQuoteTime;
	protected HashMap<Integer,TimeStamp> nextQuoteTime;
	protected HashMap<Integer,TimeStamp> lastClearTime;
	protected TimeStamp lastTransTime;
	protected Integer lastTransID;		// ID of the last transaction fetched
	protected BestBidAsk lastNBBOQuote;
	protected BestBidAsk lastGlobalQuote;
	protected int tickSize;
	
	// For quote generation
	protected Quoter quoter;
	
	// Agent parameters
	protected int privateValue;
	protected int positionLimit;
	protected AgentProperties params;		// stores all parameters
	protected String agentType;
	protected TimeStamp arrivalTime;

	// 	Tracking cash flow
	protected int cashBalance;
	protected int positionBalance;
	protected int averageCost;
	protected int realizedProfit;

	/**
	 * Constructor
	 * @param agentID
	 */
	public Agent(int agentID, SystemData d, AgentProperties p, Log l) {
		super(agentID, d, l);
		
		rand = new Random();
		
		// initialize all containers
		marketIDs = new ArrayList<Integer>();
		bidPrice = new HashMap<Integer,Price>();
		askPrice = new HashMap<Integer,Price>();
		currentBid = new HashMap<Integer,Bid>();
		lastQuoteTime = new HashMap<Integer,TimeStamp>();
		nextQuoteTime = new HashMap<Integer,TimeStamp>();
		lastClearTime = new HashMap<Integer,TimeStamp>();
		quotes = new HashMap<Integer,Vector<Quote>>();
		initBid = new HashMap<Integer,Integer>();
		initAsk = new HashMap<Integer,Integer>();
		prevBid = new HashMap<Integer,Integer>();
		prevAsk = new HashMap<Integer,Integer>();
		
		lastGlobalQuote = new BestBidAsk();
		lastNBBOQuote = new BestBidAsk();
		
		privateValue = -1;
		cashBalance = 0;
		positionBalance = 0;
		averageCost = 0;
		realizedProfit = 0;
		
		tickSize = d.tickSize;
		quoter = d.getQuoter();
	}
	
	
	/**
	 * @return observation to include in the output file
	 */
	public abstract HashMap<String, Object> getObservation();
	
	
	/**
	 * Set AgentProperties for this agent.
	 * @param p AgentProperties object
	 */
	public void setProperties(AgentProperties p) {
		params = p;
	}
	
	/**
	 * Add property to AgentProperties container.
	 * @param key
	 * @param val
	 */
	public void addProperty(String key, String val) {
		params.put(key, val);
	}
	
	/**
	 * @return AgentProperties of this agent.
	 */
	public AgentProperties getProperties() {
		return params;
	}
	
	/**
	 * @return arrival time for an agent.
	 */
	public TimeStamp getArrivalTime() {
		return arrivalTime; 
	}
	
	
	/**
	 * @return private value of agent.
	 */
	public int getPrivateValue() {
		return privateValue;
	}
	
	/**
	 * Method to get the type of the agent.
	 * @return
	 */
	public String getType() {
		return agentType;
	}
	
	
	public String toString() {
		return new String("(" + this.getID() + ")");
	}
	
	/**
	 * Computes a randomized sleep time based on sleepTime & sleepVar.
	 * @param sleepTime
	 * @param sleepVar
	 * @return
	 */
	public int getRandSleepTime(int sleepTime, double sleepVar) {
		return (int) Math.round(getNormalRV(sleepTime, sleepVar));
	}
	
	/**
	 * Clears all the agent's data structures.
	 */
	protected void clearAll() {
		marketIDs.clear();
		bidPrice.clear();
		askPrice.clear();
		currentBid.clear();
		lastQuoteTime.clear();
		nextQuoteTime.clear();
		lastClearTime.clear();
		quotes.clear();
		initBid.clear();
		initAsk.clear();
		prevBid.clear();
		prevAsk.clear();
	}
	
	/**
	 * @param mktID
	 * @return number of quotes
	 */
	public int getQuoteSize(int mktID) {
		return quotes.get(mktID).size();
	}

	/**
	 * Gets latest quote from a given market. If no quote available, returns 
	 * a default Quote object.
	 * 
	 * @param mktID
	 * @return most recent quote for the specified market
	 */
	public Quote getLatestQuote(int mktID) {
		if (quotes.isEmpty()) {
			return new Quote();
		} else if (quotes.get(mktID).isEmpty()) {
			return new Quote();
		}
		return quotes.get(mktID).lastElement();
	}

	/**
	 * @param mktID
	 * @param idx
	 * @return quote at a specified index
	 */
	public Quote getQuoteAt(int mktID, int idx) {
		return quotes.get(mktID).get(idx);
	}

	/**
	 * @param mktID
	 * @return bid price for the specified market
	 */
	public Price getBidPrice(int mktID) {
		return bidPrice.get(mktID);
	}

	/**
	 * @param mktID
	 * @return ask price for the specified market
	 */
	public Price getAskPrice(int mktID) {
		return askPrice.get(mktID);
	}

	/**
	 * @return number of markets that agent is active in
	 */
	public int getNumMarkets() {
		return marketIDs.size();
	}

	/**
	 * @return current cash balance
	 */
	public int getCashBalance() {
		return cashBalance;
	}

	/**
	 * @return array of marketIDs
	 */
	public ArrayList<Integer> getMarketIDs() {
		return marketIDs;
	}
	
	/**
	 * Enters market by adding market to data structures.
	 * @param mkt
	 * @param ts
	 */
	protected void enterMarket(Market mkt, TimeStamp ts) {
		mkt.agentIDs.add(this.ID);
		mkt.buyers.add(this.ID);
		mkt.sellers.add(this.ID);
		quotes.put(mkt.ID, new Vector<Quote>());
		arrivalTime = ts;

		// Initialize bid/ask containers
		prevBid.put(mkt.ID, 0);
		prevAsk.put(mkt.ID, 0);
		initBid.put(mkt.ID, -1);
		initAsk.put(mkt.ID, -1);
	}
	
	/**
	 * Exits market by removing all entries hashed by the specified market ID.
	 *  
	 * @param mktID
	 */
	protected void exitMarket(int mktID) {
		marketIDs.remove(marketIDs.indexOf(mktID));
		bidPrice.remove(mktID);
		askPrice.remove(mktID);
		currentBid.remove(mktID);
		lastQuoteTime.remove(mktID);
		nextQuoteTime.remove(mktID);
		lastClearTime.remove(mktID);
		quotes.remove(mktID);
		initBid.remove(mktID);
		initAsk.remove(mktID);
		prevBid.remove(mktID);
		prevAsk.remove(mktID);
	}

	/**
	 * Wrapper method to group activities of 1) submitting bid to market,
	 * 2) processing/clearing bid.
	 * 
	 * @param mkt
	 * @param price
	 * @param quantity
	 * @param ts
	 * @return
	 */
	public ActivityHashMap addBid(Market mkt, int price, int quantity, TimeStamp ts) {
		ActivityHashMap actMap = new ActivityHashMap();
		actMap.insertActivity(new SubmitBid(this, mkt, price, quantity, ts));
		return actMap;
	}

	/**
	 * Wrapper method to group activities of 1) submitting multiple-offer bid to a market,
	 * 2) process/clear bid.
	 * 
	 * @param mkt
	 * @param price
	 * @param quantity
	 * @param ts
	 * @return
	 */
	public ActivityHashMap addMultipleBid(Market mkt, int[] price, int[] quantity, TimeStamp ts) {
		ActivityHashMap actMap = new ActivityHashMap();
		actMap.insertActivity(new SubmitMultipleBid(this, mkt, price, quantity, ts));
		return actMap;
	}

	/**
	 * Submit a bid to the specified market.
	 * 
	 * @param mkt
	 * @param price
	 * @param quantity
	 * @param ts
	 * @return
	 */
	public ActivityHashMap submitBid(Market mkt, int price, int quantity, TimeStamp ts) {
		
		if (quantity == 0) return null;

		log.log(Log.INFO, ts.toString() + " | " + mkt.toString() + " " + this.toString() + 
				": +(" + price + ", " + quantity + ")");
		
		int p = Market.quantize(price, tickSize);
		PQBid pqBid = new PQBid(this.ID, mkt.ID);
		pqBid.addPoint(quantity, new Price(p));
		pqBid.timestamp = ts;
		currentBid.put(mkt.ID, pqBid);
		return mkt.addBid(pqBid, ts);
	}	

	/**
	 * Submits a multiple-point/offer bid to the specified market.
	 * 
	 * @param mkt
	 * @param price
	 * @param quantity
	 * @param ts
	 * @return
	 */
	public ActivityHashMap submitMultipleBid(Market mkt, int[] price, int[] quantity, TimeStamp ts) {

		if (price.length != quantity.length) {
			log.log(Log.ERROR, "Agent::submitMultipleBid: Price/Quantity arrays are not the same length");
			return null;
		}
		
		log.log(Log.INFO, ts.toString() + " | " + mkt.toString() + " " + this.toString() +  
				": +(" + price.toString() +	", " + quantity.toString() + ")");
		
		PQBid pqBid = new PQBid(this.ID, mkt.ID);
		pqBid.timestamp = ts;
		for (int i = 0; i < price.length; i++) {
			if (quantity[i] != 0) {
				int p = Market.quantize(price[i], tickSize);
				pqBid.addPoint(quantity[i], new Price(p));
			}
		}
		currentBid.put(mkt.ID, pqBid);	
		return mkt.addBid(pqBid, ts);
	}

	/**
	 * Withdraws a bid by replacing it with an empty bid.
	 * 
	 * @param b
	 * @param mkt
	 * @return
	 */
	public ActivityHashMap withdrawBid(Market mkt, TimeStamp ts) {
		log.log(Log.INFO, ts.toString() + " | " + this.toString() + " withdraw bid from " + mkt.toString());
		return mkt.removeBid(this.ID, ts);
	}

	/**
	 * Update quotes from all markets that the agent is active in.
	 * 
	 * @param ts
	 * @return
	 */
	public ActivityHashMap updateAllQuotes(TimeStamp ts) {
		for (Iterator<Integer> i = marketIDs.iterator(); i.hasNext(); ) {
			Market mkt = data.markets.get(i.next());
			updateQuotes(mkt, ts);
		}
		lastGlobalQuote = quoter.findBestBidOffer(marketIDs);
		lastNBBOQuote = quoter.lastQuote;
		
		log.log(Log.INFO, ts.toString() + " | " + this.toString() + " Global" + lastGlobalQuote + 
				", NBBO" + lastNBBOQuote);

		return null;
	}

	/**
	 * Updates quotes for the given market.
	 * 
	 * @param mkt
	 * @param ts
	 */
	public void updateQuotes(Market mkt, TimeStamp ts) {
		Quote q = mkt.quote(ts);
		if (q != null) {
			if (q.lastAskPrice == null)
				askPrice.put(mkt.ID, new Price(Consts.INF_PRICE));
			else
				askPrice.put(mkt.ID, q.lastAskPrice);
			if (q.lastBidPrice == null)
				bidPrice.put(mkt.ID, new Price(0));
			else
				bidPrice.put(mkt.ID, q.lastBidPrice);

			if (q.lastQuoteTime != null)
				lastQuoteTime.put(mkt.ID, q.lastQuoteTime);

			if (q.lastClearTime != null && lastClearTime.get(mkt.ID) != q.lastClearTime) {
				lastClearTime.put(mkt.ID, q.lastClearTime);
			}
		} else {
			log.log(Log.ERROR, "Agent::updateQuotes: Quote is null.");
		}
		quotes.get(mkt.ID).add(q);
	}

	
	/**
	 * Logs transactions and prints a summary of performance so far.
	 * 
	 * @param ts
	 */
	public void logTransactions(TimeStamp ts) {
		
		int rp = getRealizedProfit();
		int up = getUnrealizedProfit();

		String s = ts.toString() + " | " + this.toString() + ": Current Position=" + positionBalance +
				", Realized Profit=" + rp + ", Unrealized Profit=" + up;
		log.log(Log.INFO, s);
	}
	
	
	
	/**
	 * Process a transaction received from the server.
	 * 
	 * For calling function having a list of transactions, it should use the last good transaction id
	 * to update lastTransID and then terminates, if a false return value is received.
	 * 
	 * @param t Transaction object to be processed.
	 * @return True if the object is good and all necessary parameters are updated; false otherwise.
	 * 
	 */
	public boolean processTransaction(Transaction t) {
		boolean flag = true;
		if (t == null) {
			log.log(Log.ERROR, "Agent::processTransaction: Corrupted (null) transaction record.");
			flag = false;
		} else {
			if (t.marketID == null) {
				log.log(Log.ERROR, "Agent::processTransaction: t.marketID is null");
				flag = false;
			}
			if (t.price == null) {
				log.log(Log.ERROR, "Agent::processTransaction: t.price is null");
				flag = false;
			}
			if (t.quantity == null) {
				log.log(Log.ERROR, "Agent::processTransaction: t.quantity is null");
				flag = false;
			}
			if (t.timestamp == null) {
				log.log(Log.ERROR, "Agent::processTransaction: t.timestamp is null");
				flag = false;
			}
		}
		if (!flag) {
			return false;
		} else {
			// check whether seller, in which case negate the quantity
			int quantity = t.quantity;
			if (this.ID == t.sellerID) {
				quantity = -quantity;
			}
			
			// update cash flow and position
			if (positionBalance == 0) {
				averageCost = t.price.getPrice();
				
			} else if (positionBalance > 0) {
				if (quantity > 0) {
					int newCost = averageCost * positionBalance + t.price.getPrice() * quantity;
					averageCost = newCost / (positionBalance + quantity);
					
				} else if (-quantity < positionBalance) {
					// closing out partial long position
					realizedProfit += (-quantity) * (t.price.getPrice() - averageCost);
					
				} else if (-quantity >= positionBalance) {
					// closing out all long position, remaining quantity will start new short position
					realizedProfit += positionBalance * (t.price.getPrice() - averageCost);
					averageCost = t.price.getPrice();
				}
				
			} else if (positionBalance < 0) {
				if (quantity < 0) {
					int newCost = averageCost * (-positionBalance) + t.price.getPrice() * (-quantity);
					averageCost = -newCost / (positionBalance + quantity);
					
				} else if (quantity < -positionBalance) {
					// closing out partial short position
					realizedProfit += quantity * (averageCost - t.price.getPrice());
					
				} else if (quantity >= -positionBalance) {
					// closing out all short position, remaining quantity will start new long position
					realizedProfit += (-positionBalance) * (averageCost - t.price.getPrice());
					averageCost = t.price.getPrice();
				}
			}
			positionBalance += quantity;
		}
		return true;
	}

	
	/**
	 * Get new transactions. All obtained transactions are processed sequentially.
	 * Whenever an incomplete transaction record is encountered, the function will stop
	 * and update lastTransID accordingly.
	 * 
	 * @param ts TimeStamp of update
	 */
	public void updateTransactions(TimeStamp ts) {
		ArrayList<PQTransaction> list = getNewTransactions();
		log.log(Log.DEBUG, ts.toString() + " | " + this.toString() + " " + "lastTransID=" + lastTransID);
		if (list != null) {
			Integer lastGoodTransID = null;
			
			transLoop:
			for (Iterator<PQTransaction> it = list.iterator(); it.hasNext();) {
				Transaction t = it.next();
				// Check that this agent is involved in the transaction
				if (t.buyerID == this.ID || t.sellerID == this.ID){ 
					boolean flag = processTransaction(t);
					if (!flag) {
						if (lastGoodTransID != null) {
							lastTransID = lastGoodTransID;
							log.log(Log.ERROR, ts.toString() + " | " + this.toString() + " " +
									"Agent::updateTransactions: Problem with transaction.");
							break transLoop;
						}
					}
					log.log(Log.INFO, ts.toString() + " | " + this.toString() + " " +
							"Agent::updateTransactions: New transaction received: (mktID=" + t.marketID +
							", transID=" + t.transID + " buyer=" + t.buyerID + ", seller=" + t.sellerID +
							", price=" + t.price.toString() + ", quantity=" + t.quantity + 
							", timeStamp=" + t.timestamp + ")");
				}
				// Update transaction ID
				lastGoodTransID = t.transID;
			}
			lastTransID = lastGoodTransID;
			log.log(Log.DEBUG, ts.toString() + " | " + this.toString() + " " + "NEW lastTransID=" + lastTransID);
		}
		lastTransTime = ts;
	}

	
	/**
	 * @return list of all transactions that have not been processed yet.
	 */
	public ArrayList<PQTransaction> getNewTransactions() {
		ArrayList<PQTransaction> temp = null;
		if (lastTransID == null)
			temp = getTransactions(new Integer(-1));
		else
			temp = getTransactions(lastTransID);
		return temp;
	}

	/**
	 * Return list of transactions generated after the given ID.
	 * 
	 * @param lastID of the transaction we don't want
	 * @return list of transactions later than lastID
	 */
	public ArrayList<PQTransaction> getTransactions(int lastID) {
		ArrayList<PQTransaction> transactions = new ArrayList<PQTransaction>();

		TreeSet<Integer> t = getTransIDs(lastID);
		if (t == null || t.size() == 0) return null;

		for (Iterator<Integer> i = t.iterator(); i.hasNext();) {
			Integer id = i.next();
			PQTransaction record = this.data.getTransaction(id);
			transactions.add(record);
		}
		return transactions;
	}

	/**
	 * @return sorted set of all transaction IDs later than the last one
	 */
	public TreeSet<Integer> getTransIDs() {
		if (lastTransID != null)
			return getTransIDs(lastTransID);
		return null;
	}

	/**
	 * @param lastID - last ID that don't want transIDs for
	 * @return sorted set of transaction IDs later that lastID
	 */
	public TreeSet<Integer> getTransIDs(int lastID) {
		TreeSet<Integer> transIDs = new TreeSet<Integer>();
		for (Iterator<Integer> it = data.getTransactionIDs().iterator(); it.hasNext(); ) {
			int id = it.next();
			if (id > lastID) {
				transIDs.add(id);
			}
		}
		if (transIDs.size() == 0) return null;

		return transIDs; 
	}

	/**
	 * Computes any unrealized profit based on market bid/ask quotes.
	 * Note: could probably do based on NBBO quote's bid ask.
	 * 
	 * @return agent's unrealized profit/gain
	 */
	public int getUnrealizedProfit() {
		int up = 0;
		int p = -1;

		if (positionBalance > 0) {
			// For long position, compare cost to bid quote (buys)
			for (Iterator<Integer> it = marketIDs.iterator(); it.hasNext(); ) {
				int mktID = it.next();
				if (p == -1 || p < bidPrice.get(mktID).getPrice())
					p = bidPrice.get(mktID).getPrice();
			}
		} else {
			// For short position, compare cost to ask quote (sells)
			for (Iterator<Integer> it = marketIDs.iterator(); it.hasNext(); ) {
				int mktID = it.next();
				if (p == -1 || p > askPrice.get(mktID).getPrice())
					p = askPrice.get(mktID).getPrice();
			}
		}
		if (positionBalance != 0) {
			log.log(Log.DEBUG, "   " + this.toString() + " bal=" + positionBalance + 
					", p=" + p + ", avgCost=" + averageCost);
		}
		if (p != -1) {
			up += positionBalance * (p - averageCost);
		}
		return up;
	}


	/**
	 * @return agent's realized profit
	 */
	public int getRealizedProfit() {
		return realizedProfit;
	}
	

	/**
	 * Find best market to buy in (i.e. lowest ask) and to sell in (i.e. highest bid).
	 * This is a global operation so it checks all markets.
	 * 
	 * bestBuy = the best price an agent can buy at (the lowest sell bid).
	 * bestSell = the best price an agent can sell at (the highest buy bid).
	 *  
	 * @return BestQuote
	 */
	protected BestQuote findBestBuySell() {
		int bestBuy = -1;
		int bestSell = -1;
		int bestBuyMkt = 0;
		int bestSellMkt = 0;

		for (Iterator<Integer> it = marketIDs.iterator(); it.hasNext(); ) {
			Market mkt = data.markets.get(it.next());
			Price bid = getBidPrice(mkt.ID);
			Price ask = getAskPrice(mkt.ID);

			// in case the bid/ask disappears
			Vector<Price> price = new Vector<Price>();
			price.add(bid);
			price.add(ask);

			if (checkBidAsk(mkt.ID, price)) {
				log.log(Log.DEBUG, "Agent::findBestBuySell: issue with bid ask");
			}
			// Best market to buy in is the one with the lowest ASK
			if (bestBuy == -1 || bestBuy > ask.getPrice()) {
				if (ask.getPrice() != -1) bestBuy = ask.getPrice();
				bestBuyMkt = mkt.ID;
			}
			// Best market to sell in is the one with the highest BID
			if (bestSell == -1 || bestSell < bid.getPrice()) {
				if (bid.getPrice() != -1) bestSell = bid.getPrice();
				bestSellMkt = mkt.ID;
			}
		}
		BestQuote q = new BestQuote();
		q.bestBuy = bestBuy;
		q.bestSell = bestSell;
		q.bestBuyMarket = bestBuyMkt;
		q.bestSellMarket = bestSellMkt;
		return q;
	}
	
	
	/**
	 * Checks the bid/ask prices for errors.
	 *  
	 * @param aucID
	 * @param price
	 * @return
	 */
	boolean checkBidAsk(int mktID, Vector<Price> price) {
		int bid = price.get(0).getPrice();
		int ask = price.get(1).getPrice();
		boolean flag = true;

		if (ask > 0 && bid > 0) {
			prevAsk.put(mktID, ask);
			prevBid.put(mktID, bid);
			flag = false;
		} else if (ask <= 0 && bid > 0) {
			double oldask = ask;
			//			ask = bid + randomSpread(countMissingAsk.get(aucID))*tickSize;
			log.log(Log.DEBUG, "Agent::checkBidAsk: ask: " + oldask + " to " + ask);
			prevAsk.put(mktID, ask);
			prevBid.put(mktID, bid);
		} else if (bid <= 0 && ask > 0) {
			double oldbid = bid;
			//			bid = ask - randomSpread(countMissingBid.get(aucID))*tickSize;
			log.log(Log.DEBUG, "Agent::checkBidAsk: bid: " + oldbid + " to " + bid);
			prevAsk.put(mktID, ask);
			prevBid.put(mktID, bid);
		} else {
			double oldbid = bid;
			double oldask = ask;
			log.log(Log.DEBUG, "Agent::checkBidAsk: bid: " + oldbid + " to " + bid + 
					", ask: " + oldask + " to " + ask);
		}
		bid = Math.max(bid, 1);
		ask = Math.max(ask, 1);

		return flag;
	}
	
	
	/** 
	 * Generate normal random variable
	 * @param mu	mean
	 * @param var	variance
	 * @return
	 */
	protected double getNormalRV(double mu, double var) {
	    double r1 = rand.nextDouble();
	    double r2 = rand.nextDouble();
	    double z = Math.sqrt(-2*Math.log(r1))*Math.cos(2*Math.PI*r2);
	    return mu + z * Math.sqrt(var);
	}
	
	
//	/**
//	 * Specifies when the agent will call its agentStrategy method.
//	 * Times will depend on the time of agent arrival into the market
//	 * and the agent's latency.
//	 * 
//	 * Note: not used because may run out of memory
//	 * 
//	 * @param mkt
//	 * @param ts
//	 * @return ActivityHashMap
//	 */
//	public ActivityHashMap setupStrategyFrequency(Market mkt, TimeStamp ts) {
//		ActivityHashMap actMap = new ActivityHashMap();
//		for (long i = ts.longValue(); i < data.simLength.longValue(); i+=(long)sleepTime) {
//			TimeStamp time = ts.sum(new TimeStamp(i));
//			actMap.insertActivity(new UpdateAllQuotes(this, time));
//			actMap.insertActivity(new AgentStrategy(this, time));
//		}
//		return actMap;
//	}

}
