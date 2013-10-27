package entity.agent;

import iterators.PoissonArrival;

import java.util.Collection;
import java.util.Iterator;
import java.util.Random;

import utils.Rands;

import com.google.common.collect.Iterators;

import data.AgentProperties;
import data.FundamentalValue;
import entity.infoproc.SIP;
import entity.market.Market;
import event.TimeStamp;

public class AgentFactory {

	protected final Random rand;
	protected final FundamentalValue fundamental;
	protected final SIP sip;
	protected final Collection<Market> markets;
	protected final Iterator<TimeStamp> arrivalProcess;
	protected final Iterator<Market> marketAssignment;

	public AgentFactory(FundamentalValue fundamental, SIP sip,
			Iterator<TimeStamp> arrivalProcess, Collection<Market> markets,
			Iterator<Market> marketProcess, Random rand) {
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
	public AgentFactory(FundamentalValue fundamental, SIP sip,
			Collection<Market> markets, double arrivalRate, Random rand) {
		this(fundamental,
				sip,
				new PoissonArrival(new TimeStamp((long)
						Math.ceil(Rands.nextExponential(rand, arrivalRate))),
				arrivalRate,
				new Random(rand.nextLong())),
				markets, 
				Iterators.cycle(markets),
				rand);
	}

	// XXX Not all agents advance all of the parameters like the market or the arrival process. One
	// just has to be sure that this happens appropriately if one factory is creating several
	// different classes of agents.
	public Agent createAgent(AgentProperties props) {
		switch (props.getAgentType()) {
		case AA:
			return new AAAgent(arrivalProcess.next(), fundamental, sip,
					marketAssignment.next(), new Random(rand.nextLong()),
					props);
		case ZIP:
			return new ZIPAgent(arrivalProcess.next(), fundamental, sip,
					marketAssignment.next(), new Random(rand.nextLong()),
					props);
		case ZIR:
			return new ZIRAgent(arrivalProcess.next(), fundamental, sip,
					marketAssignment.next(), new Random(rand.nextLong()),
					props);
		case ZI:
			return new ZIAgent(arrivalProcess.next(), fundamental, sip,
					marketAssignment.next(), new Random(rand.nextLong()),
					props);
		case BASICMM:
			return new BasicMarketMaker(fundamental, sip,
					marketAssignment.next(), new Random(rand.nextLong()),
					props);
		case LA:
			return new LAAgent(markets, fundamental, sip, new Random(
					rand.nextLong()), props);
		case DA:
			return new DummyAgent(fundamental,sip,marketAssignment.next());

		case ODA:
			return new OrderDataAgent(fundamental,sip,marketAssignment.next(), new Random(rand.nextLong()),  );

		default:
			throw new IllegalArgumentException("Can't create AgentType: "
					+ props.getAgentType());
		}
	}

}
