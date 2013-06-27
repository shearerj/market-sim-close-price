package entity;

import data.*;
import event.*;
import logger.Logger;
import model.*;
import market.*;
import activity.*;
import systemmanager.*;

import java.util.*;

/**
 * This is the implementation of the Adaptive Aggressive strategy 
 * NOT READY FOR USE
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

	//
	//Private variables
	//
	//system variables
	private double pvVar; //Variance used to generate limit
	private boolean debugging;
	
	//Agent market variables
	private AAStrategy strat; //Holder class for all of the strategy parameters and variables
	private boolean isBuyer;		//randomly assigned at initialization
	private ArrivalTime reentry;	//re-entry times
	private int maxAbsPosition;		//maxPosition the agent can take on

	//
	//Internal Strategy Class
	//
	private class AAStrategy {
		private int testing;
		//parameters
		private int limit;  		//limit price 
		private int historical; 	//number of historical prices to look at
		private double lambda_r; 	//coefficient of relative perturbation of delta
		private double lambda_a; 	//coefficient of absolute perturbation of delta
		private int eta; 			//price determination coefficient
		private double gamma; 		//long term learning variable 
		private double beta_r; 		//learning coefficient for r (short term)
		private double beta_t; 		//learning coefficient for theta (long term)
	
		//Agent strategy variables
		private ArrayList<Transaction> trans;
		private int mostRecentPrice;
		private double movingAverage;	
		private double mvgAvgNum;		//number of terms used for current movingAverage
		private double aggression;		//short term learning variable
		private double theta;			//long term learning variable
		private double thetaMax;		//max possible value for theta
		private double thetaMin;		//min possible value for theta
		private double alphaMax;		//max experienced value for alpha (for theta, not PV)
		private double alphaMin;		//min experienced value for alpha

		public AAStrategy(int test) {
			testing = test; //for unit tests
			movingAverage = -1;
			//Agent Parameters
			//Private Value
	        ArrayList<Integer> alphas = new ArrayList<Integer>();
	        for(int i= -maxAbsPosition; i < maxAbsPosition; i++) {
	        	alphas.add((int) Math.round(getNormalRV(0, pvVar))); 
	        }
	        alpha = new PrivateValue(alphas);
	        limit = alpha.getValueFromQuantity(0).getPrice();
			
	        //Parameters are currently taken from Vytelingum paper
			historical = Integer.parseInt(params.get(AAAgent.HISTORICAL_KEY));
			lambda_r = 0.01;
			lambda_a = 0.01;
			eta = Integer.parseInt(params.get(AAAgent.ETA_KEY));
			gamma = 2;
			beta_r = (rand.nextDouble() * 0.4) + 0.2;
			beta_t = (rand.nextDouble() * 0.4) + 0.2;
			//Initial aggression and theta
			theta = Double.parseDouble(params.get(AAAgent.THETA_KEY));
			aggression = Double.parseDouble(params.get(AAAgent.AGGRESSION_KEY));
			//Long term learning ranges
			thetaMax = Double.parseDouble(params.get(AAAgent.THETAMAX_KEY));
			thetaMin = Double.parseDouble(params.get(AAAgent.THETAMIN_KEY));
			alphaMax = alphaMin = -1;
		}
		
		/**
		 * Updates the short term learning variable
		 * @param ts, tau
		 */
		private void updateAggression(TimeStamp ts, int tau) {
			if(trans.size() == 0) return;	//If no transactions yet, cannot update
			
			//Determining r_shout
			double r_shout = determineAggression(mostRecentPrice);
			
			//Determining whether agent must be more or less aggressive
			//See the Vytelingum paper (sec 4.3)
			//Differs from paper b/c we do not take into account the most recent bid/ask submitted, only transactions
			int sign = 1;
			//Buyers
			if(isBuyer) {
				//if target price is greater, agent should be less aggressive
				if(tau >= mostRecentPrice) sign = -1;
				//if transaction price is greater, agent should be more aggressive
				else sign = 1;
			}
			else {
				//if target price is less than most recent transaction, agent should be less aggressive
				if(tau <= mostRecentPrice) sign = -1;
				//if target price is greater than most recent transaction, agent should be more aggressive
				else sign = 1;
			}
			//Do not update aggression if it is not necessary
			if(sign == 0) return;
			
			//Updating aggression
			double delta = (1 + sign*lambda_r) * r_shout + sign*lambda_a;
			aggression = aggression + beta_r * (delta - aggression);
		}
		
		/**
	     * Update the long term learning variable
	     * @param ts Current TimeStamp
	     */
	    private void updateAdaptiveness(TimeStamp ts){
	    	//If number of transactions < historical, do not update
	    	if(mvgAvgNum < historical) return;
			
	    	//Determining alpha
			double alpha = 0;
			int num = 0;
			for(int i=trans.size()-1; i >=0 && num < historical; i--) {
				double price = trans.get(i).price.getPrice();
				if(price != -1) {
					alpha += Math.pow(price - movingAverage, 2);
					num++;
				}
			}
			alpha = (1/movingAverage) * Math.sqrt(alpha / num);
			
			
			//Determining alphaBar, updating range
			if(alpha < alphaMin || alpha == -1) alphaMin = alpha;
			if(alpha > alphaMax || alpha == -1) {
				alphaMax = alpha;
			}
			double alphaBar = (alpha - alphaMin) / (alphaMax - alphaMin);
			
			//Determining theta star
			double thetaStar = (thetaMax - thetaMin) * (1 - alphaBar*Math.exp(gamma*(alphaBar-1))) + thetaMin;
			
			
			//Updating theta
			theta = theta + beta_t * (thetaStar - theta);
			Logger.log(Logger.INFO, ts + " | " + this + " " + agentType + ": theta=" + theta);
			return;
	    }
	    
		private void findMovingAverage() { 
			//Getting the Past quotes
			// EW: changed to get transactions for the model, not just a specific market
			trans = new ArrayList<Transaction>(data.getTrans(modelID));
			if(trans.size() == 0) return; //Error checking
			mostRecentPrice = trans.get(trans.size() - 1).price.getPrice();
			double total = 0;
			double num = 0;
			//Iterate through past Quotes and use valid prices until you have 
			for(int i = trans.size() - 1; i >= 0 && num < historical; i--) {
				total += (double) trans.get(i).price.getPrice();
				num += 1;
			}
			if(num == 0) return;
			movingAverage = total / num;
			mvgAvgNum = num;
			//System.out.println(movingAverage);
		}
		
		private void determinePriceLimit(int quantity, TimeStamp ts) {
			int fundPrice = data.getFundamentalAt(ts).getPrice();
			int deviation = getPrivateValueAt(positionBalance + quantity).getPrice();
			limit = fundPrice + deviation;
			if(testing != -1) limit = testing;
		}
		
		/** 
		 * 
		 * @return price target according to the AA Strategy
		 */
		private double determinePriceTarget() {
			double tau;
			//Buyers
			if(isBuyer) {
				//Intramarginal - price limit is greater than p*
				if(limit > movingAverage) {
					//passive
					if(aggression < 0) {
						tau = movingAverage * (1 - (Math.exp(-1 * aggression * theta) - 1)/(Math.exp(theta) - 1));
					}
					//aggressive
					else {
						tau =  movingAverage + (limit - movingAverage)*(Math.exp(aggression*theta) - 1)/(Math.exp(theta) - 1);
					}
				}
				//Extramarginal - price limit is less than p*
				else {
					//passive
					if(aggression < 0) {
						tau = limit * (1 - (Math.exp(-1 * aggression * theta) - 1)/(Math.exp(theta) - 1));
					}
					//aggressive
					else tau = limit;
				}
			}
			//Sellers
			else {
				//Intramarginal - cost is less than p*
				if(limit < movingAverage) {
					//passive
					if(aggression < 0) {
						tau = movingAverage + (Consts.INF_PRICE - movingAverage)*(Math.exp(-1 * aggression * theta) - 1)/(Math.exp(theta) - 1);
					}
					//aggressive
					else {
						tau = limit + (movingAverage - limit) * (1 - (Math.exp(aggression*theta) - 1)/(Math.exp(theta) - 1));
					}
				}
				//Extramarginal - cost is greater than p*
				else {
					//passive
					if(aggression < 0) {
						tau = limit + (Consts.INF_PRICE - limit) * (Math.exp(-1 * aggression * theta) - 1) / (Math.exp(theta) - 1);
					}
					//aggressive
					else tau = limit;
				}
			}
			return tau;
		}
		
		/** 
		 * Given a price, returns the level of aggression that would result in the agent submitting a bid/ask at the price
		 * @param mostRecentPrice
		 * @return
		 */
		private double determineAggression(int mostRecentPrice) {
			double tau = mostRecentPrice;
			double r_shout = 0;
			//Buyers
			if(isBuyer) {
				//Intramarginal
				if(limit > movingAverage) {
					if(tau == movingAverage) return 0;
					//r < 0
					if(tau < movingAverage) {
						r_shout = (-1/theta) * Math.log( (1 - tau/movingAverage)*(Math.exp(theta) - 1) + 1 );
					}
					//r > 0
					else {
						r_shout = (1/theta) * Math.log( (tau - movingAverage) * (Math.exp(theta) - 1) / (limit - movingAverage));
					}
				}
				//Extramarginal
				else {
					if(tau < limit) {
						r_shout = (-1/theta) * Math.log( (1 - tau/limit) * (Math.exp(theta) - 1) + 1);
					}
					//TODO - SHOULD NOT REACH HERE
					else {
						//I'M NOT SURE WHAT TO DO - FUNCTION UNDEFINED
						r_shout = 0.75;
					}
				}
			}
			
			//Sellers
			else {
				//Intramarginal
				if(limit < movingAverage) {
					if(tau == movingAverage) return 0;
					//r < 0
					if(tau > movingAverage) {
						r_shout = (-1/theta) * Math.log( (tau - movingAverage) * (Math.exp(theta)-1) / (Consts.INF_PRICE - movingAverage) + 1);
					}
					//r > 0
					else {
						r_shout = (1/theta) * Math.log( (1 - (tau - limit)/(movingAverage - limit)) * (Math.exp(theta) - 1) + 1);
					}
				}
				//Extramarginal
				else {
					if(tau > limit) {
						r_shout = (-1/theta) * Math.log( (tau - limit) * (Math.exp(theta) - 1) / (Consts.INF_PRICE - limit) + 1);
					}
					//TODO - SHOULD NOT REACH HERE
					else {
						//ROUND AND ROUND IT GOES, WHAT HAPPENS NOBODY KNOWS
						r_shout = 0.75;
					}
				}
			}
			if(r_shout == Double.NaN || r_shout == Double.NEGATIVE_INFINITY || r_shout == Double.POSITIVE_INFINITY) return 0;
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
	    private Collection<Activity> biddingLayer(int targetPrice, int quantity, TimeStamp ts) {
	    	String s = ts + " | " + this + " " + agentType + ":";
	    	Collection<Activity> actMap = new ArrayList<Activity>();
	    	
			//Determining the offer price to (possibly) submit
			int bestBid = getBidPrice(market.getID()).getPrice();
			int bestAsk = getAskPrice(market.getID()).getPrice();
			
			//System.out.println("Bid: " + bestBid + " Ask: " + bestAsk);
			//System.out.println(data.getFundamentalAt(ts).getPrice());
			
			//if no bid or no ask, submit least aggressive price
			if(bestBid == -1 || bestAsk == -1) {
				int price = isBuyer ? 0 : Consts.INF_PRICE;
				actMap.addAll(executeSubmitNMSBid(new Price(price), quantity, ts));
				return actMap;
			}
			
	    	//Pricing
	    	int sign = isBuyer ? 1 : -1;
	    	int price;
			targetPrice = isBuyer ? Math.min(limit, targetPrice) : Math.max(limit, targetPrice);
			
			//See equations 10 and 11 in Vytelingum paper section 4.4 - bidding layer
			//Extra 1 added/subtracted in equations is to make sure agent submits better bid if
			//difference/eta computes to be zero
			if(targetPrice == -1) {
				if(isBuyer) {
					price = bestBid + (int) ((Math.min(bestAsk, strat.limit) - bestBid)/strat.eta) + 1;
					price = (int) Math.min(price, strat.limit); //verifying price <= limit
				}
				else {
					price = bestAsk - (int) ((bestAsk - Math.max(bestBid, strat.limit))/strat.eta) - 1;
					price = (int) Math.max(price, strat.limit); //verifying price >= limit
					
				}
			}
			//Had to convert from int to double for the division by eta (then back) - unnecessary if eta is an int
			else {
				if(isBuyer) {
					price = bestBid + (targetPrice - bestBid)/strat.eta + 1;
					price = (int) Math.min(price, strat.limit);
				}
				else {
					price = bestAsk - (bestAsk - targetPrice)/strat.eta - 1;
					price = (int) Math.max(price, strat.limit);
				}
			}
			
			//Can only submit offer if the offer would not cause position balance to exceed the agent's maximum position
			int newPosBal = positionBalance + quantity;
			if(newPosBal < (-1 * maxAbsPosition) || newPosBal > maxAbsPosition) {
				s += "new order would exceed max position " + maxAbsPosition 
						+ "; no submission";
				Logger.log(Logger.INFO, s);
				return actMap;
			}
			
			//Submitting a bid - See Vytelingum paper section 4.4 - bidding layer
			//If best offer is outside of limit price, no bid is submitted
			if((isBuyer && strat.limit <= bestBid) || (!isBuyer && strat.limit >= bestAsk)) {
				s += "best offer is outside of limit price: " + strat.limit + "; no submission";
				Logger.log(Logger.INFO, s);
				return actMap;
			}
			
			//If moving Average is valid
			//Buyers
			if(isBuyer) {	
				//if bestAsk < targetPrice, accept bestAsk
				//else submit bid given by EQ 10/11
				int submitPrice = (bestAsk <= price) ? bestAsk : price;
				actMap.addAll(executeSubmitNMSBid(new Price(submitPrice), quantity, ts));
			}
			//Seller
			else {
				//If outstanding bid >= target price, submit ask at bid price
				//else submit bid given by EQ 10/11
				int submitPrice = (bestBid >= price) ? bestBid : price;
				actMap.addAll(executeSubmitNMSBid(new Price(submitPrice), quantity, ts));
				Logger.log(Logger.INFO, ts + " AA: Best ask: " + bestAsk + "\tAgent limit= " + limit + "\tsubmission= " + submitPrice);
			}
	    	
	    	return actMap;
	    }
	}
	
	
	//
	//Public Methods
	//
	/** Constructor
	 * @param agentID
	 * @param modelID
	 * @param d
	 * @param p
	 * @param l
	 */
    public AAAgent(int agentID, int modelID, SystemData d, ObjectProperties p) {
		//Call constructor for SMAgent
		super(agentID, modelID, d, p);
		marketSubmittedBid = market;
		
		//System variables
		agentType = Consts.getAgentType(this.getName());
		
		//Agent market variables
		arrivalTime = new TimeStamp(Long.parseLong(params.get(Agent.ARRIVAL_KEY)));
		reentry = new ArrivalTime(arrivalTime, this.data.reentryRate, rand);
		maxAbsPosition = Integer.parseInt(params.get(AAAgent.MAXQUANTITY_KEY));
	
		//Agent Strategy variables
		int testing;
		if( params.containsKey(AAAgent.TEST_KEY) ) {
			testing = Integer.parseInt(params.get(AAAgent.TEST_KEY));
		}
		else testing = -1;
		strat = new AAStrategy(testing);
		//Randomly assigning agent as a buyer or seller
		isBuyer = rand.nextBoolean();
		
		//Debugging Output
		debugging = Boolean.parseBoolean(params.get(AAAgent.DEBUG_KEY));
		if(debugging) printInitDebugInfo();
    }

	//Trivial Classes
	@Override
	public HashMap<String, Object> getObservation(){ 
		HashMap<String,Object> obs = new HashMap<String,Object>();
		obs.put(Observations.ROLE_KEY, getRole());
		obs.put(Observations.PAYOFF_KEY, getRealizedProfit());
		obs.put(Observations.STRATEGY_KEY, getFullStrategy());
		return obs;
	}
	
	/**
	 * @return ArrivalTime object holding re-entries into market
	 */
	public ArrivalTime getReentryTimes() { return reentry; }
	
    @Override
    public Collection<Activity> agentStrategy(TimeStamp ts) {
    	String s = ts + " | " + this + " " + agentType + ":";
    	Collection<Activity> actMap = new ArrayList<Activity>();
    	//Randomizing buyer or seller
    	//isBuyer = rand.nextBoolean();
    	
    	
    	//Updating market info
    	updateAllQuotes(ts);
    	updateTransactions(ts);

		//Update the moving average
    	strat.findMovingAverage(); 	
		
    	//Determining Quantity
    	int quantity = isBuyer ? 1 : -1;
    	
    	//Updating Price Limit
    	strat.determinePriceLimit(quantity, ts);
    	
		//Determine the Target Price
		int targetPrice;
		if(strat.movingAverage == -1) targetPrice = -1;
		//Can only compute target price if movingAverage is valid
		else targetPrice = (int) Math.round(strat.determinePriceTarget());
		
		
		//Bidding Layer - only if at least one ask and one bid have been submitted
		//See Vytelingum paper section 4.4 - Bidding Layer
		actMap.addAll(strat.biddingLayer(targetPrice, quantity, ts));
		
		//Update the short term learning variable
		if(strat.movingAverage != -1) strat.updateAggression(ts, targetPrice);
		
		//Update long term learning variable
		if(strat.movingAverage != -1) strat.updateAdaptiveness(ts);

		//Market Reentry
		TimeStamp tsNew = reentry.next();
		actMap.add(new AgentStrategy(this, reentry.next()));
		
		
		Logger.log(Logger.INFO, s + " " + isBuyer + " private valuation=" + strat.limit);
		Logger.log(Logger.INFO, s + " adaptiveness=" + strat.theta);
		return actMap;
	}
	
    public boolean getBuyerStatus() { return isBuyer; }
    
    public double getAggression() { return strat.aggression; }
    public void setAggression(double in) { strat.aggression = in; }
    
    public double getAdaptiveness() {return strat.theta; }
    public void setAdaptivness(double in) {strat.theta = in; }
    
	//
	//Private Methods
	//
	
	/**
	 * Prints out the Agent parameters for debugging
	 */
	private void printInitDebugInfo() {
		//Identify the agent and primary market
		System.out.println("AAA Agent [" + this.getID() + "] - " + isBuyer);
		//Print out the agent parameters
		System.out.println("eta = " + strat.eta);
		System.out.println("theta= " + strat.theta);
		System.out.println("thetaMax= " + strat.thetaMax);
		System.out.println("thetaMin= " + strat.thetaMin);
		System.out.println("historical = " + strat.historical);
		System.out.println("beta_r = " + strat.beta_r);
		System.out.println("beta_t = " + strat.beta_t);
	}


}

