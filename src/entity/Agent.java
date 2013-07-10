package entity;


import static logger.Logger.log;
import static logger.Logger.Level.DEBUG;
import static logger.Logger.Level.ERROR;
import static logger.Logger.Level.INFO;
import static systemmanager.Consts.INF_PRICE;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeSet;

import market.BestBidAsk;
import market.Bid;
import market.PQBid;
import market.Price;
import market.PrivateValue;
import market.Quote;
import market.Transaction;
import market.TransactionIDComparator;
import model.MarketModel;
import systemmanager.Consts;
import utils.RandPlus;
import activity.Activity;
import activity.Liquidate;
import activity.SubmitBid;
import activity.SubmitMultipleBid;
import activity.WithdrawBid;
import event.TimeStamp;

/**
 * Base class for all agents.
 * 
 * @author ewah
 */
public abstract class Agent extends Entity {

	public final static String SLEEPTIME_KEY = "sleepTime";
	public final static String SLEEPVAR_KEY = "sleepVar";
	public final static String REENTRY_RATE = "reentryRate";

	protected int logID; // ID for logging purposes (should same across models)
	protected final RandPlus rand;

	// -- begin reorg -- stuff above line existed before and is still
	// relevant...

	protected MarketModel model;
	protected Map<Double, Double> surplusMap; // hashed by rho value

	// -- end reorg --

	protected int modelID; // ID of associated model

	// Market information (all hashed by market ID, as ID may be negative)
	protected HashMap<Integer, Price> bidPrice;
	protected HashMap<Integer, Price> askPrice;
	protected HashMap<Integer, Bid> currentBid;
	protected HashMap<Integer, ArrayList<Quote>> quotes;
	protected HashMap<Integer, Integer> initBid;
	protected HashMap<Integer, Integer> initAsk;
	protected HashMap<Integer, Integer> prevBid;
	protected HashMap<Integer, Integer> prevAsk;
	protected HashMap<Integer, TimeStamp> lastQuoteTime;
	protected HashMap<Integer, TimeStamp> nextQuoteTime;
	protected HashMap<Integer, TimeStamp> lastClearTime;
	protected BestBidAsk lastNBBOQuote;
	protected BestBidAsk lastGlobalQuote;
	protected int tickSize;

	// For quote generation
	protected Sip_Prime sip;
	//protected IP_LA ip_LA;
	//protected IP_SM ip_SM;

	// Agent parameters
	protected PrivateValue alpha;
	protected String agentType;
	protected TimeStamp arrivalTime;

	// Transaction information
	protected TimeStamp lastTransTime;
	protected Transaction lastTransaction; // last transaction seen
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
	public final static String SLEEPRATE_KEY = "sleepRate";
	public final static String NUMRUNGS_KEY = "numRungs";
	public final static String RUNGSIZE_KEY = "rungSize";
	public final static String MAXQUANTITY_KEY = "maxqty";
	public final static String MARKETID_KEY = "marketID";

	// FIXME Agent probably needs more than this...
	public Agent(int agentID, TimeStamp arrivalTime, MarketModel model,
			PrivateValue pv, RandPlus rand) {
		super(agentID);
		this.model = model;
		this.rand = rand;
		this.arrivalTime = arrivalTime;
		this.alpha = pv;
	}
	
