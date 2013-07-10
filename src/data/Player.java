package data;

import entity.Agent;

public class Player {
	
	protected final String role;
	protected final String strategy;
	protected final Agent agent;

	public Player(String role, String strategy, Agent agent) {
		this.role = role;
		this.strategy = strategy;
		this.agent = agent;
	}

	public String getRole() {
		return role;
	}

	public String getStrategy() {
		return strategy;
	}

	public Agent getAgent() {
		return agent;
	}

}
