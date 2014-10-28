package entity.agent;

import static com.google.common.base.Preconditions.checkNotNull;
import static logger.Log.Level.INFO;

import java.util.Iterator;
import java.util.Random;

import systemmanager.Simulation;
import data.Props;
import entity.agent.position.PrivateValue;
import entity.market.Market;
import event.TimeStamp;

public abstract class ReentryAgent extends SMAgent {

	private static final long serialVersionUID = 4722377972197300345L;

	protected Iterator<TimeStamp> reentry; // wait times between reentry
	protected boolean arrived;

	public ReentryAgent(Simulation sim, PrivateValue privateValue, TimeStamp arrivalTime, Market market, Random rand, 
			Iterator<TimeStamp> reentry, Props props) {
		super(sim, privateValue, arrivalTime, rand, market, props);
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
