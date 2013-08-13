package entity.agent;

import static logger.Logger.log;
import static logger.Logger.Level.INFO;
import static utils.Compare.max;
import static utils.Compare.min;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import model.MarketModel;
import utils.RandPlus;
import utils.Pair;
import activity.Activity;
import activity.AgentStrategy;
import activity.SubmitNMSOrder;
import data.EntityProperties;
import data.Keys;
import entity.market.Market;
import entity.market.Price;
import entity.market.Quote;
import entity.market.Transaction;
import event.TimeStamp;

/**
 * This is the implementation of the Adaptive Aggressive strategy.
 * Documentation is on its way
 * 
 * 
 * @author drhurd
 */

public class AAAgent extends ReentryAgent {
	
	private static final long serialVersionUID = 2418819222375372886L;
	
	// Agent market variables
	private boolean isBuyer; // randomly assigned at initialization
	private int maxAbsPosition; // maxPosition the agent can take on

	//Agent parameters
	private int historical; // number of historical prices to look at
	private double lambda_r; // coefficient of relative perturbation of delta
	private double lambda_a; // coefficient of absolute perturbation of delta
	private int eta; // price determination coefficient
	private double gamma; // long term learning variable
	private double beta_r; // learning coefficient for r (short term)
	private double beta_t; // learning coefficient for theta (long term)
	
	//Agent strategy variables
	private double aggression; // short term learning variable
	private double theta; // long term learning variable
	private double thetaMax; // max possible value for theta
	private double thetaMin; // min possible value for theta
	private double alphaMax; // max experienced value for alpha (for theta, not PV)
	private double alphaMin; // min experienced value for alpha


	public AAAgent(int agentID, TimeStamp arrivalTime, MarketModel model,
			Market market, RandPlus rand, EntityProperties params) {
		this(agentID, arrivalTime, model, market, rand, params,
				new PrivateValue(params.getAsInt(Keys.MAX_QUANTITY, 1),
						params.getAsDouble(Keys.PRIVATE_VALUE_VAR, 100000000), rand), 
				params.getAsDouble(Keys.REENTRY_RATE, 0.005),
				params.getAsInt(Keys.TICK_SIZE, 1));
	}
	
	public AAAgent(int agentID, TimeStamp arrivalTime, MarketModel model,
			Market market, RandPlus rand, EntityProperties params,
			PrivateValue privateValue, double reentryRate, int tickSize) {
		super(agentID, arrivalTime, model, market, privateValue, rand,
				reentryRate,
				tickSize);
		
		//Initializing Max Absolute Position
		this.maxAbsPosition = params.getAsInt(Keys.MAX_QUANTITY, 1);
		
		
		//Determining whether agent is a buyer or a seller
		this.isBuyer = params.getAsBoolean(Keys.BUYER_STATUS, rand.nextBoolean());

		//Initializing parameters
		// Parameters are currently taken from Vytelingum paper
		lambda_r = 0.05;
		lambda_a = 0.02;
		alphaMax = alphaMin = -1;
		gamma = 2;
		beta_r = (rand.nextDouble() * 0.4) + 0.2;
		beta_t = (rand.nextDouble() * 0.4) + 0.2;
		
		//Initializing strategy variables
		theta = params.getAsDouble(Keys.THETA, 0);
		aggression = params.getAsDouble(Keys.AGGRESSION, 0);
		historical = params.getAsInt(Keys.HISTORICAL, 5);
		eta = params.getAsInt(Keys.ETA, 3);
		thetaMax = params.getAsDouble(Keys.THETA_MAX, 4);
		thetaMin = params.getAsDouble(Keys.THETA_MIN, -4);
		
	}

	@Override
	public Collection<Activity> agentStrategy(TimeStamp ts) {
		Collection<Activity> actMap = new ArrayList<Activity>();

		List<Transaction> trans = sip.getTransactions();
		
		// Update the moving average
		Pair<Double, Double> pair = findMovingAverage(trans);
		double movingAverage = pair.left();
		double mvgAvgNum = pair.right();
		
		//Determining the most recent Price
		Price lastPrice;
		if(trans.size() != 0)
			lastPrice = trans.get(trans.size() - 1).getPrice();
		else
			lastPrice = new Price(-1);
		
		// Determining Quantity
		int quantity = isBuyer ? 1 : -1;

		// Updating Price Limit
		Price limit = determinePriceLimit(quantity, ts);

		// Determine the Target Price
		Price targetPrice = determinePriceTarget(limit, movingAverage);

		// Bidding Layer - only if at least one ask and one bid have been
		// submitted
		// See Vytelingum paper section 4.4 - Bidding Layer
		actMap.addAll(biddingLayer(limit, targetPrice, quantity, ts));

		// Update the short term learning variable
		double oldAggression = aggression;
		updateAggression(limit, targetPrice, movingAverage, lastPrice);

		//Asserting that aggression updated correctly
		if(isBuyer) {
			if(lastPrice.lessThan(targetPrice)) 
				assert(oldAggression > aggression);
			else
				assert(oldAggression < aggression);
		}
		else {
			if(lastPrice.greaterThan(targetPrice))
				assert(oldAggression > aggression);
			else
				assert(oldAggression < aggression);
		}
		
		// Update long term learning variable
		updateAdaptiveness(movingAverage, mvgAvgNum, trans);
		
		// Market Reentry
		// TimeStamp tsNew = reentry.next();
		actMap.add(new AgentStrategy(this, reentry.next()));

		return actMap;
	}
	
