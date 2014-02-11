package entity.agent;

import java.util.List;
import java.util.Random;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;

import systemmanager.Keys;
import static fourheap.Order.OrderType.BUY;
import static fourheap.Order.OrderType.SELL;
import static logger.Log.log;
import static logger.Log.Level.INFO;
import utils.Rands;
import activity.Activity;
import activity.SubmitNMSOrder;
import data.EntityProperties;
import data.FundamentalValue;
import entity.infoproc.SIP;
import entity.market.Market;
import entity.market.Price;
import entity.market.Transaction;
import event.TimeStamp;
import fourheap.Order.OrderType;

/**
 * ZIPAGENT
 * 
 * Zero-Intelligence Plus Agent.
 *
 * Based on Cliff & Bruten, "Zero is Not Enough: On the lower limit of agent
 * intelligence for continuous double auction markets," HP Laboratories 
 * technical report, HPL-97-141, 1997.
 * 
 * Since each reentry behaves like a "new" agent, lastOrderPrice does not need
 * to be an array. Instead, it's reset at the beginning of each AgentStrategy
 * execution.
 * 
 * @author ewah
 */
public class ZIPAgent extends WindowAgent {

	private static final long serialVersionUID = 8138883791556301413L;

	protected boolean withdrawOrders;
	
	protected OrderType type;				// buy or sell
	protected Margin margin;				// one for each position, mu in Cliff1997
	protected Price limitPrice;				// lambda in Cliff1997
	protected double momentumChange;		// momentum update, in Eq (15) of Cliff1997
	protected double beta;					// learning rate, beta in Cliff1997
	protected double gamma;					// momentum coefficient, gamma in Cliff1997
	protected Price lastOrderPrice;			// for last order price, p_i in Cliff1997

	// Strategy parameters (tunable)
	protected final double rangeCoeffA;	// range for A, coefficient of absolute perturbation
	protected final double rangeCoeffR;	// range for R, coefficient of relative perturbation

	public ZIPAgent(TimeStamp arrivalTime, FundamentalValue fundamental, SIP sip, 
			Market market, Random rand, double reentryRate, double pvVar, 
			int tickSize, int maxAbsPosition, int bidRangeMin, int bidRangeMax, 
			boolean withdrawOrders, int windowLength, double marginMin, double marginMax, 
			double gammaMin, double gammaMax, double betaMin, double betaMax, 
			double rangeCoeffA, double rangeCoeffR) {
		super(arrivalTime, fundamental, sip, market, rand, reentryRate,
				new PrivateValue(maxAbsPosition, pvVar, rand), tickSize,
				bidRangeMin, bidRangeMax, windowLength);

		checkArgument(rangeCoeffA > 0, "Coefficient A's range must be positive");
		checkArgument(rangeCoeffR > 0, "Coefficient A's range must be positive");
		checkArgument(betaMin >= 0, "Min beta must be positive");
		checkArgument(betaMax <= 1, "Max beta must be less than 1");
		checkArgument(betaMax >= betaMin, "Max beta must be greater than min beta");
		checkArgument(gammaMin >= 0, "Min gamma must be positive");
		checkArgument(gammaMax <= 1, "Max gamma must be less than 1");
		checkArgument(gammaMax >= gammaMin, "Max gamma must be greater than min gamma");
		checkArgument(marginMin >= 0, "Min margin must be positive");
		checkArgument(marginMax >= marginMin, "Max (initial) margin must be greater than min margin");
		
		this.rangeCoeffA = rangeCoeffA;
		this.rangeCoeffR = rangeCoeffR;

		// Initializing variables
		momentumChange = 0;	// initialized to 0
		lastOrderPrice = null;
		limitPrice = null;
		beta = Rands.nextUniform(rand, betaMin, betaMax);
		gamma = Rands.nextUniform(rand, gammaMin, gammaMax);
		margin = new Margin(maxAbsPosition, rand, marginMin, marginMax);
	}

	public ZIPAgent(TimeStamp arrivalTime, FundamentalValue fundamental, SIP sip, Market market,
			Random rand, EntityProperties props) {
		this(arrivalTime, fundamental, sip, market, rand,
				props.getAsDouble(Keys.REENTRY_RATE, 0.005), 
				props.getAsDouble(Keys.PRIVATE_VALUE_VAR, 100000000),
				props.getAsInt(Keys.TICK_SIZE, 1),
				props.getAsInt(Keys.MAX_QUANTITY, 10),
				props.getAsInt(Keys.BID_RANGE_MIN, 0),
				props.getAsInt(Keys.BID_RANGE_MAX, 5000), 
				props.getAsBoolean(Keys.WITHDRAW_ORDERS, true),
				props.getAsInt(Keys.WINDOW_LENGTH, 5000),
				props.getAsDouble(Keys.MARGIN_MIN, 0.05),
				props.getAsDouble(Keys.MARGIN_MAX, 0.35),
				props.getAsDouble(Keys.GAMMA_MIN, 0),
				props.getAsDouble(Keys.GAMMA_MAX, 0.1),
				props.getAsDouble(Keys.BETA_MIN, 0.1),
				props.getAsDouble(Keys.BETA_MAX, 0.5),
				props.getAsDouble(Keys.COEFF_A, 0.05), 
				props.getAsDouble(Keys.COEFF_R, 0.05));
	}
	
