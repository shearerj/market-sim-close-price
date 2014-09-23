package entity.agent;

import static com.google.common.base.Preconditions.checkNotNull;
import static logger.Log.Level.INFO;

import java.util.Iterator;
import java.util.Random;

import systemmanager.Simulation;
import data.Props;
import entity.market.Market;
import event.TimeStamp;

public abstract class ReentryAgent extends SMAgent {

	private static final long serialVersionUID = 4722377972197300345L;

	protected Iterator<TimeStamp> reentry; // wait times between reentry

	public ReentryAgent(Simulation sim, TimeStamp arrivalTime, Market market, Random rand, 
			Iterator<TimeStamp> reentry, Props props) {
		super(sim, arrivalTime, rand, market, props);
		this.reentry = checkNotNull(reentry);
	}
	
	@Override
	public void agentStrategy() {
		if (!currentTime().equals(arrivalTime))
			log(INFO, "%s wake up.", this);
		
		if (!reentry.hasNext()) // Empty iterator
			return;
		reenterIn(reentry.next());
	}
}