	private Pair<Double, Double> findMovingAverage(List<Transaction> trans) {
		if (trans.size() == 0) 
			return new Pair<Double, Double>(-1.0, 0.0); // Error checking
		double total = 0;
		double num = 0;
		// Iterate through past Quotes and use valid prices
		for (int i = trans.size() - 1; i >= 0 && num < historical; i--) {
			total += (double) trans.get(i).getPrice().getInTicks();
			num += 1;
		}
		if (num == 0) {
			return new Pair<Double, Double>(-1.0, 0.0);
		}
		double movingAverage = total / num;
		double mvgAvgNum = num;
		return new Pair<Double, Double>(movingAverage, mvgAvgNum);
	}

	
	private Price determinePriceLimit(int quantity, TimeStamp ts) {
		Price fundPrice = model.getFundamentalAt(ts);
		Price deviation = privateValue.getValueAtPosition(positionBalance + quantity);
		return fundPrice.plus(deviation);
	}
	
	/**
	 * 
	 * @return price target according to the AA Strategy
	 */
	private Price determinePriceTarget(Price limit, double movingAverage) {
		//Error Checking - cannot compute if movingAverage is invalid
		if(movingAverage == -1) return new Price(-1);
		double tau;
		// Buyers
		if (isBuyer) {
			// Intramarginal - price limit is greater than p*
			if (limit.getInTicks() > movingAverage) {
				// passive
				if(aggression == -1) 
					tau = Price.ZERO.getInTicks(); 
				else if(aggression < 0) {
					tau = movingAverage
							* (1 - (Math.exp(-aggression * theta) - 1)
									/ (Math.exp(theta) - 1));
				}
				//active
				else if(aggression == 0)
					tau = movingAverage;
				// aggressive
				else if(aggression < 1){
					tau = movingAverage + (limit.getInTicks() - movingAverage)
							* (Math.exp(aggression * theta) - 1)
							/ (Math.exp(theta) - 1);
				}
				else
					tau = limit.getInTicks();
			}
			// Extramarginal - price limit is less than p*
			else {
				// passive
				if(aggression == -1)
					tau = Price.ZERO.getInTicks();
				if (aggression < 0) {
					tau = limit.getInTicks()
							* (1 - (Math.exp(-1 * aggression * theta) - 1)
									/ (Math.exp(theta) - 1));
				}
				// aggressive
				else
					tau = limit.getInTicks();
			}
		}
		// Sellers
		else {
			// Intramarginal - cost is less than p*
			if (limit.getInTicks() < movingAverage) {
				// passive
				if(aggression == -1)
					tau = Price.INF.getInTicks();
				else if(aggression < 0) {
					tau = movingAverage
							+ (Price.INF.getInTicks() - movingAverage)
							* (Math.exp(-1 * aggression * theta) - 1)
							/ (Math.exp(theta) - 1);
				}
				//active
				else if(aggression == 0) {
					tau = movingAverage;
				}
				// aggressive
				else if(aggression < 1){
					tau = limit.getInTicks()
							+ (movingAverage - limit.getInTicks())
							* (1 - (Math.exp(aggression * theta) - 1)
									/ (Math.exp(theta) - 1));
				}
				else
					tau = limit.getInTicks();
			}
			// Extramarginal - cost is greater than p*
			else {
				// passive
				if(aggression == -1)
					tau = Price.INF.getInTicks();
				else if(aggression < 0) {
					tau = limit.getInTicks() + Price.INF.minus(limit).getInTicks()
							* (Math.exp(-1 * aggression * theta) - 1)
							/ (Math.exp(theta) - 1);
				}
				// aggressive
				else
					tau = limit.getInTicks();
			}
		}
		return new Price((int) Math.round(tau));
	}

	
	/**
	 * @param limit
	 * @param targetPrice
	 * @param quantity
	 * @param ts
	 * @return
	 */
	private Collection<? extends Activity> biddingLayer(Price limit, Price targetPrice,
			int quantity, TimeStamp ts) {
		String s = this + " " + getName() + ":";

		// Determining the offer price to (possibly) submit
		Quote quote = marketIP.getQuote();
		Price bestBid = quote.getBidPrice();
		Price bestAsk = quote.getAskPrice();

		// Can only submit offer if the offer would not cause position
		// balance to exceed the agent's maximum position
		int newPosBal = positionBalance + quantity;
		if (newPosBal < (-1 * maxAbsPosition) || newPosBal > maxAbsPosition) {
			s += "new order would exceed max position " + maxAbsPosition
					+ "; no submission";
			log(INFO, s);
			return Collections.emptySet(); 
		}
		
		// if no bid or no ask, submit least aggressive price
		if (bestBid == null || bestAsk == null ||
				bestBid.equals(new Price(-1)) || bestAsk.equals(new Price(-1))) {
			Price price = isBuyer ? Price.ZERO : Price.INF;
			return Collections.singleton(new SubmitNMSOrder(AAAgent.this, price, quantity, primaryMarket, TimeStamp.IMMEDIATE));
		}
		
		// If best offer is outside of limit price, no bid is submitted
		if ((isBuyer && limit.lessThanEqual(bestBid))
			|| (!isBuyer && limit.greaterThanEquals(bestAsk))) {
			s += "best offer is outside of limit price: " + limit
					+ "; no submission";
			log(INFO, s);
			return Collections.emptySet();
		}

		// Pricing - verifying targetPrice
		Price price;
		if(!targetPrice.equals(new Price(-1)))
			targetPrice = isBuyer ? min(limit, targetPrice) : 
			max(limit, targetPrice);

		// See equations 10 and 11 in Vytelingum paper section 4.4 - bidding
		// layer
		// Extra 1 added/subtracted in equations is to make sure agent
		// submits better bid if
		// difference/eta computes to be zero
		if (targetPrice.equals(new Price(-1))) {
			if (isBuyer) {
				Price offset = min(bestAsk, limit).minus(bestBid).times(1.0/eta);
				price = bestBid.plus(offset).plus(new Price(1));
				price = min(price, limit);
			} else {
				Price offset = bestAsk.minus( max(bestBid, limit) ).times(1.0/eta);
				price = bestAsk.minus(offset).minus(new Price(1));
				price = max(price, limit);				
			}
		}
		else {
			if (isBuyer) {
				Price offset = targetPrice.minus(bestBid).times(1.0/eta);
				price = bestBid.plus(offset).plus(new Price(1));
				price = min(price, limit);
			} else {
				Price offset = bestAsk.minus(targetPrice).times(1.0/eta);
				price = bestAsk.minus(offset).minus(new Price(1));
				price = max(price, limit);
			}
		}

		// Submitting a bid - See Vytelingum paper section 4.4 - bidding
		// layer
		if (isBuyer) { // Buyer
			// if bestAsk < targetPrice, accept bestAsk
			// else submit bid given by EQ 10/11
			Price submitPrice = bestAsk.lessThanEqual(price) ? bestAsk : price;
			return Collections.singleton(new SubmitNMSOrder(AAAgent.this, submitPrice, quantity, primaryMarket, TimeStamp.IMMEDIATE));
		} else { // Seller
			// If outstanding bid >= target price, submit ask at bid price
			// else submit bid given by EQ 10/11
			Price submitPrice = bestBid.greaterThanEquals(price) ? bestBid : price;
			return Collections.singleton(new SubmitNMSOrder(AAAgent.this, submitPrice, quantity, primaryMarket, TimeStamp.IMMEDIATE));
		}
	}
	
