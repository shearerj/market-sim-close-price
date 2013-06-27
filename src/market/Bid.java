package market;

import entity.Agent;
import entity.Market;
import event.TimeStamp;

public abstract class Bid {

	private final Integer bidID;
	private final Agent agent;
	private final Market market;
	public TimeStamp timestamp;
	
	public Bid(Agent agent, Market market) {
		this.agent = agent;
		this.market = market;
		this.bidID = this.hashCode();
	}

	public Bid(Bid other) {
		this.bidID = other.bidID;
		this.agent = other.agent;
		this.market = other.market;
		this.timestamp = other.timestamp;
	}
	
	public Agent getAgent() {
		return agent;
	}

	public Market getMarket() {
		return market;
	}
	
	/**
	 * accessor function for agentID
	 *
	 * @return the agentID of the bid
	 */
	public int getAgentID() {
		return -1;
	}



	/**
	 * accessor function for bidID
	 *
	 * @return bidID
	 */
	public Integer getBidID() {
		return bidID.intValue();
	}


	// ABSTRACT FUNCTIONS
	/**
	 * does this bid buy-dominate other (buy more q at all p)
	 *
	 * @param other bid to compare to
	 * @return 1/0/-1 based on dominate/non/dominated
	 */
	public abstract int bDominates(Bid other);

	/**
	 * does this bid sell-dominate other (sell more q at all p)
	 *
	 * @param other bid to compare to
	 * @return 1/0/-1 based on dominate/non/dominated
	 */
	public abstract int sDominates(Bid other);

	/**
	 * return the netoffer that this bid represents > or >= other
	 *
	 * @param other  min bid at which to evaluate this bid
	 * @param strict whether to compare based on price > or >=, 1=strict
	 * @return number of units this is an offer for at a price of quote
	 */
	public abstract Bid netOffer(Bid other, int strict);

	/**
	 * does this bid contain the other bid, i.e. buy/sell >= other at all p
	 *
	 * @param other bid to compare to
	 * @return true if containment is valid
	 */
	public abstract boolean contains(Bid other);

	/**
	 * does bid contain offers to sell
	 *
	 * @return true if contains sells
	 */
	public abstract boolean containsSellOffers();

	/**
	 * does bid contain offers to buy
	 *
	 * @return true if contains buys
	 */
	public abstract boolean containsBuyOffers();

	//
	//    public abstract String allocString();
	//
	//    public abstract String sameBidArray(Bid other);
	//
	//    public abstract void addPoints(PQPoint[] points);
	//
	//    public abstract void addPoint(PQPoint p);
	//
	//    public abstract void addPoint(int q, Price p);
	//
	//    public abstract PQPoint [] getBidArray();
}
