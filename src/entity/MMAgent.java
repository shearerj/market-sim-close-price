package entity;

import static logger.Logger.log;
import static logger.Logger.Level.INFO;

import java.util.ArrayList;
import java.util.Collection;

import market.PrivateValue;
import model.MarketModel;
import systemmanager.Consts;
import utils.RandPlus;
import activity.Activity;
import activity.AgentStrategy;
import event.TimeStamp;

/**
 * MMAGENT
 * 
 * Multi-market agent. An MMAgent arrives in all markets in a model, and its
 * strategy is executed across multiple markets.
 * 
 * An MMAgent is capable of seeing the quotes in multiple markets with zero
 * delay. These agents also bypass Regulation NMS restrictions as they have
 * access to private data feeds, enabling them to compute their own version of
 * the NBBO.
 * 
 * @author ewah
 */
public abstract class MMAgent extends Agent {

	// TODO Are these really indicative of an MMAgent? Should the MMAgent have
	// its own copy of Markets, or should it just go to Model?
	protected final int sleepTime;
	protected final double sleepVar;

	public MMAgent(int agentID, TimeStamp arrivalTime, MarketModel model,
			PrivateValue pv, int sleepTime, double sleepVar, RandPlus rand) {
		super(agentID, arrivalTime, model, pv, rand);
		this.sleepTime = sleepTime;
		this.sleepVar = sleepVar;
	}

	/**
	 * Agent arrives in a single market.
	 */
	public Collection<Activity> agentArrival(TimeStamp currentTime) {

		StringBuilder sb = new StringBuilder();
		for (Market market : model.getMarkets())
			sb.append(market).append(",");
		log(INFO,
				currentTime.toString() + " | " + this + "->"
						+ sb.substring(0, sb.length() - 1));

		// Insert agent strategy call once it has arrived in the market
		// FIXME I think this needs to be changed bassed off of the new event
		// manager
		Collection<Activity> actMap = new ArrayList<Activity>();
		if (sleepTime == 0) {
			actMap.add(new AgentStrategy(this, Consts.INF_TIME));
		} else {
			actMap.add(new AgentStrategy(this, currentTime));
		}
		return actMap;
	}

}
