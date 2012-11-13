package entity;

import event.*;
import activity.*;
import systemmanager.*;

import java.util.HashMap;

import market.Quote;

/**
 * Template for ZIPAgent.
 * 
 * @author ewah
 *
 */
public class ZIPAgent extends MMAgent {

	private int margin;				// margin for limit order
	private int mainMarketID;	// ID of ZIP agent's primary market
	
	private double pvVar;			// variance from private value random process
	
	public ZIPAgent(int agentID, SystemData d, AgentProperties p, Log l) {
		super(agentID, d, p, l);
		agentType = Consts.getAgentType(this.getClass().getSimpleName());
		params = p;
		arrivalTime = new TimeStamp(0);
		pvVar = this.data.privateValueVar;
		privateValue = Math.max(0, this.data.nextPrivateValue() + 
				(int) Math.round(getNormalRV(0, pvVar)) * Consts.SCALING_FACTOR);
		
		// Choose market ID based on whether agentID is even or odd
		if (agentID % 2 == 0) {
			mainMarketID = data.getMarketIDs().get(0);
//			altMarketID = data.getMarketIDs().get(1);
		} else {
			mainMarketID = data.getMarketIDs().get(1);
//			altMarketID = data.getMarketIDs().get(0);
		}
	}
	
	@Override
	public HashMap<String, Object> getObservation() {
		return null;
	}
	
	@Override
	public ActivityHashMap agentStrategy(TimeStamp ts) {
		ActivityHashMap actMap = new ActivityHashMap();

		// gets best buy and sell offers (for all markets)
		Quote mainMarketQuote = data.getMarket(mainMarketID).quote(ts);
                
                //Find the transaction mode- is our agent buying or selling?
                int p = 0; //Price
		int q = 1; //Buy/sell variable
                // 0.50% chance of either buying or selling
		if (rand.nextDouble() < 0.5)
			q = -q; 
                
                //Find the best market for transaction
                
                //Given best buy/sell price, determine the margin
                    //Buying:- We buy from (0 , \lambda]
                    /*
                     * Is the best bid above our private valuation?
                     *  Yes - Do we quote our private valuation? Something less than our PV?
                     *  
                     *  No - Calculate margin:
                     *      \mu = c*(q_{t-1} - p_{t-1})
                     *      p_t = \mu + p_{t-1} 
                     *       
                     */
                    
                    //Selling:- We sell from [\lambda, +\infinity)
                    /*
                     * Is the best bid below our private valuation?
                     *  Yes - Do we quote our private valuation? Something less than our PV?
                     *  
                     *  No - Calculate margin:
                     *      \mu = c*(q_{t-1} - p_{t-1})
                     *      p_t = \mu + p_{t-1} 
                     *       
                     */
                
                //Set the best buy/sell price subject to constraints and sleep
                
		// basic ZIP behavior
		if (q > 0) {
			//p = (int) Math.max(0, ((this.privateValue - 2*bidRange) + rand.nextDouble()*2*bidRange));
                        //Set p as 
		} else {
			//p = (int) Math.max(0, (this.privateValue + rand.nextDouble()*2*bidRange));
		}
                
		
		
		// Insert events for the agent to sleep, then wake up again at timestamp tsNew
		int sleepTime = Integer.parseInt(params.get("sleepTime"));
		double sleepVar = Double.parseDouble(params.get("sleepVar"));
		TimeStamp tsNew = ts.sum(new TimeStamp(getRandSleepTime(sleepTime, sleepVar)));
		actMap.insertActivity(Consts.MARKETMAKER_PRIORITY, new UpdateAllQuotes(this, tsNew));
		actMap.insertActivity(Consts.MARKETMAKER_PRIORITY, new AgentStrategy(this, tsNew));
		return actMap;
	}
}
