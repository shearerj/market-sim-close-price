package generators;

import utils.RandPlus;
import event.TimeStamp;

public class PoissonArrivalGenerator extends Generator<TimeStamp> {
	
	protected TimeStamp lastTime;
	protected ExponentialInterarrivalGenerator gen;

	public PoissonArrivalGenerator(TimeStamp initialTime, double rate, RandPlus rand) {
		gen = new ExponentialInterarrivalGenerator(rate, rand);
		lastTime = initialTime;
	}

	@Override
	public TimeStamp next() {
		lastTime = lastTime.plus(gen.next());
		return lastTime;
	}

}