	/**
	 * Constructor
	 * @param agentID
	 * @param modelID
	 * @param d
	 * @param p
	 * @param l
	 */
	public Agent(int agentID, int modelID) {
		super(agentID);
		
		// -- Begin Reorg --
		//model = d.models.get(modelID); // not sure if we need this?
		
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
		
		//lastGlobalQuote = new BestBidAsk();
		//lastNBBOQuote = new BestBidAsk();
		
		alpha = null;		// if no PV, this will always be null
		arrivalTime = new TimeStamp(0);
		
		tickSize = data.tickSize;
		sip = getModel().getSip();
		//ip_LA = mkt.getIPLA();
		//ip_SM = mkt.getIPSM();
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

	@Deprecated
	public abstract Collection<? extends Activity> updateAllQuotes(TimeStamp ts);

	/**
	 * @return true if has non-null private value.
	 */
	@Deprecated
	// This should not exist, agents without private value should have 0 private
	// value...
	public final boolean hasPrivateValue() {
		return alpha != null;
	}

	/**
	 * Given additional quantity to buy/sell, return associated private
	 * valuation (requires looking at current position balance).
	 * 
	 * Required because of indexing of quantities vector in PrivateValue.
	 * 
	 * @param quantity
	 *            additional units to buy or sell
	 * @return
	 */
	// TODO put this in PrivateValue
	public Price getPrivateValueAt(int quantity) {
		if (quantity > 0) {
			// if buying
			if (positionBalance >= 0) {
				// if nonnegative current position, look at next position (+q)
				return alpha.getValueFromQuantity(positionBalance + quantity);
			} else {
				// if negative current position, look at current position
				return alpha.getValueFromQuantity(positionBalance);
			}

		} else if (quantity < 0) {
			// if selling
			if (positionBalance > 0) {
				// if positive current position, look at current position
				return alpha.getValueFromQuantity(positionBalance);
			} else {
				// if non-positive current position, look at next position
				// (-|q|)
				return alpha.getValueFromQuantity(positionBalance + quantity);
			}

		} else {
			// not selling or buying
			return new Price(0);
		}
	}

	/**
	 * @return Method to get the type of the agent.
	 */
	@Deprecated
	// Doesn't make any sense
	public String getType() {
		return agentType;
	}

	/**
	 * @return role of agent
	 */
	@Deprecated
	// Shouldn't ever be used
	public String getRole() {
		if (this instanceof HFTAgent) {
			return Consts.ROLE_HFT;
		} else if (this instanceof BackgroundAgent) {
			return Consts.ROLE_BACKGROUND;
		} else if (this instanceof MarketMaker) {
			return (Consts.ROLE_MARKETMAKER);
		} else {
			System.err.println(this.getClass().getSimpleName()
					+ "::getRole(): " + "invalid agent!");
			System.exit(1);
		}
		return "";
	}

	/**
	 * @return strategy for observation file in format TYPE:STRATEGY
	 */
	@Deprecated
	// Only necessary for players, not agents...
	public String getFullStrategy() {
		return this.getType() + ":" + params.getAsString(Agent.STRATEGY_KEY);
	}

	/**
	 * Sets logID of the agent.
	 * 
	 * @param logID
	 */
	@Deprecated
	public void setLogID(int logID) {
		this.logID = logID;
	}

	/**
	 * @return logID
	 */
	@Deprecated
	public int getLogID() {
		return this.logID;
	}

	/**
	 * Computes a randomized sleep time based on sleepTime & sleepVar.
	 * 
	 * @param sleepTime
	 * @param sleepVar
	 * @return
	 */
	@Deprecated
	// sleeptime not even defined for general agents. This should be moved
	// somewhere else
	public int getRandSleepTime(int sleepTime, double sleepVar) {
		return (int) Math.round(rand.nextGaussian(sleepTime, sleepVar));
	}

	/**
	 * @param mktID
	 * @return number of quotes
	 */
	@Deprecated
	// Should just call size on getQuote
	public int getQuoteSize(int mktID) {
		return getQuote(mktID).size();
	}

	/**
	 * If quotes contains list for that market, return it; otherwise return
	 * empty list.
	 */
	// TODO possibly remove / move somewhere else / at least use full market.
	@Deprecated
	public ArrayList<Quote> getQuote(int mktID) {
		if (quotes.get(mktID) != null) {
			return quotes.get(mktID);
		} else {
			return new ArrayList<Quote>();
		}
	}

	/**
	 * Add given quote to the market. Initializes empty list if doesn't exist
	 * yet.
	 * 
	 * @param mktID
	 * @param q
	 */
	// TODO This general quote functionality seems misplaced. If an agent needs
	// to track market quotes it can implement that seperately along with any
	// other data gathering it needs. It seems incorrect for it to be general
	// agent functionality especially seeing as agents may want more refined
	// information. This may be partially taken up in the information processors
	@Deprecated
	public void addQuote(int mktID, Quote q) {
		if (quotes.get(mktID) == null) {
			// create for that market
			quotes.put(mktID, new ArrayList<Quote>());
		}
		quotes.get(mktID).add(q);
	}

	/**
	 * Gets latest quote from a given market. If no quote available, returns a
	 * default Quote object.
	 * 
	 * @param mktID
	 * @return most recent quote for the specified market
	 */
	@Deprecated
	public Quote getLatestQuote(int mktID) {
		if (quotes.isEmpty() || getQuote(mktID) == null) {
			return new Quote();
		} else if (getQuote(mktID).isEmpty()) {
			return new Quote();
		}
		// return last element in the list
		return quotes.get(mktID).get(quotes.size() - 1);
	}

	/**
	 * @param mktID
	 * @param idx
	 * @return quote at a specified index
	 */
	@Deprecated
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
	@Deprecated
	// Agent should just go to market for this information
	public Price getBidPrice(int mktID) {
		return bidPrice.get(mktID);
	}

	/**
	 * @param mktID
	 * @return ask price for the specified market
	 */
	@Deprecated
	// Agent should just go to market for this information
	public Price getAskPrice(int mktID) {
		return askPrice.get(mktID);
	}

	/***********************************
	 * Methods for Activities
	 * 
	 **********************************/

	/**
	 * Enters market by adding market to data structures.
	 */
	// TODO Is this really necessary?
	protected void enterMarket(Market mkt, TimeStamp ts) {
		mkt.agentIDs.add(this.id); // Necessary?
		mkt.buyers.add(this.id); // Necessary?
		mkt.sellers.add(this.id); // Necessary?
		quotes.put(mkt.id, new ArrayList<Quote>());

		// Initialize bid/ask containers
		prevBid.put(mkt.id, 0);
		prevAsk.put(mkt.id, 0);
		initBid.put(mkt.id, -1);
		initAsk.put(mkt.id, -1);
	}

	/**
	 * Exits market by removing all entries hashed by the specified market ID.
	 */
	// TODO Is this really necessary?
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
	 */
	public Collection<? extends Activity> submitBid(Market mkt, Price price,
			int quantity, TimeStamp ts) {
		return Collections.singleton(new SubmitBid(this, mkt, price, quantity,
				ts));
	}

	/**
	 * Wrapper method to submit multiple-point bid to market after checking
	 * permissions.
	 */
	public Collection<? extends Activity> submitMultipleBid(Market mkt,
			Map<Price, Integer> priceQuantMap, TimeStamp currentTime) {
		return Collections.singleton(new SubmitMultipleBid(this, mkt,
				priceQuantMap, currentTime));
	}

	/**
	 * Wrapper method to expire the agent's bid from a market after a specified
	 * duration.
	 * 
	 * TODO how does this work? Expires all bids? The first bid it finds? The
	 * oldest bid? Maybe should contain a reference to the bid in question...
	 */
	public Collection<? extends Activity> expireBid(Market market,
			TimeStamp duration, TimeStamp currentTime) {
		TimeStamp withdrawTime = currentTime.sum(new TimeStamp(duration));
		log(INFO, currentTime + " | " + market + " " + this + ": bid duration="
				+ duration);
		return Collections.singleton(new WithdrawBid(this, market, withdrawTime));
	}

	/**
	 * Withdraws a bid from the given market.
	 */
	public Collection<? extends Activity> executeWithdrawBid(Market market,
			TimeStamp ts) {
		log(INFO, ts + " | " + this + " withdraw bid from " + market);
		return market.removeBid(this.id, ts);
	}

	/**
	 * Submit a bid to the specified market.
	 */
	public Collection<? extends Activity> executeSubmitBid(Market market,
			Price price, int quantity, TimeStamp ts) {
		if (quantity == 0)
			return Collections.emptySet();

		log(INFO, ts + " | " + this + " " + agentType + "::submitBid: +("
				+ price + ", " + quantity + ") to " + market);

		Price p = price.quantize(tickSize);
		PQBid pqBid = new PQBid(this, market, ts);
		pqBid.addPoint(quantity, p);
		// quantity can be +/-
		if (hasPrivateValue()) {
			data.addPrivateValue(pqBid.getBidID(), getPrivateValueAt(quantity));
		} else {
			data.addPrivateValue(pqBid.getBidID(), null);
		}
		currentBid.put(market.id, pqBid);
		return market.addBid(pqBid, ts);
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
	// TODO swtich form parallel array lists to Map<Price, Integer>
	public Collection<? extends Activity> executeSubmitMultipleBid(Market mkt,
			Map<Price, Integer> priceQuantityMap, TimeStamp ts) {
		log(INFO, ts + " | " + mkt + " " + this + ": +" + priceQuantityMap);

		PQBid pqBid = new PQBid(this, mkt, ts);
		for (Entry<Price, Integer> priceQuant : priceQuantityMap.entrySet()) {
			int quantity = priceQuant.getValue();
			Price price = priceQuant.getKey();
			if (quantity == 0)
				continue; // TODO add check in PQBid instead
			pqBid.addPoint(quantity, price.quantize(tickSize));
		}
		// TODO incorporate multi-point PVs?
		currentBid.put(mkt.id, pqBid);
		return mkt.addBid(pqBid, ts);
	}

	/**
	 * Liquidate agent's position at the the value of the global fundamental at
	 * the specified time. Price is determined by the fundamental at the time of
	 * liquidation.
	 */
	public Collection<? extends Activity> liquidateAtFundamental(TimeStamp ts) {
		log(INFO, ts + " | " + this + " liquidating...");
		return Collections.singleton(new Liquidate(this,
				model.getFundamentalAt(ts), ts)); // FIXME maybe infinite time?
	}

	/**
	 * Liquidates an agent's position at the specified price.
	 */
	public Collection<? extends Activity> executeLiquidate(Price price,
			TimeStamp ts) {

		log(INFO, ts + " | " + this + " pre-liquidation: position="
				+ positionBalance + ", profit=" + realizedProfit);

		// If no net position, no need to liquidate
		if (positionBalance == 0)
			return Collections.emptyList();

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

		log(INFO, ts + " | " + this + " post-liquidation: position="
				+ positionBalance + ", profit=" + realizedProfit + ", price="
				+ price);
		return Collections.emptyList();
	}

	/**
	 * Update global and NBBO quotes for the agent's model.
	 */
	public Collection<? extends Activity> executeUpdateAllQuotes(TimeStamp ts) {
		lastGlobalQuote = sip.getGlobalQuote(modelID);
		lastNBBOQuote = sip.getNBBOQuote(modelID);

		log(INFO, ts + " | " + this + " Global" + lastGlobalQuote + ", NBBO"
				+ lastNBBOQuote);
		return Collections.emptyList();
	}

	/**
	 * Updates quotes for the given market.
	 * 
	 * @param mkt
	 * @param ts
	 */
	@Deprecated
	// Should just be done in strategy. Doesn't should need fields to store this
	// info...
	public void updateQuotes(Market mkt, TimeStamp ts) {
		Quote q = mkt.quote(ts);
		if (q != null) {
			if (q.lastAskPrice == null)
				askPrice.put(mkt.id, INF_PRICE);
			else
				askPrice.put(mkt.id, q.lastAskPrice);
			if (q.lastBidPrice == null)
				bidPrice.put(mkt.id, new Price(0));
			else
				bidPrice.put(mkt.id, q.lastBidPrice);

			if (q.lastQuoteTime != null)
				lastQuoteTime.put(mkt.id, q.lastQuoteTime);

			if (q.lastClearTime != null
					&& lastClearTime.get(mkt.id) != q.lastClearTime) {
				lastClearTime.put(mkt.id, q.lastClearTime);
			}
		} else {
			log(ERROR, "Agent::updateQuotes: Quote is null.");
		}
		addQuote(mkt.id, q);
	}

	/***********************************
	 * Transaction-related methods
	 * 
	 **********************************/

	/**
	 * Logs transactions for the agent and prints a summary of performance so
	 * far.
	 * 
	 * @param currentTime
	 */
	// TODO Looging is good. Not sure if this is particularly necessary though.
	// Maybe market can just do this at clear...
	public void logTransactions(TimeStamp currentTime) {

		int rp = getRealizedProfit();

		// int up = getUnrealizedProfit();

		String s = currentTime.toString() + " | " + this
				+ " Agent::logTransactions: " + this.getModel().getFullName()
				+ ": Current Position=" + positionBalance
				+ ", Realized Profit=" + rp;
		// + ", Unrealized Profit=" + up;
		log(INFO, s);
	}

	/**
	 * Process a transaction received from the server.
	 * 
	 * For calling function having a list of transactions, it should use the
	 * last good transaction id to update lastTransID and then terminates, if a
	 * false return value is received.
	 * 
	 * @param t
	 *            Transaction object to be processed.
	 * @return True if the object is good and all necessary parameters are
	 *         updated; false otherwise.
	 * 
	 */
	@Deprecated
	// Transaction handeling probably shouldn't happen in agent?
	// TODO Ths seems bad. A lot of the erro checking should never happen.
	public boolean processTransaction(Transaction t) {
		boolean flag = true;
		if (t == null) {
			log(ERROR,
					"Agent::processTransaction: Corrupted (null) transaction record.");
			flag = false;
		} else {
			if (t.getMarket() == null) {
				log(ERROR, "Agent::processTransaction: t.market is null");
				flag = false;
			}
			if (t.getPrice() == null) {
				log(ERROR, "Agent::processTransaction: t.price is null");
				flag = false;
			}
			if (t.getQuantity() <= 0) {
				log(ERROR, "Agent::processTransaction: t.quantity is null");
				flag = false;
			}
			if (t.getExecTime() == null) {
				log(ERROR, "Agent::processTransaction: t.timestamp is null");
				flag = false;
			}
		}
		if (!flag) {
			return false;
		} else {
			// check whether seller, in which case negate the quantity
			int quantity = t.getQuantity();
			if (this.id == t.getSeller().getID()) {
				quantity = -quantity;
			}
			// update cash flow and position
			if (positionBalance == 0) {
				averageCost = t.getPrice().getPrice();

			} else if (positionBalance > 0) {
				if (quantity > 0) {
					int newCost = averageCost * positionBalance
							+ t.getPrice().getPrice() * quantity;
					averageCost = newCost / (positionBalance + quantity);

				} else if (-quantity < positionBalance) {
					// closing out partial long position
					int rprofit = realizedProfit;
					rprofit += (-quantity)
							* (t.getPrice().getPrice() - averageCost);
					realizedProfit = rprofit;

				} else if (-quantity >= positionBalance) {
					// closing out all long position
					// remaining quantity will start new short position
					int rprofit = realizedProfit;
					rprofit += positionBalance
							* (t.getPrice().getPrice() - averageCost);
					realizedProfit = rprofit;
					averageCost = t.getPrice().getPrice();
				}

			} else if (positionBalance < 0) {
				if (quantity < 0) {
					int newCost = averageCost * (-positionBalance)
							+ t.getPrice().getPrice() * (-quantity);
					averageCost = -newCost / (positionBalance + quantity);

				} else if (quantity < -positionBalance) {
					// closing out partial short position
					int rprofit = realizedProfit;
					rprofit += quantity
							* (averageCost - t.getPrice().getPrice());
					realizedProfit = rprofit;

				} else if (quantity >= -positionBalance) {
					// closing out all short position
					// remaining quantity will start new long position
					int rprofit = realizedProfit;
					rprofit += (-positionBalance)
							* (averageCost - t.getPrice().getPrice());
					realizedProfit = rprofit;
					averageCost = t.getPrice().getPrice();
				}
			}
			positionBalance += quantity;
		}
		return true;
	}

	/**
	 * Get new transactions. All obtained transactions are processed
	 * sequentially. Whenever an incomplete transaction record is encountered,
	 * the function will stop and update lastTransID accordingly.
	 * 
	 * @param ts
	 *            TimeStamp of update
	 */
	@Deprecated
	// Transaction handling shouldn't happen in agent
	public void updateTransactions(TimeStamp ts) {
		Collection<Transaction> list = getNewTransactions();

		log(DEBUG, ts + " | " + this + " " + "lastTrans=" + lastTransaction);

		if (list != null) {
			Transaction lastGoodTrans = null;

			for (Transaction t : list) {
				// Check that this agent is involved in the transaction
				if (t.getBuyer().getID() == this.id
						|| t.getSeller().getID() == this.id) {
					boolean flag = processTransaction(t);
					if (!flag && lastGoodTrans != null) {
						lastTransaction = lastGoodTrans;
						log(ERROR,
								ts
										+ " | "
										+ this
										+ " "
										+ "Agent::updateTransactions: Problem with transaction.");
						break;
					}

					log(INFO,
							ts
									+ " | "
									+ this
									+ " "
									+ "Agent::updateTransactions: New transaction received: ("
									+ "transID=" + t.getTransID() + ", mktID="
									+ t.getMarket().getID() + ", buyer="
									+ data.getAgentLogID(t.getBuyer().getID())
									+ ", seller="
									+ data.getAgentLogID(t.getSeller().getID())
									+ ", price=" + t.getPrice() + ", quantity="
									+ t.getQuantity() + ", timeStamp="
									+ t.getExecTime() + ")");
				}
				// Update transactions
				lastGoodTrans = t;
				transactions.add(t);
			}
			lastTransaction = lastGoodTrans;
			log(DEBUG, ts + " | " + this + " " + "NEW lastTrans="
					+ lastGoodTrans);
		}
		lastTransTime = ts;
	}

	/**
	 * Gets all transactions that have not been processed yet.
	 */
	@Deprecated
	// Transaction handling shouldn't happen in agent
	public Collection<Transaction> getNewTransactions() {

		if (lastTransaction == null) {
			// get all transactions for this model
			return model.getTrans();
		} else {
			TreeSet<Transaction> tmp = new TreeSet<Transaction>(idComparator);
			tmp.addAll(model.getTrans());
			// get all transactions after the last seen transaction (not
			// inclusive)
			return new ArrayList<Transaction>(tmp.tailSet(lastTransaction,
					false));
		}
	}

	/***********************************
	 * Determining position & profit
	 * 
	 **********************************/

	/**
	 * Computes any unrealized profit based on market bid/ask quotes. Checks the
	 * markets belonging to the Agent's model.
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
				log(DEBUG, this.getModel().getFullName() + ": " + this
						+ " bal=" + positionBalance + ", p=" + p + ", avgCost="
						+ averageCost);
			}
			if (p != -1) {
				up += positionBalance * (p - averageCost);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return up;
	}

	/***********************************
	 * Methods for agent strategies
	 * 
	 **********************************/

	/**
	 * Checks the bid/ask prices for errors.
	 */
	@Deprecated
	// Why does this exist? Never should have errors
	boolean checkBidAsk(int mktID, ArrayList<Price> price) {
		if (price.size() < 2)
			return false;

		int bid = price.get(0).getPrice();
		int ask = price.get(1).getPrice();
		boolean flag = true;

		if (ask > 0 && bid > 0) {
			prevAsk.put(mktID, ask);
			prevBid.put(mktID, bid);
			flag = false;
		} else if (ask <= 0 && bid > 0) {
			double oldask = ask;
			log(DEBUG, "Agent::checkBidAsk: ask: " + oldask + " to " + ask);
			prevAsk.put(mktID, ask);
			prevBid.put(mktID, bid);
		} else if (bid <= 0 && ask > 0) {
			double oldbid = bid;
			log(DEBUG, "Agent::checkBidAsk: bid: " + oldbid + " to " + bid);
			prevAsk.put(mktID, ask);
			prevBid.put(mktID, bid);
		} else {
			double oldbid = bid;
			double oldask = ask;
			log(DEBUG, "Agent::checkBidAsk: bid: " + oldbid + " to " + bid
					+ ", ask: " + oldask + " to " + ask);
		}
		bid = Math.max(bid, 1);
		ask = Math.max(ask, 1);

		return flag;
	}

	public double addSurplus(double rho, int fund, Transaction tr,
			boolean isBuyer) {
		if (!this.surplusMap.containsKey(rho))
			this.surplusMap.put(rho, 0.0);

		// Determining Execution Time
		double submissionTime;
		if (isBuyer)
			submissionTime = tr.getBuyBid().getSubmitTime().getLongValue();
		else
			submissionTime = tr.getSellBid().getSubmitTime().getLongValue();
		double timeToExecution = tr.getExecTime().getLongValue()
				- submissionTime;

		// Updating surplus
		double oldSurplus = this.surplusMap.get(rho);
		double discounted = 0;
		int sign = isBuyer ? -1 : 1;
		if (this.getPrivateValue() != null) {
			for (int q = 1; q <= tr.getQuantity(); q++) {
				int valuation = this.getPrivateValueAt(q).getPrice() + fund;
				int surplus = sign * (tr.getPrice().getPrice() - valuation);
				discounted += Math.exp(-rho * timeToExecution * surplus);
			}
			this.surplusMap.put(rho, oldSurplus + discounted);
		} else {
			int surplus = sign * tr.getPrice().getPrice();
			discounted = Math.exp(-rho * timeToExecution * surplus);
			discounted *= tr.getQuantity();
			this.surplusMap.put(rho, oldSurplus + discounted);
		}
		log(INFO, tr.getExecTime() + " | " + this
				+ " Agent::updateTransactions: SURPLUS at rho=" + rho
				+ " for this transaction: " + discounted);
		return discounted;
	}

	/**
	 * @return arrival time for an agent.
	 */
	public final TimeStamp getArrivalTime() {
		return arrivalTime;
	}

	/**
	 * @return MarketModel of the agent.
	 */
	public final MarketModel getModel() {
		return model;
	}

	/**
	 * @return private value.
	 */
	public final PrivateValue getPrivateValue() {
		return alpha;
	}

	/**
	 * @return current cash balance
	 */
	public int getCashBalance() {
		return cashBalance;
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

	public Map<Double, Double> getSurplus() {
		return Collections.unmodifiableMap(surplusMap);
	}

	public double getSurplus(double rho) {
		return surplusMap.get(rho);
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ model.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof Agent))
			return false;
		Agent agent = (Agent) obj;
		return super.equals(agent) && model.equals(agent.model);
	}

	@Override
	public String toString() {
		return new String("(" + this.logID + ", " + this.getModel() + ")");
	}
}
