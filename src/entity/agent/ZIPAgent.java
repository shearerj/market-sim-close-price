package entity.agent;

import java.util.List;
import java.util.Random;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;

import systemmanager.Keys;
import static fourheap.Order.OrderType.BUY;
import static fourheap.Order.OrderType.SELL;
import static logger.Logger.log;
import static logger.Logger.format;
import static logger.Logger.Level.INFO;
import utils.Rands;
import activity.Activity;
import activity.SubmitNMSOrder;
import data.EntityProperties;
import data.FundamentalValue;
import entity.infoproc.BestBidAsk;
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
 * intelligence for continuous double auction markets," HP Laboratories technical report,
 * HPL-97-141, 1997.
 * 
 * @author ewah
 */
/**
 * @author ewah
 *
 */
public class ZIPAgent extends WindowAgent {

	private static final long serialVersionUID = 8138883791556301413L;

	private Margin margin;					// one for each position, mu in Cliff1997
	private Price limitPrice;				// lambda in Cliff1997
	private double momentumChange;			// momentum update, in Eq (15) of Cliff1997
	private double beta;					// learning rate, beta in Cliff1997
	private double gamma;					// momentum coefficient, gamma in Cliff1997
	private Price lastPrice;				// for last order price, p_i in Cliff1997

	// Strategy parameters (tunable)
	private final double rangeCoeffA;	// range for A, coefficient of absolute perturbation
	private final double rangeCoeffR;	// range for R, coefficient of relative perturbation

	public ZIPAgent(TimeStamp arrivalTime, FundamentalValue fundamental, SIP sip, 
			Market market, Random rand, double reentryRate, double pvVar, 
			int tickSize, int maxAbsPosition, int bidRangeMin, int bidRangeMax, 
			int windowLength, double marginMin, double marginMax, double gammaMin, 
			double gammaMax, double betaMin, double betaMax, double rangeCoeffA, 
			double rangeCoeffR) {
		super(arrivalTime, fundamental, sip, market, rand, reentryRate,
				new PrivateValue(maxAbsPosition, pvVar, rand), tickSize,
				bidRangeMin, bidRangeMax, windowLength);

		this.rangeCoeffA = rangeCoeffA;
		this.rangeCoeffR = rangeCoeffR;

		// Initializing variables
		momentumChange = 0;	// initialized to 0
		lastPrice = null;
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

		StringBuilder sb = new StringBuilder().append(this).append(" ");
		sb.append(getName()).append(':').append("::agentStrategy: ");
		
		// update NBBO
		BestBidAsk lastNBBOQuote = sip.getNBBO();

//		Quote quote = marketIP.getQuote();
//		Price bid = quote.getBidPrice();
//		Price ask = quote.getAskPrice();

		// can buy and sell
		OrderType type = rand.nextBoolean() ? BUY : SELL;
		
		double currentMargin = getCurrentMargin(positionBalance, type, currentTime);
		log(INFO, sb.append("initial mu=").append(format(currentMargin)));

		if (lastPrice == null) {
			switch (type) {
			case BUY:
				lastPrice = lastNBBOQuote.getBestBid(); // FIXME or just use quote from its own market?
				break;
			case SELL:
				lastPrice = lastNBBOQuote.getBestAsk();
				break;
			}
		}

		// Check if there are any transactions in the market model yet
		List<Transaction> pastTransactions = getWindowTransactions(currentTime);
		if (!pastTransactions.isEmpty()) {
			// Determine limit price or lambda
			limitPrice = getLimitPrice(type, 1, currentTime);

			log(INFO, sb.append("#new transactions=").append(pastTransactions.size()));
			for (Transaction trans : pastTransactions) {
				// Update margin
				log(INFO, sb.append("mu=").append(format(currentMargin)));
				updateMargin(type, trans, currentTime);
				currentMargin = getCurrentMargin(positionBalance, type, currentTime);

//				double oldMargin = newMargin;
//				Price lastTransPrice = trans.getPrice();
//				// Asserting that margin updated correctly
//				if (isBuyer) {
//					if (lastTransPrice.lessThanEqual(lastPrice)) 
//						assert Math.abs(oldMargin) <= Math.abs(margin); // raise margin
//					else
//						assert Math.abs(oldMargin) >= Math.abs(margin); // lower margin
//				} else {
//					if (lastTransPrice.greaterThanEqual(lastPrice))
//						assert Math.abs(oldMargin) <= Math.abs(margin); // raise margin
//					else
//						assert Math.abs(oldMargin) >= Math.abs(margin); // lower margin
//				}
			}

			// Even if no new transactions this round, will still submit a new order
			Price orderPrice = pcomp.max(Price.ZERO, computeOrderPrice(currentMargin, currentTime));
			acts.add(new SubmitNMSOrder(this, primaryMarket, type, orderPrice, 1, currentTime));
			lastPrice = orderPrice;

		} else {
			// zero transactions
			log(INFO, sb.append("No transactions!"));
			acts.addAll(executeZIStrategy(type, 1, currentTime));
		}

		// FIXME add these variables to background agent?
//		lastPositionBalance = positionBalance;	// update position balance
//		numEntries++;
//		maxPositionHeld = Math.max(maxPositionHeld, lastPositionBalance);	// update max position held

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
		StringBuilder sb = new StringBuilder().append(this).append(" ");
		sb.append(getName()).append(':').append("::agentStrategy: ");
		
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
		log(INFO, sb.append("updated mu=").append(format(currentMargin)).
				append("-->mu=").append(format(newMargin)));
		// TODO does this new margin get assigned to the margins? (the initial one)
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
		StringBuilder sb = new StringBuilder().append(this).append(" ").append(getName()).
				append("::computeOrderPrice: ");
		double orderPrice = limitPrice.intValue() * (1 + currentMargin);
		log(INFO, sb.append("limitPrice=").append(limitPrice).append(" * (1+mu)=").
				 append(format(1 + currentMargin)).append(", returns ").append(orderPrice));
		return new Price(orderPrice);
	}


