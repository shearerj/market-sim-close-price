package entity.agent;

import static com.google.common.base.Preconditions.checkArgument;
import static fourheap.Order.OrderType.BUY;
import static fourheap.Order.OrderType.SELL;
import static logger.Log.Level.INFO;

import java.util.ArrayList;
import java.util.List;

import logger.Log;
import systemmanager.Keys.BetaR;
import systemmanager.Keys.BetaT;
import systemmanager.Keys.BuyerStatus;
import systemmanager.Keys.Debug;
import systemmanager.Keys.Eta;
import systemmanager.Keys.Gamma;
import systemmanager.Keys.InitAggression;
import systemmanager.Keys.LambdaA;
import systemmanager.Keys.LambdaR;
import systemmanager.Keys.NumHistorical;
import systemmanager.Keys.Theta;
import systemmanager.Keys.ThetaMax;
import systemmanager.Keys.ThetaMin;
import utils.Rand;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.collect.Range;

import data.FundamentalValue;
import data.Props;
import data.Stats;
import entity.agent.position.Aggression;
import entity.market.Market;
import entity.market.Price;
import entity.market.Transaction;
import entity.sip.MarketInfo;
import event.Timeline;
import fourheap.Order.OrderType;

/**
 * AAAgent
 *
 * Based on: Vytelingum, Cliff, Jennings, "Strategic bidding in continuous 
 * double auctions," Artificial Intelligence, 172, pp. 1700-1729. 2008.
 * 
 * @author drhurd, ewah
 *
 */
public class AAAgent extends WindowAgent {

	private static final long serialVersionUID = 2418819222375372886L;
	private static final Ordering<Price> pcomp = Ordering.natural();

	// TODO Make the variables that don't change private final
	
	// Agent variables
	protected OrderType type; // randomly assigned at initialization
	protected Price lastTransactionPrice;
	protected Price equilibriumPrice;
	protected Price targetPrice; 
	private boolean debug;

	// Agent strategy variables
	// Based on Vytelingum's sensitivity analysis, eta and N are most important
	private double lambdaR; // coefficient of relative perturbation of delta, (0,1)?
	private double lambdaA; // coefficient of absolute perturbation of delta, positive
	private double gamma; // long term learning variable, set to 2
	private double betaR; // learning coefficient for r (short term) (0,1)
	private double betaT; // learning coefficient for theta (long term) (0,1)
	private int eta; // price determination coefficient. [1,inf)
	private int numHistorical; // number of historical prices to look at, N > 0

	// Agent parameters
	private double rho;		// factor for weighted moving average (0.9 from paper)
	protected Aggression aggressions;
	protected double aggression; // current short term learning variable, r in [-1,1]
	protected double theta; // long term learning variable
	private double thetaMax; // max possible value for theta
	private double thetaMin; // min possible value for theta
	private double alphaMax; // max experienced value for alpha (for theta, not PV)
	private double alphaMin; // min experienced value for alpha

	protected AAAgent(int id, Stats stats, Timeline timeline, Log log, Rand rand, MarketInfo sip, FundamentalValue fundamental,
			Market market, Props props) {
		super(id, stats, timeline, log, rand, sip, fundamental, market, props);

		this.type = props.get(BuyerStatus.class);
		this.lastTransactionPrice = null;
		this.equilibriumPrice = null;
		this.targetPrice = null;

		//Initializing parameters
		this.aggression = props.get(InitAggression.class);
		this.theta = props.get(Theta.class);
		this.thetaMin = props.get(ThetaMin.class);
		this.thetaMax = props.get(ThetaMax.class);
		this.alphaMin = Price.INF.intValue();
		this.alphaMax = -1;
		this.aggressions = Aggression.create(getMaxAbsPosition(), aggression);
		this.rho = 0.9; 		// from paper, to emphasize converging pattern
		
		//Initializing strategy variables
		this.numHistorical = props.get(NumHistorical.class);
		this.lambdaA = props.get(LambdaA.class);
		this.lambdaR = props.get(LambdaR.class);
		this.gamma = props.get(Gamma.class);
		this.eta = props.get(Eta.class);
		this.betaR = props.get(BetaR.class);		// paper randomizes to U[0.2, 0.6]
		this.betaT = props.get(BetaT.class);		// paper randomizes to U[0.2, 0.6]
		
		// Debugging
		this.debug = props.get(Debug.class);
		
		// Check arguments
		checkArgument(betaR > 0 && betaR < 1, "Beta for r must be in (0,1)");
		checkArgument(betaT > 0 && betaT < 1, "Beta for theta must be in (0,1)");
		checkArgument(eta >= 1, "Eta must be in [1, inf)");
		checkArgument(numHistorical > 0, "Number of historical prices must be positive");
		checkArgument(lambdaA >= 0, "lambdaA must be positive");
		checkArgument(lambdaR >= 0, "lambdaR must be positive");
	}

