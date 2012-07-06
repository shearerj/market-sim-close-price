package activity.market;

import event.TimeStamp;

public abstract class Order {

	public Integer bidID;
	protected Integer agentID;
	protected TimeStamp timestamp;  // processing time
	protected TimeStamp timeClosed;
	protected Integer marketID;
	
	public Integer expireTime;
	public Integer expireMode;
	
	
	public Bid() {
		// empty constructor
	}

	/**
     * accessor function for timestamp
     *
     * @return the bid's timestamp
     */
    public TimeStamp getTimeStamp() {
        return timestamp;
    }

    /**
     * accessor function for agentID
     *
     * @return the agentID of the bid
     */
    public int getAgentID() {
        return agentID.intValue();
    }

    public void setAgentID(int id) {
        agentID = new Integer(id);
    }

    /**
     * accessor function for bidID
     *
     * @return bidID
     */
    public int getBidID() {
        return bidID.intValue();
    }
    
    /**
     * accessor function for marketID
     * 
     * @return marketID
     */
    public int getMarketID() {
    	return marketID.intValue();
    }
    
	
//    // ABSTRACT FUNCTIONS
//    /**
//     * does this bid buy-dominate other (buy more q at all p)
//     *
//     * @param other bid to compare to
//     * @return 1/0/-1 based on dominate/non/dominated
//     */
//    public abstract int bDominates(Bid other);
//
//    /**
//     * does this bid sell-dominate other (sell more q at all p)
//     *
//     * @param other bid to compare to
//     * @return 1/0/-1 based on dominate/non/dominated
//     */
//    public abstract int sDominates(Bid other);
//
//    /**
//     * return the netoffer that this bid represents > or >= other
//     *
//     * @param other  min bid at which to evaluate this bid
//     * @param strict whether to compare based on price > or >=, 1=strict
//     * @return number of units this is an offer for at a price of quote
//     */
//    public abstract Bid netOffer(Bid other, int strict);
//
//    /**
//     * does this bid contain the other bid, i.e. buy/sell >= other at all p
//     *
//     * @param other bid to compare to
//     * @return true if containment is valid
//     */
//    public abstract boolean contains(Bid other);
//
//    /**
//     * does bid contain offers to sell
//     *
//     * @return true if contains sells
//     */
//    public abstract boolean containsSellOffers();
//
//    /**
//     * does bid contain offers to buy
//     *
//     * @return true if contains buys
//     */
//    public abstract boolean containsBuyOffers();
//
//    /**
//     * generate a string representing a quote (i.e. bid that would have to be
//     * beaten for btq)
//     *
//     * @return the string representation of a quote
//     */
//    public abstract String quoteString();
//
//    public abstract String allocString();
//
//    public abstract void parseBidString() throws BadBidStringException;
//
//    public abstract String sameBidArray(Bid other);

//    public abstract void addPoints(PQPoint[] points);
//
//    public abstract void addPoint(PQPoint p);
//
//    public abstract void addPoint(int q, Price p);
//
//    public abstract PQPoint [] getBidArray();
}
