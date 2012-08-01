package entity;

import event.*;
import market.*;
import activity.*;
import systemmanager.*;

import java.util.Iterator;
import java.util.Vector;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Properties;
import java.util.TreeSet;

/**
 * Base class for all agents.
 * 
 * @author ewah
 */
public abstract class Agent extends Entity {

	public static final int INF_PRICE = 99999999;

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

	// Agent parameters
	protected int positionLimit;
	protected int tickSize = 1;
	protected int sleepTime;			// how frequently agentStrategy is called
	protected String agentType;
	protected TimeStamp arrivalTime;

	// 	Tracking cash flow
	protected int cashBalance = 0;
	protected int positionBalance = 0;
	protected int averageCost = 0;
	protected int realizedProfit = 0;

	/**
	 * Constructor
	 * @param agentID
	 */
	public Agent(int agentID, SystemData d) {
		super(agentID, d);
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
	}

	// Abstract function specifying agent's strategy for participating in the market.
	public abstract ActivityHashMap agentStrategy(TimeStamp ts);
	
	
	/**
	 * Initializes agent parameters based on config file.
	 * 
	 * @param p
	 */
	public void initializeParams(SystemProperties p) {
		sleepTime = Integer.parseInt(p.get(agentType).get("sleepTime"));
	}
	
