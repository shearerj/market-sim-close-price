package generator;

public class IDGenerator extends Generator<Integer> {
	
	protected int next;
	protected int step;

	public IDGenerator(int start, int step) {
		this.next = start;
		this.step = step;
	}
	
	public IDGenerator(int step) {
		this(step, step);
	}
	
	public IDGenerator() {
		this(1);
	}
	
	@Override
	public Integer next() {
		int ret = next;
		next += step;
		return ret;
	}

}
