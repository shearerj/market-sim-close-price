package entity;

import generators.Generator;

import java.util.Iterator;

import model.MarketModel;

import utils.RandPlus;
import data.AgentProperties;
import event.TimeStamp;

public class SMAgentFactory implements Iterator<SMAgent> {

	protected final RandPlus rand;
	protected final MarketModel model;
	protected final AgentProperties props;
	protected int nextID;
	protected final int finalID;
	protected final Generator<TimeStamp> arrivalProcess;
	protected final Generator<Market> marketAssignment;

	public SMAgentFactory(MarketModel model, AgentProperties props, int initialID, int num, Generator<TimeStamp> arrivalProcess, Generator<Market> marketProcess, RandPlus rand) {
		this.rand = rand;
		this.props = props;
		this.model = model;
		this.nextID = initialID;
		this.finalID = initialID + num;
		this.arrivalProcess = arrivalProcess;
		this.marketAssignment = marketProcess;
	}
	
	public SMAgentFactory(MarketModel model, AgentProperties props, Generator<TimeStamp> arrivalProcess, Generator<Market> marketProcess, RandPlus rand) {
		this(model, props, 0, 0, arrivalProcess, marketProcess, rand);
	}

	protected SMAgent createAgent(AgentProperties props) {
		switch (props.getAgentType()) {
		// TODO Other agent types
		case AA:
			return null; // FIXME change
		case ZIP:
			return null; // FIXME change
		case ZIR:
			return null; // FIXME change
		case ZI:
			return new ZIAgent(nextID++, arrivalProcess.next(), model, marketAssignment.next(),
					new RandPlus(rand.nextLong()), props);
		case BASICMM:
			return null; // FIXME implement / figure out how to handle this properly
		default:
			return null;
		}
	}
	
	public final int nextID() {
		return nextID;
	}

	@Override
	public boolean hasNext() {
		return nextID != finalID; // Thus 0 creates an infinite number
	}

	@Override
	public SMAgent next() {
		return createAgent(props);
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException("Remove not supported");
	}
	
	/**
	 * Returns an Iterable object fur use with the for : each syntax
	 */
	public Iterable<SMAgent> iterable() {
		final Iterator<SMAgent> it = this;
		return new Iterable<SMAgent>() {
			@Override
			public Iterator<SMAgent> iterator() {
				return it;
			}
		};
	}

}
