package entity.agent;

import static logger.Logger.log;
import static logger.Logger.Level.INFO;

import java.util.Collection;

import model.MarketModel;
import utils.RandPlus;
import activity.Activity;
import entity.market.Market;
import entity.market.PrivateValue;
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

	// XXX Should the MMAgent have its own copy of Markets, or should it just go to Model?
	public MMAgent(int agentID, TimeStamp arrivalTime, MarketModel model,
			PrivateValue pv, RandPlus rand, int tickSize) {
		super(agentID, arrivalTime, model, pv, rand, tickSize);
	}

	@Override
	public Collection<? extends Activity> agentArrival(TimeStamp currentTime) {
		StringBuilder sb = new StringBuilder();
		for (Market market : model.getMarkets())
			sb.append(market).append(",");
		log(INFO, this + "->" + sb.substring(0, sb.length() - 1));

		return super.agentArrival(currentTime);
	}

}
