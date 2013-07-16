package entity;

import static logger.Logger.log;
import static logger.Logger.Level.INFO;
import static utils.Compare.max;
import static utils.Compare.min;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import market.Price;
import market.PrivateValue;
import market.Quote;
import market.Transaction;
import model.MarketModel;
import utils.RandPlus;
import utils.Pair;
import activity.Activity;
import activity.AgentStrategy;
import activity.SubmitNMSBid;
import data.ArrivalTime;
import data.EntityProperties;
import data.Keys;
import event.TimeStamp;

/**
 * This is the implementation of the Adaptive Aggressive strategy.
 * Documentation is on its way
 * 
 * 
 * @author drhurd
 */

public class AAAgent extends BackgroundAgent {
	public final static String HISTORICAL_KEY = "historical";
	public final static String ETA_KEY = "eta";
	public final static String AGGRESSION_KEY = "aggression";
	public final static String THETA_KEY = "theta";
	public final static String THETAMAX_KEY = "thetaMax";
	public final static String THETAMIN_KEY = "thetaMin";
	public final static String DEBUG_KEY = "debug";
	public final static String TEST_KEY = "AAtesting";
	public final static String BUYERSTATUS_KEY = "buyerStatus";

	//
	// Private variables
	//
	// system variables
//	private boolean debugging;

	// Agent market variables
	private AAStrategy strat; // Holder class for all of the strategy parameters
								// and variables
	private boolean isBuyer; // randomly assigned at initialization
	private ArrivalTime reentry; // re-entry times
	private int maxAbsPosition; // maxPosition the agent can take on

	//
	// Internal Strategy Class
	//
	private class AAStrategy {
		// parameters
		private int historical; // number of historical prices to look at
		private double lambda_r; // coefficient of relative perturbation of
									// delta
		private double lambda_a; // coefficient of absolute perturbation of
									// delta
		private int eta; // price determination coefficient
		private double gamma; // long term learning variable
		private double beta_r; // learning coefficient for r (short term)
		private double beta_t; // learning coefficient for theta (long term)

		// Agent strategy variables
//		private Price mostRecentPrice;
		private double aggression; // short term learning variable
		private double theta; // long term learning variable
		private double thetaMax; // max possible value for theta
		private double thetaMin; // min possible value for theta
		private double alphaMax; // max experienced value for alpha (for theta,
									// not PV)
		private double alphaMin; // min experienced value for alpha

