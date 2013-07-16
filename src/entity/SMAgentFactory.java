package entity;

import generators.Generator;
import generators.PoissonArrivalGenerator;
import generators.RoundRobinGenerator;

import model.MarketModel;

import systemmanager.Consts;
import utils.RandPlus;
import data.AgentProperties;
import event.TimeStamp;

public class SMAgentFactory {

	protected final RandPlus rand;
	protected final MarketModel model;
	protected final Generator<Integer> nextID;
	protected final Generator<TimeStamp> arrivalProcess;
	protected final Generator<Market> marketAssignment;

	public SMAgentFactory(MarketModel model, Generator<Integer> ids,
			Generator<TimeStamp> arrivalProcess,
			Generator<Market> marketProcess, RandPlus rand) {
		this.rand = rand;
		this.model = model;
		this.nextID = ids;
		this.arrivalProcess = arrivalProcess;
		this.marketAssignment = marketProcess;
	}

	/**
	 * SMAgent factory with Poisson arrivals and round robin market selection.
	 */
	public SMAgentFactory(MarketModel model, Generator<Integer> ids,
			long arrivalRate, RandPlus rand) {
		this(model, ids, new PoissonArrivalGenerator(Consts.START_TIME,
				arrivalRate, new RandPlus(rand.nextLong())),
				new RoundRobinGenerator<Market>(model.getMarkets()), rand);
	}

	public SMAgent createAgent(AgentProperties props) {
		switch (props.getAgentType()) {
		case AA:
			return new AAAgent(nextID.next(), arrivalProcess.next(), model,
					marketAssignment.next(), new RandPlus(rand.nextLong()),
					props);
		case ZIP:
			return new ZIPAgent(nextID.next(), arrivalProcess.next(), model,
					marketAssignment.next(), new RandPlus(rand.nextLong()),
					props);
		case ZIR:
			return new ZIRAgent(nextID.next(), arrivalProcess.next(), model,
					marketAssignment.next(), new RandPlus(rand.nextLong()),
					props);
		case ZI:
			return new ZIAgent(nextID.next(), arrivalProcess.next(), model,
					marketAssignment.next(), new RandPlus(rand.nextLong()),
					props);
		case BASICMM:
			// XXX Should this advance the arrival process, even though it's not used?
			return new BasicMarketMaker(nextID.next(), model,
					marketAssignment.next(), new RandPlus(rand.nextLong()),
					props);
		default:
			throw new IllegalArgumentException("Can't create AgentType: "
					+ props.getAgentType());
		}
	}

}
