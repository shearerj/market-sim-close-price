package entity;

import event.*;
import activity.*;
import systemmanager.*;

import java.util.HashMap;

import java.util.Random;
import market.Quote;

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
public class ZIPAgent extends MMAgent {

    //private int margin;				// margin for limit order
    private int mainMarketID;	// ID of ZIP agent's primary market
    private int altMarketID;	// ID of ZIP agent's alternate market


    private double pvVar;			// variance from private value random process

    private int bidRange;				// range for limit order        

    private double c; 

    private int buyPrice_old;
    private int sellPrice_old;

    private Random rand_U_buy, rand_U_sell;
    private Random rand_Gamma, rand_Beta;
    private Random rand_R, rand_A;


    private double alpha_buy_prev, alpha_sell_prev;        
    private double delta_buy_prev, delta_sell_prev;
    private double tau_buy_prev, tau_sell_prev;

    private double mu_buy_next, mu_sell_next;

    private boolean DEBUG_BASIC_ENB = true;
    private boolean DEBUG_ADVANCED_ENB = true;

    private int bestSellPrice , bestBuyPrice;

    public Consts.SubmittedBidMarket submittedBidType;     



    public ZIPAgent(int agentID, SystemData d, AgentProperties p, Log l) {
        super(agentID, d, p, l);
        agentType = Consts.getAgentType(this.getClass().getSimpleName());
        params = p;
        arrivalTime = new TimeStamp(0);
        pvVar = this.data.privateValueVar;
        bidRange = this.data.bidRange;                               
        privateValue = Math.max(0, this.data.nextPrivateValue() + 
                        (int) Math.round(getNormalRV(0, pvVar)) * Consts.SCALING_FACTOR);

        // Choose market ID based on whether agentID is even or odd
        if (agentID % 2 == 0) {
                mainMarketID = data.getMarketIDs().get(0);
                altMarketID = data.getMarketIDs().get(1);
        } else {
                mainMarketID = data.getMarketIDs().get(1);
                altMarketID = data.getMarketIDs().get(0);
        }

        c = Double.parseDouble(params.get("c_StepSize"));

        buyPrice_old = -1;
        sellPrice_old = -1;                


        rand_U_buy = new Random();
        rand_U_sell = new Random();
        rand_Gamma = new Random();
        rand_Beta = new Random();
        rand_A = new Random();
        rand_R = new Random();

        alpha_buy_prev = 0;

        if(DEBUG_BASIC_ENB || DEBUG_ADVANCED_ENB){
            System.out.println("ZIP Agent ["+this.getID()+"] init. in Market "+mainMarketID);
            System.out.println("Private Value = "+privateValue);
            System.out.println("C = "+c);
        }
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
            System.out.println("ZIP Agent ["+this.getID()+"] is in ("+q+") mode");
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
        int bestSellMarketID = mainMarketID; //Submit to the Main Mrkt by default
        bestSellPrice = -1;                
        bestBuyPrice = -1;                                                
        submittedBidType = Consts.SubmittedBidMarket.MAIN;// default is submitting to main market

        //if (q > 0) {
        //Check if valid markets have been initialized
        if( lastNBBOQuote.bestAsk != -1 || mainMarketQuote.lastAskPrice.getPrice() == -1 &&
                lastNBBOQuote.bestAsk != -1) {

            if(DEBUG_BASIC_ENB){
                System.out.println("lastNBBOQuote.bestAsk = "+lastNBBOQuote.bestAsk);
                System.out.println("mainMarketQuote.lastAskPrice = "+mainMarketQuote.lastAskPrice.getPrice());
            }

            //Get highest transacting ask price

            if (lastNBBOQuote.bestAsk > mainMarketQuote.lastAskPrice.getPrice()){
                //^Do other markets have better asking prices according to the NBBO
                if(DEBUG_BASIC_ENB)
                    System.out.println("Alt. Market has a higher asking price");
                nbboBetter_sell = true;
                bestSellPrice = lastNBBOQuote.bestAsk;
                bestSellMarketID = lastNBBOQuote.bestAskMarket;//Highest selling price
                if(bestSellMarketID == altMarketID)
                    submittedBidType = Consts.SubmittedBidMarket.ALTERNATE;
            } else {
                if(DEBUG_BASIC_ENB)
                    System.out.println("Main Market has a higher asking price");

                bestSellPrice = mainMarketQuote.lastAskPrice.getPrice();
            }
        } else {//In case the market have not been initialized, use a random price.
            bestSellPrice = (int) Math.max(0, ((this.privateValue) + rand.nextDouble()*bidRange));
            if(DEBUG_BASIC_ENB){
                System.out.println("Markets have not been initialized");                            
            }
        }
        //} else {
        if( lastNBBOQuote.bestBid != -1 || mainMarketQuote.lastBidPrice.getPrice() == -1 &&
                lastNBBOQuote.bestBid != -1) {

            if(DEBUG_BASIC_ENB){
                System.out.println("lastNBBOQuote.bestBid = "+lastNBBOQuote.bestBid);
                System.out.println("mainMarketQuote.lastBidPrice = "+mainMarketQuote.lastBidPrice.getPrice());
            }

            if (lastNBBOQuote.bestBid < mainMarketQuote.lastBidPrice.getPrice()) {
                // don't need lastNBBOQuote.bestBid != -1 due to first condition, will always > -1
                if(DEBUG_BASIC_ENB)
                    System.out.println("Alt. Market has a lower bid price");
                nbboBetter_buy = true;
                bestBuyPrice = lastNBBOQuote.bestBid;
                bestBuyMarketID = lastNBBOQuote.bestBidMarket;//Lowest buying price                                
                if(bestBuyMarketID == altMarketID)
                    submittedBidType = Consts.SubmittedBidMarket.ALTERNATE;

            } else {
                if(DEBUG_BASIC_ENB)
                    System.out.println("Main Market has a lower bid price");
                bestBuyPrice = mainMarketQuote.lastBidPrice.getPrice();
            }
        } else {//In case the market have not been initialized, use a random price.                        
            bestBuyPrice = (int) Math.max(0, ((this.privateValue - 2*bidRange) + rand.nextDouble()*2*bidRange)); 
            if(DEBUG_BASIC_ENB){
                System.out.println("Markets have not been initialized");                            
            }
        }

        p_new_buy = getP_New(-1, buyPrice_old);
        buyPrice_old = p_new_buy;

        p_new_sell = getP_New(+1, sellPrice_old);
        sellPrice_old = p_new_sell;


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

            actMap.appendActivityHashMap(addBid(data.markets.get(bestSellMarketID), p_new_sell, -q, ts));

            log.log(Log.INFO, ts.toString() + " | " + this.toString() + " " + agentType + 
                    "::agentStrategy: " + "+(" + p_new_sell + "," + (-q) + ") to " + 
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

            actMap.appendActivityHashMap(addBid(data.markets.get(bestBuyMarketID), p_new_buy, -q, ts));

            log.log(Log.INFO, ts.toString() + " | " + this.toString() + " " + agentType + 
                    "::agentStrategy: " + "+(" + p_new_buy + "," + (-q) + ") to " + 
                    data.getMarket(bestBuyMarketID));
        }


        if(DEBUG_ADVANCED_ENB || DEBUG_BASIC_ENB){
            System.out.println("Current Profit = "+this.getRealizedProfit());
            System.out.println("\n");
        }



        // Insert events for the agent to sleep, then wake up again at timestamp tsNew
        int sleepTime = Integer.parseInt(params.get("sleepTime"));
        double sleepVar = Double.parseDouble(params.get("sleepVar"));
        TimeStamp tsNew = ts.sum(new TimeStamp(getRandSleepTime(sleepTime, sleepVar)));
        actMap.insertActivity(Consts.MARKETMAKER_PRIORITY, new UpdateAllQuotes(this, tsNew));
        actMap.insertActivity(Consts.MARKETMAKER_PRIORITY, new AgentStrategy(this, tsNew));
        return actMap;

    }


