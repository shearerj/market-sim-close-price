package edu.umich.srg.collect;

import edu.umich.srg.collect.SparseList.Entry;

public interface SparseList<E> extends Iterable<Entry<E>> {

  void add(long index, E element);

  void add(Entry<E> entry);

  int size();

  interface Entry<E> {

    long getIndex();

    E getElement();

  }

  static <E> Entry<E> immutableEntry(long index, E element) {
    return new Entry<E>() {

      @Override
      public long getIndex() {
        return index;
      }

      @Override
      public E getElement() {
        return element;
      }

    };
  }

}
