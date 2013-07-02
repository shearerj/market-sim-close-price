package entity;

import data.*;
import event.*;
import logger.Logger;
import model.*;
import market.*;
import activity.*;
import systemmanager.*;
import utils.RandPlus;

import java.util.*;

/**
 * Base class for all agents.
 * 
 * @author ewah
 */
public abstract class Agent extends Entity {
	
	protected int logID;			// ID for logging purposes (should same across models)
	protected final RandPlus rand;
	
	// -- begin reorg -- stuff above line existed before and is still relevant...
	
	protected MarketModel model;
	protected HashMap<Double,Double> surplus; //hashed by rho value
	
	// -- end reorg --
	
	protected int modelID;			// ID of associated model

	
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
	protected BestBidAsk lastNBBOQuote;
	protected BestBidAsk lastGlobalQuote;
	protected int tickSize;
	
	// For quote generation
	protected SIP sip;
	
	// Agent parameters
	protected PrivateValue alpha;
	protected String agentType;
	protected TimeStamp arrivalTime;

	// Transaction information
	protected TimeStamp lastTransTime;
	protected Transaction lastTransaction;	// last transaction seen
	protected TransactionIDComparator idComparator;
	protected List<Transaction> transactions;
	
	// Tracking cash flow
	protected int cashBalance;
	protected int positionBalance;
	protected int averageCost;
	protected int realizedProfit;
	// for liquidation
	protected int preLiqPosition;
	protected int preLiqRealizedProfit;
	
	// keys for accessing ObjectProperties object
	public final static String FUNDAMENTAL_KEY = "fundamentalAtArrival";
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
	public final static String MAXQUANTITY_KEY = "maxqty";
	public final static String MARKETID_KEY = "marketID";
	
	// FIXME Agent probably needs more than this...
	public Agent(int agentID, TimeStamp arrivalTime, MarketModel model, RandPlus rand) {
		super(agentID);
		this.model = model;
		this.rand = rand;
		this.arrivalTime = arrivalTime;
	}
	
	/**
	 * Constructor
	 * @param agentID
	 * @param modelID
	 * @param d
	 * @param p
	 * @param l
	 */
	public Agent(int agentID, int modelID, SystemData d, ObjectProperties p) {
		super(agentID, d, p);
		
		// -- Begin Reorg --
		model = d.models.get(modelID);
		
		// -- End Reorg --
		
		
		rand = new RandPlus();
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
		
		lastTransaction = null;
		idComparator = new TransactionIDComparator();
		transactions = new ArrayList<Transaction>();
		
		lastGlobalQuote = new BestBidAsk();
		lastNBBOQuote = new BestBidAsk();
		
		alpha = null;		// if no PV, this will always be null
		arrivalTime = new TimeStamp(0);
		
		tickSize = data.tickSize;
		sip = data.getSIP();
	}
		
	/** 
	 * @param ts
	 * @return
	 */
	public abstract Collection<? extends Activity> agentStrategy(TimeStamp ts);
	
	/**
	 * @param ts
	 * @return
	 */
	public abstract Collection<? extends Activity> agentArrival(TimeStamp ts);
	
	/**
	 * @param ts
	 * @return
	 */
	public abstract Collection<? extends Activity> agentDeparture(TimeStamp ts);

	/**
	 * @param ts
	 * @return
	 */
	public abstract Collection<? extends Activity> updateAllQuotes(TimeStamp ts);
	
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
	 * @return private value vector.
	 */
	public PrivateValue getPrivateValue() {
		return alpha;
	}
	
	/**
	 * @return true if has non-null private value.
	 */
	public boolean hasPrivateValue() {
		return alpha != null;
	}
	