	/**
	 * Update profit margin. Eq (12) in Cliff1997
	 * 
	 * @param type
	 * @param lastTrans
	 * @param currentTime
	 */
	public void updateMargin(OrderType type, Transaction lastTrans, TimeStamp currentTime) {
		StringBuilder sb = new StringBuilder().append(this).append(" ").append(getName()).
				append("::updateMargin: ");
		log(INFO, sb.append("lastTransPrice=").append(lastTrans.getPrice()));
		updateMomentumChange(lastTrans, currentTime);
		double newMargin = (lastPrice.intValue() + momentumChange) / limitPrice.intValue() - 1;
		margin.setValue(positionBalance, type, newMargin);
	}


	/**
	 * General momentum update rule. Eq (15) in Cliff1997
	 * 
	 * @param lastTrans
	 * @param currentTime
	 */
	public void updateMomentumChange(Transaction lastTrans, TimeStamp currentTime) {
		StringBuilder sb = new StringBuilder().append(this).append(" ").append(getName()).
				append("::updateMomentumChange: ");
		log(INFO, sb.append("original change=").append(format(momentumChange)));

		double originalChange = momentumChange;
		double delta = computeDelta(lastTrans, currentTime);
		log(INFO, sb.append("delta=").append(format(delta)));
		momentumChange = gamma * momentumChange + (1-gamma) * delta;

		if (originalChange != 0) {
			log(INFO, sb.append("new change=").append(format(momentumChange)).append(", using "). 
					append(format(100*(momentumChange-originalChange)/originalChange) + "%"));
		} else {
			log(INFO, sb.append("first update, change=").append(format(momentumChange)));
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
		Price tau = computeTargetPrice(lastTrans, currentTime);
		return beta * (tau.intValue() - lastPrice.intValue());
	}


	/**
	 * Determine target price (tau). Eq (14) in Cliff1997
	 * 
	 * @param lastTrans
	 * @param currentTime
	 * @return
	 */
	public Price computeTargetPrice(Transaction lastTrans, TimeStamp currentTime){
		StringBuilder sb = new StringBuilder().append(this).append(" ").append(getName()).
				append("::computeTargetPrice: ");
		Price lastTransPrice = lastTrans.getPrice();
		log(INFO, sb.append("lastPrice=").append(lastPrice).append(", lastTransPrice=").
				append(lastTransPrice));

		boolean increasing = checkIncreasing(lastTrans, currentTime);
		double R = computeRCoefficient(increasing);
		double A = computeACoefficient(increasing);
		log(INFO, sb.append("increasing? ").append(increasing). 
				append(": R=").append(format(R)).append(", A=").append(format(A)));

		double tau = R * lastTransPrice.intValue() + A;
		log(INFO, sb.append("targetPrice=").append(format(tau)));

		return new Price(tau);
	}

	/**
	 * Check if transaction prices have been increasing.
	 * 
	 * NOTE: this check only works if comparing last order price to last 
	 * transaction price. When windowing, there will never be a last order price
	 * to compare against. What to do?
	 *  
	 * @param lastTrans
	 * @param ts
	 * @return
	 */
	protected boolean checkIncreasing(Transaction lastTrans, TimeStamp ts) {
		Price lastTransPrice = lastTrans.getPrice();

		if (lastPrice == null) return true;
		// always return true if no prior order price or NBBO 
		// FIXME this may not be the best decision

		boolean increasing = lastTransPrice.greaterThan(lastPrice);
//		if (lastPrice == lastNBBOQuote.bestBid || lastPrice == lastNBBOQuote.bestAsk) {
//			// otherwise, lastPrice probably happened more recently than the last 
//			// transaction if using the NBBO quote as the price
//			increasing = lastPrice.greaterThan(lastTransPrice);
//		}
		return increasing;
	}

	/**
	 * Compute new coefficient of Relative Perturbation.
	 * 
	 * @param increasing
	 * @return
	 */
	public double computeRCoefficient(boolean increasing){
		if(increasing){
			return Rands.nextUniform(rand, 1, 1+rangeCoeffR);
		} else {
			return Rands.nextUniform(rand, 1-rangeCoeffR, 1);
		}
	}

	/**
	 * Compute new coefficient of Absolute Perturbation
	 * 
	 * @param increasing
	 * @return
	 */
	public double computeACoefficient(boolean increasing){
		if(increasing){
			return Rands.nextUniform(rand, 0, rangeCoeffA);
		} else {
			return Rands.nextUniform(rand, -rangeCoeffA, 0);
		}
	}
}