	public static AAAgent create(int id, Stats stats, Timeline timeline, Log log, Rand rand, MarketInfo sip, FundamentalValue fundamental,
			Market market, Props props) {
		return new AAAgent(id, stats, timeline, log, rand, sip, fundamental, market, props);
	}

	@Override
	protected void agentStrategy() {
		super.agentStrategy();

		// re-initialize variables
		lastTransactionPrice = null;
		equilibriumPrice = null;
		targetPrice = null;
		if (!debug) type = rand.nextBoolean() ? BUY : SELL;

		// Update aggression
		aggression = aggressions.getValue(getPosition(), type);

		// Updating Price Limit (valuation of security)
		Price limitPrice = getLimitPrice(type);

		List<Transaction> transactions = getWindowTransactions();
		if (!transactions.isEmpty())
			lastTransactionPrice = transactions.get(transactions.size()-1).getPrice();

		// Estimate equilibrium price using weighted moving average
		equilibriumPrice = this.estimateEquilibrium(transactions);
		
		log(INFO, "%s::agentStrategy: estimateEquilibrium: price=%s", this, equilibriumPrice);

		// Aggressiveness layer
		// ----------------------
		// Determine the target price tau using current r & theta
		targetPrice = this.determineTargetPrice(limitPrice, equilibriumPrice);
		log(INFO, "%s::agentStrategy: determineTargetPrice: target=%s", this, targetPrice);

		// Adaptive layer
		// ----------------------
		// Update the short term learning variable (aggressiveness r)
		double oldAggression = aggression;
		this.updateAggression(limitPrice, targetPrice, equilibriumPrice, lastTransactionPrice);
		log(INFO, "%s::agentStrategy: updateAggression: lastPrice=%s, r=%.4f-->r_new=%.4f", 
				this, lastTransactionPrice, oldAggression, aggression);

		// Update long term learning variable (adaptiveness theta)
		double oldTheta = theta;
		this.updateTheta(equilibriumPrice, transactions);
		log(INFO, "%s::agentStrategy: updateTheta: theta=%.4f-->theta_new=%.4f", this, oldTheta, theta);

		// Bidding Layer
		biddingLayer(limitPrice, targetPrice, 1);
	}

	/**
	 * Section 4.2, Eq (3, 4, 5, 6) in Vytelingum et al
	 * 
	 * @param limitPrice
	 * @param equilibriumPrice
	 * @return price target according to the AA Strategy
	 */
	protected Price determineTargetPrice(Price limitPrice, Price equilibriumPrice) {
		if (equilibriumPrice == null) return null;

		Price tau = null; // target price
		double eqPrice = equilibriumPrice.intValue();
		double limit = limitPrice.intValue();

		switch (type) {
		case BUY:
			// Intramarginal - price limit > p* (competitive equilibrium price)
			if (limitPrice.greaterThan(equilibriumPrice)) {
				if (aggression == -1) { 		// passive
					tau = Price.ZERO;
				} else if (aggression < 0) {
					tau = Price.of(eqPrice * (1 - tauChange(-aggression)));	// Eq (3)
				} else if (aggression == 0) {	// active
					tau = equilibriumPrice;
				} else if (aggression < 1) {	// aggressive
					tau = Price.of(eqPrice + (limit - eqPrice) 
							* tauChange(aggression));	// Eq (3)
				} else { 						// completely aggressive
					tau = limitPrice;
				}
			}
			// Extramarginal - price limit is less than p*
			else {
				if (aggression == -1) {			// passive
					tau = Price.ZERO;
				} else if (aggression < 0) {	// less aggressive on limit
					tau = Price.of(limit * (1 - tauChange(-aggression)));	// Eq (5)
				} else {
					tau = limitPrice;			// aggression clipped at 0
				}
			}
			break;
			
		case SELL:
			// Intramarginal - cost is less than p*
			if (limitPrice.lessThan(equilibriumPrice)) {
				if (aggression == -1) {			// passive
					tau = Price.INF;
				} else if (aggression < 0) {
					tau = Price.of(eqPrice + (Price.INF.intValue() - eqPrice) 
							* tauChange(-aggression)); // Eq (4)
				} else if (aggression == 0) {	// active
					tau = equilibriumPrice;
				} else if (aggression < 1){		// aggressive
					tau = Price.of(limit + (eqPrice - limit) 
							* (1 - tauChange(aggression)));	// Eq (4)
				} else { 						// completely aggressive
					tau = limitPrice;
				}
			}
			// Extramarginal - cost is greater than p*
			else {
				if (aggression == -1) { 		// passive
					tau = Price.INF;
				} else if (aggression < 0) {	// less aggressive on limit
					tau = Price.of(limit + (Price.INF.intValue() - limit) *
							tauChange(-aggression));	// Eq (6)
				} else { 						// aggressive
					tau = limitPrice;
				}
			}
			break;
		}
		return tau;
	}


