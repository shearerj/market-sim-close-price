package data;

import static data.Observations.*;
import com.google.gson.JsonObject;

import entity.agent.Agent;

public class Player {
	
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
	
	public JsonObject toJson() {
		JsonObject observation = new JsonObject();
		observation.addProperty(ROLE, role);
		observation.addProperty(STRATEGY, strategy);
		// FIXME Get surplus instead of realized profit?
		observation.addProperty(PAYOFF, agent.getSurplus(0));
		return observation;
	}

}
