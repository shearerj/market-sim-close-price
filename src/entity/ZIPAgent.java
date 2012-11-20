package entity;

import event.*;
import activity.*;
import systemmanager.*;

import java.util.HashMap;

import market.Quote;

/**
 * Use kappa to set 'c' for the ZIP agent
 * 
 * Use \mu from Cliff's paper
 * 
 * Ensure that transaction price != \lambda
 * 
 * 
 * 
 * @author ewah, sgchako, kunshao, marzuq, gshiva
 *
 */
public class ZIPAgent extends MMAgent {

	//private int margin;				// margin for limit order
	private int mainMarketID;	// ID of ZIP agent's primary market
	private int altMarketID;	// ID of ZIP agent's alternate market
        
	
	private double pvVar;			// variance from private value random process
        
        private int bidRange;				// range for limit order
        
        private int p_old;  //Price at previous time step
        
        private double c; //Correction step size for the ZIP agent
        
        public Consts.SubmittedBidMarket submittedBidType;
	
                
	public ZIPAgent(int agentID, SystemData d, AgentProperties p, Log l) {
		super(agentID, d, p, l);
		agentType = Consts.getAgentType(this.getClass().getSimpleName());
		params = p;
		arrivalTime = new TimeStamp(0);
		pvVar = this.data.privateValueVar;
                bidRange = this.data.bidRange;                               
		privateValue = Math.max(0, this.data.nextPrivateValue() + 
				(int) Math.round(getNormalRV(0, pvVar)) * Consts.SCALING_FACTOR);
		
		// Choose market ID based on whether agentID is even or odd
		if (agentID % 2 == 0) {
			mainMarketID = data.getMarketIDs().get(0);
			altMarketID = data.getMarketIDs().get(1);
		} else {
			mainMarketID = data.getMarketIDs().get(1);
			altMarketID = data.getMarketIDs().get(0);
		}
                
                c = this.data.kappa;
                
                p_old = -1;
	}
	
	@Override
	public HashMap<String, Object> getObservation() {
                //Return prfit here as System.out.println();
                System.out.println("ZIP Agent Profit = "+this.getRealizedProfit());
		return null;
	}
	
