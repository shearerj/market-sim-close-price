package entity.agent;

import java.util.Random;

import systemmanager.Simulation;
import data.Props;
import entity.agent.position.PrivateValues;
import event.TimeStamp;

public class NoOpAgent extends Agent {
	
	private static final long serialVersionUID = -7232513254416667984L;

	protected NoOpAgent(Simulation sim, Random rand, Props props) {
		super(sim, PrivateValues.zero(), TimeStamp.ZERO, rand, props);
	}

	public static NoOpAgent create(Simulation sim, Random rand, Props props) {
		return new NoOpAgent(sim, rand, props);
	}

	@Override public void agentStrategy() { }
	
}
