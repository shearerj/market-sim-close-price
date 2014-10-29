package entity.agent;

import static com.google.common.base.Preconditions.checkNotNull;
import static logger.Log.Level.INFO;

import java.util.Iterator;
import java.util.Random;

import logger.Log;
import data.FundamentalValue;
import data.Props;
import data.Stats;
import entity.agent.position.PrivateValue;
import entity.market.Market;
import entity.sip.MarketInfo;
import event.TimeLine;
import event.TimeStamp;

public abstract class ReentryAgent extends SMAgent {

	private static final long serialVersionUID = 4722377972197300345L;

	protected final Iterator<TimeStamp> reentry; // wait times between reentry
	protected boolean arrived;

	public ReentryAgent(int id, Stats stats, TimeLine timeline, Log log, Random rand, MarketInfo sip, FundamentalValue fundamental,
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
}
