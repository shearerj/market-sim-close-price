package entity.agent;

import static com.google.common.base.Preconditions.checkNotNull;
import static logger.Log.Level.INFO;

import java.util.Iterator;

import com.google.common.base.Function;
import com.google.common.collect.Iterators;

import logger.Log;
import utils.Iterators2;
import utils.Rand;
import data.FundamentalValue;
import data.Props;
import data.Stats;
import entity.agent.position.PrivateValue;
import entity.market.Market;
import entity.sip.MarketInfo;
import event.TimeStamp;
import event.Timeline;

public abstract class ReentryAgent extends SMAgent {

	private static final long serialVersionUID = 4722377972197300345L;

	protected final Iterator<TimeStamp> reentry; // wait times between reentry
	protected boolean arrived;

	public ReentryAgent(int id, Stats stats, Timeline timeline, Log log, Rand rand, MarketInfo sip, FundamentalValue fundamental,
			PrivateValue privateValue, TimeStamp arrivalTime, Market market, Iterator<TimeStamp> reentry, Props props) {
		super(id, stats, timeline, log, rand, sip, fundamental, privateValue, arrivalTime, market, props);
		this.reentry = checkNotNull(reentry);
		this.arrived = false;
	}
	
	@Override
	protected void agentStrategy() {
		if (arrived)
			log(INFO, "%s wake up.", this);
		else
			arrived = true;
		
		if (!reentry.hasNext()) // Empty iterator
			return;
		reenterIn(reentry.next());
	}

	public static Iterator<TimeStamp> exponentials(double rate, Rand rand) {
		return Iterators.transform(Iterators2.exponentials(rate, rand), new Function<Double, TimeStamp>(){
			@Override public TimeStamp apply(Double dub) { return TimeStamp.of((long) (double) dub); }
		});
	}
}
