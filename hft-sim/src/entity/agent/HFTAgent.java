package entity.agent;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import systemmanager.Simulation;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;

import data.Props;
import data.Stats;
import entity.agent.position.PrivateValues;
import entity.market.Market;
import entity.market.Market.MarketView;
import entity.market.Price;
import event.Activity;
import event.TimeStamp;

/**
 * Abstract class for high-frequency traders. Creates the necessary information
 * processors and links them to the appropriate markets.
 * 
 * @author ewah
 */
public abstract class HFTAgent extends MMAgent {

	private static final long serialVersionUID = -1483633963238206201L;

	protected HFTAgent(final Simulation sim, TimeStamp arrivalTime, Map<Market, TimeStamp> marketLatencies,
			Random rand, Props props) {
		super(sim, PrivateValues.zero(), arrivalTime, toViews(marketLatencies), rand, props);
		for (MarketView market : markets)
			market.notify(this);
	}
	
	@Override
	public HFTAgentView getView(TimeStamp latency) {
		return new HFTAgentView(latency);
	}
	
	protected void quoteUpdate(MarketView market) {
		agentStrategy();
	}

	public class HFTAgentView extends AgentView {
		protected HFTAgentView(TimeStamp latency) {
			super(latency);
		}
		
		public void quoteUpdate(final MarketView market) {
			sim.scheduleActivityIn(getLatency(), new Activity() {
				@Override public void execute() {
					HFTAgent.this.quoteUpdate(market);
				}
				@Override public String toString() { return "Quote Update"; }
			});
		}
		
	}
	
	private static Collection<MarketView> toViews(Map<Market, TimeStamp> marketLatencies) {
		Builder<MarketView> builder = ImmutableList.builder();
		for (Entry<Market, TimeStamp> marketLatency : marketLatencies.entrySet())
			builder.add(marketLatency.getKey().getView(marketLatency.getValue()));
		return builder.build();
	}

	@Override
	public void liquidateAtPrice(Price price) {
		super.liquidateAtPrice(price);
		postStat(Stats.CLASS_PROFIT + "hft", getProfit());
	}
	
}
