package entity.agent;

import iterators.PoissonArrival;

import java.util.Collection;
import java.util.Iterator;
import java.util.Random;

import systemmanager.Scheduler;
import utils.Rands;

import com.google.common.collect.Iterators;

import data.AgentProperties;
import data.FundamentalValue;
import entity.infoproc.SIP;
import entity.market.Market;
import event.TimeStamp;

public class AgentFactory {

	protected final Scheduler scheduler;
	protected final Random rand;
	protected final FundamentalValue fundamental;
	protected final SIP sip;
	protected final Collection<Market> markets;
	protected final Iterator<TimeStamp> arrivalProcess;
	protected final Iterator<Market> marketAssignment;

	public AgentFactory(Scheduler scheduler, FundamentalValue fundamental,
			SIP sip, Iterator<TimeStamp> arrivalProcess,
			Collection<Market> markets, Iterator<Market> marketProcess,
			Random rand) {
		
		this.scheduler = scheduler;
		this.rand = rand;
		this.fundamental = fundamental;
		this.sip = sip;
		this.markets = markets;
		this.arrivalProcess = arrivalProcess;
		this.marketAssignment = marketProcess;
	}

	/**
	 * SMAgent factory with Poisson arrivals and round robin market selection.
	 * 
	 * @param fundamental
	 * @param sip
	 * @param markets
	 * @param arrivalRate
	 * @param rand
	 */
	public AgentFactory(Scheduler scheduler, FundamentalValue fundamental,
			SIP sip, Collection<Market> markets, double arrivalRate, Random rand) {
		
		this(scheduler, fundamental, sip, new PoissonArrival(
				TimeStamp.create((long) Math.ceil(Rands.nextExponential(rand, arrivalRate))),
				arrivalRate, new Random(rand.nextLong())), markets,
				Iterators.cycle(markets), rand);
	}

	// All Agents should advance rand, marketAssignment, and arrivlProcess
	public Agent createAgent(AgentProperties props) {
		switch (props.getAgentType()) {
		case AA:
			return new AAAgent(scheduler, arrivalProcess.next(), fundamental,
					sip, marketAssignment.next(), new Random(rand.nextLong()),
					props);
		case ZIP:
			return new ZIPAgent(scheduler, arrivalProcess.next(), fundamental,
					sip, marketAssignment.next(), new Random(rand.nextLong()),
					props);
		case ZIR:
			return new ZIRAgent(scheduler, arrivalProcess.next(), fundamental,
					sip, marketAssignment.next(), new Random(rand.nextLong()),
					props);
		case ZI:
			return new ZIAgent(scheduler, arrivalProcess.next(), fundamental,
					sip, marketAssignment.next(), new Random(rand.nextLong()),
					props);
		case MARKETDATA:
			return new MarketDataAgent(scheduler, fundamental, sip,
					marketAssignment.next(), new Random(rand.nextLong()), props);
		case BASICMM:
			arrivalProcess.next();
			return new BasicMarketMaker(scheduler, fundamental, sip,
					marketAssignment.next(), new Random(rand.nextLong()), props);
		case MAMM:
			arrivalProcess.next();
			return new MAMarketMaker(scheduler, fundamental, sip,
					marketAssignment.next(), new Random(rand.nextLong()),
					props);
		case WMAMM:
			arrivalProcess.next();
			return new WMAMarketMaker(scheduler, fundamental, sip,
					marketAssignment.next(), new Random(rand.nextLong()),
					props);
		case LA:
			arrivalProcess.next();
			marketAssignment.next();
			return new LAAgent(scheduler, fundamental, sip, markets, new Random(
					rand.nextLong()), props);
		case NOOP:
			arrivalProcess.next();
			marketAssignment.next();
			return new NoOpAgent(scheduler, fundamental, sip, new Random(rand.nextLong()),
					props);

		default:
			throw new IllegalArgumentException("Can't create AgentType: "
					+ props.getAgentType());
		}
	}

}