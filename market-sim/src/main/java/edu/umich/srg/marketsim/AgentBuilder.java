package edu.umich.srg.marketsim;

import java.util.Collection;
import java.util.Map;
import java.util.Random;

import com.google.common.collect.ImmutableMap;

import edu.umich.srg.egtaonline.spec.Spec;
import edu.umich.srg.marketsim.agent.Agent;
import edu.umich.srg.marketsim.agent.NoOpAgent;
import edu.umich.srg.marketsim.agent.NoiseAgent;
import edu.umich.srg.marketsim.agent.ZIRAgent;
import edu.umich.srg.marketsim.fundamental.Fundamental;
import edu.umich.srg.marketsim.market.Market;

class AgentBuilder {
	
	static final Map<String, AgentCreator> nameMap = ImmutableMap.<String, AgentCreator> builder()
			.put("noise", NoiseAgent::createFromSpec)
			.put("noop", NoOpAgent::createFromSpec)
			.put("zir", ZIRAgent::createFromSpec)
			.build();

	static interface AgentCreator {

		Agent createAgent(Sim sim, Fundamental fundamental, Collection<Market> markets, Market market, Spec spec,
				Random rand);

	}
	
}
