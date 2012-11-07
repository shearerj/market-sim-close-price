package entity;

import event.*;
import activity.*;
import systemmanager.*;

import java.util.HashMap;

/**
 * Basic market maker. See description in 2011 EC paper.
 * Participates in only a single market at a time.
 * 
 * @author ewah, gshiva
 */
public class MarketMakerAgent extends SMAgent {
        private int bidRange;				// range for limit order
        
        private boolean DEBUG_ENB = true;
        
        private int mainMarketID;			// assigned at initialization

	public MarketMakerAgent(int agentID, SystemData d, AgentProperties p, Log l, int mktID) {
		super(agentID, d, p, l, mktID);
		agentType = Consts.getAgentType(this.getClass().getSimpleName());
		params = p;
              
                bidRange = this.data.bidRange;  //Sets buy-sell spread for the Market Maker
                
                //Bids need to expire every tik and a new bid is put up
                
                if(DEBUG_ENB){
                    //?? Where's the AgentProperties being used?
                    System.out.println("MarketMaker Agent initialized at market "+params.get("Market"));
                    System.out.println("Bid Range = "+bidRange);
                }
		arrivalTime = new TimeStamp(0);
                
                if (agentID % 2 == 0) {
			mainMarketID = data.getMarketIDs().get(0);
		} else {
			mainMarketID = data.getMarketIDs().get(1);
		}

	}
	
	@Override
	public HashMap<String, Object> getObservation() {
                if(DEBUG_ENB){
                    System.out.println("MarketMaker.getObservation() called, returning null");
                }
		return null;
	}
	
	@Override
	public ActivityHashMap agentStrategy(TimeStamp ts) {

		ActivityHashMap actMap = new ActivityHashMap();

		int bid = getBidPrice(mainMarketID).getPrice();
		int ask = getAskPrice(mainMarketID).getPrice();

		//int numRungs = 10;  //Size of the bid-ask spread
		int numRungs = bidRange;  //Size of the bid-ask spread

		int[] prices = new int[numRungs];
		int[] quantities = new int[numRungs]; //Set to 1 for each bid/ask price

		// This is a dummy market maker that simply submits lots and lots of bids
		for(int j=0; j<numRungs; j++) {
			if(j<numRungs/2) {//First half of array are buys
				quantities[j] = 1;
				prices[j] = bid-(10*j);//Depth set at +/-.01*numRungs		}
			}
			else { //Second half of array are sells
				quantities[j] = -1;
				prices[j] = ask+(10*j);//Depth set at +/-.01*numRungs		}
			}
                        
                        if(DEBUG_ENB){
                            System.out.println("MarketMaker price :"+prices[j]);
                            if(quantities[j] > 1)
                                System.out.println("price is buying");
                            else
                                System.out.println("price is selling");
                        }
                        
			log.log(Log.INFO,"MarketMaker::Price "+prices[j]+" @ Quantity "+quantities[j]);
			//				System.out.print("(" + quantities[j] + ", " + prices[j] + ") | ");
			actMap.appendActivityHashMap(addBid(data.markets.get(mainMarketID), prices[j], quantities[j], ts));
		}
		int sleepTime = Integer.parseInt(params.get("sleepTime")); //Does the Market Maker sleep? Shouldn't ti submit bids everytime it gets updates?
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
		
		double sleepVar = Double.parseDouble(params.get("sleepVar"));
		TimeStamp tsNew = ts.sum(new TimeStamp(getRandSleepTime(sleepTime, sleepVar)));
		actMap.insertActivity(Consts.MARKETMAKER_PRIORITY, new UpdateAllQuotes(this, tsNew));
		actMap.insertActivity(Consts.MARKETMAKER_PRIORITY, new AgentStrategy(this, market, tsNew));
		return actMap;
	}
}