	/**
	 * Given additional quantity to buy/sell, return associated private 
	 * valuation (requires looking at current position balance).
	 * 
	 * Required because of indexing of quantities vector in PrivateValue.
	 *  
	 * @param q		additional units to buy or sell
	 * @return
	 */
	public Price getPrivateValueAt(int q) {
		if (q > 0) {
			// if buying
			if (positionBalance >= 0) {
				// if nonnegative current position, look at next position (+q)
				return alpha.getValueFromQuantity(positionBalance + q);
			} else {
				// if negative current position, look at current position
				return alpha.getValueFromQuantity(positionBalance);
			}
			
		} else if (q < 0){
			// if selling
			if (positionBalance > 0) {
				// if positive current position, look at current position
				return alpha.getValueFromQuantity(positionBalance);
			} else {
				// if non-positive current position, look at next position (-|q|)
				return alpha.getValueFromQuantity(positionBalance + q);
			}
			
		} else {
			// not selling or buying
			return new Price(0);
		}
	}
	
	
	/**
	 * Initialize list of private values given max quantity.
	 * @param q		max position
	 * @return
	 */
	public ArrayList<Integer> initPrivateValues(int q) {
		ArrayList<Integer> alphas = new ArrayList<Integer>();
		for (int i = -q; i <= q; i++) {
			if (i != 0)	alphas.add((int) Math.round(rand.nextGaussian(0, data.pvVar)));
		}
		return alphas;
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
		return model;
	}
	
	/**
	 * Method to get the type of the agent.
	 * @return
	 */
	public String getType() {
		return agentType;
	}

	/**
	 * @return role of agent
	 */
	public String getRole() {
		if (this instanceof HFTAgent) {
			return Consts.ROLE_HFT;
		} else if (this instanceof BackgroundAgent) {
			return Consts.ROLE_BACKGROUND;
		} else if (this instanceof MarketMaker) {
			return (Consts.ROLE_MARKETMAKER);
		} else {
			System.err.println(this.getClass().getSimpleName() + "::getRole(): " +
					"invalid agent!");
			System.exit(1);
		}
		return "";
	}
	
