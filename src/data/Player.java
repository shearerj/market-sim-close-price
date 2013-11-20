package data;

import java.io.Serializable;

import entity.agent.Agent;

public class Player implements Serializable {
	
	private static final long serialVersionUID = 7233996258432288503L;
	
	protected final String role;
	protected final String strategy;
	protected final Agent agent;

	public Player(String role, String strategy, Agent agent) {
		this.role = role;
		this.strategy = strategy;
		this.agent = agent;
	}

	public Agent getAgent() {
		return agent;
	}
	
	public PlayerObservation getObservation() {
		return new PlayerObservation(role, strategy, agent.getPayoff());
	}

}
