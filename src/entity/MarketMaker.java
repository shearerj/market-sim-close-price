package entity;

import java.util.Iterator;

import event.*;
import market.*;
import activity.*;
import systemmanager.*;

/**
 * Basic market maker. See description in 2011 EC paper.
 * 
 * @author ewah
 */
public class MarketMaker extends Agent {

	public MarketMaker(int agentID, SystemData d) {
		super(agentID, d);
		agentType = "MM";
		
		// for testing
		sleepTime = 30;
	}

	public ActivityHashMap agentStrategy(TimeStamp ts) {

		ActivityHashMap actMap = new ActivityHashMap();

		// now cycle through all markets & submit bids
		for (Iterator<Integer> i = marketIDs.iterator(); i.hasNext(); ) {
			Market mkt = data.markets.get(i.next());

			// for now will just trade with the first market it's in
			double bid = getBidPrice(mkt.ID).getPrice();
			double ask = getAskPrice(mkt.ID).getPrice();

			int numRungs = 10;

			double[] prices = new double[numRungs];
			int[] quantities = new int[numRungs];

			// This is a dummy market maker that simply submits lots and lots of bids
			for(int j=0; j<numRungs; j++) {
				if(j<numRungs/2) {//First half of array are buys
					quantities[j] = 1;
					prices[j] = bid-(.1*j);//Depth set at +/-.01*numRungs		}
				}
				else { //Second half of array are sells
					quantities[j] = -1;
					prices[j] = ask+(.1*j);//Depth set at +/-.01*numRungs		}
				}
				//			addMessage("Price "+prices[j]+" at Quantity "+quantities[j]);
//				System.out.print("(" + quantities[j] + ", " + prices[j] + ") | ");
				actMap.appendActivityHashMap(addBid(mkt, prices[j], quantities[j], ts));
			}
		}
		if (!marketIDs.isEmpty()) {
			actMap.insertActivity(new UpdateAllQuotes(this, ts.sum(new TimeStamp(sleepTime))));
			actMap.insertActivity(new AgentStrategy(this, ts.sum(new TimeStamp(sleepTime))));
		}
		return actMap;
	}
}