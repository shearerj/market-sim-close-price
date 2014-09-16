package utils;

public interface SparseIterable<E> extends Iterable<E> {

	@Override
	public SparseIterator<E> iterator();
	
}