	@Override
	public Iterable<? extends Activity> agentStrategy(TimeStamp currentTime) {
		Builder<Activity> acts = ImmutableList.<Activity> builder().addAll(
				super.agentStrategy(currentTime));

		// withdrawOrders
		if (withdrawOrders) acts.addAll(withdrawAllOrders(currentTime));

		// can buy and sell
		lastOrderPrice = null;
		type = rand.nextBoolean() ? BUY : SELL;
		
		double currentMargin = getCurrentMargin(positionBalance, type, currentTime);
		log.log(INFO, "%s::agentStrategy: initial mu=%.4f", this, currentMargin);

		// Check if there are any transactions in the market model yet
		List<Transaction> pastTransactions = getWindowTransactions(currentTime);
		if (!pastTransactions.isEmpty()) {
			// Determine limit price or lambda
			limitPrice = this.getLimitPrice(type, currentTime);

			log.log(INFO, "%s::agentStrategy: #new transactions=", this, pastTransactions.size());
			for (Transaction trans : pastTransactions) {
				// Update margin
				this.updateMargin(trans, currentTime);
				currentMargin = this.getCurrentMargin(positionBalance, type, currentTime);
				log.log(INFO, "%s::agentStrategy: mu=%.4f", this, currentMargin);
			}

			// Even if no new transactions this round, will still submit a new order
			Price orderPrice = pcomp.max(Price.ZERO, computeOrderPrice(currentMargin, currentTime));
			acts.add(new SubmitNMSOrder(this, primaryMarket, type, orderPrice, 1, currentTime));
			lastOrderPrice = orderPrice;

		} else {
			// zero transactions
			log.log(INFO, "%s::agentStrategy: No transactions!", this);
			acts.addAll(executeZIStrategy(type, 1, currentTime));
		}

		return acts.build();
	}

	
	/**
	 * @param positionBalance
	 * @param type
	 * @param currentTime
	 * @return
	 */
	protected double getCurrentMargin(int positionBalance, OrderType type, 
			TimeStamp currentTime) {
		
		double currentMargin = margin.getValue(positionBalance, type);

		// Ensures margin is within the correct range for buyer or seller
		double newMargin = currentMargin; 
		switch (type) {
		case BUY:
			// buyer margin constrained to in [-1, 0]
			newMargin = Math.min(0, Math.max(-1, currentMargin));
			break;
		case SELL:
			// seller margin constrained to in [0, inf)
			newMargin = Math.max(0, currentMargin);
			break;
		}
		log.log(INFO, "%s::agentStrategy: updated mu=%.4f-->mu=%.4f", this, currentMargin, newMargin);
		// set margin
		margin.setValue(positionBalance, type, newMargin);
		return newMargin;
	}
	
	/**
	 * Price calculation based on profit margin. Eq (9) in Cliff1997
	 * @param currentMargin
	 * @param currentTime
	 * 
	 * @return order price p_i
	 */
	public Price computeOrderPrice(double currentMargin, TimeStamp currentTime) {
		Price orderPrice = new Price(limitPrice.intValue() * (1 + currentMargin));
		log.log(INFO, "%s::computeOrderPrice: limitPrice=%s * (1+mu)=%.4f, returns %s",
				this, limitPrice, 1 + currentMargin, orderPrice);
		return orderPrice;
	}


	/**
	 * Update profit margin. Eq (12) in Cliff1997
	 * @param lastTrans
	 * @param currentTime
	 */
	public void updateMargin(Transaction lastTrans, TimeStamp currentTime) {
		log.log(INFO, "%s::updateMargin: lastTransPrice=%s", this, lastTrans.getPrice());
		this.updateMomentumChange(lastTrans, currentTime);
		if (limitPrice.intValue() > 0) {
			double newMargin = (lastOrderPrice.intValue() + momentumChange) 
					/ limitPrice.intValue() - 1;
			log.log(INFO, "%s::updateMargin: (lastOrderPrice + change)/limit - 1 = (%s + %f) / %s - 1 = new margin %.4f",
					this, lastOrderPrice, momentumChange, limitPrice, newMargin);
			
			margin.setValue(positionBalance, type, newMargin);
		} else {
			log.log(INFO, "%s::updateMargin: No update to margin as limit price is 0", this);
		}
	}


