package entity;

import event.*;
import model.*;
import market.*;
import activity.*;
import systemmanager.*;

import java.util.*;

/**
 * Base class for all agents.
 * 
 * @author ewah
 */
public abstract class Agent extends Entity {

	protected int logID;			// ID for logging purposes (same across models)
	protected int modelID;			// ID of associated model
	protected Random rand;
	
	// Market information (all hashed by market ID, as ID may be negative)
	protected HashMap<Integer,Price> bidPrice;
	protected HashMap<Integer,Price> askPrice;
	protected HashMap<Integer,Bid> currentBid;
	protected HashMap<Integer,ArrayList<Quote>> quotes;
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
	protected SIP sip;
	
	// Agent parameters
	protected int privateValue;
	protected String agentType;
	protected TimeStamp arrivalTime;

	// Tracking cash flow
	protected int cashBalance;
	protected int positionBalance;
	protected int averageCost;
	protected int realizedProfit;
	// for liquidation
	protected int preLiqPosition;
	protected int preLiqRealizedProfit;
	
	// keys for accessing ObjectProperties object
	public final static String FUNDAMENTAL_KEY = "fundamental";
	public final static String ARRIVAL_KEY = "arrivalTime";
	public final static String RANDSEED_KEY = "seed";
	public final static String STRATEGY_KEY = "strategy";
	public final static String BIDRANGE_KEY = "bidRange";
	public final static String ARRIVALRATE_KEY = "arrivalRate";
	public final static String SLEEPTIME_KEY = "sleepTime";
	public final static String SLEEPVAR_KEY = "sleepVar";
	public final static String SLEEPRATE_KEY = "sleepRate";
	public final static String NUMRUNGS_KEY = "numRungs";
	public final static String RUNGSIZE_KEY = "rungSize";
	public final static String ALPHA_KEY = "alpha";
	
	/**
	 * Constructor
	 * @param agentID
	 * @param modelID
	 * @param d
	 * @param p
	 * @param l
	 */
	public Agent(int agentID, int modelID, SystemData d, ObjectProperties p, Log l) {
		super(agentID, d, p, l);
		
		rand = new Random();
		this.modelID = modelID;
		agentType = Consts.getAgentType(this.getName());
		
		// initialize all containers
		bidPrice = new HashMap<Integer,Price>();
		askPrice = new HashMap<Integer,Price>();
		currentBid = new HashMap<Integer,Bid>();
		lastQuoteTime = new HashMap<Integer,TimeStamp>();
		nextQuoteTime = new HashMap<Integer,TimeStamp>();
		lastClearTime = new HashMap<Integer,TimeStamp>();
		quotes = new HashMap<Integer,ArrayList<Quote>>();
		initBid = new HashMap<Integer,Integer>();
		initAsk = new HashMap<Integer,Integer>();
		prevBid = new HashMap<Integer,Integer>();
		prevAsk = new HashMap<Integer,Integer>();
		
		lastGlobalQuote = new BestBidAsk();
		lastNBBOQuote = new BestBidAsk();
		
		privateValue = -1;
		arrivalTime = new TimeStamp(0);
		
		tickSize = data.tickSize;
		sip = data.getSIP();
	}
	
	/** 
	 * @param ts
	 * @return
	 */
	public abstract ActivityHashMap agentStrategy(TimeStamp ts);
	
	/**
	 * @param ts
	 * @return
	 */
	public abstract ActivityHashMap agentArrival(TimeStamp ts);
	
	/**
	 * @return
	 */
	public abstract ActivityHashMap agentDeparture(TimeStamp ts);
	
	
	/**
	 * @return observation to include in the output file
	 */
	public abstract HashMap<String, Object> getObservation();
	
	
	
	/**
	 * Set EntityProperties for this agent.
	 * @param p EntityProperties object
	 */
	public void setProperties(ObjectProperties p) {
		params = p;
	}
	
	/**
	 * Add property to EntityProperties container.
	 * @param key
	 * @param val
	 */
	public void addProperty(String key, String val) {
		params.put(key, val);
	}
	
