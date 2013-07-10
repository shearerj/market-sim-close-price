package entity;

import static logger.Logger.log;
import static logger.Logger.Level.DEBUG;
import static logger.Logger.Level.ERROR;
import static logger.Logger.Level.INFO;
import static systemmanager.Consts.INF_TIME;

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

	protected final RandPlus rand;

	protected final MarketModel model;
	protected final Map<Double, Double> surplusMap; // hashed by rho value
	// For quote generation
	protected final SIP sip;

	// Agent parameters
	protected final PrivateValue privateValue;
	protected final TimeStamp arrivalTime;

	// -- end reorg --
	
	protected String agentType;

	// Market information (all hashed by market ID, as ID may be negative)
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
	protected int tickSize;

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
			PrivateValue pv, RandPlus rand, SIP sip) {
		super(agentID);
		this.model = model;
		this.rand = rand;
		this.arrivalTime = arrivalTime;
		this.sip = sip;

		// Constructors
		this.currentBid = new HashMap<Integer, Bid>();
		this.surplusMap = new HashMap<Double, Double>();
		this.transactions = new ArrayList<Transaction>();
		this.idComparator = new TransactionIDComparator();
		this.privateValue = pv;
	}

	public abstract Collection<? extends Activity> agentStrategy(
			TimeStamp currentTime);

	public abstract Collection<? extends Activity> agentArrival(
			TimeStamp currentTime);

	/**
	 * @return Method to get the type of the agent.
	 */
	@Deprecated
	// FIXME should return Consts.AgentType corresponding to current agent. Need
	// a good way to do this.
	public String getType() {
		return agentType;
	}

	/***********************************
	 * Methods for Activities
	 * 
	 **********************************/

	/**
	 * Wrapper method to submit bid to market after checking permissions.
	 */
	public Collection<? extends Activity> submitBid(Market market, Price price,
			int quantity, TimeStamp scheduledTime) {
		return Collections.singleton(new SubmitBid(this, market, price,
				quantity, scheduledTime));
	}

	/**
	 * Wrapper method to submit multiple-point bid to market after checking
	 * permissions.
	 */
	public Collection<? extends Activity> submitMultipleBid(Market market,
			Map<Price, Integer> priceQuantMap, TimeStamp scheduledTime) {
		return Collections.singleton(new SubmitMultipleBid(this, market,
				priceQuantMap, scheduledTime));
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
		return market.removeBid(this, ts);
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
	public Collection<? extends Activity> liquidateAtFundamental(
			TimeStamp currentTime) {
		log(INFO, currentTime + " | " + this + " liquidating...");
		return Collections.singleton(new Liquidate(this,
				model.getFundamentalAt(currentTime), INF_TIME));
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
									+ t.getBuyer().getID() + ", seller="
									+ t.getSeller().getID() + ", price="
									+ t.getPrice() + ", quantity="
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
			// get all transactions after the last seen transaction (not
			// inclusive)
			TreeSet<Transaction> tmp = new TreeSet<Transaction>(idComparator);
			tmp.addAll(model.getTrans());
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
	 * XXX This is probably correct, but this is doing it at instantaneous
	 * market price, not at NBBO price, or something like that.
	 * 
	 * @return agent's unrealized profit/gain
	 */
	public int getUnrealizedProfit() {
		Price p = null;

		Collection<Market> markets = model.getMarkets();

		if (positionBalance > 0) {
			// For long position, compare cost to bid quote (buys)
			for (Market market : markets)
				if (market.getBidPrice().greaterThan(p))
					p = market.getBidPrice();
		} else {
			// For short position, compare cost to ask quote (sells)
			for (Market market : markets) {
				if (market.getAskPrice().lessThan(p)) {
					p = market.getAskPrice();
				}
			}
		}

		if (positionBalance != 0)
			log(DEBUG, this.getModel().getFullName() + ": " + this + " bal="
					+ positionBalance + ", p=" + p + ", avgCost=" + averageCost);

		return p == null ? 0 : positionBalance * (p.getPrice() - averageCost);
	}

	/***********************************
	 * Methods for agent strategies
	 * 
	 **********************************/

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
			for (int quantity = 1; quantity <= tr.getQuantity(); quantity++) {
				Price valuation = privateValue.getValueFromQuantity(
						positionBalance, quantity).plus(new Price(fund));
				int surplus = sign * tr.getPrice().minus(valuation).getPrice();
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

	public final TimeStamp getArrivalTime() {
		return arrivalTime;
	}

	public final MarketModel getModel() {
		return model;
	}

	public final PrivateValue getPrivateValue() {
		return privateValue;
	}

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
		Double surplus = surplusMap.get(rho);
		return surplus == null ? Double.NaN : surplus;
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
		return new String("(" + id + ", " + model + ")");
	}
}
