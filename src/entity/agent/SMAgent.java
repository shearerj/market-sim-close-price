package entity.agent;

import static logger.Logger.log;
import static logger.Logger.Level.INFO;

import java.util.Collection;

import data.FundamentalValue;

import utils.Rands;
import activity.Activity;
import entity.infoproc.SIP;
import entity.infoproc.SMIP;
import entity.market.Market;
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

	private static final long serialVersionUID = 3156640550886695881L;
	
	// TODO Only store market ip and submit orders through it...?
	protected final Market primaryMarket;
	protected final SMIP marketIP;

	public SMAgent(TimeStamp arrivalTime, FundamentalValue fundamental, SIP sip,
			Market market, PrivateValue pv, Rands rand, int tickSize) {
		super(arrivalTime, fundamental, sip, pv, rand, tickSize);
		this.primaryMarket = market;
		this.marketIP = market.getSMIP();
	}

	@Override
	public Collection<? extends Activity> agentArrival(TimeStamp currentTime) {
		log(INFO, this + "->" + primaryMarket);
		return super.agentArrival(currentTime);
	}

}
