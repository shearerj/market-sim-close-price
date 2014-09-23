package data;

import java.io.Serializable;
import java.util.Map;

import entity.agent.Agent;

/**
 * A class that represents a player. It contains a role, strategy, and a
 * reference to the player.
 * 
 * It is accessed by getting a player observation. This calls the
 * player.getPayoff function which can be overridden to return the correct thing
 * for different agent types.
 * 
 * @author erik
 * 
 */
public class Player implements Serializable {
	
	private static final long serialVersionUID = 7233996258432288503L;
	
	protected final String descriptor;
	protected final Agent agent;

	public Player(String descriptor, Agent agent) {
		this.descriptor = descriptor;
		this.agent = agent;
	}

	public Agent getAgent() {
		return agent;
	}
	
	public String getDescriptor() {
		return descriptor;
	}
	
	public double getPayoff() {
		return agent.getPayoff();
	}
	
	public Map<String, Double> getFeatures() {
		return agent.getFeatures();
	}
}
