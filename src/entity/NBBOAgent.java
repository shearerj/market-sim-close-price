package entity;

import event.*;
import activity.*;
import systemmanager.*;
import market.*;

import java.util.Random;
import java.util.Vector;
import java.util.Properties;

/**
 * NBBOAgent
 *
 * A zero-intelligence (ZI) agent operating in two-market setting with an NBBO market.
 *
 * The NBBO agent trades with only one market, and will not require the execution
 * speed of "market order." It will look at both the one market and the NBBO, and if
 * the NBBO is better, it will check if there is a match in the other market. 
 * If there is no match for the bid/ask, it will not make the trade.
 *
 * This NBBO agent bases its private value on a stochastic process, the parameters
 * of which are specified at the beginning of the game by the Game Creator.
 * The agent's private valuation is determined by value of the random process at the
 * time it enters the game. The private value is used to calculate the agent's
 * surplus (and thus the market's allocative efficiency).
 *
 * This ZI agent submits only ONE limit order with an expiration that is determined
 * when the agent is initialized. The parameters determining the distribution from
 * which the expiration is drawn are given by the strategy configuration.
 *
 * The NBBO agent is always only active in one market, and it is used in two-market
 * scenarios (for latency arbitrage simulations).
 *
 * @author ewah
 */
public class NBBOAgent extends Agent {

	private Random rand;
	private int meanPV;
//	private double arrivalRate;
//	private double kappa;
//	private double shockVar;
	
	public int privateValue;
	
	private int tradeMarketID;		// assigned at initialization
	private int altMarketID;
	
	
	/**
	 * Overloaded constructor.
	 * @param agentID
	 * @param d SystemData object
	 */
	public NBBOAgent(int agentID, SystemData d) {
		super(agentID, d);
		agentType = "NBBO";

		rand = new Random();
	}
	
	/**
	 * Sets private value based on random process. 
	 * @param pv
	 */
	public void setPrivateValue(int pv) {
		this.privateValue = pv;
	}
	
	@Override
	public void initializeParams(SystemProperties p) {
		meanPV = Integer.parseInt(p.get(agentType).get("meanPV"));
//		arrivalRate = Double.parseDouble(p.get(agentType).get("arrivalRate"));
//		kappa = Double.parseDouble(p.get(agentType).get("kappa"));
//		shockVar = Double.parseDouble(p.get(agentType).get("shockVar"));
		
		sleepTime = Integer.parseInt(p.get(agentType).get("sleepTime"));
		
		// Hard code the market indices
		assert (this.data.numMarkets >= 2) : "NBBO agents need at least 2 markets!";
		tradeMarketID = -1;
		altMarketID = -2;
		if (rand.nextDouble() >= 0.5) {
			altMarketID = tradeMarketID;
			tradeMarketID--;
		}
	}
	
	@Override
	public TimeStamp nextArrivalTime() {
		return this.data.nextArrival();
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
//		System.out.println(agentType + "Agent " + this.ID + ": AgentStrategy");
		
		ActivityHashMap actMap = new ActivityHashMap();

		// identify best buy and sell offers
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
		int ask = lastNBBOQuote.bestAsk;
		int bid = lastNBBOQuote.bestBid;

		// if NBBO better check other market for matching quote (exact match)
		boolean nbboWorse = false;
		if (q > 0) {
			if (bestQuote.bestBuy > bid) nbboWorse = true;
		} else {
			if (bestQuote.bestSell < ask) nbboWorse = true;
		}

		if (nbboWorse) {
			System.out.println("NBBO Worse");
			System.out.println(agentType + "::agentStrategy: " + ": NBBO (" + lastNBBOQuote.bestAsk + 
					", " + lastNBBOQuote.bestBid + " ) better than Market " + tradeMarketID + " (" + bestQuote.bestSell +
					", " + bestQuote.bestBuy + ")");

			// since NBBO is better, check other market for a matching quote
			Quote altQuote = getLatestQuote(altMarketID);
			if (altQuote.lastAskPrice.getPrice() == ask && altQuote.lastBidPrice.getPrice() == bid) {
				// there is a match! so trade in the other market
				actMap.appendActivityHashMap(addBid(data.markets.get(altMarketID), p, q, ts));
				System.out.println("bid submitted! to Market " + altMarketID);
//				addMessage(AGENTTYPE + "::agentStrategy: asset " + altAssetID +
//					": bid (" + p + "," + q + ") submitted to market " + altMarketID);
			} else {
				// no match, so no trade!
				System.out.println(agentType + "::agentStrategy: " + ": No bid submitted -- no match to NBBO (" +
						lastNBBOQuote.bestAsk +	", " + lastNBBOQuote.bestBid + ") in Market " + altMarketID +
						" (" + altQuote.lastAskPrice.getPrice() + ", " + altQuote.lastBidPrice.getPrice() + ")");
			}

		} else { // current market's quote is better
			System.out.println(data.markets.get(tradeMarketID));
			actMap.appendActivityHashMap(addBid(data.markets.get(tradeMarketID), p, q, ts));
//			addMessage(AGENTTYPE + "::agentStrategy: asset " + tradeAssetId +
//					": bid (" + p + "," + q + ") submitted to market " + tradeMarketID);
		}
		
		// only submits one bid so this other part isn't called
//		if (!marketIDs.isEmpty()) {
//			TimeStamp tsNew = ts.sum(new TimeStamp(sleepTime));
//			actMap.insertActivity(new UpdateAllQuotes(this,tsNew));
//			actMap.insertActivity(new UpdateNBBO(this, tsNew));
//			actMap.insertActivity(new AgentStrategy(this, tsNew));
//		}
		return actMap;
	}
}
