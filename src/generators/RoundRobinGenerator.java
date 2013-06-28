package generators;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class RoundRobinGenerator<E> extends Generator<E> {

	List<E> items;
	int position;
	
	public RoundRobinGenerator(Collection<E> items) {
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
