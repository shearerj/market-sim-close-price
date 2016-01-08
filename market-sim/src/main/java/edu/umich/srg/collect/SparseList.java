package edu.umich.srg.collect;

import edu.umich.srg.collect.SparseList.Entry;

import java.util.Iterator;
import java.util.Map;
import java.util.stream.Stream;

public interface SparseList<E> extends Iterable<Entry<E>> {

  void add(long index, E element);

  void add(Entry<E> entry);

  Stream<? extends Entry<? extends E>> stream();

  int size();

  interface Entry<E> {

    long getIndex();

    E getElement();

  }

  public static <E> Entry<E> asSparseEntry(Map.Entry<? extends Number, ? extends E> mapEntry) {
    return new Entry<E>() {
      @Override
      public long getIndex() {
        return mapEntry.getKey().longValue();
      }

      @Override
      public E getElement() {
        return mapEntry.getValue();
      }

      @Override
      public String toString() {
        return mapEntry.toString();
      }
    };
  }

  public static <E> Iterator<Entry<E>> sparseView(
      Iterator<? extends Map.Entry<? extends Number, ? extends E>> base) {
    return new Iterator<Entry<E>>() {
      @Override
      public boolean hasNext() {
        return base.hasNext();
      }

      @Override
      public Entry<E> next() {
        return asSparseEntry(base.next());
      }
    };
  }

  public static <E> Iterable<Entry<E>> sparseView(
      Iterable<? extends Map.Entry<? extends Number, ? extends E>> base) {
    return () -> sparseView(base.iterator());
  }

  public static <E> Entry<E> immutableEntry(long index, E element) {
    return new Entry<E>() {

      @Override
      public long getIndex() {
        return index;
      }

      @Override
      public E getElement() {
        return element;
      }

      @Override
      public String toString() {
        return index + "=" + element;
      }

    };
  }

}
