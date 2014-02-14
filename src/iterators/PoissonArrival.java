package iterators;

import java.io.Serializable;
import java.util.Iterator;
import java.util.Random;

import com.google.common.collect.AbstractSequentialIterator;

import event.TimeStamp;

public class PoissonArrival extends AbstractSequentialIterator<TimeStamp> implements Serializable {
	
	private static final long serialVersionUID = -999049122857549847L;
	
	protected final Iterator<TimeStamp> gen;

	public PoissonArrival(TimeStamp initialTime, double rate, Random rand) {
		super(initialTime);
		gen = ExpInterarrivals.create(rate, rand);
	}

	@Override
	protected TimeStamp computeNext(TimeStamp previous) {
		return previous.plus(gen.next());
	}

}
