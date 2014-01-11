package iterators;

import java.io.Serializable;
import java.util.Random;

import com.google.common.collect.AbstractIterator;

import utils.Rands;
import event.TimeStamp;

public class ExpInterarrivals extends AbstractIterator<TimeStamp> implements Serializable {
	
	private static final long serialVersionUID = 3285386017387161748L;
	
	protected final Random rand;
	protected final double rate;

	public ExpInterarrivals(double rate, Random rand) {
		this.rand = rand;
		this.rate = rate;
	}

	@Override
	protected TimeStamp computeNext() {
		if (rate > 0)
			return new TimeStamp((long) Math.ceil(Rands.nextExponential(rand, rate)));
		else 
			return TimeStamp.INFINITE;
	}

}
