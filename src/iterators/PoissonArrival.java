package iterators;

import java.io.Serializable;
import java.util.Random;

import utils.Rands;

import com.google.common.collect.AbstractSequentialIterator;

import event.TimeStamp;

public class PoissonArrival extends AbstractSequentialIterator<TimeStamp> implements Serializable {
	
	private static final long serialVersionUID = -999049122857549847L;
	
	protected final ExpInterarrivals gen;

	public PoissonArrival(TimeStamp initialTime, double rate, Random rand) {
		super(initialTime.plus(new TimeStamp((long) Math.ceil(Rands.nextExponential(rand, rate)))));
		gen = new ExpInterarrivals(rate, rand);
	}

	@Override
	protected TimeStamp computeNext(TimeStamp previous) {
		return previous.plus(gen.next());
	}

}
