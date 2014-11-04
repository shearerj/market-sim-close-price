package entity.agent;

import java.util.Random;

import logger.Log;
import data.FundamentalValue;
import data.Props;
import data.Stats;
import entity.agent.position.PrivateValues;
import entity.sip.MarketInfo;
import event.Timeline;
import event.TimeStamp;

public class NoOpAgent extends Agent {
	
	protected NoOpAgent(int id, Stats stats, Timeline timeline, Log log, Random rand, MarketInfo sip, FundamentalValue fundamental,
			Props props) {
		super(id, stats, timeline, log, rand, sip, fundamental, PrivateValues.zero(), TimeStamp.ZERO, props);
	}

	public static NoOpAgent create(int id, Stats stats, Timeline timeline, Log log, Random rand, MarketInfo sip, FundamentalValue fundamental,
			Props props) {
		return new NoOpAgent(id, stats, timeline, log, rand, sip, fundamental, props);
	}

	@Override protected void agentStrategy() { }

	private static final long serialVersionUID = -7232513254416667984L;
	
}