		public AAStrategy(EntityProperties params) {
			// Agent Parameters

			// Parameters are currently taken from Vytelingum paper
			lambda_r = 0.05;
			lambda_a = 0.02;
			alphaMax = alphaMin = -1;
			gamma = 2;
			beta_r = (rand.nextDouble() * 0.4) + 0.2;
			beta_t = (rand.nextDouble() * 0.4) + 0.2;
			
			// Initial aggression and theta
			theta = params.getAsDouble(AAAgent.THETA_KEY, 0);
			aggression = params.getAsDouble(AAAgent.AGGRESSION_KEY, 0);
			// Long term learning ranges
			thetaMax = params.getAsDouble(AAAgent.THETAMAX_KEY, 4);
			thetaMin = params.getAsDouble(AAAgent.THETAMIN_KEY, -4);
			historical = params.getAsInt(AAAgent.HISTORICAL_KEY, 5);
			eta = params.getAsInt(AAAgent.ETA_KEY, 3);
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
				if (tau.greaterThanEquals(mostRecentPrice))
					sign = -1;
				// if transaction price is greater, agent should be more
				// aggressive
				else
					sign = 1;
			} else {
				// if target price is less than most recent transaction, agent
				// should be less aggressive
				if (tau.lessThanEqual(mostRecentPrice))
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
				double price = trans.get(i).getPrice().getPrice();
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

		private Pair<Double, Double> findMovingAverage(List<Transaction> trans) {
			if (trans.size() == 0) 
				return new Pair<Double, Double>(-1.0, 0.0); // Error checking
			double total = 0;
			double num = 0;
			// Iterate through past Quotes and use valid prices
			for (int i = trans.size() - 1; i >= 0 && num < historical; i--) {
				total += (double) trans.get(i).getPrice().getPrice();
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
				if (limit.getPrice() > movingAverage) {
					// passive
					if(aggression == -1) 
						tau = Price.ZERO.getPrice(); 
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
						tau = movingAverage + (limit.getPrice() - movingAverage)
								* (Math.exp(aggression * theta) - 1)
								/ (Math.exp(theta) - 1);
					}
					else
						tau = limit.getPrice();
				}
				// Extramarginal - price limit is less than p*
				else {
					// passive
					if(aggression == -1)
						tau = Price.ZERO.getPrice();
					if (aggression < 0) {
						tau = limit.getPrice()
								* (1 - (Math.exp(-1 * aggression * theta) - 1)
										/ (Math.exp(theta) - 1));
					}
					// aggressive
					else
						tau = limit.getPrice();
				}
			}
			// Sellers
			else {
				// Intramarginal - cost is less than p*
				if (limit.getPrice() < movingAverage) {
					// passive
					if(aggression == -1)
						tau = Price.INF.getPrice();
					else if(aggression < 0) {
						tau = movingAverage
								+ (Price.INF.getPrice() - movingAverage)
								* (Math.exp(-1 * aggression * theta) - 1)
								/ (Math.exp(theta) - 1);
					}
					//active
					else if(aggression == 0) {
						tau = movingAverage;
					}
					// aggressive
					else if(aggression < 1){
						tau = limit.getPrice()
								+ (movingAverage - limit.getPrice())
								* (1 - (Math.exp(aggression * theta) - 1)
										/ (Math.exp(theta) - 1));
					}
					else
						tau = limit.getPrice();
				}
				// Extramarginal - cost is greater than p*
				else {
					// passive
					if(aggression == -1)
						tau = Price.INF.getPrice();
					else if(aggression < 0) {
						tau = limit.getPrice() + Price.INF.minus(limit).getPrice()
								* (Math.exp(-1 * aggression * theta) - 1)
								/ (Math.exp(theta) - 1);
					}
					// aggressive
					else
						tau = limit.getPrice();
				}
			}
			return new Price((int) Math.round(tau));
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
			double tau = mostRecentPrice.getPrice();
			double r_shout = 0;
			// Buyers
			if (isBuyer) {
				// Intramarginal
				if (limit.getPrice() > movingAverage) {
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
										/ (limit.getPrice() - movingAverage));
					}
				}
				// Extramarginal
				else {
					if (tau < limit.getPrice()) {
						r_shout = (-1 / theta)
								* Math.log((1 - tau / limit.getPrice())
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
				if (limit.getPrice() < movingAverage) {
					if (tau == movingAverage)
						return 0;
					// r < 0
					if (tau > movingAverage) {
						r_shout = (-1 / theta)
								* Math.log((tau - movingAverage)
										* (Math.exp(theta) - 1)
										/ (Price.INF.getPrice() - movingAverage)
										+ 1);
					}
					// r > 0
					else {
						r_shout = (1 / theta)
								* Math.log((1 - (tau - limit.getPrice())
										/ (movingAverage - limit.getPrice()))
										* (Math.exp(theta) - 1) + 1);
					}
				}
				// Extramarginal
				else {
					if (tau > limit.getPrice()) {
						r_shout = (-1 / theta)
								* Math.log((tau - limit.getPrice())
										* (Math.exp(theta) - 1)
										/ Price.INF.minus(limit).getPrice() + 1);
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
		 * 
		 * @param bestBid
		 * @param bestAsk
		 * @param targetPrice
		 * @param ts
		 * @return
		 */
		private Collection<? extends Activity> biddingLayer(Price limit, Price targetPrice,
				int quantity, TimeStamp ts) {
			String s = ts + " | " + this + " " + getName() + ":";

			// Determining the offer price to (possibly) submit
			Quote quote = marketIP.getQuote();
			Price bestBid = quote.getAskPrice();
			Price bestAsk = quote.getBidPrice();

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
				return Collections.singleton(new SubmitNMSBid(AAAgent.this, price, quantity, primaryMarket, TimeStamp.IMMEDIATE));
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
					Price offset = min(bestAsk, limit).minus(bestBid).times(1.0/strat.eta);
					price = bestBid.plus(offset).plus(new Price(1));
					price = min(price, limit);
				} else {
					Price offset = bestAsk.minus( max(bestBid, limit) ).times(1.0/strat.eta);
					price = bestAsk.minus(offset).minus(new Price(1));
					price = max(price, limit);				
				}
			}
			else {
				if (isBuyer) {
					Price offset = targetPrice.minus(bestBid).times(1.0/strat.eta);
					price = bestBid.plus(offset).plus(new Price(1));
					price = min(price, limit);
				} else {
					Price offset = bestAsk.minus(targetPrice).times(1.0/strat.eta);
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
				return Collections.singleton(new SubmitNMSBid(AAAgent.this, submitPrice, quantity, primaryMarket, TimeStamp.IMMEDIATE));
			} else { // Seller
				// If outstanding bid >= target price, submit ask at bid price
				// else submit bid given by EQ 10/11
				Price submitPrice = bestBid.greaterThanEquals(price) ? bestBid : price;
				return Collections.singleton(new SubmitNMSBid(AAAgent.this, submitPrice, quantity, primaryMarket, TimeStamp.IMMEDIATE));
			}
		}
	}

	public AAAgent(int agentID, TimeStamp arrivalTime, MarketModel model,
			Market market, RandPlus rand, EntityProperties params) {
		// TODO change "null" to proper private value initialization
		super(agentID, arrivalTime, model, market, new PrivateValue(
				params.getAsInt(Keys.MAX_QUANTITY, 1), params.getAsDouble(
						"pvVar", 100), rand), rand, params.getAsInt("tickSize", 1000));
		//Initialize market
		this.marketSubmittedBid = this.primaryMarket;
		
		//Initializing Reentry times
		double reentryRate = params.getAsDouble(Keys.REENTRY_RATE, 0);
		this.reentry = new ArrivalTime(arrivalTime, reentryRate, rand);
		
		//Initializing Max Absolute Position
		this.maxAbsPosition = params.getAsInt(Keys.MAX_QUANTITY, 1);
		
		//Initializing Strategy Class
		this.strat = new AAStrategy(params);
		
		//Determining whether agent is a buyer or a seller
		this.isBuyer = params.getAsBoolean(BUYERSTATUS_KEY, rand.nextBoolean());

		//Debugging Output
		//this.debugging = debugging;
		boolean debugging = params.getAsBoolean(DEBUG_KEY, false);
		if (debugging) printInitDebugInfo();
	}

	/**
	 * @return ArrivalTime object holding re-entries into market
	 */
	public ArrivalTime getReentryTimes() {
		return reentry;
	}

	@Override
	public Collection<Activity> agentStrategy(TimeStamp ts) {
		String s = ts + " | " + this + " " + getName() + ":";
		Collection<Activity> actMap = new ArrayList<Activity>();

		List<Transaction> trans = sip.getTransactions();
		
		// Update the moving average
		Pair<Double, Double> pair = strat.findMovingAverage(trans);
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
		Price limit = strat.determinePriceLimit(quantity, ts);

		// Determine the Target Price
		Price targetPrice = strat.determinePriceTarget(limit, movingAverage);

		// Bidding Layer - only if at least one ask and one bid have been
		// submitted
		// See Vytelingum paper section 4.4 - Bidding Layer
		actMap.addAll(strat.biddingLayer(limit, targetPrice, quantity, ts));

		// Update the short term learning variable
		strat.updateAggression(limit, targetPrice, movingAverage, lastPrice);

		// Update long term learning variable
		strat.updateAdaptiveness(movingAverage, mvgAvgNum, trans);

		// Market Reentry
		// TimeStamp tsNew = reentry.next();
		actMap.add(new AgentStrategy(this, reentry.next()));

		log(INFO, s + " " + isBuyer + " private valuation="
				+ limit);
		log(INFO, s + " adaptiveness=" + strat.theta);
		return actMap;
	}

	public boolean getBuyerStatus() {
		return isBuyer;
	}

	public double getAggression() {
		return strat.aggression;
	}

	public void setAggression(double in) {
		strat.aggression = in;
	}

	public double getAdaptiveness() {
		return strat.theta;
	}

	public void setAdaptivness(double in) {
		strat.theta = in;
	}

	//
	// Private Methods
	//

	/**
	 * Prints out the Agent parameters for debugging
	 */
	private void printInitDebugInfo() {
		// Identify the agent and primary market
		System.out.println("AAA Agent [" + this.getID() + "] - " + isBuyer);
		// Print out the agent parameters
		System.out.println("eta = " + strat.eta);
		System.out.println("theta= " + strat.theta);
		System.out.println("thetaMax= " + strat.thetaMax);
		System.out.println("thetaMin= " + strat.thetaMin);
		System.out.println("historical = " + strat.historical);
		System.out.println("beta_r = " + strat.beta_r);
		System.out.println("beta_t = " + strat.beta_t);
	}

}
