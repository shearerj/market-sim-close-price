package entity.agent;

import static logger.Logger.log;
import static logger.Logger.Level.INFO;

import java.util.Collection;

import data.FundamentalValue;

import utils.RandPlus;
import activity.Activity;
import entity.infoproc.SIP;
import entity.market.Market;
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

	private static final long serialVersionUID = 2297636044775909734L;
	
	protected final Collection<Market> markets; 
	
	// TODO Just keep the SMIPs and not the actual markets...
	public MMAgent(TimeStamp arrivalTime, Collection<Market> markets,
			FundamentalValue fundamental, SIP sip, PrivateValue pv,
			RandPlus rand, int tickSize) {
		super(arrivalTime, fundamental, sip, pv, rand, tickSize);
		this.markets = markets;
	}

	@Override
	public Collection<? extends Activity> agentArrival(TimeStamp currentTime) {
		StringBuilder sb = new StringBuilder();
		for (Market market : markets)
			sb.append(market).append(",");
		log(INFO, this + "->" + sb.substring(0, sb.length() - 1));

		return super.agentArrival(currentTime);
	}

}