	@Override
	public ActivityHashMap agentStrategy(TimeStamp ts) {
		ActivityHashMap actMap = new ActivityHashMap();

		// gets best buy and sell offers (for all markets)
		Quote mainMarketQuote = data.getMarket(mainMarketID).quote(ts);
                
                //Find the transaction mode- is our agent buying or selling?
                int p_new = 0; //New bid price for this time step 
		int q = 1; //Buy/sell variable
                // 0.50% chance of either buying or selling
		if (rand.nextDouble() < 0.5)
			q = -q; 
                //TODO - figure out p_{t=0} - I'm guessing random init, subject to private valuation
                
                //Find the best market for transaction adn the corresponding best price
                boolean nbboBetter = false;
                int bestMarketID = mainMarketID;
                submittedBidType = Consts.SubmittedBidMarket.MAIN;	// default is submitting to main market
                int bestPrice = -1;
                if (q > 0) {
                    //Check if valid markets have been initialized
                    if( lastNBBOQuote.bestAsk != -1 ||
                        mainMarketQuote.lastAskPrice.getPrice() == -1 &&
                        lastNBBOQuote.bestAsk != -1) {
                            if (lastNBBOQuote.bestAsk < mainMarketQuote.lastAskPrice.getPrice()){
                                //^Do other markets have better asking prices according to the NBBO
                                nbboBetter = true;
                                bestPrice = lastNBBOQuote.bestAsk;
                                bestMarketID = lastNBBOQuote.bestAskMarket;    
                                
                                if(bestMarketID == altMarketID)
                                    submittedBidType = Consts.SubmittedBidMarket.ALTERNATE;
                                
                            }
                            else{
                                bestPrice = mainMarketQuote.lastAskPrice.getPrice();
                            }
                    } else {//In case the market have not been initialized, use a random price.
                        p_old = (int) Math.max(0, ((this.privateValue - 2*bidRange) + rand.nextDouble()*2*bidRange));
                        bestPrice = p_old;                    
                    }
                } else {
                    if( lastNBBOQuote.bestBid != -1 ||
                        mainMarketQuote.lastBidPrice.getPrice() == -1 &&
                        lastNBBOQuote.bestBid != -1) {
                            if (lastNBBOQuote.bestBid > mainMarketQuote.lastBidPrice.getPrice()) {
                                // don't need lastNBBOQuote.bestBid != -1 due to first condition, will always > -1
                                nbboBetter = true;
                                bestPrice = lastNBBOQuote.bestAsk;
                                bestMarketID = lastNBBOQuote.bestAskMarket;
                                
                                if(bestMarketID == altMarketID)
                                    submittedBidType = Consts.SubmittedBidMarket.ALTERNATE;
                                
                        } else {
                                bestPrice = mainMarketQuote.lastAskPrice.getPrice();
                        }
                    } else {//In case the market have not been initialized, use a random price.
                        p_old = (int) Math.max(0, (this.privateValue + rand.nextDouble()*2*bidRange));
                        bestPrice = p_old;                        
                    }
                }
                
                assert  bestPrice>=0:"ERROR: bestPrice = "+bestPrice+" which is !(>=0).";
                /* ^Make sure bestPrice is >= 0, In case the markets and NBBO have
                 * just been initialized, the best price is set to p_old (which
                 * in turn is set arbitraily). Either way, the best price cannot
                 * be -1.
                 */
                
                //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                
                /*
                 * SPENCER - start here:
                 * Note:-   variable 'bestPrice' <=> q_{t-1}
                 *          variable 'p_old' <=> p_{t-1}
                 *          variable 'privateValue' <=> \lambda
                 *          variable 'p_new' <=> p_{t}
                 * 
                 * 'q>0' implies we're buying and 'q<0' means we're selling
                 * 
                 * 'c' has been defined, be sure to use in calculating mu
                 */
                
                
                //Given best buy/sell price, determine the margin (\mu)
                if (q > 0) {
                    //Buying:- We buy from (0 , \lambda)
                    /*
                     * Is the best bid above our private valuation?
                     *  Yes - Quote our private valuation
                     *      p_t = \lambda - 1
                     *  No - Calculate margin:
                     *      
                     *      p_t = getMu() + \lambda
                     *       
                     */
                    if(q > privateValue)
                        p_new = privateValue - 1;                    
                    else
                        p_new = privateValue + getMu(privateValue, p_old, q);
                    
                    } else {
                    //Selling:- We sell from (\lambda, +\infinity)
                    /*
                     * Is the best bid below our private valuation?
                     *  Yes - Quote our private valuation
                     *      p_t = \lambda + 1
                     *  
                     *  No - Calculate margin:
                     *      
                     *      p_t = getMu() + \lambda 
                     *       
                     */
                    if(q < privateValue)
                        p_new = privateValue + 1;
                    else
                        p_new = privateValue + getMu(privateValue, p_old, q);
                    

                //Set the best buy/sell price subject to constraints and sleep

                        
                }
                
                assert p_new !=privateValue : "ERROR: Agent is trying to "
                        + "transact at its private value : PV = "+privateValue+ 
                        " transaction value = "+p_new;
                
                //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                
                //Submit bid to the correct market
                if (nbboBetter) {
                    // nbboBetter = true indicates that the alternative market has a better quote
                    log.log(Log.INFO, ts.toString() + " | " + this.toString() + " " + agentType + 
                                    "::agentStrategy: " + "NBBO(" + lastNBBOQuote.bestBid + ", " + 
                                    lastNBBOQuote.bestAsk + ") better than " + data.getMarket(mainMarketID) + 
                                    " Quote(" + mainMarketQuote.lastBidPrice.getPrice() + 
                                    ", " + mainMarketQuote.lastAskPrice.getPrice() + ")");
                } else {
                    // main market is better than the alternate market (according to NBBO)
                    log.log(Log.INFO, ts.toString() + " | " + this.toString() + " " + agentType + 
                                    "::agentStrategy: " + "NBBO(" + lastNBBOQuote.bestBid + ", " + 
                                    lastNBBOQuote.bestAsk + ") worse than/same as " + data.getMarket(mainMarketID) + 
                                    " Quote(" + mainMarketQuote.lastBidPrice.getPrice() + 
                                    ", " + mainMarketQuote.lastAskPrice.getPrice() + ")");
                }
                
                p_old = p_new;
                
                actMap.appendActivityHashMap(addBid(data.markets.get(bestMarketID), p_new, q, ts));
                
                log.log(Log.INFO, ts.toString() + " | " + this.toString() + " " + agentType + 
                        "::agentStrategy: " + "+(" + p_new + "," + q + ") to " + 
                        data.getMarket(bestMarketID));
		
                //QUESTION - Does the bid need an expiration time?
		
		// Insert events for the agent to sleep, then wake up again at timestamp tsNew
		int sleepTime = Integer.parseInt(params.get("sleepTime"));
		double sleepVar = Double.parseDouble(params.get("sleepVar"));
		TimeStamp tsNew = ts.sum(new TimeStamp(getRandSleepTime(sleepTime, sleepVar)));
		actMap.insertActivity(Consts.MARKETMAKER_PRIORITY, new UpdateAllQuotes(this, tsNew));
		actMap.insertActivity(Consts.MARKETMAKER_PRIORITY, new AgentStrategy(this, tsNew));
		return actMap;
	}
        
        /*
         * TODO - Calculate Mu here
         * 
         * p = \lambda + \mu
         * 
         * TEMP:
         * \mu = c*(q_{t-1} - p_{t-1})
         *  c = 1
         */
        private int getMu(int lambda, int p_old, int q){
            double step = (double) (q - p_old); //Performing operations in double
            step = c*step;
            return (int) step; //Type casting back to int to return appropriate values
        }
}
