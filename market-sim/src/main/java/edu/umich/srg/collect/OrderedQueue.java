package edu.umich.srg.collect;

import java.util.Collection;
import java.util.Queue;

/**
 * FIXME Info, basically adds an "addAllOrdered" that guarantees that the
 * elements come out in the specified order
 * 
 * @author erik
 *
 * @param <E>
 */
public interface OrderedQueue<E> extends Queue<E> {

	boolean addAllOrdered(Collection<? extends E> elements);
	
}
