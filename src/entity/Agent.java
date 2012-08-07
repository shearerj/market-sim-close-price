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
import java.util.LinkedList;

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

	public static final int INF_PRICE = 99999999;
	protected Random rand;
	
	// Market information (hashed by market ID, as ID may be negative)
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

	// For quote generation
	protected Quoter quoter;
	
	// Agent parameters
	protected int tickSize;
	protected int positionLimit;
	protected int sleepTime;			// how frequently agentStrategy is called
	protected double sleepVar;				// variance of sleep time
	protected String agentType;
	protected TimeStamp arrivalTime;
	protected LinkedList<Activity> infiniteActs;	// infinitely fast activities

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
		
		infiniteActs = new LinkedList<Activity>();
		
		cashBalance = 0;
		positionBalance = 0;
		averageCost = 0;
		realizedProfit = 0;
		
		tickSize = d.tickSize;
		quoter = d.getQuoter();
	}
	
	
	/**
	 * Method to get the arrival time for an agent.
	 * @return
	 */
	public TimeStamp getArrivalTime() {
		return arrivalTime; 
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
	 * 
	 * @return
	 */
	public int getRandSleepTime() {
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
	 * @param mktID
	 * @return most recent quote for the specified market
	 */
	public Quote getLatestQuote(int mktID) {
		if (quotes.isEmpty()) {
			return null;
		} else if (quotes.get(mktID).isEmpty()) {
			return null;
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
	 * @return linked list of infinitely fast activities
	 */
	public LinkedList<Activity> getInfinitelyFastActs() {
		return infiniteActs;
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
		actMap.insertActivity(new Clear(mkt, ts));
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
		actMap.insertActivity(new Clear(mkt, ts));
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

		log.log(Log.INFO, ts.toString() + " | " + this.toString() + "," + mkt.toString() + 
				": +(" + price + ", " + quantity + ")");
		
		int p = quantize(price, tickSize);
		PQBid pqBid = new PQBid(this.ID, mkt.ID);
		pqBid.addPoint(quantity, new Price(p));
		pqBid.timestamp = ts;
		mkt.addBid(pqBid);
		currentBid.put(mkt.ID, pqBid);
		return null;
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
			log.log(Log.ERROR, "Agent::submitMultipleBid: Price and Quantity arrays are not the same length");
			return null;
		}
		
		log.log(Log.INFO, ts.toString() + " | " + this.toString() + "," + mkt.toString() + 
				": +(" + price.toString() +	", " + quantity.toString() + ")");
		
		PQBid pqBid = new PQBid(this.ID, mkt.ID);
		pqBid.timestamp = ts;
		for (int i = 0; i < price.length; i++) {
			if (quantity[i] != 0) {
				int p = quantize(price[i], tickSize);
				pqBid.addPoint(quantity[i], new Price(p));
			}
		}
		mkt.addBid(pqBid);
		currentBid.put(mkt.ID, pqBid);		
		return null;
	}

	/**
	 * Withdraws a bid by replacing it with an empty bid.
	 * 
	 * @param b
	 * @param mkt
	 * @return
	 */
	public ActivityHashMap withdrawBid(Market mkt, TimeStamp ts) {
		log.log(Log.INFO, ts.toString() + " | " + this.toString() + "," + mkt.toString() +
				": withdraw bid");
		mkt.removeBid(this.ID);
		return null;
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
		
		log.log(Log.INFO, ts.toString() + " | " + this.toString() + ": Global" + lastGlobalQuote + ", NBBO" + lastNBBOQuote);

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
				askPrice.put(mkt.ID, new Price(INF_PRICE));
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
		
		double rp = getRealizedProfit();
		double up = getUnrealizedProfit();

		String s = ts.toString() + " | " + this.toString() + ": Current Position=" + positionBalance +
				", Realized Profit=" + rp + ", Unrealized Profit=" + up;
		//			    s += ", Private Value=" + this.privateValue;
		
		ArrayList<PQTransaction> at = getTransactions(-1);
		String atSt = at.toString();
		String transactionData = "Transactions: " + atSt;
		log.log(Log.INFO, s);
		log.log(Log.INFO, transactionData);

	}
	
	
	
	/**
	 * Process a transaction received from the server.
	 * 
	 * @param t Transaction object to be processed.
	 * @return True if the object is good and all necessary parameters are updated; false otherwise.
	 * For calling function having a list of transactions, it should use the last good transaction id
	 * to update lastTransID and then terminates, if a false return value is received.
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
			// update cash flow and position
			if (positionBalance == 0) {
				averageCost = t.price.getPrice();
			} else if (positionBalance > 0) {
				if (t.quantity > 0) {
					int newCost = averageCost * positionBalance + t.price.getPrice() * t.quantity;
					averageCost = newCost / (positionBalance + t.quantity);
					
				} else if (-t.quantity < positionBalance) {
					// closing out partial long position
					realizedProfit += (-t.quantity) * (t.price.getPrice() - averageCost);
					
				} else if (-t.quantity >= positionBalance) {
					// closing out all long position, remaining quantity will start new short position
					realizedProfit += positionBalance * (t.price.getPrice() - averageCost);
					averageCost = t.price.getPrice();
				}
				
			} else if (positionBalance < 0) {
				if (t.quantity < 0) {
					int newCost = averageCost * (-positionBalance) + t.price.getPrice() * (-t.quantity);
					averageCost = -newCost / (positionBalance + t.quantity);
					
				} else if (t.quantity < -positionBalance) {
					// closing out partial short position
					realizedProfit += t.quantity * (averageCost - t.price.getPrice());
					
				} else if (t.quantity >= -positionBalance) {
					// closing out all short position, remaining quantity will start new long position
					realizedProfit += (-positionBalance) * (averageCost - t.price.getPrice());
					averageCost = t.price.getPrice();
				}
			}
			positionBalance += t.quantity;
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
		if (list != null) {
			Integer lastGoodTransID = null;
			for (Iterator<PQTransaction> it = list.iterator(); it.hasNext();) {
				Transaction t = it.next();
				boolean flag = processTransaction(t);
				if (!flag) {
					if (lastGoodTransID != null) {
						lastTransID = lastGoodTransID;
						log.log(Log.ERROR, "Agent::updateTransactions: Problem with transaction.");
						break;
					}
				} else {
					lastGoodTransID = t.transID;
				}
				log.log(Log.INFO, "Agent::updateTransactions: New transaction received: (mktID=" + t.marketID +
						", transID=" + t.transID + " buyer=" + t.buyerID + ", seller=" + t.sellerID +
						", price=" + t.price.toString() + ", quantity=" + t.quantity + ", timeStamp=" + t.timestamp + ")");
			}
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
	public ArrayList<PQTransaction> getTransactions(Integer lastID) {
		ArrayList<PQTransaction> transactions = new ArrayList<PQTransaction>();

		TreeSet<Integer> t = getTransIDs(lastID);
		if (t == null || t.size() == 0) return null;

		for (Iterator<Integer> i = t.iterator(); i.hasNext();) {
			Integer id = (Integer) i.next();
			PQTransaction record = this.data.transData.get(id);
			// transInfo replaced by this.dvata.transData.get()
			transactions.add(record);
		}
		return transactions;
	}

	/**
	 * @return sorted set of all transaction IDs later than the last one
	 */
	public TreeSet<Integer> getTransIDs() {
		if (lastTransID != null) return getTransIDs(lastTransID);
		return null;
	}

	/**
	 * @param lastID - last ID that don't want transIDs for
	 * @return sorted set of transaction IDs later that lastID
	 */
	public TreeSet<Integer> getTransIDs(Integer lastID) {
		TreeSet<Integer> transIDs = new TreeSet<Integer>();
		for (Iterator<Integer> i = this.data.transData.keySet().iterator(); i.hasNext();) {
			transIDs.add(i.next());
		}
		if (transIDs.size() == 0) return null;

		return transIDs; 
	}


	/**
	 * Find best market to buy in (i.e. lowest ASK) and to sell in (i.e. highest BID)
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
				log.log(Log.ERROR, "Agent::findBestBuySell: issue with bid ask");
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
	 * Checks the bid/ask prices for errors. TODO - still need to finish
	 *  
	 * @param aucID
	 * @param price
	 * @return
	 */
	boolean checkBidAsk(int mktID, Vector<Price> price) {
		int bid = price.get(0).getPrice();
		int ask = price.get(1).getPrice();
		boolean flag = true;

		//		if (ask <= 0) countMissingAsk.get(aucID).addLast(1); else countMissingAsk.get(aucID).addLast(0);
		//		if (bid <= 0) countMissingBid.get(aucID).addLast(1); else countMissingBid.get(aucID).addLast(0);
		//		if (countMissingAsk.get(aucID).size() > windowSizeCount) countMissingAsk.get(aucID).removeLast();
		//		if (countMissingBid.get(aucID).size() > windowSizeCount) countMissingBid.get(aucID).removeLast();

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
			//			ask = (prevAsk[aucID]+prevBid[aucID])/2 + (randomSpread(countMissingAsk.get(aucID))+randomSpread(countMissingBid.get(aucID)))*tickSize/2;
			//			bid = (prevAsk[aucID]+prevBid[aucID])/2 - (randomSpread(countMissingAsk.get(aucID))+randomSpread(countMissingBid.get(aucID)))*tickSize/2;
			log.log(Log.DEBUG, "Agent::checkBidAsk: bid: " + oldbid + " to " + bid + ", ask: " + oldask + " to " + ask);
		}
		bid = Math.max(bid, 1);
		ask = Math.max(ask, 1);

		//		price[0] = Math.min(bid, ask);
		//		price[1] = Math.max(bid, ask);

		return flag;
	}

	
	/**
	 * @return agent's unrealized profit
	 */
	public int getUnrealizedProfit() {
		int up = 0;
		int p = -1;

		if (positionBalance > 0) {
			// For long position, compare cost to bid quote
			for (Iterator<Integer> it = marketIDs.iterator(); it.hasNext(); ) {
				int mktID = it.next();
				if (p == -1 || p < bidPrice.get(mktID).getPrice())
					p = bidPrice.get(mktID).getPrice();
			}
		} else {
			// For short position, compare cost to ask quote
			for (Iterator<Integer> it = marketIDs.iterator(); it.hasNext(); ) {
				int mktID = it.next();
				if (p == -1 || p > askPrice.get(mktID).getPrice())
					p = askPrice.get(mktID).getPrice();
			}
		}
		up += positionBalance * (p - averageCost);
		return up;
	}


	/**
	 * @return agent's realized profit
	 */
	public int getRealizedProfit() {
		return realizedProfit;
	}


	public int getSurplus() {
		// get surplus of agent. maybe should be an abstract function
		// TODO
		return 0;
	}
	
	
	
	/**
	 * Quantizes the given integer based on the given granularity. Formula from
	 * Wikipedia (http://en.wikipedia.org/wiki/Quantization_signal_processing)
	 * 
	 * @param num integer to quantize
	 * @param n granularity (e.g. tick size)
	 * @return
	 */
	public int quantize(int num, int n) {
		double tmp = 0.5 + Math.abs((double) num) / ((double) n);
		return Integer.signum(num) * n * (int)Math.floor(tmp);
	}
	
	
	/** 
	 * Generate normal random variable
	 * @param mu	mean
	 * @param var	variance
	 * @return
	 */
	private double getNormalRV(double mu, double var) {
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
