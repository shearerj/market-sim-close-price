package generator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class RoundRobinGenerator<E> extends Generator<E> {

	private static final long serialVersionUID = -8846597461654930791L;
	
	protected final List<E> items;
	protected int position;
	
	public RoundRobinGenerator(Collection<? extends E> items) {
		this.position = 0;
		this.items = new ArrayList<E>(items);
	}
	
	@Override
	public E next() {
		E item = items.get(position);
		position = (position + 1) % items.size();
		return item;
	}

}
