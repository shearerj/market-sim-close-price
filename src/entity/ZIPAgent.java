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
        privateValue = Math.max(0, this.data.nextPrivateValue() + 
                        (int) Math.round(getNormalRV(0, pvVar)) * Consts.SCALING_FACTOR);

	// set the alternate ID if the primary model is a two-market model
	mainMarketID = mktID;
	if (data.getModel(modelID) instanceof TwoMarket) {
		altMarketID = ((TwoMarket) data.getModel(modelID)).getAlternateMarket(mktID);
	} else {
		altMarketID = mainMarketID;
	}

        c_R = Double.parseDouble(params.get("c_R"));
        c_A = Double.parseDouble(params.get("c_A"));
        
        learningRate = Double.parseDouble(params.get("beta"));        
        learningRate = getNormalRV(learningRate, Double.parseDouble(params.get("betaVar")));
        if(learningRate <= 0)
            learningRate = 0.1;
        
               
        gamma = Double.parseDouble(params.get("gamma"));

        buyPrice_old = privateValue;
        sellPrice_old = privateValue;                
        
        
        if(DEBUG_BASIC_ENB || DEBUG_ADVANCED_ENB){
            System.out.println("ZIP Agent ["+this.getID()+"] init. in Market "+mainMarketID);
            System.out.println("Private Value = "+privateValue);
            System.out.println("c_R = "+c_R);
            System.out.println("c_A = "+c_A);
            System.out.println("beta = "+learningRate);
            System.out.println("gamma = "+gamma);
        }
        
        zipBuyer = new ZIP_algorithm(gamma, learningRate, c_A, c_R, true, privateValue, 0.0);
        zipSeller = new ZIP_algorithm(gamma, learningRate, c_A, c_R, false, privateValue, 0.0);        
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

        // gets best buy and sell offers (for all markets)
        Quote mainMarketQuote = data.getMarket(mainMarketID).quote(ts);


        //Find the transaction mode- is our agent buying or selling?
        int q = 1; //Buy/sell variable
        // 0.50% chance of either buying or selling
        if (rand.nextDouble() < 0.5)
                q = -q;

        if(DEBUG_ADVANCED_ENB || DEBUG_BASIC_ENB)
            System.out.println("\n");
        if(DEBUG_BASIC_ENB){
            String ZIP_mode = "SELL";
            if(q < 0)
                ZIP_mode = "BUY";
            System.out.println("ZIP Agent ["+this.getID()+"] is in "+ZIP_mode+" mode");
            System.out.println("Private Value = "+privateValue);
        }
        /*
         * if q = +1 - We want to set our ask price (sell)
         * if q = -1 - We want to set out bid price (buy)
         */

        int p_new_buy = 0; //New bid price for this time step 
        int p_new_sell = 0; //New bid price for this time step 


        //Find the best market for transaction adn the corresponding best price
        boolean nbboBetter_buy = false;
        boolean nbboBetter_sell = false;
        
        int bestBuyMarketID = mainMarketID; //Submit to the Main Mrkt by default
        int bestSellMarketID = mainMarketID;
        bestSellPrice = -1;                
        bestBuyPrice = -1;                                                
        //submittedBidType = Consts.SubmittedBidMarket.MAIN;// default is submitting to main market

        if(DEBUG_BASIC_ENB){
            System.out.println("lastNBBOQuote.bestAsk = "+lastNBBOQuote.bestAsk);
            System.out.println("mainMarketQuote.lastAskPrice = "+mainMarketQuote.lastAskPrice.getPrice());                        
        }        
        //Check if valid markets have been initialized
        if( mainMarketQuote.lastAskPrice.getPrice() != -1 && lastNBBOQuote.bestAsk != -1) {

            //Get highest transacting ask price

            if(lastNBBOQuote.bestAsk > mainMarketQuote.lastAskPrice.getPrice()){
                //^Do other markets have better asking prices according to the NBBO
                if(DEBUG_BASIC_ENB)
                    System.out.println("Alt. Market has a higher asking price");
                nbboBetter_sell = true;
                bestSellPrice = lastNBBOQuote.bestAsk;
                bestSellMarketID = lastNBBOQuote.bestAskMarket;//Highest selling price
//                if(bestSellMarketID == altMarketID)
//                    submittedBidType = Consts.SubmittedBidMarket.ALTERNATE;
            } else {
                if(DEBUG_BASIC_ENB)
                    System.out.println("Main Market has a higher asking price");

                bestSellPrice = mainMarketQuote.lastAskPrice.getPrice();
            }
            
        } else if(mainMarketQuote.lastAskPrice.getPrice() != -1){
            if(DEBUG_BASIC_ENB)
                    System.out.println("NBBO is uninitialized, using Main Market price");
            bestSellPrice = mainMarketQuote.lastAskPrice.getPrice();
            
        } else if(lastNBBOQuote.bestAsk != -1){
            if(DEBUG_BASIC_ENB)
                    System.out.println("Main Market is uninitialized, using NBBO price");
            nbboBetter_sell = true;
            bestSellPrice = lastNBBOQuote.bestAsk;
            bestSellMarketID = lastNBBOQuote.bestAskMarket;//Highest selling price
//            if(bestSellMarketID == altMarketID)
//                submittedBidType = Consts.SubmittedBidMarket.ALTERNATE;
            
        } else {//In case the market have not been initialized, use a random price.
            bestSellPrice = (int) Math.max(0, ((this.privateValue) + rand.nextDouble()*bidRange));
            if(DEBUG_BASIC_ENB){
                System.out.println("Both markets have not been initialized");    
                System.out.println("Random Best Sell Price = "+bestSellPrice);
            }
        }
        
        if(DEBUG_BASIC_ENB){            
            System.out.println("lastNBBOQuote.bestBid = "+lastNBBOQuote.bestBid);
            System.out.println("mainMarketQuote.lastBidPrice = "+mainMarketQuote.lastBidPrice.getPrice());
        }
        if( mainMarketQuote.lastBidPrice.getPrice() != -1 && lastNBBOQuote.bestBid != -1) {
            if (lastNBBOQuote.bestBid < mainMarketQuote.lastBidPrice.getPrice()) {
                // don't need lastNBBOQuote.bestBid != -1 due to first condition, will always > -1
                if(DEBUG_BASIC_ENB)
                    System.out.println("Alt. Market has a lower bid price");
                nbboBetter_buy = true;
                bestBuyPrice = lastNBBOQuote.bestBid;
                bestBuyMarketID = lastNBBOQuote.bestBidMarket;//Lowest buying price                                
//                if(bestBuyMarketID == altMarketID)
//                    submittedBidType = Consts.SubmittedBidMarket.ALTERNATE;

            } else {
                if(DEBUG_BASIC_ENB)
                    System.out.println("Main Market has a lower bid price");
                bestBuyPrice = mainMarketQuote.lastBidPrice.getPrice();
            }
        } else if( mainMarketQuote.lastBidPrice.getPrice() != -1 && lastNBBOQuote.bestBid == -1) {
            if(DEBUG_BASIC_ENB)
                    System.out.println("NBBO is not initialized, using Main Market bid price");
                bestBuyPrice = mainMarketQuote.lastBidPrice.getPrice();
        } else if( mainMarketQuote.lastBidPrice.getPrice() == -1 && lastNBBOQuote.bestBid != -1) {
            if(DEBUG_BASIC_ENB)
                    System.out.println("Main Market is not initialized, using NBBO bid price");                
                nbboBetter_buy = true;
                bestBuyPrice = lastNBBOQuote.bestBid;
                bestBuyMarketID = lastNBBOQuote.bestBidMarket;//Lowest buying price                                
//                if(bestBuyMarketID == altMarketID)
//                    submittedBidType = Consts.SubmittedBidMarket.ALTERNATE;
        }
        else {//In case both markets have not been initialized, use a random price.                        
            bestBuyPrice = (int) Math.max(0, ((this.privateValue - 2*bidRange) + rand.nextDouble()*2*bidRange)); 
            if(DEBUG_BASIC_ENB){
                System.out.println("Both markets have not been initialized");
                System.out.println("Random Best Buy Price = "+bestBuyPrice);
            }
        }
        
        if(DEBUG_BASIC_ENB){
            System.out.println("P_old_buy = "+buyPrice_old);
            System.out.println("P_old_sell = "+sellPrice_old);
        }
        
        int temp = bestSellPrice;
        
        double mu_buy = zipBuyer.update(buyPrice_old, bestBuyPrice);
        double temp_p = (double)privateValue*(1.0+mu_buy);
        p_new_buy =  (int) temp_p;
        if(p_new_buy == privateValue)
            p_new_buy -= tickSize; 
        
        double mu_sell = zipSeller.update(sellPrice_old, bestSellPrice);
        temp_p = (double)privateValue*((1.0+mu_sell));
        p_new_sell =  (int) temp_p;
        if(p_new_sell == privateValue)
            p_new_sell += tickSize;
        
           
        
        
        assert p_new_buy !=privateValue : "ERROR: Agent is trying to "
                + "transact at its private value : PV = "+privateValue+ 
                " bid transaction value = "+p_new_buy;

        assert p_new_sell !=privateValue : "ERROR: Agent is trying to "
                + "transact at its private value : PV = "+privateValue+ 
                " bid transaction value = "+p_new_sell;



        if(DEBUG_BASIC_ENB){                    
            System.out.println("P_new_buy = "+p_new_buy);
            System.out.println("P_new_sell = "+p_new_sell);
        }

        //Submit bid to the correct market
        if(q > 0){
            if(nbboBetter_sell) {
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

            actMap.appendActivityHashMap(submitBid(data.markets.get(bestSellMarketID), p_new_sell, q, ts));

            log.log(Log.INFO, ts.toString() + " | " + this.toString() + " " + agentType + 
                    "::agentStrategy: " + "+(" + p_new_sell + "," + (q) + ") to " + 
                    data.getMarket(bestSellMarketID));
        }
        else{
            if(nbboBetter_buy) {
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

            actMap.appendActivityHashMap(submitBid(data.markets.get(bestBuyMarketID), p_new_buy, q, ts));

            log.log(Log.INFO, ts.toString() + " | " + this.toString() + " " + agentType + 
                    "::agentStrategy: " + "+(" + p_new_buy + "," + (q) + ") to " + 
                    data.getMarket(bestBuyMarketID));
        }


        if(DEBUG_ADVANCED_ENB || DEBUG_BASIC_ENB){
            System.out.println("Current Profit = "+this.getRealizedProfit());            
        }

        buyPrice_old = p_new_buy;
        sellPrice_old = p_new_sell;        

        // Insert events for the agent to sleep, then wake up again at timestamp tsNew
        int sleepTime = Integer.parseInt(params.get("sleepTime"));
        double sleepVar = Double.parseDouble(params.get("sleepVar"));
        TimeStamp tsNew = ts.sum(new TimeStamp(getRandSleepTime(sleepTime, sleepVar)));
        actMap.insertActivity(Consts.MARKETMAKER_PRIORITY, new UpdateAllQuotes(this, tsNew));
        actMap.insertActivity(Consts.MARKETMAKER_PRIORITY, new AgentStrategy(this, tsNew));
        return actMap;

    }
}