	/**
	 * Bidding layer. Section 4.4, Eq (10, 11)
	 * If buyer's (seller's) limit price is lower (higher) than the current 
	 * bid/ask, cannot submit any bid/ask and waits until next round.
	 * 
	 * Note that lambda A must be multiplied by TICKS_PER_DOLLAR, as the paper
	 * adds it directly to the price.
	 * 
	 * @param limitPrice
	 * @param targetPrice
	 * @param quantity
	 * @param currentTime
	 * @return
	 */
	protected void biddingLayer(Price limitPrice, Price targetPrice, 
			int quantity) {
		// Determining the offer price to (possibly) submit
		Optional<Price> obid = getNBBO().getBestBid(),
			oask = getNBBO().getBestAsk();

		// if no bid or no ask, submit ZI strategy bid & exit bidding layer
		if (!obid.isPresent() || !oask.isPresent()) {
			log(INFO, "%s::biddingLayer: Bid/Ask undefined.", this);
			executeZIStrategy(type, quantity);
			return;
		}
		Price bid = obid.get(),
			ask = oask.get();
		
		// If best offer is outside of limit price, no bid is submitted
		if ((type.equals(BUY) && limitPrice.lessThanEqual(bid))
				|| (type.equals(SELL) && limitPrice.greaterThan(ask))) {
			log(INFO, "%s::biddingLayer: Best price is outside of limit price: %s; no submission", 
					this, limitPrice);
			return;
		}

		// Can only submit offer if the offer would not cause position
		// balance to exceed the agent's maximum position
		int newPosBal = getPosition() + quantity;
		
		
		if (!Range.closed(-getMaxAbsPosition(), getMaxAbsPosition()).contains(newPosBal)) {
			log(INFO, "%s::biddingLayer: New order would exceed max position: %d; no submission", 
					this, getMaxAbsPosition());
			return;
		}

		// Pricing - verifying targetPrice
		// Limit should never be less (greater) than the target price for buyers (sellers)
		if (targetPrice != null) {
			if ((type.equals(BUY) && limitPrice.lessThan(targetPrice)) 
					|| (type.equals(SELL) && limitPrice.greaterThan(targetPrice)))
				targetPrice = limitPrice;
		}

		// See Eq 10 and 11 in section 4.4 - bidding layer
		Price orderPrice = null;
		if (targetPrice == null) {
			// if no transactions, so cannot determine target price
			if (type.equals(BUY)) {
				Price askCeil = Price.of((1 + lambdaR) * ask.intValue() 
						+ lambdaA * Price.TICKS_PER_DOLLAR);
				Price offset = Price.of( (pcomp.min(askCeil, limitPrice).intValue() - bid.intValue()) 
						* (1.0/eta));
				orderPrice = Price.of(bid.intValue() + offset.intValue());
				orderPrice = pcomp.min(orderPrice, limitPrice);
			} else {
				Price bidFloor = Price.of((1 - lambdaR) * bid.intValue() 
						- lambdaA * Price.TICKS_PER_DOLLAR);
				Price offset = Price.of( (ask.intValue() - pcomp.max(bidFloor, limitPrice).intValue()) 
						* (1.0/eta));
				orderPrice = Price.of(ask.intValue() - offset.intValue());
				orderPrice = pcomp.max(orderPrice, limitPrice);				
			}
			
		} else {
			// can determine target price
			if (type.equals(BUY)) {
				Price offset = Price.of((targetPrice.intValue() - bid.intValue()) * (1.0/eta));
				orderPrice = Price.of(bid.intValue() + offset.intValue());
				orderPrice = pcomp.min(orderPrice, limitPrice);
				// if bestAsk <= target, submit at bestAsk, else submit price given by EQ 10/11
				orderPrice = ask.lessThanEqual(targetPrice) ? ask : orderPrice;
			} else {
				Price offset = Price.of((ask.intValue() - targetPrice.intValue()) * (1.0/eta));
				orderPrice = Price.of(ask.intValue() - offset.intValue());
				orderPrice = pcomp.max(orderPrice, limitPrice);
				// If bestBid >= target, submit at bestBid, else submit price given by EQ 10/11
				orderPrice = bid.greaterThanEqual(targetPrice) ? bid : orderPrice;
			}
		}
		
		submitNMSOrder(type, orderPrice, 1);
	}


