package entity;

import java.util.Iterator;
import java.util.Random;
import java.util.Vector;

import market.BestQuote;
import market.Quote;

import activity.ActivityHashMap;
import activity.AgentStrategy;
import activity.UpdateAllQuotes;
import activity.UpdateNBBO;
import event.TimeStamp;

import systemmanager.SystemData;

/**
 * Given an array of quantities & prices, submits hard-coded bids.
 * 
 * @author ewah
 */
public class TestAgent extends Agent {

	public int privateValue;
	private Random rand;
	
	/**
	 * Overloaded constructor.
	 * @param agentID
	 * @param d SystemData object
	 */
	public TestAgent(int agentID, SystemData d) {
		super(agentID, d);
		agentType = "TEST";

		rand = new Random();
		privateValue = 1000*rand.nextInt(25)+10000;
	}

	@Override
	public ActivityHashMap agentArrival(Market mkt, TimeStamp ts) {
//		System.out.println(agentType + "Agent " + this.ID + ": AgentArrival in Market " + mkt.ID);

		// buyer/seller based on config file
		mkt.agentIDs.add(this.ID);
		mkt.buyers.add(this.ID);
		mkt.sellers.add(this.ID);
		marketIDs.add(mkt.ID);
		quotes.put(mkt.ID, new Vector<Quote>());
		arrivalTime = ts;
		
		// Initialize bid/ask containers
		prevBid.put(mkt.ID, 0);
		prevAsk.put(mkt.ID, 0);
		initBid.put(mkt.ID, -1);
		initAsk.put(mkt.ID, -1);
		
		ActivityHashMap actMap = new ActivityHashMap();
		actMap.insertActivity(new UpdateAllQuotes(this, ts));
		actMap.insertActivity(new UpdateNBBO(this, ts));
		actMap.insertActivity(new AgentStrategy(this, ts));
		return actMap;
	}
	
	public ActivityHashMap agentStrategy(TimeStamp ts) {

		ActivityHashMap actMap = new ActivityHashMap();
		
		BestQuote bestQuote = findBestBuySell();

		int p = 0;
		int q = 1;
		if (rand.nextDouble() < 0.5) q = -q; // 0.50% chance of being either long or short
		// basic ZI behavior - price is based on uniform dist 2 SDs away from PV
		int bidSD = 5000; // arbitrary for now, TODO
		if (q > 0) {
			p = (this.privateValue - 2*bidSD) + rand.nextInt()*2*bidSD;
		} else {
			p = this.privateValue + rand.nextInt()*2*bidSD;
		}
//		int ask = lastNBBOQuote.bestAsk;
//		int bid = lastNBBOQuote.bestBid;
//		
//		// if NBBO better check other market for matching quote (exact match)
//		boolean nbboWorse = false;
//		if (q > 0) {
//			if (bestQuote.bestBuy > bid) nbboWorse = true;
//		} else {
//			if (bestQuote.bestSell < ask) nbboWorse = true;
//		}
		
		// Cycle through all markets & submit bids
//		for (Iterator<Integer> i = marketIDs.iterator(); i.hasNext(); ) {
//			Market mkt = data.markets.get(i.next());
//
//			int p = data.test.poll(); // remove first element. And only 3 agents
//			int q = 1;
//			if (p < 0) {
//				q = -q;
//				p = Math.abs(p);
//			}
//			actMap.appendActivityHashMap(addBid(mkt, p, q, ts));
//			
//		}
		if (!marketIDs.isEmpty()) {
			TimeStamp tsNew = ts.sum(new TimeStamp(sleepTime));
			actMap.insertActivity(new UpdateAllQuotes(this, tsNew));
			actMap.insertActivity(new UpdateNBBO(this, tsNew));
			actMap.insertActivity(new AgentStrategy(this, tsNew));
		}
		return actMap;
	}
}
