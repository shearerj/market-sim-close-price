package generators;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import utils.RandPlus;

public class UniformRandomGenerator<E> extends Generator<E> {
	
	protected final List<E> items;
	protected final RandPlus rand;

	public UniformRandomGenerator(Collection<E> items, RandPlus rand) {
		this.items = new ArrayList<E>(items);
		this.rand = rand;
	}
	
	public UniformRandomGenerator(Collection<E> items) {
		this(items, new RandPlus());
	}

	@Override
	public E next() {
		return items.get(rand.nextInt(items.size()));
	}

}
