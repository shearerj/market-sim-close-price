package entity;

import java.util.Random;

import event.*;
import activity.*;
import systemmanager.*;
import market.*;

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
 * @author ewah
 */
public class NBBOAgent extends Agent {

	public double privateValue;
	private Random rand;

	public Quote nbbo;

	/**
	 * Overloaded constructor.
	 * @param agentID
	 * @param d SystemData object
	 */
	public NBBOAgent(int agentID, SystemData d) {
		super(agentID, d);
		agentType = "NBBO";

		// TODO for testing
		rand = new Random();
		privateValue = 10*rand.nextDouble()+50;
	}
	
	public ActivityHashMap agentStrategy(TimeStamp ts) {
		return null;
	}

//	public ActivityHashMap agentStrategy(TimeStamp ts) {
//
//
//		// get current NBBO quote
//		BestQuote nbboQuote = findBestBuySell(getAssetID(nbboMarketID));
//		// TODO - need to actually get the NBBO here
//
//		// identify best buy and sell (in mulitple markets)
//		BestQuote bestQuote = findBestBuySell(tradeAssetId);
//
//		// for normal bidding: qty = U(0, 200)
//		double p = 0;
////		int q = quantityMin;
//		int q = 1;
//		// + with 0.50% chance of being either long or short
//		double prob = 0.5;
//
//		if (rand.nextDouble() < prob) {
//			q = -q;
//		}
//
//		// basic ZI behavior - price is based on uniform dist 2 SDs away from PV
//		double bidSD = 5; // arbirtary for now
//		if (q > 0) {
//			//p = rand.nextDouble()*bidLimit + (this.privateValue-bidLimit);
//			// buy
//			p = (this.privateValue - 2*bidSD) + rand.nextDouble()*2*bidSD;
//		} else {
//			p = this.privateValue + rand.nextDouble()*2*bidSD;
//		}
//
//		p = (double) Math.round(p * 10) / 10;  // truncate to 2 decimals
//
//		double ask = nbboQuote.bestSell;
//		double bid = nbboQuote.bestBuy;
//
//		// if NBBO better check other market for matching quote (exact match)
//		boolean nbboWorse = false;
//		if (q > 0) {
//			if (bestQuote.bestBuy > bid) nbboWorse = true;
//		} else {
//			if (bestQuote.bestSell < ask) nbboWorse = true;
//		}
//
//		if (nbboWorse)
//		{
////			addMessage(AGENTTYPE + "::agentStrategy: asset " + tradeAssetId +
////					": NBBO (" + nbboQuote.bestSell + ", " + nbboQuote.bestBuy +
////					" ) better than market " + tradeMarketID + " (" + bestQuote.bestSell +
////					", " + bestQuote.bestBuy + ")");
//
//			// since NBBO is better, check other market for a matching quote
//			// in this simulation, the other market ID = m_numMarket - tradeMarketID
//			int altMarketID = 3 - tradeMarketID;
//			int altAssetID = getAssetID(altMarketID);
//
//			// note here that the input param assetID is assumed to be same as marketID
//			// also, this only works because there is only market trading this assetID
//			// BestQuote altQuote = findBestBuySell(altAssetID);
//			if (altQuote.bestBuy == ask && altQuote.bestSell == bid)
//			{
//			//  // there is a match! so trade in the other market
//			limitOrder = addExpiringBid(altAssetID, altMarketID, p, q, expiration);
//			bidSubmitted = true;
//			//addBid(altAssetID, altMarketID, p, q);
////			addMessage(AGENTTYPE + "::agentStrategy: asset " + altAssetID +
////					": bid (" + p + "," + q + ") submitted to market " + altMarketID);
//			} else {
//			  // no match, so no trade!
//			//  addMessage(AGENTTYPE + "::agentStrategy: asset " + altAssetID +
//			//    ": No bid submitted -- no match to NBBO (" +
//			//    nbboQuote.bestSell + ", " + nbboQuote.bestBuy + ") in market " + altMarketID +
//			//    " (" + altQuote.bestSell + ", " + altQuote.bestBuy + ")");
//			}
//
//		} else {
//			// current market's quote is better
//			limitOrder = addExpiringBid(tradeAssetId, tradeMarketID, p, q, expiration);
//			// update flag so won't submit any more bids after this one
//			bidSubmitted = true;
////			addMessage(AGENTTYPE + "::agentStrategy: asset " + tradeAssetId +
////					": bid (" + p + "," + q + ") submitted to market " + tradeMarketID);
//		}
//
//		return null;
//	}
}
