package entity.agent;

import java.util.Collection;
import java.util.Collections;

import data.FundamentalValue;

import activity.Activity;
import activity.AgentStrategy;
import utils.RandPlus;
import entity.infoproc.SIP;
import entity.market.Market;
import event.TimeStamp;
import generator.ExponentialInterarrivalGenerator;
import generator.Generator;


/**
 * Establishes a common implementation for determining re-entry time
 * @author drhurd
 *
 */
public abstract class ReentryAgent extends BackgroundAgent {

	private static final long serialVersionUID = -4312261553207167428L;
	
	protected Generator<TimeStamp> reentry; // re-entry times
	
	public ReentryAgent(TimeStamp arrivalTime, FundamentalValue fundamental, SIP sip,
			Market market, PrivateValue pv, RandPlus rand,
			Generator<TimeStamp> reentry, int tickSize) {
		super(arrivalTime, fundamental, sip, market, pv, rand, tickSize);
	
		this.reentry = reentry;
	}
	
	/**
	 * Shortcut constructor for exponential interarrivals (e.g. poisson reentries)
	 */
	public ReentryAgent(TimeStamp arrivalTime, FundamentalValue fundamental, SIP sip,
			Market market, PrivateValue pv, RandPlus rand, double reentryRate,
			int tickSize) {
		this(arrivalTime, fundamental, sip, market, pv, rand,
				new ExponentialInterarrivalGenerator(reentryRate, rand),
				tickSize);
	}

	@Override
	public Collection<? extends Activity> agentStrategy(TimeStamp currentTime) {
		TimeStamp nextStrategy = currentTime.plus(reentry.next());
		return Collections.singleton(new AgentStrategy(this, nextStrategy));
	}

}
