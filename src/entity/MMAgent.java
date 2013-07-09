package entity;

import static logger.Logger.log;
import static logger.Logger.Level.INFO;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

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
			PrivateValue pv, int sleepTime, double sleepVar, RandPlus rand, SIP sip) {
		super(agentID, arrivalTime, model, pv, rand, sip);
		this.sleepTime = sleepTime;
		this.sleepVar = sleepVar;
	}

	/**
	 * Agent arrives in a single market.
	 */
	public Collection<Activity> agentArrival(TimeStamp ts) {

		StringBuilder sb = new StringBuilder();
		for (Integer id : this.getModel().getMarketIDs()) {
			Market mkt = data.markets.get(id);
			this.enterMarket(mkt, ts);
			sb.append(mkt).append(",");
		}
		log(INFO,
				ts.toString() + " | " + this + "->"
						+ sb.substring(0, sb.length() - 1));

		// Insert agent strategy call once it has arrived in the market
		Collection<Activity> actMap = new ArrayList<Activity>();
		if (sleepTime == 0) {
			actMap.add(new AgentStrategy(this, Consts.INF_TIME));
		} else {
			actMap.add(new AgentStrategy(this, ts));
		}
		return actMap;
	}

	/**
	 * Agent departs all markets, if it is active. //TODO fix later
	 * 
	 * @return Collection<Activity>
	 */
	public Collection<Activity> agentDeparture(TimeStamp ts) {
		for (Integer id : data.getMarketIDs()) {
			Market mkt = data.markets.get(id);

			mkt.agentIDs.remove(mkt.agentIDs.indexOf(this.id));
			mkt.buyers.remove(mkt.buyers.indexOf(this.id));
			mkt.sellers.remove(mkt.sellers.indexOf(this.id));
			mkt.removeBid(this.id, ts);
			this.exitMarket(id);
		}
		return Collections.emptyList();
	}

	/**
	 * Updates quotes for all markets.
	 * 
	 * @param ts
	 * @return
	 */
	public Collection<? extends Activity> updateAllQuotes(TimeStamp ts) {
		for (Integer id : data.getModel(modelID).getMarketIDs()) {
			Market mkt = data.getMarket(id);
			updateQuotes(mkt, ts);
		}
		return this.executeUpdateAllQuotes(ts);
	}
}
