package entity.agent;

import java.util.Collection;
import java.util.Iterator;

import logger.Log;
import utils.Rand;
import data.FundamentalValue;
import data.Props;
import data.Stats;
import entity.agent.position.PrivateValue;
import entity.market.Market.MarketView;
import entity.sip.MarketInfo;
import event.TimeStamp;
import event.Timeline;

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
	
	protected MMAgent(int id, Stats stats, Timeline timeline, Log log, Rand rand, MarketInfo sip, FundamentalValue fundamental,
			PrivateValue privateValue, Iterator<TimeStamp> arrivalIntervals, Collection<MarketView> markets, Props props) {
		super(id, stats, timeline, log, rand, sip, fundamental, privateValue, arrivalIntervals, props);
		this.markets = markets;
	}

}
