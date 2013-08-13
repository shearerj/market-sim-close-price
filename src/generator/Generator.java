package generator;

import java.io.Serializable;
import java.util.Iterator;

public abstract class Generator<E> implements Iterator<E>, Serializable {

	private static final long serialVersionUID = 4983959735784637652L;

	@Override
	public final boolean hasNext() {
		return true;
	}

	@Override
	public final void remove() {
		throw new UnsupportedOperationException("Can't remove elements from a generator");
	}

}
