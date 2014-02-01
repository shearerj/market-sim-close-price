package entity.agent;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Iterator;
import java.util.Random;

import activity.Activity;
import activity.AgentStrategy;

import com.google.common.collect.ImmutableList;

import data.FundamentalValue;
import entity.infoproc.SIP;
import entity.market.Market;
import event.TimeStamp;

public abstract class ReentryAgent extends SMAgent {

	private static final long serialVersionUID = 4722377972197300345L;

	protected Iterator<TimeStamp> reentry; // re-entry times
	
	public ReentryAgent(TimeStamp arrivalTime, FundamentalValue fundamental,
			SIP sip, Market market, Random rand, Iterator<TimeStamp> reentry,
			int tickSize) {
		super(arrivalTime, fundamental, sip, market, rand, tickSize);
		
		this.reentry = checkNotNull(reentry);
	}
	
	@Override
	public Iterable<? extends Activity> agentStrategy(TimeStamp currentTime) {
		TimeStamp waitTime = reentry.next();
		if (waitTime.equals(TimeStamp.INFINITE))
			return ImmutableList.of(new AgentStrategy(this, TimeStamp.INFINITE));
		
		TimeStamp nextStrategy = currentTime.plus(waitTime);
		return ImmutableList.of(new AgentStrategy(this, nextStrategy));
	}
}
