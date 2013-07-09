package entity;

import generators.Generator;

import model.MarketModel;

import utils.RandPlus;
import data.AgentProperties;
import event.TimeStamp;

public class SMAgentFactory {

	protected final RandPlus rand;
	protected final MarketModel model;
	protected final Generator<Integer> nextID;
	protected final Generator<TimeStamp> arrivalProcess;
	protected final Generator<Market> marketAssignment;

	public SMAgentFactory(MarketModel model,
			Generator<Integer> ids, Generator<TimeStamp> arrivalProcess,
			Generator<Market> marketProcess, RandPlus rand) {
		this.rand = rand;
		this.model = model;
		this.nextID = ids;
		this.arrivalProcess = arrivalProcess;
		this.marketAssignment = marketProcess;
	}

	public SMAgent createAgent(AgentProperties props) {
		switch (props.getAgentType()) {
		case AA:
			return null; // FIXME implement
		case ZIP:
			return null; // FIXME implement
		case ZIR:
			return null; // FIXME implement
		case ZI:
			return new ZIAgent(nextID.next(), arrivalProcess.next(), model,
					marketAssignment.next(), new RandPlus(rand.nextLong()), null,
					props); //FIXME add SIP
		case BASICMM:
			return null; // FIXME implement
		default:
			return null;
		}
	}

}
