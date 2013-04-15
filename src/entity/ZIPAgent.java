package entity;

import event.*;
import model.*;
import market.*;
import activity.*;
import systemmanager.*;

import java.util.HashMap;


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
public class ZIPAgent extends BackgroundAgent {

	//private int margin;				// margin for limit order
	private int mainMarketID;	// ID of ZIP agent's primary market
	private int altMarketID;	// ID of ZIP agent's alternate market

	private double pvVar;			// variance from private value random process
	private int bidRange;				// range for limit order        

	private double c_R; 
	private double c_A; 
	private int buyPrice_old;
	private int sellPrice_old;

	private ZIP_algorithm zipBuyer, zipSeller;

	private boolean DEBUG_BASIC_ENB = true;
	private boolean DEBUG_ADVANCED_ENB = true;

	private int bestSellPrice , bestBuyPrice;
	private double learningRate;
	private double gamma;

	//public Consts.SubmittedBidMarket submittedBidType;

	public ZIPAgent(int agentID, int modelID, SystemData d, ObjectProperties p, Log l) {
		super(agentID, modelID, d, p, l);
		agentType = Consts.getAgentType(this.getName());
		arrivalTime = new TimeStamp(0);
		pvVar = this.data.privateValueVar;
		bidRange = Integer.parseInt(params.get(ZIPAgent.BIDRANGE_KEY));
		int mktID = this.getMarketID();
		//        privateValue = Math.max(0, this.data.nextFundamentalValue() + (int) Math.round(getNormalRV(0, pvVar)));

		// set the alternate ID if the primary model is a two-market model
		mainMarketID = mktID;
		if (data.getModel(modelID) instanceof TwoMarket) {
			altMarketID = ((TwoMarket) data.getModel(modelID)).getAlternateMarket(mktID);
		} else {
			altMarketID = mainMarketID;
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
		int q = 1; //Buy/sell variable
		// 0.50% chance of either buying or selling
		if (rand.nextDouble() < 0.5)
			q = -q;
		}
		/*
		 * if q = +1 - We want to set our ask price (sell)
		 * if q = -1 - We want to set out bid price (buy)
		 */

		// Insert events for the agent to sleep, then wake up again at timestamp tsNew
		int sleepTime = Integer.parseInt(params.get("sleepTime"));
		double sleepVar = Double.parseDouble(params.get("sleepVar"));
		TimeStamp tsNew = ts.sum(new TimeStamp(getRandSleepTime(sleepTime, sleepVar)));
		actMap.insertActivity(Consts.MARKETMAKER_PRIORITY, new UpdateAllQuotes(this, tsNew));
		actMap.insertActivity(Consts.MARKETMAKER_PRIORITY, new AgentStrategy(this, tsNew));
		return actMap;

	}
}