	/**
	 * @return EntityProperties of this agent.
	 */
	public ObjectProperties getProperties() {
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
	 * @return model ID of agent.
	 */
	public int getModelID() {
		return modelID;
	}

	/**
	 * @return MarketModel of the agent.
	 */
	public MarketModel getModel() {
		return data.getModel(modelID);
	}
	
	/**
	 * Method to get the type of the agent.
	 * @return
	 */
	public String getType() {
		return agentType;
	}
	
	/**
	 * Sets logID of the agent.
	 * @param logID
	 */
	public void setLogID(int logID) {
		this.logID = logID;
	}
	
	/**
	 * @return logID
	 */
	public int getLogID() {
		return this.logID;
	}
	
	@Override
	public String toString() {
		return new String("(" + this.logID + "," + this.getModel() + ")");
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
		return getQuote(mktID).size();
	}

	/**
	 * If quotes contains list for that market, return it; otherwise return 
	 * empty list.
	 * 
	 * @param mktID
	 * @return
	 */
	public ArrayList<Quote> getQuote(int mktID) {
		if (quotes.get(mktID) != null) {
			return quotes.get(mktID);
		} else {
			return new ArrayList<Quote>();
		}
	}
	
	/**
	 * Add given quote to the market. Initializes empty list if doesn't exist yet.
	 * @param mktID
	 * @param q
	 */
	public void addQuote(int mktID, Quote q) {
		if (quotes.get(mktID) == null) {
			// create for that market
			quotes.put(mktID, new ArrayList<Quote>());
		} 
		quotes.get(mktID).add(q);
	}
	
	/**
	 * Gets latest quote from a given market. If no quote available, returns 
	 * a default Quote object.
	 * 
	 * @param mktID
	 * @return most recent quote for the specified market
	 */
	public Quote getLatestQuote(int mktID) {
		if (quotes.isEmpty() || getQuote(mktID) == null) {
			return new Quote();
		} else if (getQuote(mktID).isEmpty()) {
			return new Quote();
		}
		// return last element in the list
		return quotes.get(mktID).get(quotes.size()-1);
	}
	
	/**
	 * @param mktID
	 * @param idx
	 * @return quote at a specified index
	 */
	public Quote getQuoteAt(int mktID, int idx) {
		if (!getQuote(mktID).isEmpty()) {
			// return new empty quote
			return new Quote();
		} else {
			return getQuote(mktID).get(idx);
		}
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
	 * @return current cash balance
	 */
	public int getCashBalance() {
		return cashBalance;
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
		quotes.put(mkt.ID, new ArrayList<Quote>());

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
	 * Wrapper method to submit bid to market after checking permissions.
	 * 
	 * @param mkt
	 * @param price
	 * @param quantity
	 * @param ts
	 * @return
	 */
	public ActivityHashMap submitBid(Market mkt, int price, int quantity, TimeStamp ts) {
		ActivityHashMap actMap = new ActivityHashMap();
		actMap.insertActivity(Consts.SUBMIT_BID_PRIORITY, new SubmitBid(this, mkt, price, quantity, ts));
		return actMap;
	}

	/**
	 * Wrapper method to submit multiple-point bid to market after checking permissions.
	 * 
	 * @param mkt
	 * @param price
	 * @param quantity
	 * @param ts
	 * @return
	 */
	public ActivityHashMap submitMultipleBid(Market mkt, ArrayList<Integer> price, ArrayList<Integer> quantity, TimeStamp ts) {
		ActivityHashMap actMap = new ActivityHashMap();
		actMap.insertActivity(Consts.SUBMIT_BID_PRIORITY, new SubmitMultipleBid(this, mkt, price, quantity, ts));
		return actMap;
	}
	
	/**
	 * Wrapper method to expire the agent's bid from a market after a specified duration.
	 * 
	 * @param mkt
	 * @param duration
	 * @param ts
	 * @return
	 */
	public ActivityHashMap expireBid(Market mkt, long duration, TimeStamp ts) {
		ActivityHashMap actMap = new ActivityHashMap();
		TimeStamp withdrawTime = ts.sum(new TimeStamp(duration));
		actMap.insertActivity(Consts.WITHDRAW_BID_PRIORITY, new WithdrawBid(this, mkt, withdrawTime));
		log.log(Log.INFO, ts + " | " + mkt + " " + this + ": bid duration=" + duration); 
		return actMap;
	}
	
	/**
	 * Withdraws a bid from the given market.
	 * 
	 * @param b
	 * @param mkt
	 * @return
	 */
	public ActivityHashMap executeWithdrawBid(Market mkt, TimeStamp ts) {
		log.log(Log.INFO, ts + " | " + this + " withdraw bid from " + mkt);
		return mkt.removeBid(this.ID, ts);
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
	public ActivityHashMap executeSubmitBid(Market mkt, int price, int quantity, TimeStamp ts) {
		if (quantity == 0) return null;

		log.log(Log.INFO, ts + " | " + mkt + " " + this + ": +(" + price + ", " + quantity + ")");
		
		int p = Market.quantize(price, tickSize);
		PQBid pqBid = new PQBid(this.ID, mkt.ID);
		pqBid.addPoint(quantity, new Price(p));
		pqBid.timestamp = ts;
		data.bidData.put(pqBid.getBidID(), pqBid);
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
	public ActivityHashMap executeSubmitMultipleBid(Market mkt, ArrayList<Integer> price, ArrayList<Integer> quantity, TimeStamp ts) {
		if (price.size() != quantity.size()) {
			log.log(Log.ERROR, "Agent::submitMultipleBid: Price/Quantity are not the same length");
			return null;
		}
		
		log.log(Log.INFO, ts + " | " + mkt + " " + this + ": +(" + price +	", " + quantity + ")");
		
		PQBid pqBid = new PQBid(this.ID, mkt.ID);
		pqBid.timestamp = ts;
		for (int i = 0; i < price.size(); i++) {
			if (quantity.get(i) != 0) {
				int p = Market.quantize(price.get(i), tickSize);
				pqBid.addPoint(quantity.get(i), new Price(p));
			}
		}
		data.addBid(pqBid);
		currentBid.put(mkt.ID, pqBid);	
		return mkt.addBid(pqBid, ts);
	}

	
	/**
	 * Liquidate agent's position at the the value of the global fundamental 
	 * at the specified time. Price is determined by the fundamental at the time
	 * of liquidation.
	 * 
	 * @param ts
	 * @return
	 */
	public ActivityHashMap liquidateAtFundamental(TimeStamp ts) {
		ActivityHashMap actMap = new ActivityHashMap();
		actMap.insertActivity(new Liquidate(this, data.getFundamentalAt(ts), ts));
		log.log(Log.INFO, ts + " | " + this + " liquidating..."); 
		return actMap;
	}

	
	/**
	 * Liquidates an agent's position at the specified price.
	 *  
	 * @param price
	 * @param ts
	 * @return
	 */
	public ActivityHashMap executeLiquidate(Price price, TimeStamp ts) {
		
		log.log(Log.INFO, ts + " | " + this + " pre-liquidation: position=" + positionBalance
				+ ", profit=" + realizedProfit);
		
		// If no net position, no need to liquidate
		if (positionBalance == 0) return null;
		
		preLiqPosition = positionBalance;
		preLiqRealizedProfit = getRealizedProfit();
		if (positionBalance > 0) {
			// need to sell
			realizedProfit += positionBalance * price.getPrice();
		} else {
			// need to buy
			realizedProfit -= positionBalance * price.getPrice();
		}
		positionBalance = 0;
		
		log.log(Log.INFO, ts + " | " + this + " post-liquidation: position=" + positionBalance
				+ ", profit=" + realizedProfit + ", price=" + price);
		return null;
	}
	
	
	
	/**
	 * Update global and NBBO quotes for the agent's model.
	 * 
	 * @param ts
	 * @return
	 */
	public ActivityHashMap updateAllQuotes(TimeStamp ts) {
		
		for (Iterator<Integer> it = data.getMarketIDs().iterator(); it.hasNext(); ) {
			Market mkt = data.getMarket(it.next());
			updateQuotes(mkt, ts);
		}
		lastGlobalQuote = sip.getGlobalQuote(modelID);
		lastNBBOQuote = sip.getNBBOQuote(modelID);
		
		log.log(Log.INFO, ts + " | " + this + " Global" + lastGlobalQuote + ", NBBO" + lastNBBOQuote);
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
		addQuote(mkt.ID, q);
	}

	
	/**
	 * Logs transactions for the agent and prints a summary of performance so far.
	 * 
	 * @param ts
	 */
	public void logTransactions(TimeStamp ts) {
		
		int rp = getRealizedProfit();
		int up = getUnrealizedProfit();

		String s = ts.toString() + " | " + this +  " Agent::logTransactions: " + 
				this.getModel().getFullName() + ": Current Position=" + 
				positionBalance + ", Realized Profit=" + rp + 
				", Unrealized Profit=" + up;
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
					int newCost = averageCost * positionBalance + 
							t.price.getPrice() * quantity;
					averageCost = newCost / (positionBalance + quantity);

				} else if (-quantity < positionBalance) {
					// closing out partial long position
					int rprofit = realizedProfit;
					rprofit += (-quantity) * (t.price.getPrice() - averageCost);
					realizedProfit = rprofit;

				} else if (-quantity >= positionBalance) {
					// closing out all long position, remaining quantity will start new short position
					int rprofit = realizedProfit;
					rprofit += positionBalance * (t.price.getPrice() - averageCost);
					realizedProfit = rprofit;
					averageCost = t.price.getPrice();
				}

			} else if (positionBalance < 0) {
				if (quantity < 0) {
					int newCost = averageCost * (-positionBalance) + 
							t.price.getPrice() * (-quantity);
					averageCost = -newCost / (positionBalance + quantity);

				} else if (quantity < -positionBalance) {
					// closing out partial short position
					int rprofit = realizedProfit;
					rprofit += quantity * (averageCost - t.price.getPrice());
					realizedProfit = rprofit;

				} else if (quantity >= -positionBalance) {
					// closing out all short position, remaining quantity will start new long position
					int rprofit = realizedProfit;
					rprofit += (-positionBalance) * (averageCost - t.price.getPrice());
					realizedProfit = rprofit;
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
		log.log(Log.DEBUG, ts + " | " + this + " " + "lastTransID=" + lastTransID);
		if (list != null) {
			Integer lastGoodTransID = null;
			
			transLoop:
			for (Iterator<PQTransaction> it = list.iterator(); it.hasNext();) {
				Transaction t = it.next();
				// Check that this agent is involved in the transaction
				if (t.buyerID == this.ID || t.sellerID == this.ID){ 
					boolean flag = processTransaction(t);
					if (!flag && lastGoodTransID != null) {
						lastTransID = lastGoodTransID;
						log.log(Log.ERROR, ts + " | " + this + " " +
								"Agent::updateTransactions: Problem with transaction.");
						break transLoop;
					}
					log.log(Log.INFO, ts + " | " + this + " " +
							"Agent::updateTransactions: New transaction received: (mktID=" + t.marketID +
							", transID=" + t.transID + " buyer=" + data.getAgentLogID(t.buyerID) + 
							", seller=" + data.getAgentLogID(t.sellerID) +
							", price=" + t.price + ", quantity=" + t.quantity + 
							", timeStamp=" + t.timestamp + ")");
					
					// Log surplus for all agents
					int bsurplus = data.getAgent(t.buyerID).getPrivateValue() - t.price.getPrice();
					int ssurplus = t.price.getPrice() - data.getAgent(t.sellerID).getPrivateValue();
					String s = ts + " | " + this + " " +
							"Agent::updateTransactions: BUYER surplus: " + data.getAgent(t.buyerID).getPrivateValue()
							+ "-" + t.price.getPrice() + "=" + bsurplus + ", "
							+ "SELLER surplus: " + t.price.getPrice() + "-" + 
							data.getAgent(t.sellerID).getPrivateValue() + "=" + ssurplus;
					log.log(Log.INFO, s);
					log.log(Log.INFO, ts + " | " + this + " " +
							"Agent::updateTransactions: SURPLUS: " + (bsurplus + ssurplus));
					
//					log.log(Log.INFO, ts + " | " + this + " " +
//							"Agent::updateTransactions: BUYER surplus: " + data.getAgent(t.buyerID).getPrivateValue()
//							+ "-" + t.price.getPrice() + "=" + bsurplus);
//					log.log(Log.INFO, ts + " | " + this + " " +
//							"Agent::updateTransactions: SELLER surplus: " + t.price.getPrice() + "-" + 
//							data.getAgent(t.sellerID).getPrivateValue() + "=" + ssurplus);
					
				}
				// Update transaction ID
				lastGoodTransID = t.transID;
			}
			lastTransID = lastGoodTransID;
			log.log(Log.DEBUG, ts + " | " + this + " " + "NEW lastTransID=" + lastTransID);
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
			PQTransaction record = this.data.getTransaction(modelID, id);
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
		for (Iterator<Integer> it = data.getTransactionIDs(modelID).iterator(); it.hasNext(); ) {
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
	 * Checks the markets belonging to the Agent's model.
	 * 
	 * TODO: could probably do based on NBBO quote's bid ask.
	 * 
	 * @return agent's unrealized profit/gain
	 */
	public int getUnrealizedProfit() {
		int up = 0;
		int p = -1;

		try {			
			ArrayList<Integer> mIDs = this.getModel().getMarketIDs();
			
			if (positionBalance > 0) {
				// For long position, compare cost to bid quote (buys)
				for (Iterator<Integer> it = mIDs.iterator(); it.hasNext(); ) {
					int mktID = it.next();
					if (p == -1 || p < bidPrice.get(mktID).getPrice()) {
						p = bidPrice.get(mktID).getPrice();
					}
				}
			} else {
				// For short position, compare cost to ask quote (sells)
				for (Iterator<Integer> it = mIDs.iterator(); it.hasNext(); ) {
					int mktID = it.next();
					if (p == -1 || p > askPrice.get(mktID).getPrice()) {
						p = askPrice.get(mktID).getPrice();
					}	
				}
			}
			if (positionBalance != 0) {
				log.log(Log.DEBUG, this.getModel().getFullName() + ": " + this + 
						" bal=" + positionBalance + 
						", p=" + p + ", avgCost=" + averageCost);
			}
			if (p != -1) {
				up += positionBalance * (p - averageCost);
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		return up;
	}

	public int getRealizedProfit() {
		return realizedProfit;
	}
	
	public int getPreLiquidationPosition() {
		return preLiqPosition;
	}
	
	public int getPreLiquidationProfit() {
		return preLiqRealizedProfit;
	}
	
	public int getPositionBalance() {
		return positionBalance;
	}
	
	
	/**
	 * Find best market to buy in (i.e. lowest ask) and to sell in (i.e. highest bid).
	 * This is a global operation so it checks all markets in marketIDs and it gets
	 * the up-to-date market quote with zero delays.
	 * 
	 * bestBuy = the best price an agent can buy at (the lowest sell bid).
	 * bestSell = the best price an agent can sell at (the highest buy bid).
	 *  
	 * NOTE: This uses only those markets belonging to the agent's model, as
	 * strategies can only be selected based on information on those markets.
	 * 
	 * @return BestQuote
	 */
	protected BestQuote findBestBuySell() {
		int bestBuy = -1;
		int bestSell = -1;
		int bestBuyMkt = 0;
		int bestSellMkt = 0;

		for (Iterator<Integer> it = this.getModel().getMarketIDs().iterator(); it.hasNext(); ) {
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
		if (price.size() < 2) return false;
		
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

	/**
	 * Generate exponential random variate, with rate parameter.
	 * @param rateParam
	 * @return
	 */
	private double getExponentialRV(double rateParam) {
		double r = rand.nextDouble();
		return -Math.log(r) / rateParam;
	}
}
