package data;

import java.util.Iterator;

import utils.PoissonProcess;
import utils.RandPlus;
import event.TimeStamp;

public class PoissonArrival implements Iterator<TimeStamp> {
	
	PoissonProcess proc;

	public PoissonArrival(TimeStamp initialTime, double rate, RandPlus rand) {
		proc = new PoissonProcess(initialTime.longValue(), rate, rand);
	}

	@Override
	public boolean hasNext() {
		return true;
	}

	@Override
	public TimeStamp next() {
		// TODO Is ceil appropraite here?
		return new TimeStamp((long) Math.ceil(proc.next()));
	}

	@Override
	public void remove() {
		proc.remove();
	}

}
