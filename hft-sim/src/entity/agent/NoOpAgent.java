package entity.agent;

import com.google.common.collect.ImmutableList;

import logger.Log;
import utils.Rand;
import data.FundamentalValue;
import data.Props;
import data.Stats;
import entity.agent.position.PrivateValues;
import entity.sip.MarketInfo;
import event.TimeStamp;
import event.Timeline;

public class NoOpAgent extends Agent {
	
	protected NoOpAgent(int id, Stats stats, Timeline timeline, Log log, Rand rand, MarketInfo sip, FundamentalValue fundamental,
			Props props) {
		super(id, stats, timeline, log, rand, sip, fundamental, PrivateValues.zero(), ImmutableList.<TimeStamp> of().iterator(), props);
		postStat(Stats.NUM_TRANS + getClass().getSimpleName().toLowerCase(), 0);
		postStat(Stats.NUM_TRANS_TOTAL, 0);
	}

	public static NoOpAgent create(int id, Stats stats, Timeline timeline, Log log, Rand rand, MarketInfo sip, FundamentalValue fundamental,
			Props props) {
		return new NoOpAgent(id, stats, timeline, log, rand, sip, fundamental, props);
	}

	@Override protected void agentStrategy() {
		throw new IllegalStateException("NoOp Agent should never enter");
	}

	private static final long serialVersionUID = -7232513254416667984L;
	
}
