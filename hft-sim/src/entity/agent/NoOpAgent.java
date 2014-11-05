package entity.agent;

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
		super(id, stats, timeline, log, rand, sip, fundamental, PrivateValues.zero(), TimeStamp.ZERO, props);
	}

	public static NoOpAgent create(int id, Stats stats, Timeline timeline, Log log, Rand rand, MarketInfo sip, FundamentalValue fundamental,
			Props props) {
		return new NoOpAgent(id, stats, timeline, log, rand, sip, fundamental, props);
	}

	@Override protected void agentStrategy() { }

	private static final long serialVersionUID = -7232513254416667984L;
	
}
