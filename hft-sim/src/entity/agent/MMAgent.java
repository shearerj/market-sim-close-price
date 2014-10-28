package entity.agent;

import java.util.Collection;
import java.util.Random;

import systemmanager.Simulation;
import data.Props;
import entity.agent.position.PrivateValue;
import entity.market.Market.MarketView;
import event.TimeStamp;

/**
 * MMAGENT
 * 
 * Multi-market agent. An MMAgent arrives in all markets in a model, and its
 * strategy is executed across multiple markets.
 * 
 * An MMAgent is capable of seeing the quotes in multiple markets with zero
 * delay. These agents also bypass Regulation NMS restrictions as they have
 * access to private data feeds, enabling them to compute their own version of
 * the NBBO.
 * 
 * @author ewah
 */
public abstract class MMAgent extends Agent {

	private static final long serialVersionUID = 2297636044775909734L;
	
	protected final Collection<MarketView> markets; 
	
	protected MMAgent(Simulation sim, PrivateValue privateValue, TimeStamp arrivalTime, Collection<MarketView> markets,
			Random rand, Props props) {
		super(sim, privateValue, arrivalTime, rand, props);
		this.markets = markets;
	}

}
