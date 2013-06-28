package entity;

import java.util.Iterator;

import data.ObjectProperties;

import model.MarketModel;


import event.TimeStamp;
import generators.Generator;

import systemmanager.Consts.AgentType;
import utils.RandPlus;

public class BackgroundAgentFactory implements Iterator<BackgroundAgent> {

	protected final RandPlus rand;
	protected final AgentType type;
	protected final MarketModel model;
	protected final ObjectProperties props;
	protected int nextID;
	protected final int finalID;
	protected final Generator<TimeStamp> arrivalProcess;
	protected final Generator<Market> marketAssignment;

	public BackgroundAgentFactory(AgentType type, MarketModel model, ObjectProperties props, int initialID, int num, Generator<TimeStamp> arrivalProcess, Generator<Market> marketProcess, RandPlus rand) {
		this.rand = rand;
		this.type = type;
		this.props = props;
		this.model = model;
		this.nextID = initialID;
		this.finalID = initialID + num;
		this.arrivalProcess = arrivalProcess;
		this.marketAssignment = marketProcess;
	}
	
	public BackgroundAgentFactory(AgentType type, MarketModel model, ObjectProperties props, Generator<TimeStamp> arrivalProcess, Generator<Market> marketProcess, RandPlus rand) {
		this(type, model, props, 0, 0, arrivalProcess, marketProcess, rand);
	}

	protected BackgroundAgent createAgent(AgentType type, ObjectProperties props) {
		switch (type) {
		case ZI:
			return new ZIAgent(nextID++, arrivalProcess.next(), model, marketAssignment.next(),
					new RandPlus(rand.nextLong()), props);
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
	public BackgroundAgent next() {
		return createAgent(type, props);
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException("Remove not supported");
	}
	
	/**
	 * Returns an Iterable object fur use with the for : each syntax
	 */
	public Iterable<BackgroundAgent> iterable() {
		final Iterator<BackgroundAgent> it = this;
		return new Iterable<BackgroundAgent>() {
			@Override
			public Iterator<BackgroundAgent> iterator() {
				return it;
			}
		};
	}

}
