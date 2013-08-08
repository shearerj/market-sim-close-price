package generator;

import java.util.Iterator;

public abstract class Generator<E> implements Iterator<E> {

	@Override
	public final boolean hasNext() {
		return true;
	}

	@Override
	public final void remove() {
		throw new UnsupportedOperationException("Can't remove elements from a generator");
	}

}
