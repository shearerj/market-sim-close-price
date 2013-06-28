package entity;

import java.util.List;

import data.AgentProperties;

import systemmanager.Consts.AgentType;
import utils.RandPlus;

public class SMAgentFactory {
	
	protected final RandPlus rand;
	protected final List<Market> markets;
	
	public SMAgentFactory(RandPlus rand, List<Market> markets) {
		this.rand = rand;
		this.markets = markets;
	}
	
	protected SMAgent createAgent(AgentType type, AgentProperties props) {
		switch (type) {
		case ZI:
			
		default:
			return null;
		}
	}

}