	/**
	 * Updates the short term learning variable
	 * 
	 * @param ts
	 *            , tau
	 */
	private void updateAggression(Price limit, Price tau, 
			double movingAverage, Price mostRecentPrice) {
		if (movingAverage == -1 || mostRecentPrice.equals(new Price(-1)))
			return; // If no transactions yet, cannot update

		// Determining r_shout
		double r_shout = determineAggression(limit, mostRecentPrice, movingAverage);

		// Determining whether agent must be more or less aggressive
		// See the Vytelingum paper (sec 4.3)
		// Differs from paper b/c we do not take into account the most
		// recent bid/ask submitted, only transactions
		int sign = 1;
		// Buyers
		if (isBuyer) {
			// if target price is greater, agent should be less aggressive
			if (tau.greaterThan(mostRecentPrice))
				sign = -1;
			// if transaction price is greater, agent should be more
			// aggressive
			else
				sign = 1;
		} else {
			// if target price is less than most recent transaction, agent
			// should be less aggressive
			if (tau.lessThan(mostRecentPrice))
				sign = -1;
			// if target price is greater than most recent transaction,
			// agent should be more aggressive
			else
				sign = 1;
		}

		// Updating aggression
		double delta = (1 + sign * lambda_r) * r_shout + sign * lambda_a;
		aggression = aggression + beta_r * (delta - aggression);
	}
	
