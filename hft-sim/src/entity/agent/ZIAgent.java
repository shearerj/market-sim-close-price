package entity.agent;

import java.util.Random;

import systemmanager.Keys.BackgroundReentryRate;
import systemmanager.Keys.MaxQty;
import systemmanager.Simulation;
import data.Props;
import entity.market.Market;
import event.TimeStamp;

/**
 * ZIAGENT
 * 
 * A zero-intelligence (ZI) agent.
 * 
 * This agent bases its private value on a stochastic process, the parameters of which are specified
 * at the beginning of the simulation by the spec file. The agent's private valuation is determined
 * by value of the random process at the time it enters, with some randomization added by using an
 * individual variance parameter. The private value is used to calculate the agent's surplus (and
 * thus the market's allocative efficiency).
 * 
 * This agent submits only ONE limit order during its lifetime.
 * 
 * NOTE: The limit order price is uniformly distributed over a range that is twice the size of
 * bidRange in either a positive or negative direction from the agent's private value.
 * 
 * @author ewah
 */
@Deprecated // Same as ZIR with 0 reentry rate and 1 max position
public class ZIAgent extends ZIRAgent {

	private static final long serialVersionUID = 1148707664467962927L;

	protected ZIAgent(Simulation sim, TimeStamp arrivalTime, Market market, Random rand, Props props) {
		super(sim, arrivalTime, market, rand, Props.withDefaults(props, MaxQty.class, 1, BackgroundReentryRate.class, 0d));
	}
	
	public static ZIAgent create(Simulation sim, TimeStamp arrivalTime, Market market, Random rand, Props props) {
		return new ZIAgent(sim, arrivalTime, market, rand, props);
	}
	
}
