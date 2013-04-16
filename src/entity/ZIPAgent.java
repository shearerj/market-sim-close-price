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
public class ZIPAgent extends SMAgent {

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

    public ZIPAgent(int agentID, int modelID, SystemData d, ObjectProperties p, Log l, int mktID) {
        super(agentID, modelID, d, p, l, mktID);
		agentType = Consts.getAgentType(this.getName());
        arrivalTime = new TimeStamp(0);
        pvVar = this.data.privateValueVar;
        bidRange = this.data.bidRange;                               
     
    }

    @Override
    public HashMap<String, Object> getObservation() {
            //Return prfit here as System.out.println();
        if(DEBUG_ADVANCED_ENB || DEBUG_BASIC_ENB)
            System.out.println("ZIP Agent ["+this.getID()+"] Final Profit = "+this.getRealizedProfit());
        return null;
    }

    @Override
    public ActivityHashMap agentStrategy(TimeStamp ts) {
        ActivityHashMap actMap = new ActivityHashMap();

        // Insert events for the agent to sleep, then wake up again at timestamp tsNew
        int sleepTime = Integer.parseInt(params.get("sleepTime"));
        double sleepVar = Double.parseDouble(params.get("sleepVar"));
        TimeStamp tsNew = ts.sum(new TimeStamp(getRandSleepTime(sleepTime, sleepVar)));
        actMap.insertActivity(Consts.MARKETMAKER_PRIORITY, new UpdateAllQuotes(this, tsNew));
        actMap.insertActivity(Consts.MARKETMAKER_PRIORITY, new AgentStrategy(this, tsNew));
        return actMap;

    }
}