	/**
	 * General momentum update rule. Eq (15) in Cliff1997
	 * 
	 * @param lastTrans
	 * @param currentTime
	 */
	public void updateMomentumChange(Transaction lastTrans, TimeStamp currentTime) {
		double originalChange = momentumChange;
		double delta = this.computeDelta(lastTrans, currentTime);
		log.log(INFO, "%s::updateMomentumChange: original change=%.4f, delta=%.4f", this, momentumChange, delta);
		momentumChange = gamma * momentumChange + (1-gamma) * delta;

		if (originalChange != 0) {
			log.log(INFO, "%s::updateMomentumChange: new change=%.4f, using %.4f%%", this, momentumChange, 100*(momentumChange-originalChange)/originalChange);
		} else {
			log.log(INFO, "%s::updateMomentumChange: first update, change=%.4f", this, momentumChange);
		}
	}

	/**
	 * Compute Delta. Eq (13) in Cliff1997
	 * 
	 * @param lastTrans
	 * @param currentTime
	 * @return
	 */
	public double computeDelta(Transaction lastTrans, TimeStamp currentTime){
		Price tau = this.computeTargetPrice(lastTrans, currentTime);
		return beta * (tau.intValue() - lastOrderPrice.intValue());
	}


	/**
	 * Determine target price (tau). Eq (14) in Cliff1997
	 * 
	 * If wish to increase margin, then sellers will increase their target price
	 * while buyers will decrease.
	 * 
	 * If wish to decrease margin, then buyers will increase their target price
	 * while sellers will decrease.
	 * 
	 * @param lastTrans
	 * @param currentTime
	 * @return
	 */
	public Price computeTargetPrice(Transaction lastTrans, TimeStamp currentTime){
		Price lastTransPrice = lastTrans.getPrice();
		log.log(INFO, "%s::computeTargetPrice: lastPrice=%s, lastTransPrice=%s", this, lastOrderPrice, lastTransPrice);

		boolean increaseMargin = this.checkIncreaseMargin(lastTrans, currentTime);
		boolean increaseTargetPrice = (type.equals(SELL) && increaseMargin) ||
									  (type.equals(BUY) && !increaseMargin);
		
		double R = this.computeRCoefficient(increaseTargetPrice);
		double A = this.computeACoefficient(increaseTargetPrice);
		Price tau = new Price(R * lastTransPrice.intValue() + A);
		log.log(INFO, "%s::computeTargetPrice: Increase margine? %b, increase target? %b: R=%.4f, A=%.4f, targetPrice=%s",
				this, increaseMargin, increaseTargetPrice, R, A, tau);

		return tau;
	}

	/**
	 * Check if should increase margin. Conditions for increase:
	 * 
	 * Buyers:
	 * - last transaction at price q
	 * - any buyer for which order price p_i >= q should increase profit margin
	 *   - buyer could have bought for even lower price and still traded
	 * 
	 * Sellers:
	 * - last transaction at price q
	 * - any seller for which its order price p_i <= q should increase (return TRUE)
	 *   - seller could have asked for higher price and still traded
	 * 
	 * Note: order prices are assumed to have been submitted before any transactions
	 * in the given window, so the agent updates its order price based on the
	 * new transaction information.
	 *  
	 * @param lastTrans
	 * @param currentTime
	 * @return
	 */
	protected boolean checkIncreaseMargin(Transaction lastTrans, TimeStamp currentTime) {
		Price lastTransPrice = lastTrans.getPrice();
		
		// If no order price yet, compute based on current margin
		if (lastOrderPrice == null)
			lastOrderPrice = this.computeOrderPrice(margin.getValue(positionBalance, type), currentTime);
		
		switch (type) {
			case BUY:
				return lastTransPrice.lessThanEqual(lastOrderPrice);
			case SELL:
				return lastTransPrice.greaterThanEqual(lastOrderPrice);
		}
		return false;
	}

	/**
	 * Compute new coefficient of Relative Perturbation.
	 * 
	 * @param increaseTargetPrice
	 * @return
	 */
	public double computeRCoefficient(boolean increaseTargetPrice){
		if (increaseTargetPrice){
			return Rands.nextUniform(rand, 1, 1+rangeCoeffR);
		} else {
			return Rands.nextUniform(rand, 1-rangeCoeffR, 1);
		}
	}

	/**
	 * Compute new coefficient of Absolute Perturbation
	 * 
	 * @param increaseTargetPrice
	 * @return
	 */
	public double computeACoefficient(boolean increaseTargetPrice){
		if (increaseTargetPrice){
			return Rands.nextUniform(rand, 0, rangeCoeffA);
		} else {
			return Rands.nextUniform(rand, -rangeCoeffA, 0);
		}
	}

}
