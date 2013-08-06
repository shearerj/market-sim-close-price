package entity;

import static logger.Logger.log;
import static logger.Logger.Level.INFO;

import java.util.Collection;
import java.util.Collections;

import model.MarketModel;
import systemmanager.Consts;
import utils.RandPlus;
import activity.Activity;
import activity.AgentStrategy;
import entity.market.Market;
import entity.market.PrivateValue;
import event.TimeStamp;

/**
 * SMAGENT
 * 
 * Single market (SM) agent, whose agent strategy is executed only within one
 * market. This does not mean that it can only trade with its specified market;
 * it means that it only checks price quotes from its primary market.
 * 
 * An SMAgent is capable of seeing the quote from its own market with zero
 * delay. It also tracks to which market it has most recently submitted a bid,
 * as it is only permitted to submit to one market at a time.
 * 
 * ORDER ROUTING (REGULATION NMS):
 * 
 * The agent's order will be routed to the alternate market ONLY if both the
 * NBBO quote is better than the primary market's quote and the submitted bid
 * will transact immediately given the price in the alternate market. The only
 * difference in outcome occurs when the NBBO is out-of-date and the agent's
 * order is routed to the main market when the alternate market is actually
 * better.
 * 
 * @author ewah
 */
public abstract class SMAgent extends Agent {

	protected final Market primaryMarket;
	// market to which bid has been submitted
	protected Market marketSubmittedBid;
	protected final SMIP marketIP;

	public SMAgent(int agentID, TimeStamp arrivalTime, MarketModel model,
			Market market, PrivateValue pv, RandPlus rand, int tickSize) {
		super(agentID, arrivalTime, model, pv, rand, tickSize);
		this.primaryMarket = market;
		this.marketIP = market.getIPSM();
	}

	/**
	 * Agent arrives in a single market.
	 * 
	 * @param primaryMarket
	 * @param ts
	 * @return Collection<Activity>
	 */
	public Collection<? extends Activity> agentArrival(TimeStamp ts) {
		log(INFO,
				ts.toString() + " | " + this + "->" + primaryMarket);
		return Collections.singleton(new AgentStrategy(this, primaryMarket, Consts.INF_TIME));
	}

}
