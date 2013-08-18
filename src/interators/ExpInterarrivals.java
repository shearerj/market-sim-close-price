package interators;

import java.io.Serializable;

import com.google.common.collect.AbstractIterator;

import utils.Rands;
import event.TimeStamp;

public class ExpInterarrivals extends AbstractIterator<TimeStamp> implements Serializable {
	
	private static final long serialVersionUID = 3285386017387161748L;
	
	protected final Rands rand;
	protected final double rate;

	public ExpInterarrivals(double rate, Rands rand) {
		this.rand = rand;
		this.rate = rate;
	}

	@Override
	protected TimeStamp computeNext() {
		return new TimeStamp((long) Math.ceil(rand.nextExponential(rate)));
	}

}
