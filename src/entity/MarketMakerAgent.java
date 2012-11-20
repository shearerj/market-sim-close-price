package entity;

import event.*;
import activity.*;
import systemmanager.*;

import java.util.HashMap;

/**
 * Basic market maker. See description in 2011 EC paper.
 * Participates in only a single market at a time.
 * 
 * @author ewah
 */
public class MarketMakerAgent extends SMAgent {

	public MarketMakerAgent(int agentID, SystemData d, ObjectProperties p, Log l, int mktID) {
		super(agentID, d, p, l, mktID);
		agentType = Consts.getAgentType(this.getClass().getSimpleName());
		
		arrivalTime = new TimeStamp(0);
	}
	
	@Override
	public HashMap<String, Object> getObservation() {
		return null;
	}
	
	@Override
	public ActivityHashMap agentStrategy(TimeStamp ts) {

		ActivityHashMap actMap = new ActivityHashMap();

//		int bid = getBidPrice(mkt.ID).getPrice();
//		int ask = getAskPrice(mkt.ID).getPrice();
//
//		int numRungs = 10;
//
//		int[] prices = new int[numRungs];
//		int[] quantities = new int[numRungs];
//
//		// This is a dummy market maker that simply submits lots and lots of bids
//		for(int j=0; j<numRungs; j++) {
//			if(j<numRungs/2) {//First half of array are buys
//				quantities[j] = 1;
//				prices[j] = bid-(10*j);//Depth set at +/-.01*numRungs		}
//			}
//			else { //Second half of array are sells
//				quantities[j] = -1;
//				prices[j] = ask+(10*j);//Depth set at +/-.01*numRungs		}
//			}
//			log.log(Log.INFO,"MarketMaker::Price "+prices[j]+" @ Quantity "+quantities[j]);
//			//				System.out.print("(" + quantities[j] + ", " + prices[j] + ") | ");
//			actMap.appendActivityHashMap(addBid(mkt, prices[j], quantities[j], ts));
//		}
		
		int sleepTime = Integer.parseInt(params.get("sleepTime"));
		double sleepVar = Double.parseDouble(params.get("sleepVar"));
		TimeStamp tsNew = ts.sum(new TimeStamp(getRandSleepTime(sleepTime, sleepVar)));
		actMap.insertActivity(Consts.MARKETMAKER_PRIORITY, new UpdateAllQuotes(this, tsNew));
		actMap.insertActivity(Consts.MARKETMAKER_PRIORITY, new AgentStrategy(this, market, tsNew));
		return actMap;
	}
}