	/**
	 * @param r
	 * @return
	 */
	protected double tauChange(double r) {
		return (Math.exp(r * theta) - 1) / (Math.exp(theta) - 1);
	}


	/**
	 * Short-term learning. Section 4.3.1, Fig 7, Eq (7) in Vytelingum et al.
	 * Uses learning rules to update its aggressiveness whenever get new transaction.
	 * 
	 * @param limit
	 * @param tau
	 * @param equilibriumPrice
	 * @param lastPrice		most recent transaction price
	 */
	protected void updateAggression(Price limit, Price tau, 
			Price equilibriumPrice,	Price lastPrice) {

		if (equilibriumPrice == null || lastPrice == null || tau == null)
			return; // If no transactions yet, cannot update

		// Determining r_shout, the level of aggression that would form a price
		// equal to most recent transaction price
		double rShout = computeRShout(limit, lastPrice, equilibriumPrice);

		// Determining whether agent must be more or less aggressive
		// Differs from paper b/c we do not take into account the most
		// recent bid/ask submitted, only transactions
		int sign = 1;
		switch (type) {
		case BUY:
			// if target price is greater, agent should be less aggressive
			if (tau.compareTo(lastPrice) > 0)
				sign = -1;
			// if transaction price greater or equal, agent should be more aggressive
			else
				sign = 1;
			break;
			
		case SELL:
			// if target price is less, agent should be less aggressive
			if (tau.compareTo(lastPrice) < 0)
				sign = -1;
			// if target price is greater or equal, agent should be more aggressive
			else
				sign = 1;
			break;
		}

		// Updating delta(t), the desired aggressiveness, and r(t+1): Eq. (7)
		// lambda_r and lambda_a are the relative & absolute inc/dec in r_shout
		double delta = (1 + sign * lambdaR) * rShout + sign * lambdaA;	
		aggression = aggression + betaR * (delta - aggression);
		aggressions.setValue(getPosition(), type, aggression);
	}

	/**
	 * Given a price, returns the level of aggression that would result in
	 * the agent submitting a bid/ask at the last transaction price.
	 * 
	 * XXX Is there a better way to handle when theta is 0?
	 * 
	 * @param limitPrice
	 * @param lastPrice
	 * @param equilibriumPrice
	 * @return rShout
	 */
	protected double computeRShout(Price limitPrice, Price lastPrice, 
			Price equilibriumPrice) {
		double tau = lastPrice.intValue();
		double limit = limitPrice.intValue();
		double eqPrice = equilibriumPrice.intValue();
		double rShout = 0;

		// handle theta = 0, can't divide by 0
		if (theta == 0) return rShout;

		switch (type) {
		case BUY:
			// Intramarginal
			if (limitPrice.greaterThan(equilibriumPrice)) {			// r = 0
				if (lastPrice.equals(equilibriumPrice))
					return 0;

				if (lastPrice.lessThan(equilibriumPrice)) {			// r < 0
					rShout = -Math.log( ((1 - tau / eqPrice)
							* (Math.exp(theta) - 1)) + 1) / theta;
				}

				else {
					rShout = Math.log( ((tau - eqPrice)				// r > 0
							* (Math.exp(theta) - 1)
							/ (limit - eqPrice)) + 1) / theta;
				}
			}
			// Extramarginal
			else {

				if (lastPrice.lessThan(limitPrice)) {				// r < 0
					rShout = -Math.log( ((1 - tau / limit)
							* (Math.exp(theta) - 1)) + 1) / theta;
				}

				else {												// r = 0
					rShout = 0;
				}
			}
			break;

		case SELL:
			// Intramarginal
			if (limitPrice.lessThan(equilibriumPrice)) {			// r = 0
				if (lastPrice.equals(equilibriumPrice))
					return 0;

				if (lastPrice.greaterThan(equilibriumPrice)) {		// r < 0
					rShout = -Math.log( ((tau - eqPrice) 
							* (Math.exp(theta) - 1)
							/ (Price.INF.intValue() - eqPrice)) + 1)
							/ theta;
				}

				else {												// r > 0
					rShout = Math.log( ((1 - (tau - limit) / (eqPrice - limit))
							* (Math.exp(theta) - 1)) + 1) / theta;
				}
			}
			// Extramarginal
			else {

				if (lastPrice.greaterThan(limitPrice)) {			// r < 0
					rShout = -Math.log( ((tau - limit) * (Math.exp(theta) - 1)
							/ (Price.INF.intValue() - limit)) + 1)
							/ theta;
				}

				else {												// r = 0
					rShout = 0;
				}
			}
			break;
		}

		if ((new Double(rShout)).equals(Double.NaN)
				|| (new Double(rShout)).equals(Double.NEGATIVE_INFINITY)
				|| (new Double(rShout)).equals(Double.POSITIVE_INFINITY))
			return 0;
		return rShout;
	}