	/**
	 * @return strategy for observation file in format TYPE:STRATEGY
	 */
	public String getFullStrategy() {
		return this.getType() + ":" + params.getAsString(Agent.STRATEGY_KEY);
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
		return (int) Math.round(rand.nextGaussian(sleepTime, sleepVar));
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

	/***********************************
	 * Methods for Activities
	 * 
	 **********************************/
	
	/**
	 * Enters market by adding market to data structures.
	 * @param mkt
	 * @param ts
	 */
	protected void enterMarket(Market mkt, TimeStamp ts) {
		mkt.agentIDs.add(this.id);
		mkt.buyers.add(this.id);
		mkt.sellers.add(this.id);
		quotes.put(mkt.id, new ArrayList<Quote>());

		// Initialize bid/ask containers
		prevBid.put(mkt.id, 0);
		prevAsk.put(mkt.id, 0);
		initBid.put(mkt.id, -1);
		initAsk.put(mkt.id, -1);
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
	public Collection<Activity> submitBid(Market mkt, int price, int quantity, TimeStamp ts) {
		Collection<Activity> actMap = new ArrayList<Activity>();
		actMap.add(new SubmitBid(this, mkt, price, quantity, ts));
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
	public Collection<Activity> submitMultipleBid(Market mkt, ArrayList<Integer> price, 
			ArrayList<Integer> quantity, TimeStamp ts) {
		Collection<Activity> actMap = new ArrayList<Activity>();
		actMap.add(new SubmitMultipleBid(this, mkt, price, quantity, ts));
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
	public Collection<? extends Activity> expireBid(Market mkt, TimeStamp duration, TimeStamp ts) {
		Collection<Activity> actMap = new ArrayList<Activity>();
		TimeStamp withdrawTime = ts.sum(new TimeStamp(duration));
		actMap.add(new WithdrawBid(this, mkt, withdrawTime));
		Logger.log(Logger.INFO, ts + " | " + mkt + " " + this + ": bid duration=" + duration); 
		return actMap;
	}
	
	/**
	 * Withdraws a bid from the given market.
	 * 
	 * @param b
	 * @param mkt
	 * @return
	 */
	public Collection<? extends Activity> executeWithdrawBid(Market mkt, TimeStamp ts) {
		Logger.log(Logger.INFO, ts + " | " + this + " withdraw bid from " + mkt);
		return mkt.removeBid(this.id, ts);
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
	public Collection<? extends Activity> executeSubmitBid(Market mkt, int price, int quantity, 
			TimeStamp ts) {
		if (quantity == 0) return Collections.emptyList();

		Logger.log(Logger.INFO, ts + " | " + this + " " + agentType + 
				"::submitBid: +(" + price + ", " 
				+ quantity + ") to " + mkt);
		
		int p = Market.quantize(price, tickSize);
		PQBid pqBid = new PQBid(this, mkt, ts);
		pqBid.addPoint(quantity, new Price(p));
		// quantity can be +/-
		if (hasPrivateValue()) {
			data.addPrivateValue(pqBid.getBidID(), getPrivateValueAt(quantity));
		} else {
			data.addPrivateValue(pqBid.getBidID(), null);
		}
		currentBid.put(mkt.id, pqBid);
		return mkt.addBid(pqBid, ts);
	}	

	/**
	 * Submits a multiple-point bid to the specified market.
	 * 
	 * @param mkt
	 * @param price
	 * @param quantity
	 * @param ts
	 * @return
	 */
	public Collection<? extends Activity> executeSubmitMultipleBid(Market mkt, List<Integer> price, 
			List<Integer> quantity, TimeStamp ts) {
		if (price.size() != quantity.size()) {
			Logger.log(Logger.ERROR, "Agent::submitMultipleBid: " 
					+ "Price/Quantity are not the same length");
			return Collections.emptyList();
		}
		
		Logger.log(Logger.INFO, ts + " | " + mkt + " " + this + ": +(" + price +	", " 
				+ quantity + ")");
		
		PQBid pqBid = new PQBid(this, mkt, ts);
		for (int i = 0; i < price.size(); i++) {
			if (quantity.get(i) != 0) {
				int p = Market.quantize(price.get(i), tickSize);
				pqBid.addPoint(quantity.get(i), new Price(p));
			}
		}
		// TODO incorporate multi-point PVs?
		currentBid.put(mkt.id, pqBid);	
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
	public Collection<Activity> liquidateAtFundamental(TimeStamp ts) {
		Collection<Activity> actMap = new ArrayList<Activity>();
		actMap.add(new Liquidate(this, model.getFundamentalAt(ts), ts));
		Logger.log(Logger.INFO, ts + " | " + this + " liquidating..."); 
		return actMap;
	}

	
	/**
	 * Liquidates an agent's position at the specified price.
	 *  
	 * @param price
	 * @param ts
	 * @return
	 */
	public Collection<Activity> executeLiquidate(Price price, TimeStamp ts) {
		
		Logger.log(Logger.INFO, ts + " | " + this + " pre-liquidation: position=" 
				+ positionBalance + ", profit=" + realizedProfit);
		
		// If no net position, no need to liquidate
		if (positionBalance == 0) return Collections.emptyList();
		
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
		
		Logger.log(Logger.INFO, ts + " | " + this + " post-liquidation: position=" 
				+ positionBalance + ", profit=" + realizedProfit + ", price=" + price);
		return Collections.emptyList();
	}

	
	/**
	 * Update global and NBBO quotes for the agent's model.
	 * 
	 * @param ts
	 * @return
	 */
	public Collection<Activity> executeUpdateAllQuotes(TimeStamp ts) {
		lastGlobalQuote = sip.getGlobalQuote(modelID);
		lastNBBOQuote = sip.getNBBOQuote(modelID);
		
		Logger.log(Logger.INFO, ts + " | " + this + " Global" + lastGlobalQuote 
				+ ", NBBO" + lastNBBOQuote);
		return Collections.emptyList();
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
				askPrice.put(mkt.id, new Price(Consts.INF_PRICE));
			else
				askPrice.put(mkt.id, q.lastAskPrice);
			if (q.lastBidPrice == null)
				bidPrice.put(mkt.id, new Price(0));
			else
				bidPrice.put(mkt.id, q.lastBidPrice);

			if (q.lastQuoteTime != null)
				lastQuoteTime.put(mkt.id, q.lastQuoteTime);

			if (q.lastClearTime != null && lastClearTime.get(mkt.id) != q.lastClearTime) {
				lastClearTime.put(mkt.id, q.lastClearTime);
			}
		} else {
			Logger.log(Logger.ERROR, "Agent::updateQuotes: Quote is null.");
		}
		addQuote(mkt.id, q);
	}

	
	/***********************************
	 * Transaction-related methods
	 *
	 **********************************/
	
	/**
	 * Logs transactions for the agent and prints a summary of performance so far.
	 * 
	 * @param ts
	 */
	public void logTransactions(TimeStamp ts) {
		
		int rp = getRealizedProfit();

		// int up = getUnrealizedProfit();

		String s = ts.toString() + " | " + this +  " Agent::logTransactions: " + 
				this.getModel().getFullName() + ": Current Position=" + 
				positionBalance + ", Realized Profit=" + rp; 
				//+ ", Unrealized Profit=" + up;
		Logger.log(Logger.INFO, s);
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
			Logger.log(Logger.ERROR, "Agent::processTransaction: Corrupted (null) transaction record.");
			flag = false;
		} else {
			if (t.getMarket() == null) {
				Logger.log(Logger.ERROR, "Agent::processTransaction: t.market is null");
				flag = false;
			}
			if (t.price == null) {
				Logger.log(Logger.ERROR, "Agent::processTransaction: t.price is null");
				flag = false;
			}
			if (t.quantity == null) {
				Logger.log(Logger.ERROR, "Agent::processTransaction: t.quantity is null");
				flag = false;
			}
			if (t.timestamp == null) {
				Logger.log(Logger.ERROR, "Agent::processTransaction: t.timestamp is null");
				flag = false;
			}
		}
		if (!flag) {
			return false;
		} else {
			// check whether seller, in which case negate the quantity
			int quantity = t.quantity;
			if (this.id == t.getSeller().getID()) {
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
					// closing out all long position
					// remaining quantity will start new short position
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
					// closing out all short position
					// remaining quantity will start new long position
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
		Collection<Transaction> list = getNewTransactions();
		
		Logger.log(Logger.DEBUG, ts + " | " + this + " " + "lastTrans=" + lastTransaction);
		
		if (list != null) {
			Transaction lastGoodTrans = null;
			 
			for (Transaction t : list) {
				// Check that this agent is involved in the transaction
				if (t.getBuyer().getID() == this.id || t.getSeller().getID() == this.id){ 
					boolean flag = processTransaction(t);
					if (!flag && lastGoodTrans != null) {
						lastTransaction = lastGoodTrans;
						Logger.log(Logger.ERROR, ts + " | " + this + " " +
								"Agent::updateTransactions: Problem with transaction.");
						break;
					}
					
					Logger.log(Logger.INFO, ts + " | " + this + " " +
							"Agent::updateTransactions: New transaction received: (" +
							"transID=" + t.transID +", mktID=" + t.getMarket().getID() +
							", buyer=" + data.getAgentLogID(t.getBuyer().getID()) + 
							", seller=" + data.getAgentLogID(t.getSeller().getID()) +
							", price=" + t.price + ", quantity=" + t.quantity + 
							", timeStamp=" + t.timestamp + ")");
				}
				// Update transactions
				lastGoodTrans = t;
				transactions.add(t);
			}
			lastTransaction = lastGoodTrans;
			Logger.log(Logger.DEBUG, ts + " | " + this + " " + "NEW lastTrans=" + lastGoodTrans);
		}
		lastTransTime = ts;
	}

	/**
	 * Gets all transactions that have not been processed yet.
	 * 
	 * @return
	 */
	public Collection<Transaction> getNewTransactions() {

		
		if (lastTransaction == null) {
			// get all transactions for this model
			return model.getTrans();
		} else {
			TreeSet<Transaction> tmp = new TreeSet<Transaction>(idComparator);
			tmp.addAll(model.getTrans());
			// get all transactions after the last seen transaction (not inclusive)
			return new ArrayList<Transaction>(tmp.tailSet(lastTransaction, false));
		}
	}

	
	/***********************************
	 * Determining position & profit
	 *
	 **********************************/
	
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
				for (int mktID : mIDs) {
					if (p == -1 || p < bidPrice.get(mktID).getPrice()) {
						p = bidPrice.get(mktID).getPrice();
					}
				}
			} else {
				// For short position, compare cost to ask quote (sells)
				for (int mktID : mIDs) {
					if (p == -1 || p > askPrice.get(mktID).getPrice()) {
						p = askPrice.get(mktID).getPrice();
					}	
				}
			}
			if (positionBalance != 0) {
				Logger.log(Logger.DEBUG, this.getModel().getFullName() + ": " + this + 
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
	
	
	/***********************************
	 * Methods for agent strategies
	 *
	 **********************************/
	
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
			Price bid = getBidPrice(mkt.id);
			Price ask = getAskPrice(mkt.id);

			// in case the bid/ask disappears
			ArrayList<Price> price = new ArrayList<Price>();
			price.add(bid);
			price.add(ask);

			if (checkBidAsk(mkt.id, price)) {
				Logger.log(Logger.DEBUG, "Agent::findBestBuySell: issue with bid ask");
			}
			// Best market to buy in is the one with the lowest ASK
			if (bestBuy == -1 || bestBuy > ask.getPrice()) {
				if (ask.getPrice() != -1) bestBuy = ask.getPrice();
				bestBuyMkt = mkt.id;
			}
			// Best market to sell in is the one with the highest BID
			if (bestSell == -1 || bestSell < bid.getPrice()) {
				if (bid.getPrice() != -1) bestSell = bid.getPrice();
				bestSellMkt = mkt.id;
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
	boolean checkBidAsk(int mktID, ArrayList<Price> price) {
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
			Logger.log(Logger.DEBUG, "Agent::checkBidAsk: ask: " + oldask + " to " + ask);
			prevAsk.put(mktID, ask);
			prevBid.put(mktID, bid);
		} else if (bid <= 0 && ask > 0) {
			double oldbid = bid;
			Logger.log(Logger.DEBUG, "Agent::checkBidAsk: bid: " + oldbid + " to " + bid);
			prevAsk.put(mktID, ask);
			prevBid.put(mktID, bid);
		} else {
			double oldbid = bid;
			double oldask = ask;
			Logger.log(Logger.DEBUG, "Agent::checkBidAsk: bid: " + oldbid + " to " + bid + 
					", ask: " + oldask + " to " + ask);
		}
		bid = Math.max(bid, 1);
		ask = Math.max(ask, 1);

		return flag;
	}
	
	int compare(Agent other) {
		if(this.id < other.getID()) return -1;
		else if(this.id == other.getID()) return 0;
		else return 1;
	}

	public HashMap<Double, Double> getSurplus() {
		return surplus;
	}
	
	public double getSurplus(double rho) {
		return this.surplus.get(rho);
	}
	
	public double addSurplus(double rho, double fund, Transaction tr, boolean isBuyer) {
		if(!this.surplus.containsKey(rho)) this.surplus.put(rho, 0.0);
		double s=0;
		double newSurplus=0;
		//Updating surplus if this agent was a buyer
		if(isBuyer) {
			if(this.getPrivateValue() != null) {
				for(int q=0; q < tr.getQuantity(); q++) {
					int dev = this.getPrivateValueAt(q).getPrice();
					s = dev + fund - tr.getPrice().getPrice();
					newSurplus = s + this.surplus.get(rho);
					this.surplus.put(rho, newSurplus);
				}
			}
			else {
				s = -tr.price.getPrice();
				newSurplus = s + this.surplus.get(rho);
				this.surplus.put(rho, newSurplus);
			}
		}
		//Updating surplus if this agent was a seller
		else {
			if(this.getPrivateValue() != null) {
				for(int q=0; q < tr.getQuantity(); q++) {
					int dev = this.getPrivateValueAt(q).getPrice();
					s = dev + fund - tr.getPrice().getPrice();
					newSurplus = s + this.surplus.get(rho);
					this.surplus.put(rho, newSurplus);				
				}
			}
			else {
				s = tr.price.getPrice();
				newSurplus = s + this.surplus.get(rho);
				this.surplus.put(rho, newSurplus);			}
		}
		Logger.log(Logger.INFO, tr.getTimestamp() + " | " + this + 
				" Agent::updateTransactions: SURPLUS for this transaction: " + s);
		return s;
	}
}
