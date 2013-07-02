package market;

import entity.Agent;
import entity.Market;
import event.TimeStamp;

public abstract class Bid {

	protected final int bidID;
	protected final Agent agent;
	protected final Market market;
	protected final TimeStamp submissionTime;
	
	public Bid(Agent agent, Market market, TimeStamp submissionTime) {
		this.agent = agent;
		this.market = market;
		this.submissionTime = submissionTime;
		this.bidID = this.hashCode();
	}

	public Bid(Bid other) {
		this.bidID = other.bidID;
		this.agent = other.agent;
		this.market = other.market;
		this.submissionTime = other.submissionTime;
	}
	
	public Agent getAgent() {
		return agent;
	}

	public Market getMarket() {
		return market;
	}
	
	public TimeStamp getSubmissionTime() {
		return submissionTime;
	}
	
	@Deprecated // TODO Call getAgent instead
	public int getAgentID() {
		return agent.getID();
	}

	/**
	 * accessor function for bidID
	 *
	 * @return bidID
	 */
	public int getBidID() {
		return bidID;
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
