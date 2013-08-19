package entity.agent;

import interators.ExpInterarrivals;

import java.util.Iterator;
import java.util.Random;

import com.google.common.collect.ImmutableList;

import data.FundamentalValue;

import activity.Activity;
import activity.AgentStrategy;
import entity.infoproc.SIP;
import entity.market.Market;
import event.TimeStamp;


/**
 * Establishes a common implementation for determining re-entry time
 * @author drhurd
 *
 */
public abstract class ReentryAgent extends BackgroundAgent {

	private static final long serialVersionUID = -4312261553207167428L;
	
	protected Iterator<TimeStamp> reentry; // re-entry times
	
	public ReentryAgent(TimeStamp arrivalTime, FundamentalValue fundamental, SIP sip,
			Market market, PrivateValue pv, Random rand,
			Iterator<TimeStamp> reentry, int tickSize) {
		super(arrivalTime, fundamental, sip, market, pv, rand, tickSize);
	
		this.reentry = reentry;
	}
	
	/**
	 * Shortcut constructor for exponential interarrivals (e.g. poisson reentries)
	 */
	public ReentryAgent(TimeStamp arrivalTime, FundamentalValue fundamental, SIP sip,
			Market market, PrivateValue pv, Random rand, double reentryRate,
			int tickSize) {
		this(arrivalTime, fundamental, sip, market, pv, rand,
				new ExpInterarrivals(reentryRate, rand),
				tickSize);
	}

	@Override
	public Iterable<? extends Activity> agentStrategy(TimeStamp currentTime) {
		TimeStamp nextStrategy = currentTime.plus(reentry.next());
		return ImmutableList.of(new AgentStrategy(this, nextStrategy));
	}

}
