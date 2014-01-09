package entity.agent;

import static logger.Logger.log;
import static logger.Logger.format;
import static logger.Logger.Level.INFO;
import static com.google.common.base.Preconditions.checkArgument;
import static fourheap.Order.OrderType.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.collect.ImmutableList.Builder;

import systemmanager.Keys;
import utils.Pair;
import activity.Activity;
import activity.SubmitNMSOrder;
import data.EntityProperties;
import data.FundamentalValue;
import entity.infoproc.SIP;
import entity.market.Market;
import entity.market.Price;
import entity.market.Quote;
import entity.market.Transaction;
import event.TimeStamp;
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
	
	// Agent market variables
//	private boolean isBuyer; // randomly assigned at initialization
	private OrderType type; // randomly assigned at initialization

	//Agent strategy variables
	// Based on Vytelingum's sensitivity analysis, eta and N are most important
	private double lambda_r; // coefficient of relative perturbation of delta, (0,1)?
	private double lambda_a; // coefficient of absolute perturbation of delta (0,1)?
	private double gamma; // long term learning variable, set to 2
	private double beta_r; // learning coefficient for r (short term) (0,1)
	private double beta_t; // learning coefficient for theta (long term) (0,1)
	private int eta; // price determination coefficient. [1,inf)
	private int historical; // number of historical prices to look at, N > 0
	
	//Agent parameters
	private double rho;		// factor for weighted moving average
	private Aggression aggressions;
	private double aggression; // current short term learning variable, r in [-1,1]
	private double theta; // long term learning variable
	private double thetaMax; // max possible value for theta
	private double thetaMin; // min possible value for theta
	private double alphaMax; // max experienced value for alpha (for theta, not PV)
	private double alphaMin; // min experienced value for alpha


	public AAAgent(TimeStamp arrivalTime, FundamentalValue fundamental, SIP sip, 
			Market market, Random rand, double reentryRate, double pvVar,
			int tickSize, int maxAbsPosition, int bidRangeMin, int bidRangeMax,
			int windowLength, double aggression, double theta, double thetaMin, 
			double thetaMax, int historical, int eta, double lambdaR, double lambdaA,
			double gamma, double betaR, double betaT) {
		super(arrivalTime, fundamental, sip, market, rand, reentryRate, 
				new PrivateValue(maxAbsPosition, pvVar, rand), tickSize, 
				bidRangeMin, bidRangeMax, windowLength);
		//Initializing parameters
		this.aggression = aggression;
		this.theta = theta;
		this.thetaMin = thetaMin;
		this.thetaMax = thetaMax;
		this.alphaMin = Price.INF.intValue();
		this.alphaMax = -1;
		
		this.aggressions = new Aggression(privateValue.getMaxAbsPosition(), aggression);
		
		//Initializing strategy variables
		this.historical = historical;
		this.lambda_a = lambdaA;
		this.lambda_r = lambdaR;
		this.gamma = gamma;
		this.eta = eta;
		this.beta_r = betaR;
		this.beta_t = betaT;
		//		beta_r = this.getUniformRV(0.2, 0.6);
		//		beta_t = this.getUniformRV(0.2, 0.6);
		
		this.rho = 0.9; // from paper, to emphasize converging pattern
	}
	
	public AAAgent(TimeStamp arrivalTime, FundamentalValue fundamental, SIP sip, 
			Market market, Random rand, EntityProperties props) {
		this(arrivalTime, fundamental, sip, market, rand,
				props.getAsDouble(Keys.REENTRY_RATE, 0.005), 
				props.getAsDouble(Keys.PRIVATE_VALUE_VAR, 100000000),
				props.getAsInt(Keys.TICK_SIZE, 1),
				props.getAsInt(Keys.MAX_QUANTITY, 10),
				props.getAsInt(Keys.BID_RANGE_MIN, 0),
				props.getAsInt(Keys.BID_RANGE_MAX, 5000),
				props.getAsInt(Keys.WINDOW_LENGTH, 5000),
				props.getAsDouble(Keys.AGGRESSION, 0),
				props.getAsDouble(Keys.THETA, -8),
				props.getAsDouble(Keys.THETA_MIN, -8),
				props.getAsDouble(Keys.THETA_MAX, 2),
				props.getAsInt(Keys.HISTORICAL, 5),
				props.getAsInt(Keys.ETA, 3), 
				props.getAsDouble(Keys.LAMBDA_R, 0.05),
				props.getAsDouble(Keys.LAMBDA_A, 0.02),
				props.getAsDouble(Keys.GAMMA, 2), 
				props.getAsDouble(Keys.BETA_R, .4),
				props.getAsDouble(Keys.BETA_T, .4));
		
		//Determining whether agent is a buyer or a seller
//		this.isBuyer = props.getAsBoolean(Keys.BUYER_STATUS, rand.nextBoolean());		//TODO
	}

	@Override
	public Collection<Activity> agentStrategy(TimeStamp currentTime) {
		Builder<Activity> acts = ImmutableList.<Activity> builder().addAll(
				super.agentStrategy(currentTime));
		
		StringBuilder sb = new StringBuilder().append(this).append(" ");
		sb.append(getName()).append("::agentStrategy: ");

		this.withdrawAllOrders(); // FIXME will this withdraw & update for future price computation?
		
		// Determining Quantity
		type = rand.nextBoolean() ? BUY : SELL;
		aggression = aggressions.getValue(positionBalance, type);

		// Updating Price Limit (valuation of security)
		Price limitPrice = getValuation(type, currentTime);
		Price lastTransactionPrice = new Price(-1);
		Price equilibriumPrice = new Price(-1);
		
		ImmutableList<Transaction> trans = this.getWindowTransactions(currentTime);
		if (!trans.isEmpty())
			lastTransactionPrice = trans.get(trans.size()-1).getPrice();
		
		// Estimate equilibrium price using weighted moving average
		MovingAverage avg = estimateEquilibrium(trans);
		equilibriumPrice = new Price(avg.getMovingAverage());
		double mvgAvgNum = avg.getMovingAverageNum();
		log(INFO, sb.append("estimateEquilibrium: price=").append(equilibriumPrice));
		
		// Aggressiveness layer
		// Determine the target price tau using current r & theta
		Price targetPrice = determineTargetPrice(limitPrice, equilibriumPrice);
		log(INFO, sb.append("determineTargetPrice: target=").append(targetPrice));
		
		// Adaptive layer
		// Update the short term learning variable (aggressiveness r)
		double oldAggression = aggression;
		updateAggression(limitPrice, targetPrice, equilibriumPrice, lastTransactionPrice);
		log(INFO, sb.append("updateAggression: lastPrice=").append(lastTransactionPrice) 
					.append(", r=").append(format(oldAggression))
					.append("-->r_new=").append(format(aggression)));
		// Update long term learning variable (adaptiveness theta)
		double oldTheta = theta;
		updateTheta(equilibriumPrice, mvgAvgNum, trans);
		log(INFO, sb.append("updateTheta: theta=").append(format(oldTheta)) 
					.append("-->theta_new=").append(format(theta)));
		
		// Asserting that aggression updated correctly
		if (type.equals(BUY)) {
			if(lastTransactionPrice.compareTo(targetPrice) < 0) 
				assert(oldAggression >= aggression); // less aggressive
			else
				assert(oldAggression <= aggression); // more aggressive
		} else {
			if(lastTransactionPrice.compareTo(targetPrice) > 0)
				assert(oldAggression >= aggression); // less aggressive
			else
				assert(oldAggression <= aggression); // more aggressive
		}

		// Bidding Layer
		acts.addAll(biddingLayer(limitPrice, targetPrice, 1, currentTime));
		
		return acts.build();
	}
	
	/**
	 * Section 4.2, Eq (3, 4, 5, 6) in Vytelingum et al
	 * 
	 * @param limitPrice
	 * @param equilibriumPrice
	 * @return price target according to the AA Strategy
	 */
	private Price determineTargetPrice(Price limitPrice, Price equilibriumPrice) {
		//Error Checking - cannot compute if movingAverage is invalid
		// NOTE: equilibrium price should never be -1 because the target
		// price will only be determined if there has been 1+ transactions
		if (equilibriumPrice == null) return new Price(-1);
		
		Price tau; // target price
		double eqPrice = equilibriumPrice.intValue();
		double limit = limitPrice.intValue();
		
		// Buyers
		if (type.equals(BUY)) {
			// Intramarginal - price limit > p* (competitive equilibrium price)
			if (limitPrice.greaterThan(equilibriumPrice)) {
				if (aggression == -1) { // passive
					tau = Price.ZERO;
				} else if (aggression < 0) {
					tau = new Price(eqPrice * (1 - tauChange(-aggression)));	// Eq (3)
				} else if (aggression == 0) {	//active
					tau = equilibriumPrice;
				} else if (aggression < 1) {	// aggressive
					tau = new Price(eqPrice + (limit - eqPrice) 
							* tauChange(aggression));	// Eq (3)
				} else { // completely aggressive, submits at limit
					tau = limitPrice;
				}
			}
			// Extramarginal - price limit is less than p*
			else {
				if (aggression == -1) {	// passive
					tau = Price.ZERO;
				} else if (aggression < 0) {
					tau = new Price(limit * (1 - tauChange(-aggression)));	// Eq (5)
				} else {
					// aggression clipped at 0
					tau = limitPrice;
				}
			}
		}
		// Sellers
		else {
			// Intramarginal - cost is less than p*
			if (limitPrice.lessThan(equilibriumPrice)) {
				if(aggression == -1) {	// passive
					tau = Price.INF;
				} else if (aggression < 0) {
					tau = new Price(eqPrice + (Price.INF.intValue() - eqPrice) 
							* tauChange(-aggression)); // Eq (4)
				} else if (aggression == 0) {	//active
					tau = equilibriumPrice;
				} else if (aggression < 1){	// aggressive
					tau = new Price(limit + (eqPrice - limit) 
							* (1 - tauChange(aggression)));	// Eq (4)
				} else { // completely aggressive
					tau = limitPrice;
				}
			}
			// Extramarginal - cost is greater than p*
			else {
				if (aggression == -1) { // passive
					tau = Price.INF;
				} else if (aggression < 0) {
					tau = new Price(limit + (Price.INF.intValue() - limit) *
							tauChange(-aggression));	// Eq (6)
				} else { // aggressive
					tau = limitPrice;
				}
			}
		}
		return tau;
	}
	
	
	/**
	 * Bidding layer. Section 4.4, Eq (10, 11)
	 * If buyer's (seller's) limit price is lower (higher) than the current bid/ask, cannot
	 * submit any bid/ask and waits until next round.
	 * 
	 * @param limitPrice
	 * @param targetPrice
	 * @param quantity
	 * @param currentTime
	 * @return
	 */
	private Iterable<? extends Activity> biddingLayer(Price limitPrice, 
			Price targetPrice, int quantity, TimeStamp currentTime) {
		StringBuilder sb = new StringBuilder().append(this).append(" ");
		sb.append(getName()).append("::biddingLayer: ");
		
		// Determining the offer price to (possibly) submit
		// BestBidAsk lastNBBOQuote = sip.getNBBO(); // XXX use NBBO?
		Quote quote = marketQuoteProcessor.getQuote();
		Price bid = quote.getBidPrice();
		Price ask = quote.getAskPrice();
		
		// if no bid or no ask, submit ZI strategy bid
		if (bid == null || ask == null) {
			log(INFO, sb.append("Bid/Ask undefined."));
			return this.executeZIStrategy(type, quantity, currentTime);
		}
		
		// If best offer is outside of limit price, no bid is submitted
		if ((type.equals(BUY) && limitPrice.lessThanEqual(bid))
			|| (type.equals(SELL) && limitPrice.greaterThan(ask))) {
			log(INFO, sb.append("Best price is outside of limit price: ")
					.append(limitPrice).append("; no submission"));
			return Collections.emptyList();
		}
		
		// Can only submit offer if the offer would not cause position
		// balance to exceed the agent's maximum position
		int newPosBal = positionBalance + quantity;
		if (newPosBal < -privateValue.getMaxAbsPosition() 
				|| newPosBal > privateValue.getMaxAbsPosition() ) {
			log(INFO, sb.append("New order would exceed max position ")
					.append(privateValue.getMaxAbsPosition())
					.append("; no submission"));
			return Collections.emptyList();
		}

		// Pricing - verifying targetPrice
		// when not first round, limit should never be less (greater) than the
		// target price for buyers (sellers)
		Price price;
		if (targetPrice != null) {
			if ((type.equals(BUY) && limitPrice.lessThan(targetPrice)) 
					|| (type.equals(SELL) && limitPrice.greaterThan(targetPrice)))
				targetPrice = limitPrice;
		}

		// See Eq 10 and 11 in section 4.4 - bidding layer
		if (targetPrice == null) {
			// first arrival, where target price undetermined
			if (type.equals(BUY)) {
				Price askCeil = new Price((int) (ask.intValue() * 
									(1 + lambda_r) + lambda_a));
				Price offset = new Price((pcomp.min(askCeil, limitPrice).intValue() - bid.intValue())
						* (1.0/eta));
				price = new Price(bid.intValue() + offset.intValue());
				price = pcomp.min(price, limitPrice);
			} else {
				Price bidFloor = new Price((bid.intValue() * 
									(1 - lambda_r) - lambda_a));
				Price offset = new Price(ask.intValue() - 
						(pcomp.max(bidFloor, limitPrice)).intValue() * (1.0/eta));
				price = new Price(ask.intValue() - offset.intValue());
				price = pcomp.max(price, limitPrice);				
			}
		} else {
			// not first entry into market
			if (type.equals(BUY)) {
				Price offset = new Price((targetPrice.intValue() - bid.intValue()) * (1.0/eta));
				price = new Price(bid.intValue() + offset.intValue());
				price = pcomp.min(price, limitPrice);
			} else {
				Price offset = new Price((ask.intValue() - targetPrice.intValue()) * (1.0/eta));
				price = new Price(ask.intValue() - offset.intValue());
				price = pcomp.max(price, limitPrice);
			}
		}
		
		// Submitting a bid
		if (type.equals(BUY)) { // Buyer
			// if bestAsk < targetPrice, accept bestAsk
			// else submit bid given by EQ 10/11
			Price submitPrice = ask.lessThanEqual(targetPrice) ? ask : price;	
			return ImmutableList.of(new SubmitNMSOrder(this, primaryMarket, type, 
					submitPrice, 1, currentTime));
			
		} else { // Seller
			// If outstanding bid >= target price, submit ask at bid price
			// else submit bid given by EQ 10/11
			Price submitPrice = bid.greaterThanEqual(targetPrice) ? bid : price;
			return ImmutableList.of(new SubmitNMSOrder(this, primaryMarket, type, 
					submitPrice, 1, currentTime));
		}
	}	// end of bidding layer
	

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
	private void updateAggression(Price limit, Price tau, 
			Price equilibriumPrice,	Price lastPrice) {
		
		if (equilibriumPrice == null || lastPrice == null)
			return; // If no transactions yet, cannot update

		// Determining r_shout, the level of aggression that would form a price
		// equal to most recent transaction price
		double rShout = computeRshout(limit, lastPrice, equilibriumPrice);

		// Determining whether agent must be more or less aggressive
		// Differs from paper b/c we do not take into account the most
		// recent bid/ask submitted, only transactions
		int sign = 1;
		// Buyers
		if (type.equals(BUY)) {
			// if target price is greater, agent should be less aggressive
			if (tau.compareTo(lastPrice) > 0)
				sign = -1;
			// if transaction price greater or equal, agent should be more aggressive
			else
				sign = 1;
		}
		// Sellers
		else {
			// if target price is less, agent should be less aggressive
			if (tau.compareTo(lastPrice) < 0)
				sign = -1;
			// if target price is greater or equal, agent should be more aggressive
			else
				sign = 1;
		}

		// Updating delta(t), the desired aggressiveness, and r(t+1): Eq. (7)
		// lambda_r and lambda_a are the relative & absolute inc/dec in r_shout
		double delta = (1 + sign * lambda_r) * rShout + sign * lambda_a;	
		aggression = aggression + beta_r * (delta - aggression);
		aggressions.setValue(positionBalance, type, aggression);
	}
	
	/**
	 * Given a price, returns the level of aggression that would result in
	 * the agent submitting a bid/ask at the last transaction price.
	 * 
	 * @param limitPrice
	 * @param lastPrice
	 * @param equilibriumPrice
	 * @return rShout
	 */
	private double computeRshout(Price limitPrice, Price lastPrice, Price equilibriumPrice) {
		double tau = lastPrice.intValue();
		double limit = limitPrice.intValue();
		double eqPrice = equilibriumPrice.intValue();
		double rShout = 0;

		// handle theta = 0, can't divide by 0
		if (theta == 0) return rShout;

		// Buyers
		if (type.equals(BUY)) {
			// Intramarginal
			if (limitPrice.greaterThan(equilibriumPrice)) {
				if (lastPrice.equals(equilibriumPrice))
					return 0;
				// r < 0
				if (lastPrice.lessThan(equilibriumPrice)) {
					rShout = -Math.log( ((1 - tau / eqPrice)
							* (Math.exp(theta) - 1)) + 1) / theta;
				}
				// r > 0
				else {
					rShout = Math.log( ((tau - eqPrice)
							* (Math.exp(theta) - 1)
							/ (limit - eqPrice)) + 1) / theta;
				}
			}
			// Extramarginal
			else {
				// r < 0
				if (lastPrice.lessThan(limitPrice)) {
					rShout = -Math.log( ((1 - tau / limit)
							* (Math.exp(theta) - 1)) + 1) / theta;
				}
				// r clipped at 0
				else {
					rShout = 0;
				}
			}
		}

		// Sellers
		else {
			// Intramarginal
			if (limitPrice.lessThan(equilibriumPrice)) {
				if (lastPrice.equals(equilibriumPrice))
					return 0;
				// r < 0
				if (lastPrice.greaterThan(equilibriumPrice)) {
					// if(theta == 0) return -0.5; //handling NaN exception
					rShout = -Math.log( ((tau - eqPrice) 
								* (Math.exp(theta) - 1)
								/ (Price.INF.intValue() - eqPrice)) + 1)
								/ theta;
				}
				// r > 0
				else {
					// if(theta == 0) return 0.5; //handling NaN exception
					rShout = Math.log( ((1 - (tau - limit) / (eqPrice - limit))
								* (Math.exp(theta) - 1)) + 1) / theta;
				}
			}
			// Extramarginal
			else {
				// r < 0
				if (lastPrice.greaterThan(limitPrice)) {
					rShout = -Math.log( ((tau - limit) * (Math.exp(theta) - 1)
								/ (Price.INF.intValue() - limit)) + 1)
								/ theta;
				}
				// r clipped at 0
				else {
					rShout = 0;
				}
			}
		}
		if (rShout == Double.NaN 
				|| rShout == Double.NEGATIVE_INFINITY
				|| rShout == Double.POSITIVE_INFINITY)
			return 0;
		return rShout;
	}
	
	
	/**
	 * Long-term learning. Section 4.3.2, Eq (8, 9) Vytelingum et al
	 * 
		protected final int offset;
	 * @param equilibriumPrice
	 * @param numTrans
	 * @param transactions
	 */
	private void updateTheta(Price equilibriumPrice, double numTrans, 
			ImmutableList<Transaction> transactions) {
		//Error Checking, must have some transactions
		if (equilibriumPrice == null) return;

		//Converting trans to an ArrayList
		ArrayList<Transaction> transList = new ArrayList<Transaction>(transactions);
		
		// Determining alpha, Eq (8)
		double alpha = 0;
		int num = 0;
		for (int i = transactions.size() - 1; i >= 0 && num < historical; i--) {
			Price price = transList.get(i).getPrice();
			if (price != null) {
				alpha += Math.pow(price.intValue() - equilibriumPrice.intValue(), 2);
				num++;
			}
		}
		alpha = (1 / equilibriumPrice.intValue()) * Math.sqrt(alpha / num);

		// Determining alphaBar, updating range, Eq (9)
		alphaMin = Math.min(alpha, alphaMin);
		alphaMax = Math.max(alpha, alphaMax);
		double alphaBar = (alpha - alphaMin) / (alphaMax - alphaMin);
		if (alphaMin == alphaMax)
			alphaBar = alpha - alphaMin;
		// TODO though maybe doesn't matter because theta won't be updated for awhile...

		// Determining theta star Eq (9)
		double thetaStar = (thetaMax - thetaMin)
				* (1 - alphaBar * Math.exp(gamma * (alphaBar - 1)))
				+ thetaMin;

		// If number of transactions < historical, do not update theta
		if (numTrans < historical)
			return;
		theta = theta + beta_t * (thetaStar - theta);	// Eq (8)
	}

	
	/**
	 * Computes weighted moving average. Truncates if fewer than required transactions
	 * available.
	 * 
	 * Section 4.1, Eq. (2) in Vytelingum et al
	 * 
	 * @param transactions
	 * @return
	 */
	protected MovingAverage estimateEquilibrium(ImmutableList<Transaction> transactions) {
		if (transactions == null) return new MovingAverage(-1, 0);
		if (transactions.size() == 0) return new MovingAverage(-1, 0); //error checking
		
		// Computing the weights for the moving average
		// normalize by dividing by sumWeights
		int numTrans = Math.min(historical, transactions.size());
		double[] weights = new double[numTrans];
		weights[0] = 1; // just for computation purposes, will be normalized later
		double sumWeights = weights[0];
		for (int i = 1; i < numTrans; i++) {
			weights[i] = rho * weights[i-1];
			sumWeights += weights[i];
		}
		
		//Computing the moving Average
		double total = 0;
		for(int i = 0; i < numTrans; i++) {
			total += transactions.get(i).getPrice().intValue() * weights[i] / sumWeights; 
		}
		// double movingAverage = total / numTrans; // this is a bug in the paper
		// return new MovingAverage(movingAverage, numTrans);
		return new MovingAverage(total, numTrans);
	}

	double getAggression() {
		return aggression;
	}

	void setAggression(double in) {
		aggression = in;
	}

	double getAdaptiveness() {
		return theta;
	}

	void setAdaptivness(double in) {
		theta = in;
	}
	
	protected static class MovingAverage extends Pair<Double, Double> {
		protected MovingAverage(double left, double right) {
			super(left, right);
		}
		protected double getMovingAverage() { return left; }
		protected double getMovingAverageNum() { return right; }
	}

	
	/**
	 * Holds aggression values for AA agents.
	 *
	 */
	protected static class Aggression implements QuantityIndexedArray<Double> {

		private static final long serialVersionUID = -8437580530274339226L;

		protected final int offset;
		protected List<Double> values;
		
		public Aggression() {
			this.offset = 0;
			this.values = Collections.emptyList();
		}
		
		public Aggression(int maxPosition, double initialValue) {
			checkArgument(maxPosition > 0, "Max Position must be positive");
			
			// Identical to legacy generation in final output
			this.offset = maxPosition;
			this.values = Lists.newArrayList();
			double[] values = new double[maxPosition * 2];
			Arrays.fill(values, initialValue);
			for (double value : values)
				this.values.add(new Double(value));
		}

		@Override
		public int getMaxAbsPosition() {
			return offset;
		}

		@Override
		public Double getValue(int currentPosition, OrderType type) {
			switch (type) {
			case BUY:
				if (currentPosition + offset <= values.size() - 1 &&
						currentPosition + offset >= 0)
					return values.get(currentPosition + offset);
				break;
			case SELL:
				if (currentPosition + offset - 1 <= values.size() - 1 && 
						currentPosition + offset - 1 >= 0)
					return values.get(currentPosition + offset - 1);
				break;
			}
			return 0.0;
		}
		
		/**
		 * @param currentPosition
		 * @param type
		 * @param value
		 */
		public void setValue(int currentPosition, OrderType type,
				double value) {
			switch (type) {
			case BUY:
				if (currentPosition + offset <= values.size() - 1 &&
						currentPosition + offset >= 0)
					values.set(currentPosition + offset, value);
				break;
			case SELL:
				if (currentPosition + offset - 1 <= values.size() - 1 && 
						currentPosition + offset - 1 >= 0)
					values.set(currentPosition + offset - 1, value);
				break;
			}
		}

		@Override
		public Double getValueFromQuantity(int currentPosition, int quantity,
				OrderType type) {
			checkArgument(quantity > 0, "Quantity must be positive");
			// TODO Auto-generated method stub
			return null;
		}
	}
}