    private void updateCoeffs(){
        calculateTauNext(bestBuyPrice, bestSellPrice);
        calculateDeltaNext();
        calculateAlphaNext();
        calculateMuNext();
    }

    private double getMu(int q, int p_old){
        double mu;
        if(p_old == -1){
            mu_sell_next = getU(rand_U_sell);
            mu_buy_next = getU(rand_U_buy)*(-1);
        }
        if(q > 0){
            mu = mu_sell_next;
        } else {
            mu = mu_buy_next;
        }
        return mu;
    }

    private void calculateMuNext(){            
        mu_sell_next = (sellPrice_old * alpha_sell_prev)/privateValue + 1; 
        //TODO - Clarify if ^ is (p_old * getAlpha())/(privateValue - 1)

        mu_buy_next = (buyPrice_old * alpha_buy_prev)/privateValue - 1; 
    }

    private void calculateAlphaNext(){
        double gamma = getGamma();            

        alpha_sell_prev = gamma*alpha_sell_prev + (1.0 - gamma)*delta_sell_prev; 

        alpha_buy_prev = gamma*alpha_buy_prev + (1.0 - gamma)*delta_buy_prev; 
    }

    private void calculateDeltaNext(){
        double beta = getBeta();
        delta_sell_prev = beta*(tau_sell_prev - sellPrice_old);
        delta_buy_prev = beta*(tau_buy_prev - buyPrice_old);
    }

    private void calculateTauNext(int bestBuy, int bestSell){
        if(rand.nextBoolean())
            tau_sell_prev = bestSell;
        else
            tau_sell_prev = getR_inc()*bestSell + getA_inc();

        if(rand.nextBoolean())
            tau_buy_prev = bestBuy;
        else
            tau_buy_prev = (getR_inc() - c)*bestSell + getA_inc() - c;
    }

    /**
     * Returns the values for increase best price by default. To decrease best
     * price, simply subtract 'c'.
     * @return U[1, 1+c]
     */
    private double getR_inc(){
        return 1.0 + rand_R.nextDouble()*c;
    }

    /**
     * Returns the values for increase best price by default. To decrease best
     * price, simply subtract 'c'.
     * @return U[0, c]
     */
    private double getA_inc(){
        return rand_R.nextDouble()*c;
    }

    private double getBeta(){
        return (rand_Beta.nextDouble()*0.4) + 0.1;
    }

    private double getGamma(){
        return rand_Gamma.nextDouble()*0.1;
    }

    /**
     * Returns a random double from a Unifrom distribution b/w 0.35 and 0.05
     * @param randU Object of class Random - allows for buying and selling to
     * have independent distributions.
     * @return double U[0.05, 0.35]
     */
    private double getU(Random randU){         
        return (randU.nextDouble()*0.3 + 0.05);
    }

    private int getP_New(int q, int p_old){
        double p_new_d = privateValue * (1 + getMu(q, p_old));
        return (int) p_new_d;
    }
}
