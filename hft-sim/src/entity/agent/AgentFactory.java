package entity.agent;

import java.util.Collection;
import java.util.Iterator;
import java.util.Random;

import logger.Log;
import systemmanager.Consts.AgentType;
import utils.Iterators2;

import com.google.common.base.Function;
import com.google.common.collect.Iterators;

import data.FundamentalValue;
import data.Props;
import data.Stats;
import entity.market.Market;
import entity.sip.MarketInfo;
import event.TimeLine;
import event.TimeStamp;

public class AgentFactory {

	private final Iterator<Integer> ids;
	private final Stats stats;
	private final TimeLine timeline;
	private final Log log;
	private final Random rand;
	private final FundamentalValue fundamental;
	private final MarketInfo sip;
	private final Collection<Market> markets;
	private final Iterator<Market> marketAssignment;

	private AgentFactory(Stats stats, TimeLine timeline, Log log, Random rand, MarketInfo sip, FundamentalValue fundamental,
			Collection<Market> markets, Iterator<Market> marketProcess) {
		this.stats = stats;
		this.timeline = timeline;
		this.log = log;
		this.rand = rand;
		this.fundamental = fundamental;
		this.sip = sip;
		this.markets = markets;
		this.marketAssignment = marketProcess;
		this.ids = Iterators2.counter();
	}
	
	/** Factory with round robin scheduling */
	public static AgentFactory create(Stats stats, TimeLine timeline, Log log, Random rand, MarketInfo sip, FundamentalValue fundamental,
			Collection<Market> markets) {
		return new AgentFactory(stats, timeline, log, rand, sip, fundamental, markets, Iterators.cycle(markets));
	}

	public Agent createAgent(AgentType type, Props props) {
		switch (type) {
		case AA:
			return AAAgent.create(ids.next(), stats, timeline, log, new Random(rand.nextLong()), sip, fundamental, marketAssignment.next(), props);
		case ZIP:
			return ZIPAgent.create(ids.next(), stats, timeline, log, new Random(rand.nextLong()), sip, fundamental, marketAssignment.next(), props);
		case ZIRP:
			return ZIRPAgent.create(ids.next(), stats, timeline, log, new Random(rand.nextLong()), sip, fundamental, marketAssignment.next(), props);
		case ZIR:
			return ZIRAgent.create(ids.next(), stats, timeline, log, new Random(rand.nextLong()), sip, fundamental, marketAssignment.next(), props);
		case MARKETDATA:
			return MarketDataAgent.create(ids.next(), stats, timeline, log, new Random(rand.nextLong()), sip, fundamental, marketAssignment.next(), props);
		case BASICMM:
			return BasicMarketMaker.create(ids.next(), stats, timeline, log, new Random(rand.nextLong()), sip, fundamental, marketAssignment.next(), props);
		case MAMM:
			return MAMarketMaker.create(ids.next(), stats, timeline, log, new Random(rand.nextLong()), sip, fundamental, marketAssignment.next(), props);
		case WMAMM:
			return WMAMarketMaker.create(ids.next(), stats, timeline, log, new Random(rand.nextLong()), sip, fundamental, marketAssignment.next(), props);
		case ADAPTIVEMM:
			return AdaptiveMarketMaker.create(ids.next(), stats, timeline, log, new Random(rand.nextLong()), sip, fundamental, marketAssignment.next(), props);
		case LA:
			return LAAgent.create(ids.next(), stats, timeline, log, new Random(rand.nextLong()), sip, fundamental, markets, props);
		case NOOP:
			marketAssignment.next();
			return NoOpAgent.create(ids.next(), stats, timeline, log, new Random(rand.nextLong()), sip, fundamental, props);

		default:
			throw new IllegalArgumentException("Can't create AgentType: " + type);
		}
	}

	// FIXME Move this to background agent? reentry agent?
	public static Iterator<TimeStamp> exponentials(double rate, Random rand) {
		return Iterators.transform(Iterators2.exponentials(rate, rand), new Function<Double, TimeStamp>(){
			@Override public TimeStamp apply(Double dub) { return TimeStamp.of((long) (double) dub); }
		});
	}
	
}