	/**
	 * Given a price, returns the level of aggression that would result in
	 * the agent submitting a bid/ask at the price
	 * 
	 * @param mostRecentPrice
	 * @return
	 */
	private double determineAggression(Price limit, Price mostRecentPrice,
			double movingAverage) {
		double tau = mostRecentPrice.getInTicks();
		double r_shout = 0;
		// Buyers
		if (isBuyer) {
			// Intramarginal
			if (limit.getInTicks() > movingAverage) {
				if (tau == movingAverage)
					return 0;
				// r < 0
				if (tau < movingAverage) {
					r_shout = (-1 / theta)
							* Math.log((1 - tau / movingAverage)
									* (Math.exp(theta) - 1) + 1);
				}
				// r > 0
				else {
					r_shout = (1 / theta)
							* Math.log((tau - movingAverage)
									* (Math.exp(theta) - 1)
									/ (limit.getInTicks() - movingAverage));
				}
			}
			// Extramarginal
			else {
				if (tau < limit.getInTicks()) {
					r_shout = (-1 / theta)
							* Math.log((1 - tau / limit.getInTicks())
									* (Math.exp(theta) - 1) + 1);
				}
				// TODO - SHOULD NOT REACH HERE
				else {
					// I'M NOT SURE WHAT TO DO - FUNCTION UNDEFINED
					r_shout = 1;
				}
			}
		}

		// Sellers
		else {
			// Intramarginal
			if (limit.getInTicks() < movingAverage) {
				if (tau == movingAverage)
					return 0;
				// r < 0
				if (tau > movingAverage) {
					r_shout = (-1 / theta)
							* Math.log((tau - movingAverage)
									* (Math.exp(theta) - 1)
									/ (Price.INF.getInTicks() - movingAverage)
									+ 1);
				}
				// r > 0
				else {
					r_shout = (1 / theta)
							* Math.log((1 - (tau - limit.getInTicks())
									/ (movingAverage - limit.getInTicks()))
									* (Math.exp(theta) - 1) + 1);
				}
			}
			// Extramarginal
			else {
				if (tau > limit.getInTicks()) {
					r_shout = (-1 / theta)
							* Math.log((tau - limit.getInTicks())
									* (Math.exp(theta) - 1)
									/ Price.INF.minus(limit).getInTicks() + 1);
				}
				// TODO - SHOULD NOT REACH HERE
				else {
					// ROUND AND ROUND IT GOES, WHAT HAPPENS NOBODY KNOWS
					r_shout = 1;
				}
			}
		}
		if (r_shout == Double.NaN || r_shout == Double.NEGATIVE_INFINITY
				|| r_shout == Double.POSITIVE_INFINITY)
			return 0;
		return r_shout;
	}
	
	/**
	 * Update the long term learning variable
	 * 
	 */
	private void updateAdaptiveness(double movingAverage,
			double mvgAvgNum, List<Transaction> trans) {
		//Error Checking, must have some transactions
		if(movingAverage == -1) return;

		// Determining alpha
		double alpha = 0;
		int num = 0;
		for (int i = trans.size() - 1; i >= 0 && num < historical; i--) {
			double price = trans.get(i).getPrice().getInTicks();
			if (price != -1) {
				alpha += Math.pow(price - movingAverage, 2);
				num++;
			}
		}
		alpha = (1 / movingAverage) * Math.sqrt(alpha / num);

		// Determining alphaBar, updating range
		if (alpha < alphaMin || alpha == -1)
			alphaMin = alpha;
		if (alpha > alphaMax || alpha == -1) {
			alphaMax = alpha;
		}
		double alphaBar = (alpha - alphaMin) / (alphaMax - alphaMin);

		// Determining theta star
		double thetaStar = (thetaMax - thetaMin)
				* (1 - alphaBar * Math.exp(gamma * (alphaBar - 1)))
				+ thetaMin;

		// If number of transactions < historical, do not update theta
		if (mvgAvgNum < historical)
			return;
		
		// Updating theta
		theta = theta + beta_t * (thetaStar - theta);
	}

	
	public boolean getBuyerStatus() {
		return isBuyer;
	}

	public double getAggression() {
		return aggression;
	}

	public void setAggression(double in) {
		aggression = in;
	}

	public double getAdaptiveness() {
		return theta;
	}

	public void setAdaptivness(double in) {
		theta = in;
	}

}
