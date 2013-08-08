package generator;

import utils.RandPlus;
import event.TimeStamp;

public class ExponentialInterarrivalGenerator extends Generator<TimeStamp> {
	
	protected final RandPlus rand;
	protected final double rate;

	public ExponentialInterarrivalGenerator(double rate, RandPlus rand) {
		this.rand = rand;
		this.rate = rate;
	}

	@Override
	public TimeStamp next() {
		// XXX Is ceil appropriate here? Benefit is it will never create TimeStamp(0)
		return new TimeStamp((long) Math.ceil(rand.nextExponential(rate)));
	}

}
