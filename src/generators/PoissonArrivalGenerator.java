package generators;

import utils.RandPlus;
import event.TimeStamp;

public class PoissonArrivalGenerator extends Generator<TimeStamp> {
	
	PoissonProcess proc;

	public PoissonArrivalGenerator(TimeStamp initialTime, double rate, RandPlus rand) {
		proc = new PoissonProcess(initialTime.longValue(), rate, rand);
	}

	@Override
	public TimeStamp next() {
		// TODO Is ceil appropraite here?
		return new TimeStamp((long) Math.ceil(proc.next()));
	}

}
