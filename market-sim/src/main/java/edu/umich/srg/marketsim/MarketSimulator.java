package edu.umich.srg.marketsim;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import com.google.common.collect.Maps;
import com.google.gson.JsonObject;

import edu.umich.srg.egtaonline.Log;
import edu.umich.srg.marketsim.agent.Agent;
import edu.umich.srg.marketsim.event.EventQueue;
import edu.umich.srg.marketsim.fundamental.Fundamental;
import edu.umich.srg.marketsim.market.Market;
import edu.umich.srg.marketsim.market.Market.AgentInfo;

public class MarketSimulator implements Sim {

	private final Log log;
	private final Collection<Market> markets;
	private final Collection<Agent> agents;
	private final Fundamental fundamental;
	private final Features features;
	private final EventQueue eventQueue;
	
	// FIXME Still need fundamental and SIP and way to calculate payoff and global statistics
	
	private MarketSimulator(Fundamental fundamental, Log log, Random rand) {
		this.fundamental = fundamental;
		this.features = new Features();
		this.log = log;
		this.markets = new ArrayList<>();
		this.agents = new ArrayList<>();
		this.eventQueue = new EventQueue(rand);
	}
	
	public static MarketSimulator create(Fundamental fundamental, Log log, Random rand) {
		return new MarketSimulator(fundamental, log, rand);
	}
	
	public void initialize() {
		for (Agent agent : agents)
			agent.initilaize();
	}
	
	public void executeUntil(TimeStamp finalTime) {
		eventQueue.executeUntil(finalTime);
	}
	
	public Market addMarket(Market market) {
		markets.add(market);
		return market;
	}
	
	public void addAgent(Agent agent) {
		agents.add(agent);
	}
	
	public JsonObject computeFeatures() {
		return features.computeFeatures(this);
	}
	
	public Map<Agent, Double> getAgentPayoffs() {
		// Get total agent holdings and profit according to all markets
		Map<Agent, TempAgentInfo> payoffs = Maps.toMap(agents, a -> new TempAgentInfo());
		for (Market market : markets) {
			for (Entry<Agent, AgentInfo> e : market.getAgentInfo()) {
				TempAgentInfo info = payoffs.get(e.getKey());
				info.holdings += e.getValue().getHoldings();
				info.profit += e.getValue().getProfit();
			}
		}
		
		// Get current fundamental price
		double fundamentalValue = fundamental.getValueAt(getCurrentTime()).doubleValue();
		
		// Add everything up
		return Maps.transformEntries(payoffs, (agent, info) -> info.profit + info.holdings * fundamentalValue + agent.payoffForPosition(info.holdings));
	}

	@Override
	public void scheduleIn(TimeStamp delay, Runnable activity) {
		eventQueue.scheduleActivityIn(delay, activity);
	}

	@Override
	public TimeStamp getCurrentTime() {
		return eventQueue.getCurrentTime();
	}

	@Override
	public void error(String format, Object... parameters) {
		log.error(format, parameters);
	}

	@Override
	public void info(String format, Object... parameters) {
		log.info(format, parameters);
	}

	@Override
	public void debug(String format, Object... parameters) {
		log.debug(format, parameters);
	}
	
	@Override
	public void addFeature(String name, double value) {
		features.accept(name, value);
	}
	
	private static class TempAgentInfo {

		private double profit = 0;
		private int holdings = 0;
		
	}
	
}
