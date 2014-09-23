package entity.agent;

import static com.google.common.base.Preconditions.checkArgument;
import static fourheap.Order.OrderType.BUY;
import static fourheap.Order.OrderType.SELL;
import static logger.Log.Level.INFO;

import java.util.List;
import java.util.Random;

import systemmanager.Keys;
import systemmanager.Simulation;
import utils.Maths;
import utils.Rands;
import data.Props;
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
	
	protected OrderType type;				// buy or sell
	protected Margin margin;				// one for each position, mu in Cliff1997
	protected Price limitPrice;				// lambda in Cliff1997 TODO Change to Optional<Price>
	protected double momentumChange;		// momentum update, in Eq (15) of Cliff1997
	protected double beta;					// learning rate, beta in Cliff1997
	protected double gamma;					// momentum coefficient, gamma in Cliff1997
	protected Price lastOrderPrice;			// for last order price, p_i in Cliff1997 TODO Change to Optional<Price>

	// Strategy parameters (tunable)
	protected final double rangeCoeffA;	// range for A, coefficient of absolute perturbation
	protected final double rangeCoeffR;	// range for R, coefficient of relative perturbation

	protected ZIPAgent(Simulation sim, TimeStamp arrivalTime, Market market, Random rand, Props props) {
		super(sim, arrivalTime, market, rand, props);

		int maxAbsPosition = props.getAsInt(Keys.MAX_QUANTITY);
		
		this.rangeCoeffA = props.getAsDouble(Keys.COEFF_A);
		this.rangeCoeffR = props.getAsDouble(Keys.COEFF_R);
		double marginMin = props.getAsDouble(Keys.MARGIN_MIN),
			marginMax = props.getAsDouble(Keys.MARGIN_MAX),
			gammaMin = props.getAsDouble(Keys.GAMMA_MIN),
			gammaMax = props.getAsDouble(Keys.GAMMA_MAX),
			betaMin = props.getAsDouble(Keys.BETA_MIN),
			betaMax = props.getAsDouble(Keys.BETA_MAX);
		
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
		
		// Initializing variables
		this.momentumChange = 0;	// initialized to 0
		this.lastOrderPrice = null;
		this.limitPrice = null;
		this.beta = Rands.nextUniform(rand, betaMin, betaMax);
		this.gamma = Rands.nextUniform(rand, gammaMin, gammaMax);
		this.margin = new Margin(maxAbsPosition, rand, marginMin, marginMax);
	}

	public static ZIPAgent create(Simulation sim, TimeStamp arrivalTime, Market market, Random rand, Props props) {
		return new ZIPAgent(sim, arrivalTime, market, rand, props);
	}
	
	@Override
	public void agentStrategy() {
		super.agentStrategy();
		
		// can buy and sell
		lastOrderPrice = null;
		type = rand.nextBoolean() ? BUY : SELL;
		
		double currentMargin = getCurrentMargin(positionBalance, type);
		log(INFO, "%s::agentStrategy: initial mu=%.4f", this, currentMargin);

		// Check if there are any transactions in the market model yet
		List<Transaction> pastTransactions = getWindowTransactions();
		if (!pastTransactions.isEmpty()) {
			// Determine limit price or lambda
			limitPrice = getLimitPrice(type);

			log(INFO, "%s::agentStrategy: #new transactions=", this, pastTransactions.size());
			for (Transaction trans : pastTransactions) {
				// Update margin
				updateMargin(trans);
				currentMargin = getCurrentMargin(positionBalance, type);
				log(INFO, "%s::agentStrategy: mu=%.4f", this, currentMargin);
			}

			// Even if no new transactions this round, will still submit a new order
			Price orderPrice = pcomp.max(Price.ZERO, computeOrderPrice(currentMargin));
			
			submitNMSOrder(type, orderPrice, 1);
			lastOrderPrice = orderPrice;

		} else {
			// zero transactions
			log(INFO, "%s::agentStrategy: No transactions!", this);
			executeZIStrategy(type, 1);
		}

	}

	
	/**
	 * @param aPositionBalance
	 * @param aType
	 * @param currentTime
	 * @return
	 */
	protected double getCurrentMargin(int aPositionBalance, OrderType aType) {
		
		double currentMargin = margin.getValue(aPositionBalance, aType);

		// Ensures margin is within the correct range for buyer or seller
		double newMargin = currentMargin; 
		switch (aType) {
		case BUY:
			// buyer margin constrained to in [-1, 0]
			newMargin = Maths.bound(currentMargin, -1, 0);
			break;
		case SELL:
			// seller margin constrained to in [0, inf)
			newMargin = Math.max(0, currentMargin);
			break;
		}
		log(INFO, "%s::agentStrategy: updated mu=%.4f-->mu=%.4f", this, currentMargin, newMargin);
		// set margin
		margin.setValue(aPositionBalance, aType, newMargin);
		return newMargin;
	}
	
	/**
	 * Price calculation based on profit margin. Eq (9) in Cliff1997
	 * @param currentMargin
	 * @param currentTime
	 * 
	 * @return order price p_i
	 */
	public Price computeOrderPrice(double currentMargin) {
		Price orderPrice = Price.of(limitPrice.intValue() * (1 + currentMargin));
		log(INFO, "%s::computeOrderPrice: limitPrice=%s * (1+mu)=%.4f, returns %s",
				this, limitPrice, 1 + currentMargin, orderPrice);
		return orderPrice;
	}


	/**
	 * Update profit margin. Eq (12) in Cliff1997
	 * @param lastTrans
	 * @param currentTime
	 */
	public void updateMargin(Transaction lastTrans) {
		log(INFO, "%s::updateMargin: lastTransPrice=%s", this, lastTrans.getPrice());
		updateMomentumChange(lastTrans);
		if (limitPrice.intValue() > 0) {
			double newMargin = (lastOrderPrice.intValue() + momentumChange) 
					/ limitPrice.intValue() - 1;
			log(INFO, "%s::updateMargin: (lastOrderPrice + change)/limit - 1 = (%s + %f) / %s - 1 = new margin %.4f",
					this, lastOrderPrice, momentumChange, limitPrice, newMargin);
			
			margin.setValue(positionBalance, type, newMargin);
		} else {
			log(INFO, "%s::updateMargin: No update to margin as limit price is 0", this);
		}
	}


	/**
	 * General momentum update rule. Eq (15) in Cliff1997
	 * 
	 * @param lastTrans
	 * @param currentTime
	 */
	public void updateMomentumChange(Transaction lastTrans) {
		double originalChange = momentumChange;
		double delta = computeDelta(lastTrans);
		log(INFO, "%s::updateMomentumChange: original change=%.4f, delta=%.4f", 
				this, momentumChange, delta);
		momentumChange = gamma * momentumChange + (1-gamma) * delta;

		if (originalChange != 0) {
			log(INFO, "%s::updateMomentumChange: new change=%.4f, using %.4f%%", 
					this, momentumChange, 100*(momentumChange-originalChange)/originalChange);
		} else {
			log(INFO, "%s::updateMomentumChange: first update, change=%.4f", 
					this, momentumChange);
		}
	}

	/**
	 * Compute Delta. Eq (13) in Cliff1997
	 * 
	 * @param lastTrans
	 * @param currentTime
	 * @return
	 */
	public double computeDelta(Transaction lastTrans){
		Price tau = computeTargetPrice(lastTrans);
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
	public Price computeTargetPrice(Transaction lastTrans){
		Price lastTransPrice = lastTrans.getPrice();
		log(INFO, "%s::computeTargetPrice: lastPrice=%s, lastTransPrice=%s", this, lastOrderPrice, lastTransPrice);

		boolean increaseMargin = checkIncreaseMargin(lastTrans);
		boolean increaseTargetPrice = type == BUY ^ increaseMargin;
		
		double R = this.computeRCoefficient(increaseTargetPrice);
		double A = this.computeACoefficient(increaseTargetPrice);
		Price tau = Price.of(R * lastTransPrice.intValue() + A);
		log(INFO, "%s::computeTargetPrice: Increase margin? %b, increase target? %b: R=%.4f, A=%.4f, targetPrice=%s",
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
	protected boolean checkIncreaseMargin(Transaction lastTrans) {
		Price lastTransPrice = lastTrans.getPrice();
		
		// If no order price yet, compute based on current margin
		if (lastOrderPrice == null)
			lastOrderPrice = computeOrderPrice(margin.getValue(positionBalance, type));
		
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
		} 
		
		return Rands.nextUniform(rand, 1-rangeCoeffR, 1);
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
		} 

		return Rands.nextUniform(rand, -rangeCoeffA, 0);
	}
}
