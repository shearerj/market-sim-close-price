package entity.market;

import java.util.Iterator;
import java.util.Random;

import logger.Log;
import systemmanager.Consts.MarketType;
import systemmanager.Keys.ClearInterval;
import utils.Iterators2;
import utils.Rand;
import data.Props;
import data.Stats;
import entity.sip.MarketInfo;
import event.TimeStamp;
import event.Timeline;

public class MarketFactory {
	
	private final Iterator<Integer> ids;
	private final Stats stats;
	private final Timeline timeline;
	private final Log log;
	private final MarketInfo sip;
	private final Random rand;

	protected MarketFactory(Stats stats, Timeline timeline, Log log, Random rand, MarketInfo sip) {
		this.stats = stats;
		this.timeline = timeline;
		this.log = log;
		this.sip = sip;
		this.rand = rand;
		this.ids = Iterators2.counter();
	}
	
	public static MarketFactory create(Stats stats, Timeline timeline, Log log, Random rand, MarketInfo sip) {
		return new MarketFactory(stats, timeline, log, rand, sip);
	}

	public Market createMarket(MarketType type, Props props) {
		switch (type) {
		case CDA:
			return CDAMarket.create(ids.next(), stats, timeline, log, Rand.from(rand), sip, props);
		case CALL:
			if (props.get(ClearInterval.class).equals(TimeStamp.ZERO)) {
				return CDAMarket.create(ids.next(), stats, timeline, log, Rand.from(rand), sip, props);
			}

			return CallMarket.create(ids.next(), stats, timeline, log, Rand.from(rand), sip, props);
		default:
			throw new IllegalArgumentException("Can't create MarketType: " + type);
		}
	}

}