	/**
	 * Long-term learning. Section 4.3.2, Eq (8, 9) Vytelingum et al
	 * 
	 * @param equilibriumPrice
	 * @param transactions
	 */
	protected void updateTheta(Price equilibriumPrice,
			List<Transaction> transactions) {

		// Error Checking, must have some transactions
		if (equilibriumPrice == null || transactions.isEmpty()) return;

		ArrayList<Transaction> transList = new ArrayList<Transaction>(transactions);

		// Determining alpha, Eq (8)
		double alpha = 0;
		int num = 0;
		for (int i = transactions.size()-1; i >= 0 && num < numHistorical; i--) {
			Price price = transList.get(i).getPrice();
			if (price != null) {
				alpha += Math.pow(price.intValue() - equilibriumPrice.intValue(), 2);
				num++;
			}
		}
		alpha = Math.sqrt(alpha / num) / equilibriumPrice.intValue();

		// Determining alphaBar, updating range, Eq (9)
		alphaMin = Math.min(alpha, alphaMin);
		alphaMax = Math.max(alpha, alphaMax);
		double alphaBar = (alpha - alphaMin) / (alphaMax - alphaMin);
		if (alphaMin == alphaMax) alphaBar = alpha - alphaMin;
		// FIXME Erik: is this the best way to handle when alphaMax = alphaMin?
		// Will primarily only happen for the first update

		// Determining thetaStar, Eq (9)
		double thetaStar = (thetaMax - thetaMin)
				* (1 - alphaBar * Math.exp(gamma * (alphaBar - 1)))
				+ thetaMin;

		// If number of transactions so far < numHistorical, keep theta fixed
		if (transactions.size() >= numHistorical)
			theta = theta + betaT * (thetaStar - theta);	// Eq (8)
	}


	/**
	 * Computes weighted moving average. Truncates if fewer than required 
	 * number of transactions available.
	 * 
	 * FIXME this seems to give more weight to older transactions, which seems wrong.
	 * FIXME there should probably just be a weighted moving average function.
	 * 
	 * Section 4.1, Eq. (2) in Vytelingum et al
	 */
	protected Price estimateEquilibrium(List<Transaction> transactions) {
		if (transactions == null) return null; // TODO This shouldn't be necessary
		if (transactions.size() == 0) return null; //error checking

		// FIXME This is almost certainly wrong, but is required to keep behavior consistent
		transactions = Lists.reverse(transactions);
		
		// Computing the weights for the moving average
		// normalize by dividing by sumWeights
		int numTrans = Math.min(numHistorical, transactions.size());
		double[] weights = new double[numTrans];
		weights[0] = 1; // just for computation purposes, will be normalized later
		double sumWeights = weights[0];
		for (int i = 1; i < numTrans; i++) {
			weights[i] = rho * weights[i-1];
			sumWeights += weights[i];
		}

		// Computing the moving Average
		double total = 0;
		for(int i = 0; i < numTrans; i++) {
			total += transactions.get(i).getPrice().intValue() * weights[i] / sumWeights; 
		}
		return Price.of(total);
	}
	
}
