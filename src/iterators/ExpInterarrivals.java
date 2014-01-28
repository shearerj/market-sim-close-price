package iterators;

import static com.google.common.base.Preconditions.checkArgument;

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
		checkArgument(rate > 0);
		this.rand = rand;
		this.rate = rate;
	}

	@Override
	protected TimeStamp computeNext() {
		return new TimeStamp(
				(long) Math.ceil(Rands.nextExponential(rand, rate)));
	}

}