	/**
	 * Method to determine the next arrival time for an agent.
	 * @return
	 */
	public TimeStamp nextArrivalTime() {
		return new TimeStamp(0);
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
		if (quotes.get(mktID).isEmpty())
			return null;
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
	 * Agent arrives in a single market.
	 * 
	 * @param marketID
	 * @param arrivalTime
	 * @return ActivityHashMap
	 */
	public ActivityHashMap agentArrival(Market mkt, TimeStamp ts) {

//		System.out.println("Agent " + this.ID + ": AgentArrival in Market " + mkt.ID);

		// buyer/seller based on the config file -- TODO
		mkt.agentIDs.add(this.ID);
		mkt.buyers.add(this.ID);
		mkt.sellers.add(this.ID);
		marketIDs.add(mkt.ID);
		quotes.put(mkt.ID, new Vector<Quote>());
//		quotes.put(mkt.ID, new Quote(mkt));
		arrivalTime = ts;
		
		// Initialize bid/ask containers
		prevBid.put(mkt.ID, 0);
		prevAsk.put(mkt.ID, 0);
		initBid.put(mkt.ID, -1);
		initAsk.put(mkt.ID, -1);
		
		ActivityHashMap actMap = new ActivityHashMap();
		actMap.insertActivity(new UpdateAllQuotes(this, ts));
		actMap.insertActivity(new AgentStrategy(this, ts));
		return actMap;
	}

	/**
	 * Specifies when the agent will call its agentStrategy method.
	 * Times will depend on the time of agent arrival into the market
	 * and the agent's latency.
	 * 
	 * Note: not used because may run out of memory
	 * 
	 * @param mkt
	 * @param ts
	 * @return ActivityHashMap
	 */
	public ActivityHashMap setupStrategyFrequency(Market mkt, TimeStamp ts) {
		ActivityHashMap actMap = new ActivityHashMap();
		for (long i = ts.longValue(); i < data.simLength.longValue(); i+=(long)sleepTime) {
			TimeStamp time = ts.sum(new TimeStamp(i));
			actMap.insertActivity(new UpdateAllQuotes(this, time));
			actMap.insertActivity(new AgentStrategy(this, time));
		}
		return actMap;
	}

	/**
	 * Agent departs a specified market, if it is active.
	 * 
	 * @param departureTime
	 * @return ActivityHashMap
	 */
	public ActivityHashMap agentDeparture(Market mkt) {

//		System.out.println("Agent " + this.ID + ": AgentDeparture from Market " + mkt.ID);
		mkt.agentIDs.remove(mkt.agentIDs.indexOf(this.ID));
		mkt.buyers.remove(mkt.buyers.indexOf(this.ID));
		mkt.sellers.remove(mkt.sellers.indexOf(this.ID));
		if (mkt.marketType.equalsIgnoreCase("CDA"))
			((CDAMarket) mkt).orderbook.removeBid(this.ID);
		this.exitMarket(mkt.ID);
		return null;
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

//		System.out.println(ts.toString() + "| ((" + this.ID + ")): +(" + price + ", " + quantity + ") to [[" + mkt.ID + "]]");
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

//		System.out.println("Agent " + this.ID + ": AddMultipleBid to Market " + mkt.ID);

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
//		System.out.println("Agent " + this.ID + ": SubmitBid (" + price + ", " + quantity + ") to Market " + mkt.ID);
		System.out.println(ts.toString() + "| ((" + this.ID + ")): +(" + price + ", " + quantity + ") to [[" + mkt.ID + "]]");
		
		if (quantity == 0) return null;

		PQBid pqBid = new PQBid(this.ID, mkt.ID);
		pqBid.addPoint(quantity, new Price(price));
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
//		System.out.println("Agent " + this.ID + ": SubmitMultipleBid to Market " + mkt.ID);

		if (price.length != quantity.length) {
			System.out.println("Price and Quantity arrays are not the same length");
			return null;
		}
		PQBid pqBid = new PQBid(this.ID, mkt.ID);
		pqBid.timestamp = ts;
		for (int i = 0; i < price.length; i++) {
			if (quantity[i] != 0) {
				pqBid.addPoint(quantity[i], new Price(price[i]));
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
	public ActivityHashMap withdrawBid(Bid b, Market mkt, TimeStamp ts) {
//		System.out.println("Agent " + this.ID + ": WithdrawBid " + " id=" + 
//							b.getBidID() + " from Market " + mkt.ID);
		mkt.removeBid(b);
		return null;
	}


	/**
	 * Updates agent's stored NBBO quote.
	 * 
	 * @param ts
	 * @return
	 */
	public ActivityHashMap updateNBBO(TimeStamp ts) {
//		System.out.println("Agent " + this.ID + ": UpdateNBBO");
				
		System.out.println("Global " + lastGlobalQuote);
		System.out.println("NBBO: " + lastNBBOQuote);
		lastNBBOQuote = findBestBidOffer();
//		log(Log.INFO, "MMWorkerBee::agentStrategy: NBBO: " + bestQuoteNBBO);
//		log(Log.INFO, "MMWorkerBee::agentStrategy: Global: " + lastGlobalQuote);
		
		int bestBid = lastGlobalQuote.bestBid;
		int bestAsk = lastGlobalQuote.bestAsk;
		if ((bestBid != -1) && (bestAsk != -1)) {
			// check for inconsistency in buy/sell prices
			if (lastGlobalQuote.bestBid > lastGlobalQuote.bestAsk) {
				int mid = (lastGlobalQuote.bestBid + lastGlobalQuote.bestAsk) / 2;
				bestBid = mid - this.tickSize;
				bestAsk = mid + this.tickSize;
			}
		}
		lastNBBOQuote.bestBid = bestBid;
		lastNBBOQuote.bestAsk = bestAsk;
		return null;
	}


	/**
	 * Update quotes from all markets that the agent is active in.
	 * 
	 * @param ts
	 * @return
	 */
	public ActivityHashMap updateAllQuotes(TimeStamp ts) {
//		System.out.println("Agent " + this.ID + ": UpdateAllQuotes");

		for (Iterator<Integer> i = marketIDs.iterator(); i.hasNext(); ) {
			Market mkt = data.markets.get(i.next());
			updateQuotes(mkt, ts);
		}
		lastGlobalQuote = findBestBidOffer();
		// TODO - need to add logging here
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
			//	TODO - add to log
		}
		quotes.get(mkt.ID).add(q);
//		quotes.put(mkt.ID, q);
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
			//	      log.log(Log.ERROR, "MMAgent::processTrans: Corrupted (null) transaction record.");
			flag = false;
		} else {
			if (t.marketID == null) {
				//	        log(Log.ERROR, "MMAgent::processTrans: t.auctionID is null");
				flag = false;
			}
			if (t.price == null) {
				//	        log(Log.ERROR, "MMAgent::processTrans: t.price is null");
				flag = false;
			}
			if (t.quantity == null) {
				//	        log(Log.ERROR, "MMAgent::processTrans: t.quantity is null");
				flag = false;
			}
			if (t.timestamp == null) {
				//	        log(Log.ERROR, "MMAgent::processTrans: t.timestamp is null");
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
						// TODO - log error before breaking!!
						break;
					}
				} else {
					lastGoodTransID = t.transID;
				}
//				log(Log.INFO, "MMAgent::updateTrans: New transaction received: (auctionID=" + t.marketID +
//						", transID=" + t.transID + " buyer=" + t.buyerID + ", seller=" + t.sellerID +
//						", price=" + t.price.toString() + ", quantity=" + t.quantity + ", timeStamp=" + t.timestamp + ")");
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
			// transInfo replaced by this.data.transData.get()
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

		for (Iterator<Integer> i = marketIDs.iterator(); i.hasNext(); ) {
			Market mkt = data.markets.get(i.next());
			Price bid = getBidPrice(mkt.ID);
			Price ask = getAskPrice(mkt.ID);

			// in case the bid/ask disappears
			Vector<Price> price = new Vector<Price>();
			price.add(bid);
			price.add(ask);

			if (checkBidAsk(mkt.ID, price)) {
				System.out.println("issue with bid ask");
				// TODO - put in log
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
	 * Find best quote across all markets (lowest ask & highest bid)
	 * @return
	 */
	protected BestBidAsk findBestBidOffer() {
	    int bestBid = -1;
	    int bestBidMkt = 0;
	    int bestAsk = -1;
	    int bestAskMkt = 0;
	    
	    for (Iterator<Integer> i = marketIDs.iterator(); i.hasNext(); ) {
			Market mkt = data.markets.get(i.next());
			Price bid = getBidPrice(mkt.ID);
			Price ask = getAskPrice(mkt.ID);

			// in case the bid/ask disappears
			Vector<Price> price = new Vector<Price>();
			price.add(bid);
			price.add(ask);

			// Best bid quote is highest BID
			if (bestBid == -1 || bestBid < bid.getPrice()) {
				if (bid.getPrice() != -1) bestBid = bid.getPrice();
				bestBidMkt = mkt.ID;
//				bestBidQuantity = 0;
//				for (Quote quote : quotes.get(aucID)) {
//					bestBidQuantity += quote.getQuantityAtBidPrice(bestBid);
//				}
			}
			// Best ask quote is lowest ASK
			if (bestAsk == -1 || bestAsk > ask.getPrice()) {
				if (ask.getPrice() != -1) bestAsk = ask.getPrice();
				bestAskMkt = mkt.ID;
			}
	    }
	    BestBidAsk q = new BestBidAsk();
	    q.bestBidMarket = bestBidMkt;
	    q.bestBid = bestBid;
	    q.bestAskMarket = bestAskMkt;
	    q.bestAsk = bestAsk;
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
			//			log(Log.INFO, "MMAgent::fixBidAsk: ask: " + oldask + " to " + ask);
			prevAsk.put(mktID, ask);
			prevBid.put(mktID, bid);
		} else if (bid <= 0 && ask > 0) {
			double oldbid = bid;
			//			bid = ask - randomSpread(countMissingBid.get(aucID))*tickSize;
			//			log(Log.INFO, "MMAgent::fixBidAsk: bid: " + oldbid + " to " + bid);
			prevAsk.put(mktID, ask);
			prevBid.put(mktID, bid);
		} else {
			double oldbid = bid;
			double oldask = ask;
			//			ask = (prevAsk[aucID]+prevBid[aucID])/2 + (randomSpread(countMissingAsk.get(aucID))+randomSpread(countMissingBid.get(aucID)))*tickSize/2;
			//			bid = (prevAsk[aucID]+prevBid[aucID])/2 - (randomSpread(countMissingAsk.get(aucID))+randomSpread(countMissingBid.get(aucID)))*tickSize/2;
			//			log(Log.INFO, "MMAgent::fixBidAsk: bid: " + oldbid + " to " + bid + ", ask: " + oldask + " to " + ask);
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
			for (int i = 1; i <= this.getNumMarkets(); i++) {
				int mktID = this.marketIDs.get(i);
				if (p == -1 || p < bidPrice.get(mktID).getPrice())
					p = bidPrice.get(mktID).getPrice();
			}
		} else {
			// For short position, compare cost to ask quote
			for (int i = 1; i <= this.getNumMarkets(); i++) {
				int mktID = this.marketIDs.get(i);
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
	public double getRealizedProfit() {
		return realizedProfit;
	}


	public int getSurplus() {
		// get surplus of agent. maybe should be an abstract fn?
		// TODO
		return 0;
	}
}
